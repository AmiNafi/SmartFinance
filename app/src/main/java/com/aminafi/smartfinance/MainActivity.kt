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
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.ui.navigation.*
import com.aminafi.smartfinance.ui.state.*
import com.aminafi.smartfinance.ui.screens.*
import com.aminafi.smartfinance.ui.dialogs.*
import com.aminafi.smartfinance.ui.components.*
import com.aminafi.smartfinance.ui.theme.SmartFinanceTheme
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * Main Activity following Single Responsibility Principle
 * Handles only Activity lifecycle and high-level composition
 * Uses Koin for dependency injection while maintaining all existing functionality
 */
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
fun FinanceApp(viewModel: FinanceViewModel = koinViewModel()) {
    // Initialize state managers following SOLID principles
    val navigationManager = remember { NavigationManager() }
    val uiStateManager = remember { UiStateManager() }
    val navigationActions = remember { DefaultNavigationActions(navigationManager) }
    val uiActions = remember { DefaultUiActions(uiStateManager) }

    val transactions by viewModel.currentMonthTransactions.collectAsState(initial = emptyList())
    val selectedMonth by viewModel.selectedMonth.collectAsState(initial = 0)
    val selectedYear by viewModel.selectedYear.collectAsState(initial = 2025)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val summary = viewModel.getMonthlySummary(transactions)
    val (currentMonthName, currentYear, fullDate) = viewModel.getCurrentDateInfo()
    val selectedMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
    )

    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 180.dp)
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
            // Month/Year Selector Header
            MonthYearSelector(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                selectedMonthName = selectedMonthName,
                onPreviousMonth = {
                    val newMonth = if (selectedMonth == 0) 11 else selectedMonth - 1
                    val newYear = if (selectedMonth == 0) selectedYear - 1 else selectedYear
                    viewModel.setSelectedMonth(newMonth)
                    viewModel.setSelectedYear(newYear)
                },
                onNextMonth = {
                    val newMonth = if (selectedMonth == 11) 0 else selectedMonth + 1
                    val newYear = if (selectedMonth == 11) selectedYear + 1 else selectedYear
                    viewModel.setSelectedMonth(newMonth)
                    viewModel.setSelectedYear(newYear)
                }
            )

            // Screen Content
            when (navigationManager.currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    transactions = transactions,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    selectedMonthName = selectedMonthName,
                    fullDate = fullDate,
                    snackbarHostState = snackbarHostState,
                    onNavigateToExpenseList = navigationActions::navigateToExpenseList,
                    onNavigateToIncomeList = navigationActions::navigateToIncomeList,
                    onShowAddTransactionDialog = uiActions::showAddTransactionDialog,
                    onShowAIConfirmationDialog = uiActions::showAIConfirmationDialog
                )

                Screen.ExpenseList -> TransactionListScreen(
                    transactions = transactions.filter { it.type == TransactionType.EXPENSE },
                    title = "Monthly Expenses",
                    onBack = navigationActions::navigateToHome,
                    onEditTransaction = uiActions::showEditTransactionDialog
                )

                Screen.IncomeList -> TransactionListScreen(
                    transactions = transactions.filter { it.type == TransactionType.INCOME },
                    title = "Monthly Income",
                    onBack = navigationActions::navigateToHome,
                    onEditTransaction = uiActions::showEditTransactionDialog
                )
            }
        }
    }

    // Transaction Dialogs
    if (uiStateManager.showAddTransactionDialog && uiStateManager.editingTransaction != null) {
        AddTransactionDialog(
            onDismiss = uiActions::hideAddTransactionDialog,
            onAddTransaction = { transaction ->
                viewModel.addTransaction(transaction)
                uiActions.hideAddTransactionDialog()
            },
            selectedMonth = selectedMonth,
            selectedYear = selectedYear
        )
    }

    if (uiStateManager.showEditTransactionDialog && uiStateManager.editingTransaction != null) {
        EditTransactionDialog(
            transaction = uiStateManager.editingTransaction!!,
            onDismiss = uiActions::hideEditTransactionDialog,
            onSave = { transaction ->
                viewModel.updateTransaction(transaction)
                uiActions.hideEditTransactionDialog()
            },
            onDelete = {
                viewModel.deleteTransaction(uiStateManager.editingTransaction!!)
                uiActions.hideEditTransactionDialog()
            }
        )
    }

    if (uiStateManager.showAIConfirmationDialog && uiStateManager.pendingAITransaction != null) {
        AIConfirmationDialog(
            pendingTransaction = uiStateManager.pendingAITransaction,
            onDismiss = uiActions::hideAIConfirmationDialog,
            onConfirm = { transaction ->
                viewModel.addAIDetectedTransaction(transaction)
                uiActions.hideAIConfirmationDialog()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "✅ Transaction added successfully!",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

@Composable
private fun MonthYearSelector(
    selectedMonth: Int,
    selectedYear: Int,
    selectedMonthName: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
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
            IconButton(onClick = onPreviousMonth) {
                Text("◀", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "$selectedMonthName $selectedYear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = onNextMonth) {
                Text("▶", style = MaterialTheme.typography.titleLarge)
            }
        }
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
