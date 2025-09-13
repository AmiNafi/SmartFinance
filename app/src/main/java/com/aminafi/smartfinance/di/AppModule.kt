package com.aminafi.smartfinance.di

import com.aminafi.smartfinance.*
import com.aminafi.smartfinance.ai.TransactionAIService
import com.aminafi.smartfinance.ai.SimpleTransactionAIService
import com.aminafi.smartfinance.ai.analyzers.*
import com.aminafi.smartfinance.data.repository.TransactionRepository
import com.aminafi.smartfinance.data.repository.TransactionRepositoryImpl
import com.aminafi.smartfinance.domain.usecase.ManageTransactionUseCase
import com.aminafi.smartfinance.domain.usecase.ManageTransactionUseCaseImpl
import com.aminafi.smartfinance.domain.usecase.ProcessAIMessageUseCase
import com.aminafi.smartfinance.domain.usecase.ProcessAIMessageUseCaseImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin dependency injection module for application-level dependencies
 * Provides all the abstractions and their implementations
 * Maintains existing functionality while enabling automatic dependency injection
 */
val appModule = module {

    // Database and DAO providers (singletons)
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().transactionDao() }

    // Repository providers (singletons)
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }

    // Analyzer providers (factories for flexibility)
    factory<AmountExtractorInterface> { AmountExtractor() }
    factory<TransactionTypeAnalyzerInterface> { TransactionTypeAnalyzer() }
    factory<TitleGeneratorInterface> { TitleGenerator() }

    // AI Service provider (singleton)
    single<TransactionAIService> {
        SimpleTransactionAIService(get(), get(), get())
    }

    // Use case providers (singletons)
    single<ProcessAIMessageUseCase> { ProcessAIMessageUseCaseImpl(get()) }
    single<ManageTransactionUseCase> { ManageTransactionUseCaseImpl(get()) }

    // ViewModel provider
    viewModel { FinanceViewModel(get(), get(), get()) }
}
