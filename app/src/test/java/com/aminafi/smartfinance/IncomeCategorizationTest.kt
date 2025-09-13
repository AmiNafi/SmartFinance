package com.aminafi.smartfinance

import com.aminafi.smartfinance.ai.SimpleTransactionAIService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify income categorization functionality
 */
class IncomeCategorizationTest {

    private val service = SimpleTransactionAIService()

    @Test
    fun testIncomeCategorization(): Unit = runBlocking {
        // Test the specific case mentioned by user
        val result = service.detectTransaction("income 340")

        assertTrue("Transaction detection should succeed", result.isSuccess)

        result.onSuccess { transaction ->
            assertEquals("Amount should be 340.0", 340.0, transaction.amount, 0.01)
            assertEquals("Type should be INCOME", TransactionType.INCOME, transaction.type)
            assertTrue("Confidence should be high", transaction.confidence > 0.8)
            println("✅ Test PASSED: 'income 340' correctly categorized as INCOME")
        }.onFailure {
            fail("Transaction detection failed: ${it.message}")
        }
    }

    @Test
    fun testExpenseCategorization(): Unit = runBlocking {
        // Test that expense still works
        val result = service.detectTransaction("expense 100")

        assertTrue("Transaction detection should succeed", result.isSuccess)

        result.onSuccess { transaction ->
            assertEquals("Amount should be 100.0", 100.0, transaction.amount, 0.01)
            assertEquals("Type should be EXPENSE", TransactionType.EXPENSE, transaction.type)
            println("✅ Test PASSED: 'expense 100' correctly categorized as EXPENSE")
        }.onFailure {
            fail("Transaction detection failed: ${it.message}")
        }
    }

    @Test
    fun testSalaryIncome(): Unit = runBlocking {
        // Test salary income
        val result = service.detectTransaction("salary 5000")

        assertTrue("Transaction detection should succeed", result.isSuccess)

        result.onSuccess { transaction ->
            assertEquals("Amount should be 5000.0", 5000.0, transaction.amount, 0.01)
            assertEquals("Type should be INCOME", TransactionType.INCOME, transaction.type)
            println("✅ Test PASSED: 'salary 5000' correctly categorized as INCOME")
        }.onFailure {
            fail("Transaction detection failed: ${it.message}")
        }
    }
}
