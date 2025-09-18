package com.aminafi.smartfinance.ai.analyzers

import com.aminafi.smartfinance.TransactionType
import kotlin.math.max
import kotlin.math.min

/**
 * Transaction Type Analyzer following Single Responsibility Principle and Open-Closed Principle
 * Handles only transaction type detection from text analysis
 * Implements TransactionTypeAnalyzer interface for extensibility
 */
class TransactionTypeAnalyzer : TransactionTypeAnalyzerInterface {

    /**
     * Analyze text to determine transaction type
     */
    override fun analyzeTransactionType(text: String): TransactionType {
        val lowerText = text.lowercase()
        val words = lowerText.split("\\s+".toRegex()).map { it.lowercase().trim() }

        var incomeScore = 0
        var expenseScore = 0

        // Direct category words (highest priority)
        incomeScore += analyzeDirectCategoryWords(lowerText)
        expenseScore += analyzeDirectExpenseWords(lowerText)

        // Preposition-based analysis
        val prepositionResult = analyzePrepositions(words, lowerText)
        incomeScore += prepositionResult.incomeScore
        expenseScore += prepositionResult.expenseScore

        // Subject-Verb-Object analysis
        val svoResult = analyzeSubjectVerbObject(words, lowerText)
        incomeScore += svoResult.incomeScore
        expenseScore += svoResult.expenseScore

        // Gift and reward patterns
        incomeScore += analyzeGiftPatterns(lowerText)

        // Family relationship patterns
        incomeScore += analyzeFamilyPatterns(lowerText)

        // Direct money flow patterns
        val flowResult = analyzeMoneyFlowPatterns(lowerText)
        incomeScore += flowResult.incomeScore
        expenseScore += flowResult.expenseScore

        // Receiving and giving patterns
        val patternResult = analyzeReceivingGivingPatterns(lowerText)
        incomeScore += patternResult.incomeScore
        expenseScore += patternResult.expenseScore

        // Possessive context analysis
        val possessiveResult = analyzePossessiveContext(words, lowerText)
        incomeScore += possessiveResult.incomeScore
        expenseScore += possessiveResult.expenseScore

        // Semantic verb analysis
        val semanticResult = analyzeSemanticVerbs(words, lowerText)
        incomeScore += semanticResult.incomeScore
        expenseScore += semanticResult.expenseScore

        return determineFinalType(incomeScore, expenseScore, lowerText, words)
    }

    private fun analyzeDirectCategoryWords(text: String): Int {
        return if (text.contains("income") || text.contains("salary") ||
            text.contains("earning") || text.contains("revenue") ||
            text.contains("profit") || text.contains("bonus") ||
            text.contains("commission")) {
            println("🎯 DIRECT INCOME WORD: Strong INCOME signal (+5)")
            5
        } else 0
    }

    private fun analyzeDirectExpenseWords(text: String): Int {
        var expenseScore = 0

        // Direct expense keywords
        if (text.contains("expense") || text.contains("cost") ||
            text.contains("spending") || text.contains("payment") ||
            text.contains("bill") || text.contains("fee")) {
            println("🎯 DIRECT EXPENSE WORD: Strong EXPENSE signal (+5)")
            expenseScore += 5
        }

        // Shopping and purchase keywords
        if (text.contains("shopping") || text.contains("shop") ||
            text.contains("bought") || text.contains("purchase") ||
            text.contains("buy") || text.contains("bought")) {
            println("🛒 SHOPPING DETECTED: Strong EXPENSE signal (+4)")
            expenseScore += 4
        }

        // Loss-related keywords (lost, missing, stolen, etc.)
        if (text.contains("lost") || text.contains("missing") ||
            text.contains("stolen") || text.contains("theft") ||
            text.contains("robbed") || text.contains("gone") ||
            text.contains("disappeared")) {
            println("💸 LOSS DETECTED: Money lost/stolen = EXPENSE (+4)")
            expenseScore += 4
        }

        return expenseScore
    }

