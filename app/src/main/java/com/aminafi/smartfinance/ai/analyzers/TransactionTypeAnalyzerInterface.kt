package com.aminafi.smartfinance.ai.analyzers

import com.aminafi.smartfinance.TransactionType

/**
 * Interface for transaction type analysis following Open-Closed Principle
 * Allows for different transaction type detection strategies without modifying existing code
 */
interface TransactionTypeAnalyzerInterface {
    fun analyzeTransactionType(text: String): TransactionType
}
