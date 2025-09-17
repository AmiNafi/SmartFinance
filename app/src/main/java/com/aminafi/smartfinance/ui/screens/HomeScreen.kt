package com.aminafi.smartfinance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.*
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import com.aminafi.smartfinance.ui.components.*
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    transactions: List<Transaction>,
    allTransactions: List<Transaction>,
    selectedMonth: Int,
    selectedYear: Int,
    selectedMonthName: String,
    fullDate: String,
    snackbarHostState: SnackbarHostState,
    onNavigateToExpenseList: () -> Unit,
    onNavigateToIncomeList: () -> Unit,
    onNavigateToSavingsList: () -> Unit,
    onShowAddTransactionDialog: (Transaction) -> Unit,
    onShowAIConfirmationDialog: (AIDetectedTransaction) -> Unit
) {
    val summary = viewModel.getMonthlySummary(transactions, allTransactions)
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Current Date Display (right after header)
        Text(
            text = fullDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Cards Column (Vertical Stack) - Income, Expenses, Savings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IncomeCard(
                amount = summary.totalIncome,
                onClick = onNavigateToIncomeList,
                monthName = selectedMonthName
            )
            ExpenseCard(
                amount = summary.totalExpenses,
                onClick = onNavigateToExpenseList,
                monthName = selectedMonthName
            )
            SavingsCard(
                amount = summary.totalSavings,
                onClick = onNavigateToSavingsList,
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
            BalanceBar(summary.monthlyBalance, summary.totalBalance)

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
                                    onShowAIConfirmationDialog(detectedTransaction)
                                    message = "" // Clear only on success
                                } else {
                                    // Get the specific error message from AI service
                                    val errorMessage = result.exceptionOrNull()?.message
                                        ?: "Sorry, I couldn't understand that transaction."

                                    // Keep the message so user can edit it
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = errorMessage,
                                            duration = SnackbarDuration.Long
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
                    val newTransaction = Transaction(
                        id = "",
                        amount = 0.0,
                        description = "",
                        type = TransactionType.EXPENSE,
                        date = Date()
                    )
                    onShowAddTransactionDialog(newTransaction)
                }
            )
        }
    }
}
