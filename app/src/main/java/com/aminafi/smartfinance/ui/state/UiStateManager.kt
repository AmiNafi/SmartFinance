package com.aminafi.smartfinance.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aminafi.smartfinance.Transaction
import com.aminafi.smartfinance.ai.AIDetectedTransaction

/**
 * UI State Manager following Single Responsibility Principle
 * Manages only UI-related state (dialogs, forms, etc.)
 */
class UiStateManager {

    // Dialog states
    var showAddTransactionDialog by mutableStateOf(false)
        private set

    var showEditTransactionDialog by mutableStateOf(false)
        private set

    var showAIConfirmationDialog by mutableStateOf(false)
        private set

    // Transaction states
    var editingTransaction by mutableStateOf<Transaction?>(null)
        private set

    var pendingAITransaction by mutableStateOf<AIDetectedTransaction?>(null)
        private set

    // Dialog actions
    fun showAddTransactionDialog(transaction: Transaction) {
        editingTransaction = transaction
        showAddTransactionDialog = true
    }

    fun hideAddTransactionDialog() {
        showAddTransactionDialog = false
        editingTransaction = null
    }

    fun showEditTransactionDialog(transaction: Transaction) {
        editingTransaction = transaction
        showEditTransactionDialog = true
    }

    fun hideEditTransactionDialog() {
        showEditTransactionDialog = false
        editingTransaction = null
    }

    fun showAIConfirmationDialog(transaction: AIDetectedTransaction) {
        pendingAITransaction = transaction
        showAIConfirmationDialog = true
    }

    fun hideAIConfirmationDialog() {
        showAIConfirmationDialog = false
        pendingAITransaction = null
    }
}

/**
 * UI Actions interface for dependency injection
 */
interface UiActions {
    fun showAddTransactionDialog(transaction: Transaction)
    fun hideAddTransactionDialog()
    fun showEditTransactionDialog(transaction: Transaction)
    fun hideEditTransactionDialog()
    fun showAIConfirmationDialog(transaction: AIDetectedTransaction)
    fun hideAIConfirmationDialog()
}

/**
 * Default implementation of UI actions
 */
class DefaultUiActions(
    private val uiStateManager: UiStateManager
) : UiActions {

    override fun showAddTransactionDialog(transaction: Transaction) {
        uiStateManager.showAddTransactionDialog(transaction)
    }

    override fun hideAddTransactionDialog() {
        uiStateManager.hideAddTransactionDialog()
    }

    override fun showEditTransactionDialog(transaction: Transaction) {
        uiStateManager.showEditTransactionDialog(transaction)
    }

    override fun hideEditTransactionDialog() {
        uiStateManager.hideEditTransactionDialog()
    }

    override fun showAIConfirmationDialog(transaction: AIDetectedTransaction) {
        uiStateManager.showAIConfirmationDialog(transaction)
    }

    override fun hideAIConfirmationDialog() {
        uiStateManager.hideAIConfirmationDialog()
    }
}
