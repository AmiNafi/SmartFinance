package com.aminafi.smartfinance.domain.usecase

import com.aminafi.smartfinance.ai.AIDetectedTransaction
import com.aminafi.smartfinance.ai.TransactionAIService

/**
 * Use case implementation for processing AI messages
 * Encapsulates AI processing business logic and follows Open-Closed Principle
 */
class ProcessAIMessageUseCaseImpl(
    private val aiService: TransactionAIService
) : ProcessAIMessageUseCase {

    override suspend operator fun invoke(message: String): Result<AIDetectedTransaction> {
        println("🤖 AI Processing: '$message'")
        println("   Using: ${aiService::class.simpleName} (pattern-based analysis)")

        val result = aiService.detectTransaction(message)
        result.onSuccess { transaction ->
            println("✅ AI Result: ${transaction.type} - $${transaction.amount} (${(transaction.confidence * 100).toInt()}% confidence)")
            println("   Title: ${transaction.title}")
            println("   Description: ${transaction.description}")
        }
        result.onFailure { error ->
            println("❌ AI Error: ${error.message}")
        }
        return result
    }
}
