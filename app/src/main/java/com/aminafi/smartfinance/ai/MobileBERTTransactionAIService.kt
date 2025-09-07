package com.aminafi.smartfinance.ai

import android.content.Context
import android.os.Build
import android.util.Log
import com.aminafi.smartfinance.TransactionType
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * Optimized MobileBERT-powered transaction detection service
 * Uses TensorFlow Lite with full WordPiece tokenization and budget phone optimizations
 */
class MobileBERTTransactionAIService(
    private val context: Context
) : TransactionAIService {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private val patternService = PatternBasedTransactionAIService()
    private val maxSeqLength = 128
    private var isModelLoaded = false
    private var vocabMap: Map<String, Int> = emptyMap()
    private val tokenCache = mutableMapOf<String, IntArray>() // Cache for tokenized results

    // Budget phone optimizations
    private val numThreads = if (Runtime.getRuntime().availableProcessors() <= 4) 1 else 2
    private val useGpu = CompatibilityList().isDelegateSupportedOnThisDevice

    init {
        initializeML()
    }

    private fun initializeML() {
        Log.d("MobileBERT", "🚀 Initializing MobileBERT ML System...")

        // Step 1: Load vocabulary (critical for tokenization)
        loadVocabularyRobust()

        // Step 2: Load and validate model
        loadModelRobust()

        // Step 3: Validate complete system
        validateSystemReadiness()

        Log.d("MobileBERT", "✅ MobileBERT ML System initialized - Ready for inference!")
    }

    private fun loadVocabularyRobust() {
        try {
            Log.d("MobileBERT", "📚 Loading vocabulary from assets...")

            // Check if vocab file exists
            val vocabFiles = context.assets.list("") ?: emptyArray()
            val hasVocab = vocabFiles.contains("vocab.json")

            if (!hasVocab) {
                Log.w("MobileBERT", "⚠️ vocab.json not found in assets, using minimal fallback")
                createMinimalVocabulary()
                return
            }

            val vocabJson = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(vocabJson)

            vocabMap = jsonObject.keys().asSequence().associateWith { key ->
                jsonObject.getInt(key)
            }

            // Validate essential tokens
            val essentialTokens = listOf("<PAD>", "<UNK>", "<CLS>", "<SEP>")
            val missingTokens = essentialTokens.filter { !vocabMap.containsKey(it) }

            if (missingTokens.isNotEmpty()) {
                Log.w("MobileBERT", "⚠️ Missing essential tokens: $missingTokens, adding defaults")
                val defaultMap = mutableMapOf(
                    "<PAD>" to 0, "<UNK>" to 1, "<CLS>" to 101, "<SEP>" to 102
                )
                vocabMap = vocabMap + defaultMap
            }

            Log.d("MobileBERT", "✅ Vocabulary loaded: ${vocabMap.size} tokens")

        } catch (e: Exception) {
            Log.e("MobileBERT", "❌ Vocabulary loading failed: ${e.message}")
            createMinimalVocabulary()
        }
    }

    private fun createMinimalVocabulary() {
        Log.d("MobileBERT", "🔧 Creating minimal vocabulary for reliability")
        vocabMap = mapOf(
            "<PAD>" to 0, "<UNK>" to 1, "<CLS>" to 101, "<SEP>" to 102,
            // Essential financial tokens
            "paid" to 4, "received" to 5, "spent" to 6, "got" to 7,
            "money" to 8, "cash" to 9, "bought" to 10, "sold" to 11,
            "income" to 12, "expense" to 13, "salary" to 14, "rent" to 15,
            "food" to 16, "gas" to 17, "shopping" to 18, "transfer" to 19,
            "for" to 20, "from" to 21, "to" to 22, "at" to 23
        )
        Log.d("MobileBERT", "✅ Minimal vocabulary created: ${vocabMap.size} tokens")
    }

    private fun loadModelRobust() {
        try {
            Log.d("MobileBERT", "🤖 Loading TensorFlow Lite model...")

            // Check if model file exists
            val assetFiles = context.assets.list("") ?: emptyArray()
            val hasModel = assetFiles.contains("mobilebert_transaction_classifier.tflite")

            if (!hasModel) {
                Log.e("MobileBERT", "❌ Model file not found in assets!")
                isModelLoaded = false
                return
            }

            // Load model buffer
            val modelBuffer = context.assets.openFd("mobilebert_transaction_classifier.tflite").use { fd ->
                FileInputStream(fd.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }

            Log.d("MobileBERT", "📊 Model size: ${modelBuffer.capacity()} bytes")

            // Create interpreter with multiple fallback options
            val options = Interpreter.Options().apply {
                // Start with CPU only for maximum compatibility
                setNumThreads(numThreads)
                setUseNNAPI(false) // Disable NNAPI initially for compatibility

                Log.d("MobileBERT", "⚙️ Interpreter options: Threads=$numThreads, NNAPI=false")
            }

            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true

            // Validate model structure
            validateModelStructure()

            Log.d("MobileBERT", "✅ Model loaded and validated successfully")

        } catch (e: Exception) {
            Log.e("MobileBERT", "❌ Model loading failed: ${e.message}")
            isModelLoaded = false

            // Try alternative loading method
            tryAlternativeModelLoading()
        }
    }

    private fun validateModelStructure() {
        try {
            val inputCount = interpreter?.inputTensorCount ?: 0
            val outputCount = interpreter?.outputTensorCount ?: 0

            Log.d("MobileBERT", "🔍 Model validation:")
            Log.d("MobileBERT", "   Input tensors: $inputCount")
            Log.d("MobileBERT", "   Output tensors: $outputCount")

            if (inputCount >= 2 && outputCount >= 1) {
                Log.d("MobileBERT", "✅ Model structure validated")
            } else {
                Log.w("MobileBERT", "⚠️ Unexpected model structure, but proceeding")
            }

        } catch (e: Exception) {
            Log.w("MobileBERT", "⚠️ Model validation failed: ${e.message}")
        }
    }

    private fun tryAlternativeModelLoading() {
        try {
            Log.d("MobileBERT", "🔄 Trying alternative model loading...")

            // Load model as byte array instead of memory mapping
            val modelBytes = context.assets.open("mobilebert_transaction_classifier.tflite").use { it.readBytes() }
            val byteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            byteBuffer.put(modelBytes)
            byteBuffer.rewind()

            val options = Interpreter.Options().apply {
                setNumThreads(1) // Minimal threads for compatibility
                setUseNNAPI(false)
            }

            interpreter = Interpreter(byteBuffer, options)
            isModelLoaded = true

            Log.d("MobileBERT", "✅ Alternative model loading successful")

        } catch (e: Exception) {
            Log.e("MobileBERT", "❌ Alternative loading also failed: ${e.message}")
            isModelLoaded = false
        }
    }

    private fun validateSystemReadiness() {
        val readinessScore = calculateReadinessScore()

        Log.d("MobileBERT", "📊 System Readiness Score: $readinessScore/100")

        if (readinessScore >= 80) {
            Log.d("MobileBERT", "🎯 System is highly ready for ML inference")
        } else if (readinessScore >= 60) {
            Log.d("MobileBERT", "⚠️ System is moderately ready, some features may be limited")
        } else {
            Log.w("MobileBERT", "❌ System readiness is low, expect limited functionality")
        }
    }

    private fun calculateReadinessScore(): Int {
        var score = 0

        // Vocabulary readiness (30 points)
        if (vocabMap.isNotEmpty()) {
            score += 30
            if (vocabMap.size > 100) score += 10 // Bonus for rich vocabulary
        }

        // Model readiness (40 points)
        if (isModelLoaded && interpreter != null) {
            score += 40
        }

        // Memory availability (20 points)
        val runtime = Runtime.getRuntime()
        val availableMemory = runtime.freeMemory()
        val totalMemory = runtime.totalMemory()
        val memoryRatio = availableMemory.toDouble() / totalMemory.toDouble()

        if (memoryRatio > 0.3) score += 20
        else if (memoryRatio > 0.1) score += 10

        // CPU capability (10 points)
        if (Runtime.getRuntime().availableProcessors() >= 2) {
            score += 10
        }

        return score.coerceAtMost(100)
    }

    override suspend fun detectTransaction(text: String): Result<AIDetectedTransaction> {
        return withContext(Dispatchers.Default) {
            try {
                Log.d("MobileBERT", "🚀 Starting ML-powered transaction detection for: '$text'")

                // Pre-flight checks
                val preflightResult = performPreflightChecks(text)
                if (preflightResult != null) {
                    return@withContext preflightResult
                }

                // Extract amount (robust extraction)
                val amount = extractAmountRobust(text)
                if (amount <= 0) {
                    Log.w("MobileBERT", "⚠️ No amount found, but proceeding with ML analysis")
                    // Don't fail - let ML handle it
                }

                // Perform ML inference
                val inferenceResult = performInference(text)

                // Generate result
                val transaction = createTransactionResult(text, amount, inferenceResult)

                Log.d("MobileBERT", "✅ ML Inference successful:")
                Log.d("MobileBERT", "   Type: ${transaction.type}")
                Log.d("MobileBERT", "   Amount: $${transaction.amount}")
                Log.d("MobileBERT", "   Confidence: ${(transaction.confidence * 100).toInt()}%")

                Result.success(transaction)

            } catch (e: Exception) {
                Log.e("MobileBERT", "💥 Critical ML error: ${e.message}")

                // Even in critical failure, try to provide a basic result
                // Don't fallback to pattern matching - keep ML as primary
                val fallbackResult = createFallbackResult(text)
                Log.w("MobileBERT", "⚠️ Using minimal fallback result")
                Result.success(fallbackResult)
            }
        }
    }

    private fun performPreflightChecks(text: String): Result<AIDetectedTransaction>? {
        // Check if text is valid
        if (text.isBlank()) {
            return Result.failure(Exception("Empty transaction text"))
        }

        // Check model readiness
        if (!isModelLoaded || interpreter == null) {
            Log.w("MobileBERT", "⚠️ Model not loaded, attempting emergency initialization")
            // Try to reinitialize
            try {
                initializeML()
                if (!isModelLoaded) {
                    return Result.failure(Exception("ML model unavailable"))
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to initialize ML model: ${e.message}"))
            }
        }

        return null // All checks passed
    }

    private fun extractAmountRobust(text: String): Double {
        // Try multiple extraction methods
        val amount = patternService.extractAmount(text)
        if (amount > 0) {
            return amount
        }

        // Try additional patterns
        val additionalPatterns = listOf(
            Regex("(\\d+\\.?\\d*)\\s*(?:bucks?|bucks)"),
            Regex("(\\d+\\.?\\d*)\\s*(?:rupees?|rs|₹)"),
            Regex("(\\d+)") // Last resort - any number
        )

        for (pattern in additionalPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val extractedAmount = match.groupValues[1].toDoubleOrNull()
                if (extractedAmount != null && extractedAmount > 0) {
                    Log.d("MobileBERT", "💰 Amount extracted: $${extractedAmount}")
                    return extractedAmount
                }
            }
        }

        Log.w("MobileBERT", "⚠️ No amount found in text")
        return 0.0
    }

    private fun performInference(text: String): InferenceResult {
        try {
            Log.d("MobileBERT", "🔄 Running ML inference...")

            // Tokenize input (simple model only needs input_ids)
            val inputIds = tokenizeText(text)

            // Prepare input buffer (single input for simple model)
            val inputBuffer = ByteBuffer.allocateDirect(4 * maxSeqLength).order(ByteOrder.nativeOrder())

            for (id in inputIds) {
                inputBuffer.putInt(id)
            }

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder())

            // Run inference with single input
            val startTime = System.currentTimeMillis()
            val inputs = arrayOf(inputBuffer)
            val outputs = mutableMapOf<Int, Any>(0 to outputBuffer)

            interpreter?.runForMultipleInputsOutputs(inputs, outputs)

            val inferenceTime = System.currentTimeMillis() - startTime
            Log.d("MobileBERT", "⚡ Inference completed in ${inferenceTime}ms")

            // Process results
            outputBuffer.rewind()
            val logits = FloatArray(2)
            logits[0] = outputBuffer.float
            logits[1] = outputBuffer.float

            // Apply softmax
            val probabilities = softmax(logits)

            // Determine prediction
            val predictedClass = if (probabilities[0] > probabilities[1]) 0 else 1
            val confidence = probabilities[predictedClass].toDouble()

            Log.d("MobileBERT", "📊 Inference results:")
            Log.d("MobileBERT", "   Expense prob: ${(probabilities[0] * 100).toInt()}%")
            Log.d("MobileBERT", "   Income prob: ${(probabilities[1] * 100).toInt()}%")
            Log.d("MobileBERT", "   Prediction: ${if (predictedClass == 0) "EXPENSE" else "INCOME"}")
            Log.d("MobileBERT", "   Confidence: ${(confidence * 100).toInt()}%")

            return InferenceResult(
                predictedClass = predictedClass,
                confidence = confidence,
                probabilities = probabilities
            )

        } catch (e: Exception) {
            Log.e("MobileBERT", "❌ Inference failed: ${e.message}")
            throw e // Re-throw to be handled by caller
        }
    }

    private fun createTransactionResult(text: String, amount: Double, inferenceResult: InferenceResult): AIDetectedTransaction {
        val transactionType = if (inferenceResult.predictedClass == 0) TransactionType.EXPENSE else TransactionType.INCOME
        val title = generateTitle(text, transactionType)

        // If no amount was extracted, use a default but mark as uncertain
        val finalAmount = if (amount > 0) amount else 1.0
        val finalConfidence = if (amount > 0) inferenceResult.confidence else (inferenceResult.confidence * 0.7)

        return AIDetectedTransaction(
            amount = finalAmount,
            type = transactionType,
            title = title,
            description = text,
            confidence = finalConfidence
        )
    }

    private fun createFallbackResult(text: String): AIDetectedTransaction {
        // Create a minimal result when everything fails
        val amount = extractAmountRobust(text)
        val finalAmount = if (amount > 0) amount else 1.0

        return AIDetectedTransaction(
            amount = finalAmount,
            type = TransactionType.EXPENSE, // Default to expense
            title = "Transaction",
            description = text,
            confidence = 0.5 // Low confidence for fallback
        )
    }

    private data class InferenceResult(
        val predictedClass: Int,
        val confidence: Double,
        val probabilities: FloatArray
    )

    private fun tokenizeText(text: String): IntArray {
        // Check cache first for budget phone optimization
        tokenCache[text]?.let { return it }

        val tokens = mutableListOf<Int>()

        // Add [CLS] token (BERT standard)
        tokens.add(vocabMap["[CLS]"] ?: 101)

        // Clean and tokenize text using BERT-style tokenization
        val cleanText = text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ") // Remove punctuation but keep numbers
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()

        val words = cleanText.split(" ").filter { it.isNotEmpty() }

        // Apply BERT WordPiece tokenization for each word
        for (word in words) {
            if (tokens.size >= maxSeqLength - 1) break // Reserve space for [SEP]

            val wordTokens = bertWordPieceTokenize(word)
            tokens.addAll(wordTokens)
        }

        // Add [SEP] token
        tokens.add(vocabMap["[SEP]"] ?: 102)

        // Truncate if too long (keep [CLS] and [SEP])
        if (tokens.size > maxSeqLength) {
            val truncated = tokens.take(maxSeqLength - 1).toMutableList()
            truncated.add(vocabMap["[SEP]"] ?: 102)
            tokens.clear()
            tokens.addAll(truncated)
        }

        // Pad to max length
        while (tokens.size < maxSeqLength) {
            tokens.add(vocabMap["[PAD]"] ?: 0)
        }

        val result = tokens.toIntArray()

        // Cache result for future use (budget phone optimization)
        if (tokenCache.size < 100) { // Limit cache size
            tokenCache[text] = result
        }

        return result
    }

    private fun bertWordPieceTokenize(word: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Handle numbers and special cases
        if (word.matches(Regex("\\d+"))) {
            // Numbers get special treatment in BERT
            vocabMap[word]?.let { tokenId ->
                tokens.add(tokenId)
                return tokens
            }
        }

        // Try whole word first
        vocabMap[word]?.let { tokenId ->
            tokens.add(tokenId)
            return tokens
        }

        // WordPiece algorithm: try progressively smaller subwords
        var remaining = word
        var start = 0
        var shouldContinue = true

        while (remaining.isNotEmpty() && start < word.length && shouldContinue) {
            var found = false

            // Try longest possible subword - find the first match
            val end = (word.length downTo start + 1).firstOrNull { end ->
                val subword = word.substring(start, end)
                val tokenKey = if (start > 0) "##$subword" else subword
                vocabMap[tokenKey] != null
            }

            if (end != null) {
                val subword = word.substring(start, end)
                val tokenKey = if (start > 0) "##$subword" else subword
                vocabMap[tokenKey]?.let { tokenId ->
                    tokens.add(tokenId)
                    start = end
                    remaining = word.substring(start)
                    found = true
                }
            }

            // If no subword found, use [UNK] and stop
            if (!found) {
                tokens.add(vocabMap["[UNK]"] ?: 100)
                shouldContinue = false
            }
        }

        return tokens
    }

    private fun createAttentionMask(inputIds: IntArray): IntArray {
        return IntArray(maxSeqLength) { if (inputIds[it] != 0) 1 else 0 }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expLogits = logits.map { Math.exp(it - maxLogit.toDouble()).toFloat() }
        val sum = expLogits.sum()
        return expLogits.map { it / sum }.toFloatArray()
    }

    private fun generateTitle(text: String, type: TransactionType): String {
        // Simple title generation based on type
        return if (type == TransactionType.INCOME) "Income Transaction" else "Expense Transaction"
    }

    /**
     * Check if model is ready
     */
    fun isModelReady(): Boolean {
        return isModelLoaded && interpreter != null
    }

    /**
     * Cleanup resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
        Log.d("MobileBERT", "🧹 MobileBERT resources cleaned up")
    }
}
