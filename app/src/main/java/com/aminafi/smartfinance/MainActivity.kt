package com.aminafi.smartfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aminafi.smartfinance.ui.FinanceApp
import com.aminafi.smartfinance.ui.theme.SmartFinanceTheme

/**
 * Main Activity following Single Responsibility Principle
 * Handles only Activity lifecycle and high-level composition
 * Uses Koin for dependency injection while maintaining all existing functionality
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartFinanceTheme {
                FinanceApp()
            }
        }
    }
}
