package com.aminafi.smartfinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aminafi.smartfinance.ui.theme.SmartFinanceTheme
import com.aminafi.smartfinance.ui.FinanceApp
import org.koin.android.ext.android.get

/**
 * Main Activity following Single Responsibility Principle
 * Handles only Activity lifecycle and high-level composition
 * Delegates UI composition to FinanceApp composable
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartFinanceTheme {
                val viewModel: FinanceViewModel = viewModel { get() }
                FinanceApp(viewModel)
            }
        }
    }
}
