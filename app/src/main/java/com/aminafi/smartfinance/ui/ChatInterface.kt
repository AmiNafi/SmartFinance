package com.aminafi.smartfinance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import kotlinx.coroutines.launch

/**
 * Chat interface for AI-powered transaction detection
 * Following Single Responsibility Principle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInterface(
    onTransactionDetected: (AIDetectedTransaction) -> Unit,
    onDismiss: () -> Unit,
    onProcessMessage: suspend (String) -> Result<AIDetectedTransaction>
) {
    var message by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingTransaction by remember { mutableStateOf<AIDetectedTransaction?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Transaction Assistant") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Chat messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    reverseLayout = false
                ) {
                    items(chatMessages) { chatMessage ->
                        ChatMessageItem(
                            message = chatMessage,
                            onRequestConfirmation = { transaction ->
                                pendingTransaction = transaction
                                showConfirmationDialog = true
                            }
                        )
                    }
                }

                // Initial welcome message
                LaunchedEffect(Unit) {
                    if (chatMessages.isEmpty()) {
                        chatMessages = listOf(
                            ChatMessage(
                                text = "Hi! Tell me about your transaction. For example: 'I bought groceries for $50' or 'Got salary of $3000'",
                                isUser = false,
                                isSystem = true
                            )
                        )
                    }
                }

                // Message input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Type your transaction...") },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (message.isNotBlank() && !isProcessing) {
                                val userMessage = message
                                message = ""
                                isProcessing = true

                                coroutineScope.launch {
                                    // Add user message
                                    chatMessages = chatMessages + ChatMessage(text = userMessage, isUser = true)

                                    try {
                                        val result = onProcessMessage(userMessage)

                                        if (result.isSuccess) {
                                            val detectedTransaction = result.getOrNull()!!
                                            val confidencePercent = (detectedTransaction.confidence * 100).toInt()

                                            chatMessages = chatMessages + ChatMessage(
                                                text = "Detected: ${detectedTransaction.title}\n" +
                                                       "Amount: $${"%.2f".format(detectedTransaction.amount)}\n" +
                                                       "Type: ${detectedTransaction.type}\n" +
                                                       "Description: ${detectedTransaction.description}\n" +
                                                       "Confidence: ${confidencePercent}%",
                                                isUser = false,
                                                detectedTransaction = detectedTransaction
                                            )
                                        } else {
                                            chatMessages = chatMessages + ChatMessage(
                                                text = "Sorry, I couldn't understand that transaction. Please try rephrasing or use the manual entry.",
                                                isUser = false,
                                                isError = true
                                            )
                                        }
                                    } catch (e: Exception) {
                                        chatMessages = chatMessages + ChatMessage(
                                            text = "Error processing message: ${e.message}",
                                            isUser = false,
                                            isError = true
                                        )
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        enabled = message.isNotBlank() && !isProcessing
                    ) {
                        Text(if (isProcessing) "..." else "Send")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Editable Confirmation Dialog
    if (showConfirmationDialog && pendingTransaction != null) {
        var editableTitle by remember { mutableStateOf(pendingTransaction!!.title) }
        var editableAmount by remember { mutableStateOf(pendingTransaction!!.amount.toString()) }
        var editableDescription by remember { mutableStateOf(pendingTransaction!!.description) }
        var editableType by remember { mutableStateOf(pendingTransaction!!.type) }

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Edit Transaction Details") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("AI detected transaction with ${(pendingTransaction!!.confidence * 100).toInt()}% confidence")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Title field
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Amount field
                    OutlinedTextField(
                        value = editableAmount,
                        onValueChange = { editableAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description field
                    OutlinedTextField(
                        value = editableDescription,
                        onValueChange = { editableDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type selection
                    Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editableType == TransactionType.INCOME,
                            onClick = { editableType = TransactionType.INCOME }
                        )
                        Text("Income", modifier = Modifier.padding(start = 8.dp))

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = editableType == TransactionType.EXPENSE,
                            onClick = { editableType = TransactionType.EXPENSE }
                        )
                        Text("Expense", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = editableAmount.toDoubleOrNull()
                        if (amount != null && amount > 0 && editableTitle.isNotBlank()) {
                            val editedTransaction = AIDetectedTransaction(
                                amount = amount,
                                type = editableType,
                                title = editableTitle,
                                description = editableDescription,
                                confidence = pendingTransaction!!.confidence
                            )
                            onTransactionDetected(editedTransaction)
                            showConfirmationDialog = false
                            pendingTransaction = null
                            // Add success message to chat
                            chatMessages = chatMessages + ChatMessage(
                                text = "✅ Transaction added successfully!",
                                isUser = false,
                                isSystem = true
                            )
                        }
                    },
                    enabled = editableAmount.toDoubleOrNull() != null &&
                             editableAmount.toDoubleOrNull() ?: 0.0 > 0 &&
                             editableTitle.isNotBlank()
                ) {
                    Text("Add Transaction")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        pendingTransaction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Process user message and get AI response
 */
private suspend fun ChatInterfaceScope.processUserMessage() {
    val userMessage = message
    message = ""
    isProcessing = true

    // Add user message
    chatMessages = chatMessages + ChatMessage(text = userMessage, isUser = true)

    try {
        val result = onProcessMessage(userMessage)

        if (result.isSuccess) {
            val detectedTransaction = result.getOrNull()!!
            val confidencePercent = (detectedTransaction.confidence * 100).toInt()

            chatMessages = chatMessages + ChatMessage(
                text = "Detected: ${detectedTransaction.title}\n" +
                       "Amount: $${"%.2f".format(detectedTransaction.amount)}\n" +
                       "Type: ${detectedTransaction.type}\n" +
                       "Description: ${detectedTransaction.description}\n" +
                       "Confidence: ${confidencePercent}%",
                isUser = false,
                detectedTransaction = detectedTransaction
            )
        } else {
            chatMessages = chatMessages + ChatMessage(
                text = "Sorry, I couldn't understand that transaction. Please try rephrasing or use the manual entry.",
                isUser = false,
                isError = true
            )
        }
    } catch (e: Exception) {
        chatMessages = chatMessages + ChatMessage(
            text = "Error processing message: ${e.message}",
            isUser = false,
            isError = true
        )
    } finally {
        isProcessing = false
    }
}

/**
 * Chat message item composable
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onRequestConfirmation: (AIDetectedTransaction) -> Unit = {}
) {
    val backgroundColor = when {
        message.isSystem -> MaterialTheme.colorScheme.surfaceVariant
        message.isError -> MaterialTheme.colorScheme.errorContainer
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )

            if (message.detectedTransaction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            message.detectedTransaction?.let { transaction ->
                                onRequestConfirmation(transaction)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Add Transaction")
                    }
                    OutlinedButton(
                        onClick = {
                            // For now, just show a simple edit option
                            // In a full implementation, this would open an edit dialog
                        }
                    ) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

/**
 * Data classes for chat functionality
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean = false,
    val isSystem: Boolean = false,
    val isError: Boolean = false,
    val detectedTransaction: AIDetectedTransaction? = null
)

/**
 * Scope for chat interface functions
 */
class ChatInterfaceScope(
    var message: String,
    var chatMessages: List<ChatMessage>,
    var isProcessing: Boolean,
    val onProcessMessage: suspend (String) -> Result<AIDetectedTransaction>
) {
    suspend fun processUserMessage() {
        // Implementation moved to composable function
    }
}
