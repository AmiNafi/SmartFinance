package com.aminafi.smartfinance

import java.util.Date

data class Transaction(
    val id: String,
    val amount: Double,
    val description: String,
    val type: TransactionType,
    val date: Date
)

enum class TransactionType {
    INCOME,
    EXPENSE
}
