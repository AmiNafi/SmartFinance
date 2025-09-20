package com.aminafi.smartfinance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.TransactionType

data class TransactionFormData(
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val title: String = "" // For AI confirmation
)

@Composable
fun TransactionForm(
    initialData: TransactionFormData,
    onDataChange: (TransactionFormData) -> Unit,
    modifier: Modifier = Modifier,
    showTitleField: Boolean = false,
    titleLabel: String = "Title"
) {
    var description by remember { mutableStateOf(initialData.description) }
    var amount by remember { mutableStateOf(initialData.amount) }
    var type by remember { mutableStateOf(initialData.type) }
    var title by remember { mutableStateOf(initialData.title) }

    // Update parent when state changes
    LaunchedEffect(description, amount, type, title) {
        onDataChange(TransactionFormData(description, amount, type, title))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (showTitleField) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(titleLabel, style = MaterialTheme.typography.bodyMedium) },
                placeholder = { Text("Transaction title") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Transaction Description", style = MaterialTheme.typography.bodyMedium) },
            placeholder = { Text("e.g., Salary, Coffee, Savings deposit") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount", style = MaterialTheme.typography.bodyMedium) },
            placeholder = { Text("0.00") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Text(
                    text = "৳",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        TransactionTypeSelector(
            selectedType = type,
            onTypeSelected = { type = it }
        )
    }
}

fun isValid(formData: TransactionFormData): Boolean {
    return formData.amount.toDoubleOrNull() != null &&
           formData.amount.toDoubleOrNull()!! > 0 &&
           formData.description.isNotBlank() &&
           (formData.title.isBlank() || formData.title.isNotBlank()) // Title optional unless specified
}

fun isValidForAI(formData: TransactionFormData): Boolean {
    return formData.amount.toDoubleOrNull() != null &&
           formData.amount.toDoubleOrNull()!! > 0 &&
           formData.title.isNotBlank() &&
           formData.description.isNotBlank()
}
