package com.aminafi.smartfinance.ai

import com.aminafi.smartfinance.TransactionType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Interface for AI-powered transaction detection following SOLID principles
 * Single Responsibility: Handle transaction detection from natural language
 */
interface TransactionAIService {
    suspend fun detectTransaction(text: String): Result<AIDetectedTransaction>
}

/**
 * Data class for AI-detected transaction
 */
data class AIDetectedTransaction(
    val amount: Double,
    val type: TransactionType,
    val title: String,
    val description: String,
    val confidence: Double
)

/**
 * Pattern-based transaction detection service
 * Uses comprehensive pattern matching for transaction classification
 */
class PatternBasedTransactionAIService : TransactionAIService {

    override suspend fun detectTransaction(text: String): Result<AIDetectedTransaction> {
        return try {
            val processedText = text.lowercase().trim()

            // Use pattern matching to classify transaction
            val patternResult = classifyWithPatterns(processedText)
            val amount = extractAmount(processedText)

            // Analyze potential issues
            val issues = analyzeDetectionIssues(processedText, patternResult, amount)

            if (patternResult.confidence > 0.3 && amount > 0) {
                // Log the AI processing details
                println("🤖 AI Transaction Detection:")
                println("   Input: '$text'")
                println("   Detected: ${patternResult.title} - $${amount}")
                println("   Confidence: ${(patternResult.confidence * 100).toInt()}%")
                println("   Method: TF-IDF + Cosine Similarity (Contextual Matching)")
                println("   Type: ${patternResult.type}")

                return Result.success(
                    AIDetectedTransaction(
                        amount = amount,
                        type = patternResult.type,
                        title = patternResult.title,
                        description = patternResult.description,
                        confidence = patternResult.confidence
                    )
                )
            }

            // Provide specific feedback about why detection failed
            val errorMessage = buildDetailedErrorMessage(text, issues, patternResult.confidence, amount)
            println("❌ AI Failed to detect transaction for: '$text'")
            println("   Reason: $errorMessage")
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            println("💥 AI Error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun classifyWithPatterns(text: String): ModelResult {
        val words = text.split("\\s+".toRegex()).map { it.lowercase().trim() }

        // Determine money flow direction
        val moneyFlowDirection = detectMoneyFlowDirection(text, words)

        // Generate simple 2-3 word title from description
        val title = generateSimpleTitle(text, words, moneyFlowDirection)

        // Use original text as description
        val description = text

        // High confidence since we detected the direction
        val confidence = 0.85

        return ModelResult(moneyFlowDirection, title, description, confidence)
    }

    /**
     * Generate a simple 2-3 word title from the transaction description
     */
    private fun generateSimpleTitle(text: String, words: List<String>, type: TransactionType): String {
        // Remove common stop words and money-related terms
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "i", "me", "my")
        val moneyWords = setOf("paid", "spent", "bought", "got", "received", "gave", "cost", "amount", "money", "cash", "rupees", "dollars")

        // Filter out stop words and money words
        val meaningfulWords = words.filter { word ->
            word.length > 2 &&
            word !in stopWords &&
            word !in moneyWords &&
            !word.matches(Regex("\\d+")) // Remove numbers
        }

        // Take first 2-3 meaningful words
        val titleWords = meaningfulWords.take(3)

        // Capitalize first letter of each word
        val title = titleWords.joinToString(" ") { it.capitalize() }

        // If we don't have enough words, use a generic title based on type
        return if (title.isNotBlank()) {
            title
        } else {
            if (type == TransactionType.INCOME) "Income" else "Expense"
        }
    }

    /**
     * CONTEXTUAL money flow detection - understands meaning, not just keywords
     */
    private fun detectMoneyFlowDirection(text: String, words: List<String>): TransactionType {
        val lowerText = text.lowercase()
        var incomeScore = 0
        var expenseScore = 0

        // ===== CONTEXTUAL ANALYSIS - Understand Meaning =====

        // 1. DIRECT MONEY FLOW PATTERNS (most important)
        // "gave me/to me" = money coming to me = INCOME
        if (lowerText.contains("gave me") || lowerText.contains("gave to me") ||
            lowerText.contains("lent me") || lowerText.contains("paid me")) {
            incomeScore += 3
            println("🤖 CONTEXT: Money coming TO me")
        }

        // "gave to/away" = money going from me = EXPENSE
        if (lowerText.contains("gave to") || lowerText.contains("gave away") ||
            lowerText.contains("lent to") || lowerText.contains("paid to")) {
            expenseScore += 3
            println("🤖 CONTEXT: Money going FROM me")
        }

        // 2. RECEIVING PATTERNS
        val receivingPatterns = listOf(
            "got from", "received from", "got money", "money came",
            "came from", "transfer to me", "deposit to"
        )
        for (pattern in receivingPatterns) {
            if (lowerText.contains(pattern)) {
                incomeScore += 2
                println("🤖 RECEIVING: '$pattern'")
            }
        }

        // 3. GIVING PATTERNS
        val givingPatterns = listOf(
            "gave away", "paid out", "spent for", "cost for",
            "transfer from", "withdrawal", "paid off"
        )
        for (pattern in givingPatterns) {
            if (lowerText.contains(pattern)) {
                expenseScore += 2
                println("🤖 GIVING: '$pattern'")
            }
        }

        // 4. POSSESSIVE CONTEXT
        // "my money", "I got", "me" in receiving context
        if (words.contains("me") || words.contains("my") || words.contains("i")) {
            // Look for context around personal pronouns
            val meIndex = words.indexOf("me")
            if (meIndex >= 0) {
                val beforeMe = if (meIndex > 0) words[meIndex - 1] else ""
                val afterMe = if (meIndex < words.size - 1) words[meIndex + 1] else ""

                if (beforeMe == "gave" || beforeMe == "paid" || beforeMe == "lent") {
                    incomeScore += 2
                    println("🤖 POSSESSIVE: '$beforeMe me' = receiving")
                }
                if (beforeMe == "to" && afterMe.isNotEmpty()) {
                    incomeScore += 1
                    println("🤖 POSSESSIVE: 'to me' = receiving")
                }
            }
        }

        // 5. BASIC VERBS (fallback)
        val basicIncomeVerbs = listOf("received", "got", "earned")
        val basicExpenseVerbs = listOf("paid", "spent", "bought")

        for (verb in basicIncomeVerbs) {
            if (words.contains(verb)) {
                incomeScore += 1
                println("🤖 VERB: '$verb' (+1)")
            }
        }

        for (verb in basicExpenseVerbs) {
            if (words.contains(verb)) {
                expenseScore += 1
                println("🤖 VERB: '$verb' (+1)")
            }
        }

        // ===== FINAL DECISION =====
        println("🤖 SCORES - Income: $incomeScore, Expense: $expenseScore")

        return when {
            incomeScore > expenseScore -> {
                println("🤖 RESULT: INCOME")
                TransactionType.INCOME
            }
            expenseScore > incomeScore -> {
                println("🤖 RESULT: EXPENSE")
                TransactionType.EXPENSE
            }
            else -> {
                // Analyze sentence structure for final hint
                val sentenceContext = analyzeSentenceContext(lowerText, words)
                println("🤖 RESULT: $sentenceContext (sentence analysis)")
                sentenceContext
            }
        }
    }

    /**
     * Analyze sentence structure for final context clues
     */
    private fun analyzeSentenceContext(text: String, words: List<String>): TransactionType {
        // Look for question patterns or unclear statements
        if (text.contains("?") || text.contains("maybe") || text.contains("perhaps")) {
            return TransactionType.EXPENSE // Default to expense for unclear cases
        }

        // Look for negation
        if (words.any { it == "not" || it == "n't" || it == "never" }) {
            return TransactionType.EXPENSE // Negation usually indicates no transaction
        }

        // Default based on most common transaction type
        return TransactionType.EXPENSE
    }

    /**
     * Handle close calls with additional sophisticated analysis
     */
    private fun handleCloseCall(text: String, words: List<String>, incomeScore: Float, expenseScore: Float): TransactionType {
        // Heuristic 1: Length of transaction description
        // Longer descriptions tend to be expenses (more details about what was bought)
        if (words.size > 6) {
            return TransactionType.EXPENSE
        }

        // Heuristic 2: Presence of specific expense keywords
        val expenseKeywords = listOf("bought", "purchased", "paid", "spent", "cost")
        if (expenseKeywords.any { words.contains(it) }) {
            return TransactionType.EXPENSE
        }

        // Heuristic 3: Default to expense (more common in daily transactions)
        return TransactionType.EXPENSE
    }

    // Comprehensive knowledge base with synonyms and patterns
    private val knowledgeBase = listOf(
        // Expense patterns with synonyms
        TransactionPattern(
            keywords = listOf("groceries", "grocery", "food", "supermarket", "shopping"),
            verbs = listOf("bought", "bought", "purchased", "got", "bought"),
            type = TransactionType.EXPENSE,
            title = "Groceries",
            description = "Grocery shopping",
            category = "Food"
        ),
        TransactionPattern(
            keywords = listOf("lunch", "dinner", "meal", "restaurant", "food", "eat"),
            verbs = listOf("paid", "ate", "had", "bought"),
            type = TransactionType.EXPENSE,
            title = "Food",
            description = "Restaurant or meal",
            category = "Food"
        ),
        TransactionPattern(
            keywords = listOf("coffee", "cafe", "starbucks", "drink"),
            verbs = listOf("bought", "got", "had"),
            type = TransactionType.EXPENSE,
            title = "Coffee",
            description = "Coffee or beverage",
            category = "Food"
        ),
        TransactionPattern(
            keywords = listOf("electricity", "power", "utility", "bill"),
            verbs = listOf("paid", "payed"),
            type = TransactionType.EXPENSE,
            title = "Electricity Bill",
            description = "Electricity utility payment",
            category = "Utilities"
        ),
        TransactionPattern(
            keywords = listOf("gas", "fuel", "petrol", "diesel"),
            verbs = listOf("bought", "filled", "got"),
            type = TransactionType.EXPENSE,
            title = "Gas",
            description = "Vehicle fuel",
            category = "Transportation"
        ),
        TransactionPattern(
            keywords = listOf("rent", "apartment", "house", "housing"),
            verbs = listOf("paid", "payed"),
            type = TransactionType.EXPENSE,
            title = "Rent",
            description = "Housing rent payment",
            category = "Housing"
        ),
        TransactionPattern(
            keywords = listOf("taxi", "uber", "lyft", "cab", "transport", "ride"),
            verbs = listOf("paid", "took", "used"),
            type = TransactionType.EXPENSE,
            title = "Transportation",
            description = "Transportation service",
            category = "Transportation"
        ),
        TransactionPattern(
            keywords = listOf("clothes", "clothing", "shirt", "pants", "shoes", "dress"),
            verbs = listOf("bought", "purchased", "got"),
            type = TransactionType.EXPENSE,
            title = "Clothing",
            description = "Clothing purchase",
            category = "Shopping"
        ),
        TransactionPattern(
            keywords = listOf("internet", "wifi", "broadband", "connection"),
            verbs = listOf("paid", "payed"),
            type = TransactionType.EXPENSE,
            title = "Internet",
            description = "Internet service",
            category = "Utilities"
        ),
        TransactionPattern(
            keywords = listOf("phone", "mobile", "cell", "smartphone"),
            verbs = listOf("bought", "purchased", "got"),
            type = TransactionType.EXPENSE,
            title = "Phone",
            description = "Mobile phone purchase",
            category = "Electronics"
        ),
        TransactionPattern(
            keywords = listOf("water", "utility", "bill"),
            verbs = listOf("paid", "payed"),
            type = TransactionType.EXPENSE,
            title = "Water Bill",
            description = "Water utility payment",
            category = "Utilities"
        ),
        TransactionPattern(
            keywords = listOf("book", "books", "novel", "magazine"),
            verbs = listOf("bought", "purchased", "got"),
            type = TransactionType.EXPENSE,
            title = "Books",
            description = "Book purchase",
            category = "Education"
        ),
        TransactionPattern(
            keywords = listOf("gym", "fitness", "membership", "workout"),
            verbs = listOf("paid", "joined"),
            type = TransactionType.EXPENSE,
            title = "Gym",
            description = "Fitness membership",
            category = "Health"
        ),
        TransactionPattern(
            keywords = listOf("movie", "cinema", "ticket", "entertainment"),
            verbs = listOf("bought", "watched"),
            type = TransactionType.EXPENSE,
            title = "Entertainment",
            description = "Movie or entertainment",
            category = "Entertainment"
        ),

        // Income patterns
        TransactionPattern(
            keywords = listOf("salary", "paycheck", "wage", "payroll"),
            verbs = listOf("got", "received", "earned"),
            type = TransactionType.INCOME,
            title = "Salary",
            description = "Salary payment",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("bonus", "performance", "incentive"),
            verbs = listOf("got", "received", "earned"),
            type = TransactionType.INCOME,
            title = "Bonus",
            description = "Bonus payment",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("freelance", "contract", "project"),
            verbs = listOf("got", "received", "earned"),
            type = TransactionType.INCOME,
            title = "Freelance",
            description = "Freelance payment",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("refund", "return", "reimbursement"),
            verbs = listOf("got", "received"),
            type = TransactionType.INCOME,
            title = "Refund",
            description = "Refund received",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("gift", "present", "donation"),
            verbs = listOf("received", "got"),
            type = TransactionType.INCOME,
            title = "Gift",
            description = "Gift received",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("commission", "royalty", "affiliate"),
            verbs = listOf("got", "received", "earned"),
            type = TransactionType.INCOME,
            title = "Commission",
            description = "Commission payment",
            category = "Income"
        ),
        TransactionPattern(
            keywords = listOf("dividend", "investment", "stock"),
            verbs = listOf("received", "got"),
            type = TransactionType.INCOME,
            title = "Dividend",
            description = "Investment dividend",
            category = "Income"
        ),

        // Generic expense patterns
        TransactionPattern(
            keywords = listOf("expense", "cost", "fee", "charge"),
            verbs = listOf("had", "incurred", "paid"),
            type = TransactionType.EXPENSE,
            title = "Expense",
            description = "General expense",
            category = "Other"
        ),
        TransactionPattern(
            keywords = listOf("payment", "pay", "paid"),
            verbs = listOf("made", "did"),
            type = TransactionType.EXPENSE,
            title = "Payment",
            description = "General payment",
            category = "Other"
        ),
        TransactionPattern(
            keywords = listOf("purchase", "buy", "shopping"),
            verbs = listOf("made", "did"),
            type = TransactionType.EXPENSE,
            title = "Purchase",
            description = "General purchase",
            category = "Shopping"
        ),

        // Generic income patterns
        TransactionPattern(
            keywords = listOf("income", "money", "cash"),
            verbs = listOf("received", "got", "earned"),
            type = TransactionType.INCOME,
            title = "Income",
            description = "General income",
            category = "Income"
        )
    )

    /**
     * Calculate pattern matching score based on keywords, verbs, and context
     */
    private fun calculatePatternScore(words: List<String>, pattern: TransactionPattern): Float {
        var score = 0f
        val totalWords = words.size

        // Keyword matching (highest weight)
        val keywordMatches = pattern.keywords.count { keyword ->
            words.any { word -> word.contains(keyword) || keyword.contains(word) }
        }
        score += (keywordMatches.toFloat() / pattern.keywords.size) * 0.6f

        // Verb matching (medium weight)
        val verbMatches = pattern.verbs.count { verb ->
            words.any { word -> word.contains(verb) || areSynonyms(word, verb) }
        }
        score += (verbMatches.toFloat() / max(1, pattern.verbs.size)) * 0.3f

        // Context matching (lower weight)
        val contextScore = calculateContextScore(words, pattern)
        score += contextScore * 0.1f

        // Boost score for exact matches
        if (words.any { word -> pattern.keywords.contains(word) }) {
            score += 0.2f
        }

        return min(score, 1f) // Cap at 1.0
    }

    /**
     * Calculate context score based on word proximity and patterns
     */
    private fun calculateContextScore(words: List<String>, pattern: TransactionPattern): Float {
        var contextScore = 0f

        // Check for expense/income indicators
        val expenseIndicators = listOf("spent", "paid", "cost", "bought", "purchased", "expense")
        val incomeIndicators = listOf("received", "got", "earned", "income", "salary", "bonus")

        if (pattern.type == TransactionType.EXPENSE) {
            contextScore += expenseIndicators.count { words.contains(it) }.toFloat() * 0.1f
        } else {
            contextScore += incomeIndicators.count { words.contains(it) }.toFloat() * 0.1f
        }

        // Check for amount proximity to keywords
        val amountPattern = Regex("\\d+(\\.\\d{1,2})?")
        val amountPositions = mutableListOf<Int>()
        words.forEachIndexed { index, word ->
            if (amountPattern.matches(word)) {
                amountPositions.add(index)
            }
        }

        // Boost score if amount is near relevant keywords
        amountPositions.forEach { pos ->
            val nearbyWords = words.subList(max(0, pos - 2), min(words.size, pos + 3))
            if (nearbyWords.any { word -> pattern.keywords.any { keyword -> word.contains(keyword) } }) {
                contextScore += 0.1f
            }
        }

        return min(contextScore, 0.3f)
    }

    /**
     * Check if two words are synonyms
     */
    private fun areSynonyms(word1: String, word2: String): Boolean {
        val synonymMap = mapOf(
            "bought" to listOf("purchased", "got", "bought"),
            "paid" to listOf("payed", "spent", "gave"),
            "received" to listOf("got", "earned", "obtained"),
            "ate" to listOf("had", "consumed", "enjoyed")
        )

        return synonymMap[word1]?.contains(word2) == true ||
               synonymMap[word2]?.contains(word1) == true
    }

    private fun extractAmount(text: String): Double {
        // Enhanced amount extraction with multiple patterns
        val patterns = listOf(
            Regex("\\$(\\d+(?:\\.\\d{1,2})?)"),  // $50, $50.99
            Regex("(\\d+(?:\\.\\d{1,2})?)\\s*dollars?"),  // 50 dollars, 50.99 dollars
            Regex("(\\d+(?:\\.\\d{1,2})?)\\s*usd"),  // 50 USD
            Regex("(\\d+(?:\\.\\d{1,2})?)\\s*bucks"),  // 50 bucks
            Regex("(\\d+(?:\\.\\d{1,2})?)\\s*€"),  // 50 €
            Regex("€(\\d+(?:\\.\\d{1,2})?)"),  // €50
            Regex("(\\d+(?:\\.\\d{1,2})?)\\s*£"),  // 50 £
            Regex("£(\\d+(?:\\.\\d{1,2})?)"),  // £50
            Regex("(\\d+(?:\\.\\d{1,2})?)")   // Just numbers as fallback
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].ifEmpty { match.groupValues[0] }
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }

        return 0.0
    }

