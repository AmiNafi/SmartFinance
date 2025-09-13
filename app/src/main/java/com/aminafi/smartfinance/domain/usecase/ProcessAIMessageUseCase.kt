package com.aminafi.smartfinance.domain.usecase

import com.aminafi.smartfinance.ai.AIDetectedTransaction

/**
 * Use case interface for processing AI messages following Open-Closed Principle
 * Allows for different AI processing strategies without modifying presentation layer
 */
interface ProcessAIMessageUseCase {
    suspend operator fun invoke(message: String): Result<AIDetectedTransaction>
}
