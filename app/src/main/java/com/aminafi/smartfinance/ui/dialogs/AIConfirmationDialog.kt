package com.aminafi.smartfinance.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import com.aminafi.smartfinance.ui.components.TransactionTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfirmationDialog(
    pendingTransaction: AIDetectedTransaction?,
    onDismiss: () -> Unit,
    onConfirm: (AIDetectedTransaction) -> Unit
) {
    if (pendingTransaction == null) return

    var editableTitle by remember { mutableStateOf(pendingTransaction.title) }
    var editableAmount by remember { mutableStateOf(pendingTransaction.amount.toString()) }
    var editableDescription by remember { mutableStateOf(pendingTransaction.description) }
    var editableType by remember { mutableStateOf(pendingTransaction.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("AI detected transaction with ${(pendingTransaction.confidence * 100).toInt()}% confidence")
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

                // Transaction Type Selection using reusable component
                TransactionTypeSelector(
                    selectedType = editableType,
                    onTypeSelected = { editableType = it }
                )
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
                            confidence = pendingTransaction.confidence
                        )
                        onConfirm(editedTransaction)
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
