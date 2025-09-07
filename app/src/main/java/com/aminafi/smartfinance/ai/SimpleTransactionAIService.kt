package com.aminafi.smartfinance.ai

import com.aminafi.smartfinance.TransactionType
import kotlinx.coroutines.delay



class SimpleTransactionAIService : TransactionAIService {
    override suspend fun detectTransaction(message: String): Result<AIDetectedTransaction> {
        // Simulate AI processing delay
        delay(500)

        // Simple keyword-based detection
        val lowerMessage = message.lowercase()

        // Extract amount (simple regex for numbers)
        val amountRegex = Regex("(\\d+(?:\\.\\d{1,2})?)")
        val amount = amountRegex.find(message)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        // Detect transaction type
        val type = when {
            lowerMessage.contains("income") || lowerMessage.contains("salary") ||
            lowerMessage.contains("received") || lowerMessage.contains("earned") -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // Analyze potential issues for better error messages
        val issues = analyzeDetectionIssues(message, amount)

        if (amount > 0) {
            // Create transaction
            val transaction = AIDetectedTransaction(
                amount = amount,
                type = type,
                title = message.take(20) + if (message.length > 20) "..." else "",
                description = message,
                confidence = 0.8
            )

            return Result.success(transaction)
        } else {
            // Provide specific feedback about why detection failed
            val errorMessage = buildDetailedErrorMessage(message, issues)
            println("❌ AI Failed to detect transaction for: '$message'")
            println("   Reason: $errorMessage")
            return Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Analyze what specific issue prevented successful transaction detection
     */
    private fun analyzeDetectionIssues(text: String, amount: Double): String {
        // Primary issue: No amount found
        if (amount == 0.0) {
            return "Missing amount - please include a price (e.g., $50 or 50 dollars)"
        }

        // Fallback: Generic issue
        return "Unable to understand transaction - please rephrase"
    }

    /**
     * Build a detailed error message explaining why detection failed
     */
    private fun buildDetailedErrorMessage(text: String, issue: String): String {
        return issue
    }
}
