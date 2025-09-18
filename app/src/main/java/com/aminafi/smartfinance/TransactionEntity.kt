package com.aminafi.smartfinance

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val description: String,
    val type: String, // Store as String for Room compatibility
    val date: Long, // Store as timestamp for Room compatibility (month/year for filtering)
    val entryDate: Long = date // Store as timestamp for Room compatibility (actual entry date), default to date for backward compatibility
) {
    // Convert to domain model
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            description = description,
            type = when (type) {
                "INCOME" -> TransactionType.INCOME
                "EXPENSE" -> TransactionType.EXPENSE
                "SAVINGS" -> TransactionType.SAVINGS
                else -> TransactionType.EXPENSE // Fallback for unknown types
            },
            date = Date(date),
            entryDate = Date(entryDate)
        )
    }

    companion object {
        // Convert from domain model
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                amount = transaction.amount,
                description = transaction.description,
                type = transaction.type.name,
                date = transaction.date.time,
                entryDate = transaction.entryDate.time
            )
        }
    }
}
