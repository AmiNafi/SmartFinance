package com.aminafi.smartfinance.domain.usecase

import com.aminafi.smartfinance.Transaction

/**
 * Use case interface for managing transactions following Open-Closed Principle
 * Allows for different transaction management strategies without modifying presentation layer
 */
interface ManageTransactionUseCase {
    suspend fun addTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
}