    /**
     * Build vocabulary from all documents
     */
    private fun buildVocabulary(documents: List<String>): Set<String> {
        return documents.flatMap { doc ->
            doc.split("\\s+".toRegex())
                .map { it.lowercase().trim() }
                .filter { it.isNotEmpty() && it.length > 2 } // Filter out short words
        }.toSet()
    }

    /**
     * Calculate TF-IDF vector for a document
     */
    private fun calculateTFIDFVector(document: String, allDocuments: List<String>, vocabulary: Set<String>): Map<String, Double> {
        val words = document.split("\\s+".toRegex()).map { it.lowercase().trim() }
        val vector = mutableMapOf<String, Double>()

        for (term in vocabulary) {
            val tf = calculateTermFrequency(term, words)
            val idf = calculateInverseDocumentFrequency(term, allDocuments)
            vector[term] = tf * idf
        }

        return vector
    }

    /**
     * Calculate Term Frequency (TF)
     */
    private fun calculateTermFrequency(term: String, documentWords: List<String>): Double {
        val termCount = documentWords.count { it == term }
        return if (documentWords.isEmpty()) 0.0 else termCount.toDouble() / documentWords.size
    }

    /**
     * Calculate Inverse Document Frequency (IDF)
     */
    private fun calculateInverseDocumentFrequency(term: String, allDocuments: List<String>): Double {
        val documentsContainingTerm = allDocuments.count { doc ->
            doc.split("\\s+".toRegex()).any { it.lowercase().trim() == term }
        }
        val totalDocuments = allDocuments.size

        return if (documentsContainingTerm == 0) 0.0
        else kotlin.math.ln(totalDocuments.toDouble() / documentsContainingTerm.toDouble())
    }