    private fun analyzePrepositions(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        val prepositionIndices = words.mapIndexedNotNull { index, word ->
            if (word in listOf("to", "from", "by", "with", "for")) index to word else null
        }

        for ((prepIndex, preposition) in prepositionIndices) {
            val beforePrep = if (prepIndex > 0) words[prepIndex - 1] else ""
            val afterPrep = if (prepIndex < words.size - 1) words[prepIndex + 1] else ""

            when (preposition) {
                "from" -> {
                    if (beforePrep in listOf("got", "received", "money", "gift", "payment")) {
                        incomeScore += 3
                        println("🤖 PREP: '$beforePrep FROM $afterPrep' = INCOME (+3)")
                    } else if (beforePrep in listOf("bought", "paid", "spent")) {
                        expenseScore += 2
                        println("🤖 PREP: '$beforePrep FROM $afterPrep' = EXPENSE (+2)")
                    }
                }
                "to" -> {
                    if (beforePrep in listOf("gave", "paid", "sent", "lent")) {
                        expenseScore += 3
                        println("🤖 PREP: '$beforePrep TO $afterPrep' = EXPENSE (+3)")
                    } else if (afterPrep == "me" || beforePrep in listOf("transfer", "deposit")) {
                        incomeScore += 3
                        println("🤖 PREP: '$beforePrep TO $afterPrep' = INCOME (+3)")
                    }
                }
                "for" -> {
                    if (beforePrep in listOf("paid", "spent", "bought")) {
                        expenseScore += 2
                        println("🤖 PREP: '$beforePrep FOR $afterPrep' = EXPENSE (+2)")
                    }
                }
                "by" -> {
                    if (beforePrep in listOf("paid", "sent")) {
                        incomeScore += 2
                        println("🤖 PREP: '$beforePrep BY $afterPrep' = INCOME (+2)")
                    }
                }
            }
        }

        return ScoreResult(incomeScore, expenseScore)
    }

