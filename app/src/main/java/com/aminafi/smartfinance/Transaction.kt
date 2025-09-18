package com.aminafi.smartfinance

import java.util.Date

data class Transaction(
    val id: String,
    val amount: Double,
    val description: String,
    val type: TransactionType,
    val date: Date,        // Month/year for filtering (selected month)
    val entryDate: Date    // Actual date when transaction was entered
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    SAVINGS
}
