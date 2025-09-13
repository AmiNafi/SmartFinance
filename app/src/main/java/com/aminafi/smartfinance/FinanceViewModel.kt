package com.aminafi.smartfinance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import com.aminafi.smartfinance.data.repository.TransactionRepository
import com.aminafi.smartfinance.domain.usecase.ManageTransactionUseCase
import com.aminafi.smartfinance.domain.usecase.ProcessAIMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
/**
 * FinanceViewModel with Koin dependency injection
 * All dependencies are automatically injected while maintaining exact same functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val processAIMessageUseCase: ProcessAIMessageUseCase,
    private val manageTransactionUseCase: ManageTransactionUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Current selected month and year
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val selectedMonth = _selectedMonth.asStateFlow()
    val selectedYear = _selectedYear.asStateFlow()

    // Get all transactions for the selected month
    val currentMonthTransactions: Flow<List<Transaction>> = combine(
        _selectedMonth,
        _selectedYear
    ) { month, year ->
        getTransactionsForMonthFlow(month, year)
    }.flatMapLatest { it }

    private fun getTransactionsForMonthFlow(month: Int, year: Int): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        return transactionRepository.getTransactionsForMonth(startOfMonth, endOfMonth)
    }

    // Update selected month and year
    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    // Get current date info
    fun getCurrentDateInfo(): Triple<String, String, String> {
        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val year = calendar.get(Calendar.YEAR).toString()
        val fullDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
        return Triple(monthName, year, fullDate)
    }

    private fun getCurrentMonthTransactionsFlow(): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Set to first day of current month
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis

        // Set to last day of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        return transactionRepository.getTransactionsForMonth(startOfMonth, endOfMonth)
    }

    // Add a new transaction
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            manageTransactionUseCase.addTransaction(transaction)
        }
    }

    // Delete a transaction
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            manageTransactionUseCase.deleteTransaction(transaction)
        }
    }

    // Update a transaction
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            manageTransactionUseCase.updateTransaction(transaction)
        }
    }

    // Get monthly summary
    fun getMonthlySummary(transactions: List<Transaction>): MonthlySummary {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = totalIncome - totalExpenses

        return MonthlySummary(totalIncome, totalExpenses, balance)
    }

    // Process AI message and detect transaction
    suspend fun processAIMessage(message: String): Result<AIDetectedTransaction> {
        return processAIMessageUseCase(message)
    }

    // Add AI-detected transaction
    fun addAIDetectedTransaction(aiTransaction: AIDetectedTransaction) {
        // Create date within selected month
        val calendar = Calendar.getInstance()
        calendar.set(_selectedYear.value, _selectedMonth.value, 15, 12, 0, 0) // Mid-month default
        val transactionDate = calendar.time

        val transaction = Transaction(
            id = System.currentTimeMillis().toString(),
            amount = aiTransaction.amount,
            description = aiTransaction.description,
            type = aiTransaction.type,
            date = transactionDate
        )
        addTransaction(transaction)
    }
}

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val balance: Double
)
