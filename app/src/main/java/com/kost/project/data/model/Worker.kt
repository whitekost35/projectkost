package com.kost.project.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class WorkerRole(val defaultWage: Double) {
    @SerialName("TUKANG") TUKANG(200000.0),
    @SerialName("KENEK") KENEK(150000.0)
}

@Serializable
@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val role: WorkerRole = WorkerRole.TUKANG,
    @SerialName("base_wage") val baseWage: Double = 0.0
)

@Serializable
enum class AttendanceStatus(val value: Double) {
    @SerialName("FULL") FULL(1.0),
    @SerialName("HALF") HALF(0.5),
    @SerialName("LEMBUR_1") LEMBUR_1(1.0),
    @SerialName("LEMBUR_2") LEMBUR_2(1.0),
    @SerialName("LEMBUREKSTRA") LEMBUR_EKSTRA(1.0),
    @SerialName("OFF") OFF(0.0)
}

@Serializable
@Entity(
    tableName = "attendance_records",
    indices = [androidx.room.Index(value = ["workerId", "date"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("worker_id") val workerId: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val status: AttendanceStatus = AttendanceStatus.OFF
)

@Serializable
@Entity(tableName = "weekly_adjustments")
data class WeeklyAdjustment(
    @PrimaryKey val id: String = "", // workerId + "_" + weekStartDate
    @SerialName("worker_id") val workerId: String = "",
    @SerialName("week_start_date") val weekStartDate: String = "", // Format: YYYY-MM-DD (Monday)
    @SerialName("manual_overtime") val manualOvertime: Double = 0.0,
    @SerialName("private_kasbon") val privateKasbon: Double = 0.0
)

@Serializable
@Entity(tableName = "global_weekly_summaries")
data class GlobalWeeklySummary(
    @PrimaryKey @SerialName("week_start_date") val weekStartDate: String, // Format: YYYY-MM-DD
    @SerialName("global_kasbon") val globalKasbon: Double = 0.0
)

@Serializable
@Entity(tableName = "global_kasbon_entries")
data class GlobalKasbonEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("week_start_date") val weekStartDate: String, // Format: YYYY-MM-DD
    val date: String, // Format: YYYY-MM-DD
    val description: String = "Kasbon Uang makan",
    val amount: Double
)

@Serializable
@Entity(tableName = "private_kasbon_entries")
data class PrivateKasbonEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("worker_id") val workerId: String,
    val date: String, // Format: YYYY-MM-DD
    val description: String = "Pinjam Uang",
    @SerialName("loan_amount") val loanAmount: Double = 0.0,
    @SerialName("payment_amount") val paymentAmount: Double = 0.0,
    val note: String = ""
)
