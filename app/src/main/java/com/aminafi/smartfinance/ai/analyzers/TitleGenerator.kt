package com.aminafi.smartfinance.ai.analyzers

import com.aminafi.smartfinance.TransactionType

/**
 * Title Generator following Single Responsibility Principle and Open-Closed Principle
 * Handles only transaction title generation from text analysis
 * Implements TitleGenerator interface for extensibility
 */
class TitleGenerator : TitleGeneratorInterface {

    /**
     * Generate intelligent transaction title from text
     */
    override fun generateTitle(text: String, type: TransactionType): String {
        val lowerText = text.lowercase()

        // Category detection based on keywords
        val title = when {
            // Food & Dining
            lowerText.contains("food") || lowerText.contains("lunch") ||
            lowerText.contains("dinner") || lowerText.contains("restaurant") ||
            lowerText.contains("coffee") || lowerText.contains("groceries") -> {
                "Food & Dining"
            }

            // Transportation
            lowerText.contains("gas") || lowerText.contains("fuel") ||
            lowerText.contains("transport") || lowerText.contains("taxi") ||
            lowerText.contains("uber") || lowerText.contains("bus") -> {
                "Transportation"
            }

            // Entertainment
            lowerText.contains("movie") || lowerText.contains("cinema") ||
            lowerText.contains("entertainment") || lowerText.contains("tickets") -> {
                "Entertainment"
            }

            // Shopping
            lowerText.contains("clothes") || lowerText.contains("shirt") ||
            lowerText.contains("tshirt") || lowerText.contains("shoes") ||
            lowerText.contains("shopping") -> {
                "Shopping"
            }

            // Bills & Utilities
            lowerText.contains("bill") || lowerText.contains("electricity") ||
            lowerText.contains("water") || lowerText.contains("internet") ||
            lowerText.contains("phone") || lowerText.contains("rent") -> {
                "Bills & Utilities"
            }

            // Salary
            lowerText.contains("salary") || lowerText.contains("payroll") -> {
                "Salary"
            }

            // Freelance Income
            lowerText.contains("freelance") || lowerText.contains("client") -> {
                "Freelance Income"
            }

            // Default fallback
            else -> getDefaultTitle(type)
        }

        return title
    }

    private fun getDefaultTitle(type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> "Income Transaction"
            TransactionType.EXPENSE -> "Expense Transaction"
            TransactionType.SAVINGS -> "Savings Transaction"
        }
    }
}
