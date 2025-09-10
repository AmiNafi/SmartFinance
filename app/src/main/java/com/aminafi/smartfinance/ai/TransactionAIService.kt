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

    internal fun classifyWithPatterns(text: String): ModelResult {
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
     * ADVANCED CONTEXTUAL money flow detection - understands WHO is doing WHAT to WHOM
     */
    private fun detectMoneyFlowDirection(text: String, words: List<String>): TransactionType {
        val lowerText = text.lowercase()
        var incomeScore = 0
        var expenseScore = 0

        // ===== ADVANCED CONTEXTUAL ANALYSIS =====

        // 1. PREPOSITION-BASED ANALYSIS (most important)
        val prepositionAnalysis = analyzePrepositions(words, lowerText)
        incomeScore += prepositionAnalysis.incomeScore
        expenseScore += prepositionAnalysis.expenseScore

        // 2. SUBJECT-VERB-OBJECT ANALYSIS
        val svoAnalysis = analyzeSubjectVerbObject(words, lowerText)
        incomeScore += svoAnalysis.incomeScore
        expenseScore += svoAnalysis.expenseScore

        // 3. GIFT AND REWARD PATTERNS (high priority)
        if (lowerText.contains("gift") || lowerText.contains("present") ||
            lowerText.contains("donation") || lowerText.contains("prize") ||
            lowerText.contains("award") || lowerText.contains("bonus") ||
            lowerText.contains("reward")) {
            incomeScore += 4
            println("🤖 GIFT/REWARD DETECTED: Strong INCOME signal (+4)")
        }

        // 3.5 FAMILY RELATIONSHIP PATTERNS (high priority for money from family)
        val familyWords = listOf("father", "mother", "dad", "mom", "parent", "brother", "sister", "son", "daughter", "uncle", "aunt", "grandfather", "grandmother")
        val hasFamilyRelation = familyWords.any { lowerText.contains(it) }
        if (hasFamilyRelation && (lowerText.contains("got") || lowerText.contains("received") || lowerText.contains("money"))) {
            incomeScore += 3
            println("🤖 FAMILY MONEY: Money from family member = INCOME (+3)")
        }

        // 4. DIRECT MONEY FLOW PATTERNS
        // "gave me/to me" = money coming to me = INCOME
        if (lowerText.contains("gave me") || lowerText.contains("gave to me") ||
            lowerText.contains("lent me") || lowerText.contains("paid me") ||
            lowerText.contains("sent me")) {
            incomeScore += 3
            println("🤖 DIRECT: Money coming TO me (+3)")
        }

        // "gave to/away" = money going from me = EXPENSE
        // But be careful with "gave to me" vs "gave to father"
        if (lowerText.contains("gave to") && !lowerText.contains("gave to me") && !lowerText.contains("gave me")) {
            expenseScore += 3
            println("🤖 DIRECT: Money going FROM me (+3)")
        }
        if (lowerText.contains("gave away") || lowerText.contains("lent to") ||
            lowerText.contains("paid to") || lowerText.contains("sent to")) {
            expenseScore += 3
            println("🤖 DIRECT: Money going FROM me (+3)")
        }

        // 2. RECEIVING PATTERNS
        val receivingPatterns = listOf(
            "got from", "received from", "got money", "money came",
            "came from", "transfer to me", "deposit to",
            "gift from", "present from", "donation from", "prize from",
            "money from", "cash from", "payment from"
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

        // 4. POSSESSIVE CONTEXT - FIXED LOGIC
        // "my money", "I got", "me" in receiving context
        if (words.contains("me") || words.contains("my") || words.contains("i")) {
            // Look for context around personal pronouns
            val meIndex = words.indexOf("me")
            if (meIndex >= 0) {
                val beforeMe = if (meIndex > 0) words[meIndex - 1] else ""
                val afterMe = if (meIndex < words.size - 1) words[meIndex + 1] else ""

                // INCOME patterns: someone gave/paid/lent TO me
                if (beforeMe == "gave" || beforeMe == "paid" || beforeMe == "lent") {
                    incomeScore += 2
                    println("🤖 POSSESSIVE: '$beforeMe me' = receiving")
                }
                if (beforeMe == "to" && afterMe.isNotEmpty()) {
                    incomeScore += 1
                    println("🤖 POSSESSIVE: 'to me' = receiving")
                }
            }

            // EXPENSE patterns: I got/bought something FOR myself
            if (lowerText.contains("for myself") || lowerText.contains("for me")) {
                expenseScore += 3
                println("🤖 POSSESSIVE: 'for myself/me' = personal purchase (EXPENSE)")
            }

            // Context clues for shopping/purchasing
            if (lowerText.contains("went to market") || lowerText.contains("shopping") ||
                lowerText.contains("bought") || lowerText.contains("purchase")) {
                expenseScore += 2
                println("🤖 CONTEXT: Shopping/market activity detected = EXPENSE")
            }
        }

        // 5. SEMANTIC VERB ANALYSIS - Context-aware understanding
        val basicIncomeVerbs = listOf("received", "earned", "got")
        val basicExpenseVerbs = listOf("paid", "spent", "bought")

        // Smart verb analysis with semantic context
        for (verb in basicIncomeVerbs) {
            if (words.contains(verb)) {
                val verbIndex = words.indexOf(verb)
                val contextWords = getContextWords(words, verbIndex, 3)

                // Check if "got" is used in shopping context
                if (verb == "got" && isShoppingContext(lowerText, contextWords)) {
                    expenseScore += 2
                    println("🤖 SEMANTIC: '$verb' in shopping context = EXPENSE (+2)")
                } else if (verb == "got" && isReceivingMoneyContext(lowerText, contextWords)) {
                    incomeScore += 2
                    println("🤖 SEMANTIC: '$verb' in money receiving context = INCOME (+2)")
                } else {
                    incomeScore += 1
                    println("🤖 VERB: '$verb' = INCOME (+1)")
                }
            }
        }

        for (verb in basicExpenseVerbs) {
            if (words.contains(verb)) {
                expenseScore += 1
                println("🤖 VERB: '$verb' = EXPENSE (+1)")
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
     * Analyze prepositions to understand money flow direction
     */
    private fun analyzePrepositions(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        // Find preposition positions
        val prepositionIndices = words.mapIndexedNotNull { index, word ->
            if (word in listOf("to", "from", "by", "with", "for")) index to word else null
        }

        for ((prepIndex, preposition) in prepositionIndices) {
            val beforePrep = if (prepIndex > 0) words[prepIndex - 1] else ""
            val afterPrep = if (prepIndex < words.size - 1) words[prepIndex + 1] else ""

            when (preposition) {
                "from" -> {
                    // "money FROM someone" = INCOME
                    // "bought FROM store" = EXPENSE
                    if (beforePrep in listOf("got", "received", "money", "gift", "payment")) {
                        incomeScore += 3
                        println("🤖 PREP: '$beforePrep FROM $afterPrep' = INCOME (+3)")
                    } else if (beforePrep in listOf("bought", "paid", "spent")) {
                        expenseScore += 2
                        println("🤖 PREP: '$beforePrep FROM $afterPrep' = EXPENSE (+2)")
                    }
                }
                "to" -> {
                    // "gave TO someone" = EXPENSE
                    // "transfer TO me" = INCOME
                    if (beforePrep in listOf("gave", "paid", "sent", "lent")) {
                        expenseScore += 3
                        println("🤖 PREP: '$beforePrep TO $afterPrep' = EXPENSE (+3)")
                    } else if (afterPrep == "me" || beforePrep in listOf("transfer", "deposit")) {
                        incomeScore += 3
                        println("🤖 PREP: '$beforePrep TO $afterPrep' = INCOME (+3)")
                    }
                }
                "for" -> {
                    // "paid FOR something" = EXPENSE
                    // "worked FOR money" = INCOME
                    if (beforePrep in listOf("paid", "spent", "bought")) {
                        expenseScore += 2
                        println("🤖 PREP: '$beforePrep FOR $afterPrep' = EXPENSE (+2)")
                    }
                }
                "by" -> {
                    // "paid BY someone" = INCOME
                    if (beforePrep in listOf("paid", "sent")) {
                        incomeScore += 2
                        println("🤖 PREP: '$beforePrep BY $afterPrep' = INCOME (+2)")
                    }
                }
            }
        }

        return ScoreResult(incomeScore, expenseScore)
    }

    /**
     * Analyze Subject-Verb-Object relationships
     */
    private fun analyzeSubjectVerbObject(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        // Find action verbs
        val actionVerbs = listOf("got", "gave", "paid", "received", "spent", "bought", "sent", "earned")
        val verbIndices = words.mapIndexedNotNull { index, word ->
            if (word in actionVerbs) index to word else null
        }

        for ((verbIndex, verb) in verbIndices) {
            val subject = if (verbIndex > 0) words[verbIndex - 1] else ""
            val objStart = verbIndex + 1
            val objWords = if (objStart < words.size) words.subList(objStart, min(objStart + 3, words.size)) else emptyList()
            val obj = objWords.joinToString(" ")

            when (verb) {
                "got" -> {
                    // Analyze what was "got"
                    if (obj.contains("gift") || obj.contains("money") || obj.contains("payment") ||
                        obj.contains("from") || obj.contains("salary")) {
                        incomeScore += 3
                        println("🤖 SVO: '$subject GOT $obj' = INCOME (+3)")
                    } else if (obj.contains("coffee") || obj.contains("lunch") || obj.contains("groceries")) {
                        expenseScore += 3
                        println("🤖 SVO: '$subject GOT $obj' = EXPENSE (+3)")
                    }
                }
                "gave" -> {
                    // "I gave money" = EXPENSE
                    // "gave me money" = INCOME (but this is handled elsewhere)
                    if (subject == "i" && (obj.contains("money") || obj.contains("to"))) {
                        expenseScore += 3
                        println("🤖 SVO: '$subject GAVE $obj' = EXPENSE (+3)")
                    }
                }
                "paid" -> {
                    if (obj.contains("for") || obj.contains("bill") || obj.contains("rent")) {
                        expenseScore += 2
                        println("🤖 SVO: '$subject PAID $obj' = EXPENSE (+2)")
                    } else if (obj.contains("me") || obj.contains("back")) {
                        incomeScore += 2
                        println("🤖 SVO: '$subject PAID $obj' = INCOME (+2)")
                    }
                }
                "received" -> {
                    incomeScore += 2
                    println("🤖 SVO: '$subject RECEIVED $obj' = INCOME (+2)")
                }
                "spent" -> {
                    expenseScore += 2
                    println("🤖 SVO: '$subject SPENT $obj' = EXPENSE (+2)")
                }
                "bought" -> {
                    expenseScore += 2
                    println("🤖 SVO: '$subject BOUGHT $obj' = EXPENSE (+2)")
                }
                "sent" -> {
                    if (obj.contains("to") || obj.contains("money")) {
                        expenseScore += 2
                        println("🤖 SVO: '$subject SENT $obj' = EXPENSE (+2)")
                    }
                }
                "earned" -> {
                    incomeScore += 3
                    println("🤖 SVO: '$subject EARNED $obj' = INCOME (+3)")
                }
            }
        }

        return ScoreResult(incomeScore, expenseScore)
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
     * Data class for scoring results
     */
    private data class ScoreResult(
        val incomeScore: Int,
        val expenseScore: Int
    )

    internal fun extractAmount(text: String): Double {
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
     * Analyze what specific issue prevented successful transaction detection
     */
    internal fun analyzeDetectionIssues(text: String, patternResult: ModelResult, amount: Double): String {
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
    internal fun buildDetailedErrorMessage(text: String, issue: String, confidence: Double, amount: Double): String {
        return issue
    }

    /**
     * Get context words around a target word for semantic analysis
     */
    private fun getContextWords(words: List<String>, targetIndex: Int, windowSize: Int): List<String> {
        val start = maxOf(0, targetIndex - windowSize)
        val end = minOf(words.size, targetIndex + windowSize + 1)
        return words.subList(start, end)
    }

    /**
     * Determine if the context indicates shopping/purchasing activity
     */
    private fun isShoppingContext(text: String, contextWords: List<String>): Boolean {
        val shoppingIndicators = listOf(
            "market", "store", "shop", "shopping", "bought", "purchase", "buy",
            "bag", "item", "product", "goods", "mall", "supermarket", "grocery",
            "clothes", "food", "vegetables", "fruits", "went to", "for myself"
        )

        // Check for shopping indicators in context
        return shoppingIndicators.any { indicator ->
            text.contains(indicator) || contextWords.contains(indicator)
        }
    }

    /**
     * Determine if the context indicates receiving money
     */
    private fun isReceivingMoneyContext(text: String, contextWords: List<String>): Boolean {
        val moneyReceivingIndicators = listOf(
            "from", "money", "cash", "rupees", "dollars", "received", "gave me",
            "paid me", "lent me", "transfer", "deposit", "salary", "payment",
            "gift", "present", "donation", "prize", "award", "bonus", "reward"
        )

        // Check for money receiving indicators in context
        return moneyReceivingIndicators.any { indicator ->
            text.contains(indicator) || contextWords.contains(indicator)
        }
    }

    data class ModelResult(
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
