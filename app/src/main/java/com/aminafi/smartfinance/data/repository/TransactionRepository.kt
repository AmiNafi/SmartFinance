package com.aminafi.smartfinance.data.repository

import com.aminafi.smartfinance.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction data operations following Open-Closed Principle
 * Allows for different data sources (Room, API, etc.) without modifying business logic
 */
interface TransactionRepository {
    fun getTransactionsForMonth(start: Long, end: Long): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
}
