package com.aminafi.smartfinance.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.TransactionType
import com.aminafi.smartfinance.ui.components.TransactionTypeSelector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    selectedMonth: Int,
    selectedYear: Int
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    val selectedMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth) }.time
    )

    // Modern Modal Bottom Sheet Style Dialog
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Ensure sheet is fully expanded when opened
    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 16.dp,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header with modern styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$selectedMonthName $selectedYear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Field with modern styling
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = {
                    Text(
                        "Transaction Description",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
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

            // Amount Field with modern styling
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = {
                    Text(
                        "Amount",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
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

            // Transaction Type Selection using reusable component
            TransactionTypeSelector(
                selectedType = type,
                onTypeSelected = { type = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Modern Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue != null && description.isNotBlank()) {
                            // Set date to selected month for filtering, entryDate to current date for display
                            val calendar = Calendar.getInstance()
                            calendar.set(selectedYear, selectedMonth, 15, 12, 0, 0) // Mid-month for filtering
                            val transactionDate = calendar.time
                            val entryDate = Date() // Actual entry date

                            val transaction = Transaction(
                                id = System.currentTimeMillis().toString(),
                                amount = amountValue,
                                description = description,
                                type = type,
                                date = transactionDate,
                                entryDate = entryDate
                            )
                            onAddTransaction(transaction)
                            amount = ""
                            description = ""
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = amount.toDoubleOrNull() != null && description.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Add Transaction",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
