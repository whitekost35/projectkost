package com.kost.project.data.repository

import com.kost.project.data.model.ProjectPeriod
import com.kost.project.data.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ExpenseRepository {
    private val _periods = MutableStateFlow<List<ProjectPeriod>>(emptyList())
    val periods: StateFlow<List<ProjectPeriod>> = _periods.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val csvPath = "C:\\Users\\aidan\\Downloads\\Data Pengeluaran Proyek.csv"
        val (parsedPeriods, parsedTransactions) = com.kost.project.data.util.CsvParser.parse(csvPath)

        if (parsedPeriods.isNotEmpty()) {
            _periods.value = parsedPeriods
            _transactions.value = parsedTransactions
            recalculateTotals()
        } else {
            val p1Id = "p1"
            val p2Id = "p2"
            val p3Id = "p3"
            val p4Id = "p4"
            val mykostId = "mykost"

            val initialPeriods = listOf(
                ProjectPeriod(id = p1Id, name = "Periode 1", dateRange = "06 Agustus 2024 - 20 Februari 2025", totalAmount = 1701598503.0, colorHex = "#4CAF50"),
                ProjectPeriod(id = p2Id, name = "Periode 2", dateRange = "21 Februari 2025 - 05 Juli 2025", totalAmount = 804822694.0, colorHex = "#2196F3"),
                ProjectPeriod(id = p3Id, name = "Periode 3", dateRange = "05 Juli 2025 - 04 April 2026", totalAmount = 583469033.0, colorHex = "#FF9800"),
                ProjectPeriod(id = p4Id, name = "Periode 4 (Saat ini)", dateRange = "13 Mei 2026 - Sekarang", totalAmount = 194888763.0, isActive = true, colorHex = "#E91E63"),
                ProjectPeriod(id = mykostId, name = "Mykost", dateRange = "03 Juli 2025 - 12 Mei 2026", totalAmount = 536140384.0, colorHex = "#9C27B0")
            )

            val initialTransactions = listOf(
                Transaction(id = UUID.randomUUID().toString(), periodId = p1Id, title = "Saldo Awal P1", amount = 1701598503.0, date = "06 Agt 2024"),
                Transaction(id = UUID.randomUUID().toString(), periodId = p2Id, title = "Saldo Awal P2", amount = 804822694.0, date = "21 Feb 2025"),
                Transaction(id = UUID.randomUUID().toString(), periodId = p3Id, title = "Saldo Awal P3", amount = 583469033.0, date = "05 Jul 2025"),
                Transaction(id = UUID.randomUUID().toString(), periodId = p4Id, title = "Saldo Awal P4", amount = 194888763.0, date = "13 Mei 2026"),
                Transaction(id = UUID.randomUUID().toString(), periodId = mykostId, title = "Saldo Awal Mykost", amount = 536140384.0, date = "03 Jul 2025")
            )

            _periods.value = initialPeriods
            _transactions.value = initialTransactions
        }
    }

    fun addTransaction(transaction: Transaction) {
        _transactions.update { it + transaction }
        recalculateTotals()
    }

    fun deleteTransaction(transactionId: String) {
        _transactions.update { it.filterNot { t -> t.id == transactionId } }
        recalculateTotals()
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        _transactions.update { it.map { t -> if (t.id == updatedTransaction.id) updatedTransaction else t } }
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val currentTransactions = _transactions.value
        _periods.update { periods ->
            periods.map { period ->
                val total = currentTransactions.filter { it.periodId == period.id }.sumOf { it.amount }
                period.copy(totalAmount = total)
            }
        }
    }
}
