package com.aminafi.smartfinance

import android.app.Application
import com.aminafi.smartfinance.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application class with Koin integration for dependency injection
 * Maintains all existing functionality while adding DI framework
 */
class SmartFinanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            androidContext(this@SmartFinanceApplication)
            modules(appModule)
        }

        // Application-level initialization can be added here if needed
        // All existing app functionality remains unchanged
    }
}
