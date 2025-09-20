package com.aminafi.smartfinance.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ui.components.TransactionForm
import com.aminafi.smartfinance.ui.components.TransactionFormData
import com.aminafi.smartfinance.ui.components.isValid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var formData by remember {
        mutableStateOf(TransactionFormData(
            description = transaction.description,
            amount = transaction.amount.toString(),
            type = transaction.type
        ))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            TransactionForm(
                initialData = formData,
                onDataChange = { formData = it }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid(formData)) {
                        val updatedTransaction = transaction.copy(
                            description = formData.description,
                            amount = formData.amount.toDouble(),
                            type = formData.type
                        )
                        onSave(updatedTransaction)
                    }
                },
                enabled = isValid(formData)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
