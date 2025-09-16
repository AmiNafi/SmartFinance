package com.aminafi.smartfinance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.FinanceViewModel
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ui.components.*
import com.aminafi.smartfinance.ui.dialogs.*
import com.aminafi.smartfinance.ui.navigation.*
import com.aminafi.smartfinance.ui.screens.*
import com.aminafi.smartfinance.ui.state.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

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
