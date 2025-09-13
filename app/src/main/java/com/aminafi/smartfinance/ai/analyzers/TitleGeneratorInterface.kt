package com.aminafi.smartfinance.ai.analyzers

import com.aminafi.smartfinance.TransactionType

/**
 * Interface for title generation following Open-Closed Principle
 * Allows for different title generation strategies without modifying existing code
 */
interface TitleGeneratorInterface {
    fun generateTitle(text: String, type: TransactionType): String
}
