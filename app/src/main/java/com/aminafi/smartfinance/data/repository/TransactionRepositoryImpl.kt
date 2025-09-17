package com.aminafi.smartfinance.data.repository

import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.TransactionDao
import com.aminafi.smartfinance.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository implementation for transaction data operations
 * Wraps Room DAO with business logic and follows Open-Closed Principle
 */
class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getTransactionsForMonth(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForMonth(start, end)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toTransaction() } }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        val entity = TransactionEntity.fromTransaction(transaction)
        transactionDao.insertTransaction(entity)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val entity = TransactionEntity.fromTransaction(transaction)
        transactionDao.deleteTransaction(entity)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val entity = TransactionEntity.fromTransaction(transaction)
        transactionDao.deleteTransaction(entity) // Delete old
        transactionDao.insertTransaction(entity) // Insert updated
    }
}
