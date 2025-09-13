package com.aminafi.smartfinance.ai.analyzers

/**
 * Amount Extractor following Single Responsibility Principle and Open-Closed Principle
 * Handles only amount extraction from text
 * Implements AmountExtractor interface for extensibility
 */
class AmountExtractor : AmountExtractorInterface {

    /**
     * Extract monetary amount from text
     */
    override fun extractAmount(text: String): Double {
        // Multiple regex patterns for different amount formats
        val patterns = listOf(
            Regex("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:dollars?|bucks?|usd|\\$)"),
            Regex("\\$(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)"),
            Regex("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:rupees?|rs|₹|inr)"),
            Regex("₹(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)"),
            Regex("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:euros?|eur|€)"),
            Regex("€(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)"),
            Regex("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:pounds?|gbp|£)"),
            Regex("£(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)"),
            // Fallback: any number
            Regex("(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }

        return 0.0
    }
}
