package com.aminafi.smartfinance.domain.usecase

import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.data.repository.TransactionRepository

/**
 * Use case implementation for managing transactions
 * Encapsulates transaction CRUD operations and follows Open-Closed Principle
 */
class ManageTransactionUseCaseImpl(
    private val transactionRepository: TransactionRepository
) : ManageTransactionUseCase {

    override suspend fun addTransaction(transaction: Transaction) {
        transactionRepository.insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionRepository.updateTransaction(transaction)
    }
}