    private fun analyzeSubjectVerbObject(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

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

    private fun analyzeGiftPatterns(text: String): Int {
        return if (text.contains("gift") || text.contains("present") ||
            text.contains("donation") || text.contains("prize") ||
            text.contains("award") || text.contains("bonus") ||
            text.contains("reward")) {
            println("🤖 GIFT/REWARD DETECTED: Strong INCOME signal (+4)")
            4
        } else 0
    }

    private fun analyzeFamilyPatterns(text: String): Int {
        val familyWords = listOf("father", "mother", "dad", "mom", "parent", "brother", "sister", "son", "daughter", "uncle", "aunt", "grandfather", "grandmother")
        val hasFamilyRelation = familyWords.any { text.contains(it) }
        return if (hasFamilyRelation && (text.contains("got") || text.contains("received") || text.contains("money"))) {
            println("🤖 FAMILY MONEY: Money from family member = INCOME (+3)")
            3
        } else 0
    }

    private fun analyzeMoneyFlowPatterns(text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        // Money coming to me patterns
        if (text.contains("gave me") || text.contains("gave to me") ||
            text.contains("lent me") || text.contains("paid me") ||
            text.contains("sent me")) {
            incomeScore += 3
            println("🤖 DIRECT: Money coming TO me (+3)")
        }

        // Money going from me patterns
        if (text.contains("gave to") && !text.contains("gave to me") && !text.contains("gave me")) {
            expenseScore += 3
            println("🤖 DIRECT: Money going FROM me (+3)")
        }
        if (text.contains("gave away") || text.contains("lent to") ||
            text.contains("paid to") || text.contains("sent to")) {
            expenseScore += 3
            println("🤖 DIRECT: Money going FROM me (+3)")
        }

        return ScoreResult(incomeScore, expenseScore)
    }

    private fun analyzeReceivingGivingPatterns(text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        val receivingPatterns = listOf(
            "got from", "received from", "got money", "money came",
            "came from", "transfer to me", "deposit to",
            "gift from", "present from", "donation from", "prize from",
            "money from", "cash from", "payment from"
        )

        val givingPatterns = listOf(
            "gave away", "paid out", "spent for", "cost for",
            "transfer from", "withdrawal", "paid off"
        )

        for (pattern in receivingPatterns) {
            if (text.contains(pattern)) {
                incomeScore += 2
                println("🤖 RECEIVING: '$pattern'")
            }
        }

        for (pattern in givingPatterns) {
            if (text.contains(pattern)) {
                expenseScore += 2
                println("🤖 GIVING: '$pattern'")
            }
        }

        return ScoreResult(incomeScore, expenseScore)
    }

    private fun analyzePossessiveContext(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        if (words.contains("me") || words.contains("my") || words.contains("i")) {
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

            if (text.contains("for myself") || text.contains("for me")) {
                expenseScore += 3
                println("🤖 POSSESSIVE: 'for myself/me' = personal purchase (EXPENSE)")
            }

            if (text.contains("went to market") || text.contains("shopping") ||
                text.contains("bought") || text.contains("purchase") ||
                text.contains("shop") || text.contains("buy")) {
                expenseScore += 3
                println("🤖 CONTEXT: Shopping/market activity detected = EXPENSE (+3)")
            }
        }

        return ScoreResult(incomeScore, expenseScore)
    }

    private fun analyzeSemanticVerbs(words: List<String>, text: String): ScoreResult {
        var incomeScore = 0
        var expenseScore = 0

        val basicIncomeVerbs = listOf("received", "earned", "got")
        val basicExpenseVerbs = listOf("paid", "spent", "bought")

        for (verb in basicIncomeVerbs) {
            if (words.contains(verb)) {
                val verbIndex = words.indexOf(verb)
                val contextWords = getContextWords(words, verbIndex, 3)

                if (verb == "got" && isShoppingContext(text, contextWords)) {
                    expenseScore += 2
                    println("🤖 SEMANTIC: '$verb' in shopping context = EXPENSE (+2)")
                } else if (verb == "got" && isReceivingMoneyContext(text, contextWords)) {
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

        return ScoreResult(incomeScore, expenseScore)
    }

    private fun analyzeSavingsPatterns(text: String): Int {
        var savingsScore = 0
        val lowerText = text.lowercase()

        // Direct savings keywords with weights
        val savingsKeywords = mapOf(
            "sav" to 2, "saving" to 2, "saved" to 3, "savings" to 4,
            "deposit" to 3, "deposited" to 4, "bank" to 2, "account" to 2,
            "put away" to 4, "set aside" to 4, "emergency" to 3, "fund" to 3,
            "future" to 3, "long term" to 3, "accumulate" to 3, "build" to 2,
            "invest" to 2, "investment" to 3, "retirement" to 3, "pension" to 3
        )

        for ((keyword, keywordWeight) in savingsKeywords) {
            if (lowerText.contains(keyword)) {
                savingsScore += keywordWeight
                println("💰 SAVINGS: '$keyword' detected (+$keywordWeight)")
            }
        }

        // Money flow to bank/account patterns with context
        val bankPatterns = listOf(
            "to bank" to 4, "to savings" to 5, "to account" to 4,
            "into savings" to 5, "into bank" to 4, "into account" to 4,
            "bank account" to 3, "savings account" to 4,
            "transfer to savings" to 5, "moved to bank" to 4
        )

        for ((pattern, patternWeight) in bankPatterns) {
            if (lowerText.contains(pattern)) {
                savingsScore += patternWeight
                println("🏦 BANK FLOW: '$pattern' detected (+$patternWeight)")
            }
        }

        // Smart context analysis
        savingsScore += analyzeSavingsContext(lowerText)

        // Exclude payment patterns with smart detection
        val exclusionPatterns = listOf(
            "paid bill" to 5, "paid rent" to 5, "paid utilities" to 5,
            "paid debt" to 5, "paid loan" to 5, "paid credit" to 5,
            "bought" to 4, "purchase" to 4, "shopping" to 3,
            "cost" to 3, "fee" to 3, "charge" to 3
        )

        for ((pattern, penalty) in exclusionPatterns) {
            if (lowerText.contains(pattern)) {
                // Check if it's actually a savings context despite containing payment words
                if (!isSavingsOverrideContext(lowerText, pattern)) {
                    savingsScore -= penalty
                    println("❌ EXCLUSION: '$pattern' detected (-$penalty)")
                } else {
                    println("⚡ OVERRIDE: '$pattern' in savings context (+1)")
                    savingsScore += 1
                }
            }
        }

        // Boost score for clear savings intent
        if (savingsScore >= 8 && hasClearSavingsIntent(lowerText)) {
            savingsScore += 2
            println("🚀 CLEAR INTENT: High confidence savings (+2)")
        }

        return maxOf(0, savingsScore)
    }

    private fun analyzeSavingsContext(text: String): Int {
        var contextScore = 0

        // Time-based context (future oriented)
        val futureWords = listOf("next month", "next year", "in future", "later", "tomorrow")
        for (word in futureWords) {
            if (text.contains(word)) {
                contextScore += 2
                println("⏰ FUTURE: '$word' context (+2)")
            }
        }

        // Purpose-based context
        val purposeWords = listOf("for vacation", "for house", "for car", "for education", "for kids")
        for (word in purposeWords) {
            if (text.contains(word)) {
                contextScore += 3
                println("🎯 PURPOSE: '$word' context (+3)")
            }
        }

        // Amount-based context (larger amounts often indicate savings)
        val words = text.split("\\s+".toRegex())
        val amountWords = words.filter { it.matches(Regex("\\$\\d+")) }
        if (amountWords.isNotEmpty()) {
            val avgAmount = amountWords.mapNotNull {
                it.replace("$", "").toDoubleOrNull()
            }.average()

            if (avgAmount > 100) { // Large amounts more likely to be savings
                contextScore += 2
                println("💵 LARGE AMOUNT: High value suggests savings (+2)")
            }
        }

        return contextScore
    }

    private fun isSavingsOverrideContext(text: String, paymentPattern: String): Boolean {
        // Check if payment pattern is actually in a savings context
        val savingsOverrideWords = listOf(
            "savings", "deposit", "bank", "account", "saved",
            "emergency", "fund", "future", "invest"
        )

        return savingsOverrideWords.any { overrideWord ->
            text.contains(overrideWord) && text.indexOf(overrideWord) > text.indexOf(paymentPattern)
        }
    }

    private fun hasClearSavingsIntent(text: String): Boolean {
        // Check for multiple savings indicators
        val savingsIndicators = listOf(
            "savings", "deposit", "bank", "account", "emergency",
            "fund", "future", "invest", "save", "put away"
        )

        val indicatorCount = savingsIndicators.count { text.contains(it) }
        return indicatorCount >= 2 // Multiple indicators = clear intent
    }

    private fun determineFinalType(incomeScore: Int, expenseScore: Int, text: String, words: List<String>): TransactionType {
        // Add savings analysis
        val savingsScore = analyzeSavingsPatterns(text.lowercase())

        println("🤖 SCORES - Income: $incomeScore, Expense: $expenseScore, Savings: $savingsScore")

        // Calculate confidence levels
        val totalScore = incomeScore + expenseScore + savingsScore
        val maxScore = maxOf(incomeScore, expenseScore, savingsScore)

        val confidence = if (totalScore > 0) maxScore.toDouble() / totalScore else 0.0
        println("🎯 CONFIDENCE: ${(confidence * 100).toInt()}%")

        // Smart decision making with confidence thresholds
        return when {
            // High confidence savings detection
            savingsScore > incomeScore && savingsScore > expenseScore && savingsScore >= 8 -> {
                println("🤖 RESULT: SAVINGS (High Confidence)")
                TransactionType.SAVINGS
            }

            // Medium confidence with context boost
            savingsScore > incomeScore && savingsScore > expenseScore && hasStrongSavingsContext(text) -> {
                println("🤖 RESULT: SAVINGS (Context Boost)")
                TransactionType.SAVINGS
            }

            // Standard income detection
            incomeScore > expenseScore && incomeScore > savingsScore -> {
                println("🤖 RESULT: INCOME")
                TransactionType.INCOME
            }

            // Standard expense detection
            expenseScore > incomeScore && expenseScore > savingsScore -> {
                println("🤖 RESULT: EXPENSE")
                TransactionType.EXPENSE
            }

            // Ambiguity resolution
            else -> resolveAmbiguity(incomeScore, expenseScore, savingsScore, text, words)
        }
    }

    private fun hasStrongSavingsContext(text: String): Boolean {
        val strongIndicators = listOf(
            "savings account", "emergency fund", "put away", "set aside",
            "for future", "long term", "deposit to", "bank savings"
        )
        return strongIndicators.any { text.contains(it) }
    }

    private fun resolveAmbiguity(incomeScore: Int, expenseScore: Int, savingsScore: Int, text: String, words: List<String>): TransactionType {
        println("🤔 AMBIGUITY: Resolving with advanced analysis...")

        // Check for question marks or uncertainty
        if (text.contains("?") || text.contains("maybe") || text.contains("perhaps")) {
            println("❓ UNCERTAIN: Defaulting to EXPENSE")
            return TransactionType.EXPENSE
        }

        // Check for negation
        if (words.any { it == "not" || it == "n't" || it == "never" }) {
            println("🚫 NEGATION: Defaulting to EXPENSE")
            return TransactionType.EXPENSE
        }

        // Amount-based decision (large amounts often indicate savings)
        val amountPattern = Regex("\\$?(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)")
        val amounts = amountPattern.findAll(text).mapNotNull {
            it.groupValues[1].replace(",", "").toDoubleOrNull()
        }.toList()

        if (amounts.isNotEmpty()) {
            val avgAmount = amounts.average()
            if (avgAmount > 500 && savingsScore > 0) {
                println("💰 LARGE AMOUNT + SAVINGS: Choosing SAVINGS")
                return TransactionType.SAVINGS
            }
            if (avgAmount < 50 && expenseScore > 0) {
                println("🪙 SMALL AMOUNT + EXPENSE: Choosing EXPENSE")
                return TransactionType.EXPENSE
            }
        }

        // Default fallback based on highest score
        val scores = mapOf(
            TransactionType.INCOME to incomeScore,
            TransactionType.EXPENSE to expenseScore,
            TransactionType.SAVINGS to savingsScore
        )

        val bestType = scores.maxByOrNull { it.value }?.key ?: TransactionType.EXPENSE
        println("📊 DEFAULT: Choosing $bestType (highest score)")

        return bestType
    }

    private fun analyzeSentenceContext(text: String, words: List<String>): TransactionType {
        if (text.contains("?") || text.contains("maybe") || text.contains("perhaps")) {
            return TransactionType.EXPENSE
        }
        if (words.any { it == "not" || it == "n't" || it == "never" }) {
            return TransactionType.EXPENSE
        }
        return TransactionType.EXPENSE
    }

    private fun getContextWords(words: List<String>, targetIndex: Int, windowSize: Int): List<String> {
        val start = maxOf(0, targetIndex - windowSize)
        val end = minOf(words.size, targetIndex + windowSize + 1)
        return words.subList(start, end)
    }

    private fun isShoppingContext(text: String, contextWords: List<String>): Boolean {
        val shoppingIndicators = listOf(
            "market", "store", "shop", "shopping", "bought", "purchase", "buy",
            "bag", "item", "product", "goods", "mall", "supermarket", "grocery",
            "clothes", "food", "vegetables", "fruits", "went to", "for myself",
            "retail", "boutique", "department", "shopping center", "online shop",
            "ecommerce", "amazon", "flipkart", "ebay", "walmart", "target",
            "costco", "ikea", "home depot", "lowes", "best buy", "electronics",
            "furniture", "books", "toys", "sports", "outdoor", "garden",
            "pharmacy", "drugstore", "cosmetics", "beauty", "jewelry",
            "shoes", "accessories", "handbag", "wallet", "perfume", "cologne"
        )
        return shoppingIndicators.any { indicator ->
            text.contains(indicator) || contextWords.contains(indicator)
        }
    }

    private fun isReceivingMoneyContext(text: String, contextWords: List<String>): Boolean {
        val moneyReceivingIndicators = listOf(
            "from", "money", "cash", "rupees", "dollars", "received", "gave me",
            "paid me", "lent me", "transfer", "deposit", "salary", "payment",
            "gift", "present", "donation", "prize", "award", "bonus", "reward"
        )
        return moneyReceivingIndicators.any { indicator ->
            text.contains(indicator) || contextWords.contains(indicator)
        }
    }

    private data class ScoreResult(val incomeScore: Int, val expenseScore: Int)
}
