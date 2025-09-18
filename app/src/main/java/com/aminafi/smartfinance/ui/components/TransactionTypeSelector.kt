package com.aminafi.smartfinance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = "Transaction Type",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = when (selectedType) {
                TransactionType.INCOME -> "💰 Income"
                TransactionType.EXPENSE -> "💸 Expense"
                TransactionType.SAVINGS -> "🏦 Savings"
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💰", style = MaterialTheme.typography.bodyLarge)
                        Text("Income", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                onClick = {
                    onTypeSelected(TransactionType.INCOME)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )

            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💸", style = MaterialTheme.typography.bodyLarge)
                        Text("Expense", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                onClick = {
                    onTypeSelected(TransactionType.EXPENSE)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )

            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🏦", style = MaterialTheme.typography.bodyLarge)
                        Text("Savings", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                onClick = {
                    onTypeSelected(TransactionType.SAVINGS)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }
}