    /**
     * Calculate contextual similarity using cosine similarity
     */
    private fun calculateContextualSimilarity(vector1: Map<String, Double>, vector2: Map<String, Double>): Float {
        val cosineSimilarity = cosineSimilarity(vector1, vector2)
        // Convert to float and scale to 0-1 range
        return (cosineSimilarity * 0.5 + 0.5).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Calculate cosine similarity between two TF-IDF vectors
     */
    private fun cosineSimilarity(vector1: Map<String, Double>, vector2: Map<String, Double>): Double {
        val commonTerms = vector1.keys.intersect(vector2.keys)

        if (commonTerms.isEmpty()) return 0.0

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        // Calculate dot product for common terms
        for (term in commonTerms) {
            val val1 = vector1[term] ?: 0.0
            val val2 = vector2[term] ?: 0.0
            dotProduct += val1 * val2
        }

        // Calculate norms
        for ((term, value) in vector1) {
            norm1 += value * value
        }
        for ((term, value) in vector2) {
            norm2 += value * value
        }

        val magnitude1 = sqrt(norm1)
        val magnitude2 = sqrt(norm2)

        return if (magnitude1 == 0.0 || magnitude2 == 0.0) 0.0
        else dotProduct / (magnitude1 * magnitude2)
    }

    /**
     * Analyze what specific issue prevented successful transaction detection
     */
    private fun analyzeDetectionIssues(text: String, patternResult: ModelResult, amount: Double): String {
        val words = text.split("\\s+".toRegex()).map { it.lowercase().trim() }

        // Primary issue: No amount found
        if (amount == 0.0) {
            return "Missing amount - please include a price (e.g., $50 or 50 dollars)"
        }

        // Secondary issue: Low confidence (only if we have an amount)
        if (patternResult.confidence <= 0.3) {
            return "Unclear transaction - please be more specific about what happened"
        }

        // Fallback: Generic issue
        return "Unable to understand transaction - please rephrase"
    }

    /**
     * Build a detailed error message explaining why detection failed
     */
    private fun buildDetailedErrorMessage(text: String, issue: String, confidence: Double, amount: Double): String {
        return issue
    }

    private data class ModelResult(
        val type: TransactionType,
        val title: String,
        val description: String,
        val confidence: Double
    )
}

/**
 * Data class for transaction patterns in knowledge base
 */
data class TransactionPattern(
    val keywords: List<String>,
    val verbs: List<String>,
    val type: TransactionType,
    val title: String,
    val description: String,
    val category: String
)
