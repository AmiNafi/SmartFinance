// Re-export dialog functions from separate files for backward compatibility
// This file now serves as a facade to maintain existing imports

import androidx.compose.runtime.Composable
import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.ai.AIDetectedTransaction

// Re-export AddTransactionDialog
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAddTransaction: (Transaction) -> Unit,
    selectedMonth: Int,
    selectedYear: Int
) = com.aminafi.smartfinance.ui.dialogs.AddTransactionDialog(
    onDismiss = onDismiss,
    onAddTransaction = onAddTransaction,
    selectedMonth = selectedMonth,
    selectedYear = selectedYear
)

// Re-export EditTransactionDialog
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit
) = com.aminafi.smartfinance.ui.dialogs.EditTransactionDialog(
    transaction = transaction,
    onDismiss = onDismiss,
    onSave = onSave,
    onDelete = onDelete
)

// Re-export AIConfirmationDialog
@Composable
fun AIConfirmationDialog(
    pendingTransaction: AIDetectedTransaction?,
    onDismiss: () -> Unit,
    onConfirm: (AIDetectedTransaction) -> Unit
) = com.aminafi.smartfinance.ui.dialogs.AIConfirmationDialog(
    pendingTransaction = pendingTransaction,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)
