package com.kost.project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ProjectPeriod(
    val id: String,
    val name: String,
    val dateRange: String,
    val totalAmount: Double,
    val isActive: Boolean = false,
    val colorHex: String
)

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    @SerialName("periodId") val periodId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val category: String? = null,
    val note: String? = null
)
