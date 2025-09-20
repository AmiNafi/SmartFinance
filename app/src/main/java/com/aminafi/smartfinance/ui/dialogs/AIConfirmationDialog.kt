package com.aminafi.smartfinance.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ai.AIDetectedTransaction
import com.aminafi.smartfinance.ui.components.TransactionForm
import com.aminafi.smartfinance.ui.components.TransactionFormData
import com.aminafi.smartfinance.ui.components.isValidForAI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfirmationDialog(
    pendingTransaction: AIDetectedTransaction?,
    onDismiss: () -> Unit,
    onConfirm: (AIDetectedTransaction) -> Unit
) {
    if (pendingTransaction == null) return

    var formData by remember {
        mutableStateOf(TransactionFormData(
            title = pendingTransaction.title,
            amount = pendingTransaction.amount.toString(),
            description = pendingTransaction.description,
            type = pendingTransaction.type
        ))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("AI detected transaction with ${(pendingTransaction.confidence * 100).toInt()}% confidence")
                Spacer(modifier = Modifier.height(16.dp))
                TransactionForm(
                    initialData = formData,
                    onDataChange = { formData = it },
                    showTitleField = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidForAI(formData)) {
                        val editedTransaction = AIDetectedTransaction(
                            amount = formData.amount.toDouble(),
                            type = formData.type,
                            title = formData.title,
                            description = formData.description,
                            confidence = pendingTransaction.confidence
                        )
                        onConfirm(editedTransaction)
                    }
                },
                enabled = isValidForAI(formData)
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
