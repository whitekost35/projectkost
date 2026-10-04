package com.kost.project.data.repository

import com.kost.project.data.model.Transaction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ExpenseRepositoryTest {
    private lateinit var repository: ExpenseRepository

    @Before
    fun setup() {
        repository = ExpenseRepository()
    }

    @Test
    fun testInitialGrandTotal() = runBlocking {
        val periods = repository.periods.value
        val grandTotal = periods.sumOf { it.totalAmount }
        assertEquals(3820919377.0, grandTotal, 0.001)
    }

    @Test
    fun testAddTransactionRecalculatesTotal() = runBlocking {
        val initialP4Total = repository.periods.value.find { it.id == "p4" }?.totalAmount ?: 0.0
        val newTransaction = Transaction(
            id = UUID.randomUUID().toString(),
            periodId = "p4",
            title = "Test Expense",
            amount = 1000000.0,
            date = "2026-08-23",
            category = "Testing"
        )
        
        repository.addTransaction(newTransaction)
        
        val updatedP4Total = repository.periods.value.find { it.id == "p4" }?.totalAmount ?: 0.0
        assertEquals(initialP4Total + 1000000.0, updatedP4Total, 0.001)
    }
}
