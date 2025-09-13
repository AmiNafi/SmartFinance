package com.aminafi.smartfinance.ai

import com.aminafi.smartfinance.TransactionType

/**
 * Interface for pattern-based transaction detection following SOLID principles
 * Single Responsibility: Handle transaction detection from natural language
 */
interface TransactionAIService {
    suspend fun detectTransaction(text: String): Result<AIDetectedTransaction>
}

/**
 * Data class for pattern-detected transaction
 */
data class AIDetectedTransaction(
    val amount: Double,
    val type: TransactionType,
    val title: String,
    val description: String,
    val confidence: Double
)
