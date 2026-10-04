package com.kost.project.data.util

import com.kost.project.data.model.ProjectPeriod
import com.kost.project.data.model.Transaction
import java.io.File
import java.util.UUID

object CsvParser {
    fun parse(filePath: String): Pair<List<ProjectPeriod>, List<Transaction>> {
        val periods = mutableListOf<ProjectPeriod>()
        val transactions = mutableListOf<Transaction>()
        
        val file = File(filePath)
        if (!file.exists()) return Pair(emptyList(), emptyList())
        
        val lines = file.readLines()
        var currentPeriodId: String? = null
        var currentPeriodName = ""
        var currentPeriodRange = ""
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            if (trimmedLine.startsWith("PERIODE", ignoreCase = true) || trimmedLine.startsWith("MYKOST", ignoreCase = true)) {
                currentPeriodName = trimmedLine.removeSuffix(";")
                currentPeriodId = UUID.randomUUID().toString()
            } else if (trimmedLine.contains("Hingga", ignoreCase = true) || (trimmedLine.contains("-") && trimmedLine.length < 50)) {
                currentPeriodRange = trimmedLine.removeSuffix(";")
                
                currentPeriodId?.let { id ->
                    periods.add(ProjectPeriod(
                        id = id,
                        name = currentPeriodName,
                        dateRange = currentPeriodRange,
                        totalAmount = 0.0,
                        colorHex = getRandomColorHex(currentPeriodName)
                    ))
                }
            } else if (trimmedLine.contains("Rp")) {
                val parts = trimmedLine.split(";")
                if (parts.size >= 2) {
                    val title = parts[0].trim()
                    val amountStr = parts[1].trim()
                        .replace("Rp", "")
                        .replace(".", "")
                        .replace(",", "")
                        .trim()
                    
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    
                    if (!title.contains("Total uang sudah keluar", ignoreCase = true)) {
                        currentPeriodId?.let { id ->
                            transactions.add(Transaction(
                                id = UUID.randomUUID().toString(),
                                periodId = id,
                                title = title,
                                amount = amount,
                                date = "CSV Date"
                            ))
                        }
                    }
                }
            }
        }
        
        if (periods.none { it.name.contains("Periode 4", ignoreCase = true) }) {
             val p4Id = UUID.randomUUID().toString()
             periods.add(ProjectPeriod(
                 id = p4Id,
                 name = "Periode 4 (Saat ini)",
                 dateRange = "Mei 2026 - Sekarang",
                 totalAmount = 0.0,
                 isActive = true,
                 colorHex = "#E91E63"
             ))
        } else {
            val last = periods.find { it.name.contains("Periode 4", ignoreCase = true) } 
                ?: periods.lastOrNull()
            
            if (last != null) {
                val index = periods.indexOf(last)
                periods[index] = last.copy(isActive = true)
            }
        }
        
        return Pair(periods, transactions)
    }
    
    private fun getRandomColorHex(name: String): String {
        return when {
            name.contains("1") -> "#4CAF50"
            name.contains("2") -> "#2196F3"
            name.contains("3") -> "#FF9800"
            name.contains("Mykost") -> "#9C27B0"
            else -> "#607D8B"
        }
    }
}
