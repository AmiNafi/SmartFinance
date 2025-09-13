package com.aminafi.smartfinance.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Navigation state manager following Single Responsibility Principle
 * Handles only navigation state and screen transitions
 */
class NavigationManager {
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun navigateBack() {
        // For now, always go back to home. Could be enhanced with back stack
        currentScreen = Screen.Home
    }
}

/**
 * Screen enum for navigation
 */
enum class Screen {
    Home,
    ExpenseList,
    IncomeList
}

/**
 * Navigation actions interface for dependency injection
 */
interface NavigationActions {
    fun navigateToHome()
    fun navigateToExpenseList()
    fun navigateToIncomeList()
    fun navigateBack()
}

/**
 * Default implementation of navigation actions
 */
class DefaultNavigationActions(
    private val navigationManager: NavigationManager
) : NavigationActions {

    override fun navigateToHome() {
        navigationManager.navigateTo(Screen.Home)
    }

    override fun navigateToExpenseList() {
        navigationManager.navigateTo(Screen.ExpenseList)
    }

    override fun navigateToIncomeList() {
        navigationManager.navigateTo(Screen.IncomeList)
    }

    override fun navigateBack() {
        navigationManager.navigateBack()
    }
}
