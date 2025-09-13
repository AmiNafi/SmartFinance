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
        return if (text.contains("expense") || text.contains("cost") ||
            text.contains("spending") || text.contains("payment") ||
            text.contains("bill") || text.contains("fee")) {
            println("🎯 DIRECT EXPENSE WORD: Strong EXPENSE signal (+5)")
            5
        } else 0
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
                text.contains("bought") || text.contains("purchase")) {
                expenseScore += 2
                println("🤖 CONTEXT: Shopping/market activity detected = EXPENSE")
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

    private fun determineFinalType(incomeScore: Int, expenseScore: Int, text: String, words: List<String>): TransactionType {
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
                val sentenceContext = analyzeSentenceContext(text, words)
                println("🤖 RESULT: $sentenceContext (sentence analysis)")
                sentenceContext
            }
        }
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
            "clothes", "food", "vegetables", "fruits", "went to", "for myself"
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
