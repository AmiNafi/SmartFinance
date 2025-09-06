package com.aminafi.smartfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import androidx.compose.foundation.text.KeyboardOptions

import com.aminafi.smartfinance.ui.theme.SmartFinanceTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Screen navigation enum
enum class Screen {
    Home,
    ExpenseList,
    IncomeList
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartFinanceTheme {
                FinanceApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel = viewModel()) {
    val transactions by viewModel.currentMonthTransactions.collectAsState(initial = emptyList())
    val selectedMonth by viewModel.selectedMonth.collectAsState(initial = 0)
    val selectedYear by viewModel.selectedYear.collectAsState(initial = 2025)
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var message by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showAIConfirmationDialog by remember { mutableStateOf(false) }
    var pendingAITransaction by remember { mutableStateOf<AIDetectedTransaction?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val summary = viewModel.getMonthlySummary(transactions)
    val (currentMonthName, currentYear, fullDate) = viewModel.getCurrentDateInfo()
    val selectedMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
    )

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 180.dp) // Position well above input area
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Fixed Month/Year Selector Header (visible on all screens)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newMonth = if (selectedMonth == 0) 11 else selectedMonth - 1
                            val newYear = if (selectedMonth == 0) selectedYear - 1 else selectedYear
                            viewModel.setSelectedMonth(newMonth)
                            viewModel.setSelectedYear(newYear)
                        }
                    ) {
                        Text("◀", style = MaterialTheme.typography.titleLarge)
                    }

                    Text(
                        text = "$selectedMonthName $selectedYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showMonthYearPicker = true }
                            .padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = {
                            val newMonth = if (selectedMonth == 11) 0 else selectedMonth + 1
                            val newYear = if (selectedMonth == 11) selectedYear + 1 else selectedYear
                            viewModel.setSelectedMonth(newMonth)
                            viewModel.setSelectedYear(newYear)
                        }
                    ) {
                        Text("▶", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            when (currentScreen) {
                Screen.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Current Date Display (right after header)
                        Text(
                            text = fullDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Cards Column (Vertical Stack) - Income First
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IncomeCard(
                                amount = summary.totalIncome,
                                onClick = { currentScreen = Screen.IncomeList },
                                monthName = selectedMonthName
                            )
                            ExpenseCard(
                                amount = summary.totalExpenses,
                                onClick = { currentScreen = Screen.ExpenseList },
                                monthName = selectedMonthName
                            )
                        }

                        // Spacer to fill remaining space
                        Spacer(modifier = Modifier.weight(1f))

                        // Bottom content area (Balance + Input)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Balance Bar
                            BalanceBar(summary.balance)

                            // Messenger-style input at bottom
                            MessengerInput(
                                message = message,
                                onMessageChange = { message = it },
                                onSend = {
                                    if (message.isNotBlank()) {
                                        val userMessage = message
                                        // Don't clear message here - let AI processing decide
                                        coroutineScope.launch {
                                            try {
                                                val result = viewModel.processAIMessage(userMessage)
                                                if (result.isSuccess) {
                                                    val detectedTransaction = result.getOrNull()!!
                                                    pendingAITransaction = detectedTransaction
                                                    showAIConfirmationDialog = true
                                                    message = "" // Clear only on success
                                                } else {
                                                    // Keep the message so user can edit it
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Sorry, I couldn't understand that transaction. Please try rephrasing.",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Keep the message so user can edit it
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Error processing message: ${e.message}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onAddManual = {
                                    // Show manual transaction dialog
                                    editingTransaction = Transaction(
                                        id = "",
                                        amount = 0.0,
                                        description = "",
                                        type = TransactionType.EXPENSE,
                                        date = Date()
                                    )
                                }
                            )
                        }
                    }
                }

                Screen.ExpenseList -> {
                    TransactionListScreen(
                        transactions = transactions.filter { it.type == TransactionType.EXPENSE },
                        title = "Monthly Expenses",
                        onBack = { currentScreen = Screen.Home },
                        onEditTransaction = { transaction ->
                            editingTransaction = transaction
                        }
                    )
                }

                Screen.IncomeList -> {
                    TransactionListScreen(
                        transactions = transactions.filter { it.type == TransactionType.INCOME },
                        title = "Monthly Income",
                        onBack = { currentScreen = Screen.Home },
                        onEditTransaction = { transaction ->
                            editingTransaction = transaction
                        }
                    )
                }
            }
        }
    }



    // Transaction Dialog (Add or Edit)
    editingTransaction?.let { transaction ->
        if (transaction.id.isEmpty()) {
            // New transaction - show Add dialog
            AddTransactionDialog(
                onDismiss = { editingTransaction = null },
                onAddTransaction = { newTransaction ->
                    viewModel.addTransaction(newTransaction)
                    editingTransaction = null
                },
                selectedMonth = selectedMonth,
                selectedYear = selectedYear
            )
        } else {
            // Existing transaction - show Edit dialog
            EditTransactionDialog(
                transaction = transaction,
                onDismiss = { editingTransaction = null },
                onSave = { updatedTransaction ->
                    viewModel.updateTransaction(updatedTransaction)
                    editingTransaction = null
                },
                onDelete = {
                    viewModel.deleteTransaction(transaction)
                    editingTransaction = null
                }
            )
        }
    }

    // AI Transaction Confirmation Dialog
    if (showAIConfirmationDialog && pendingAITransaction != null) {
        var editableTitle by remember { mutableStateOf(pendingAITransaction!!.title) }
        var editableAmount by remember { mutableStateOf(pendingAITransaction!!.amount.toString()) }
        var editableDescription by remember { mutableStateOf(pendingAITransaction!!.description) }
        var editableType by remember { mutableStateOf(pendingAITransaction!!.type) }

        AlertDialog(
            onDismissRequest = { showAIConfirmationDialog = false },
            title = { Text("Edit Transaction Details") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("AI detected transaction with ${(pendingAITransaction!!.confidence * 100).toInt()}% confidence")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Title field
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Amount field
                    OutlinedTextField(
                        value = editableAmount,
                        onValueChange = { editableAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description field
                    OutlinedTextField(
                        value = editableDescription,
                        onValueChange = { editableDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type selection
                    Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editableType == TransactionType.INCOME,
                            onClick = { editableType = TransactionType.INCOME }
                        )
                        Text("Income", modifier = Modifier.padding(start = 8.dp))

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = editableType == TransactionType.EXPENSE,
                            onClick = { editableType = TransactionType.EXPENSE }
                        )
                        Text("Expense", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = editableAmount.toDoubleOrNull()
                        if (amount != null && amount > 0 && editableTitle.isNotBlank()) {
                            val editedTransaction = AIDetectedTransaction(
                                amount = amount,
                                type = editableType,
                                title = editableTitle,
                                description = editableDescription,
                                confidence = pendingAITransaction!!.confidence
                            )
                            viewModel.addAIDetectedTransaction(editedTransaction)
                            showAIConfirmationDialog = false
                            pendingAITransaction = null
                            // Show success snackbar
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "✅ Transaction added successfully!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    enabled = editableAmount.toDoubleOrNull() != null &&
                             editableAmount.toDoubleOrNull() ?: 0.0 > 0 &&
                             editableTitle.isNotBlank()
                ) {
                    Text("Add Transaction")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAIConfirmationDialog = false
                        pendingAITransaction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MonthlySummary(summary: MonthlySummary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Income: $${"%.2f".format(summary.totalIncome)}")
                Text("Expenses: $${"%.2f".format(summary.totalExpenses)}")
                Text(
                    text = "Balance: $${"%.2f".format(summary.balance)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionList(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(transaction.date),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}$${"%.2f".format(transaction.amount)}",
                color = if (transaction.type == TransactionType.INCOME)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    selectedMonth: Int,
    selectedYear: Int
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    val selectedMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction - $selectedMonthName $selectedYear") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    RadioButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME }
                    )
                    Text("Income", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE }
                    )
                    Text("Expense", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && description.isNotBlank()) {
                        // Create date within selected month
                        val calendar = Calendar.getInstance()
                        calendar.set(selectedYear, selectedMonth, 15, 12, 0, 0) // Mid-month default
                        val transactionDate = calendar.time

                        val transaction = Transaction(
                            id = System.currentTimeMillis().toString(),
                            amount = amountValue,
                            description = description,
                            type = type,
                            date = transactionDate
                        )
                        onAddTransaction(transaction)
                        amount = ""
                        description = ""
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BalanceBar(balance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Balance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "৳${"%.2f".format(balance)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (balance >= 0)
                Color(0xFF4CAF50) // Green for positive
            else
                MaterialTheme.colorScheme.error // Red for negative
        )
    }
}

@Composable
fun ExpenseCard(amount: Double, onClick: () -> Unit, modifier: Modifier = Modifier, monthName: String = "Monthly") {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE57373) // Solid red background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat design
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$monthName Expenses",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "৳${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun IncomeCard(amount: Double, onClick: () -> Unit, modifier: Modifier = Modifier, monthName: String = "Monthly") {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF66BB6A) // Solid green background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat design
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$monthName Income",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "৳${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun TransactionList(
    transactions: List<Transaction>,
    title: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Transaction list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
        }
    }
}

@Composable
fun MessengerInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddManual: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add button (outside the text field)
            IconButton(
                onClick = onAddManual,
                modifier = Modifier.size(40.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Message input field with send button inside
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Add transaction or ask AI...") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("➤", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    title: String,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItemEditable(
                    transaction = transaction,
                    onClick = { onEditTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
fun TransactionItemEditable(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}৳${"%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.INCOME)
                    Color(0xFF4CAF50) // Green for income
                else
                    MaterialTheme.colorScheme.error // Red for expenses
            )
                Text(
                    text = transaction.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember { mutableStateOf(transaction.description) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME }
                    )
                    Text("Income", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE }
                    )
                    Text("Expense", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && description.isNotBlank()) {
                        val updatedTransaction = transaction.copy(
                            description = description,
                            amount = amountValue,
                            type = type
                        )
                        onSave(updatedTransaction)
                    }
                },
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FinanceAppPreview() {
    SmartFinanceTheme {
        FinanceApp()
    }
}
