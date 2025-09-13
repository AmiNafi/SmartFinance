package com.aminafi.smartfinance.ai.analyzers

/**
 * Interface for amount extraction following Open-Closed Principle
 * Allows for different amount extraction strategies without modifying existing code
 */
interface AmountExtractorInterface {
    fun extractAmount(text: String): Double
}
