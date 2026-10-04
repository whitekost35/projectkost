package com.kost.project.data.local

import androidx.room.*
import com.kost.project.data.model.AttendanceRecord
import com.kost.project.data.model.GlobalKasbonEntry
import com.kost.project.data.model.GlobalWeeklySummary
import com.kost.project.data.model.PrivateKasbonEntry
import com.kost.project.data.model.WeeklyAdjustment
import com.kost.project.data.model.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers")
    fun getAllWorkers(): Flow<List<Worker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    // Attendance
    @Query("SELECT * FROM attendance_records")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    fun getAttendanceForRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(records: List<AttendanceRecord>)

    // Weekly Adjustments
    @Query("SELECT * FROM weekly_adjustments")
    fun getAllAdjustments(): Flow<List<WeeklyAdjustment>>

    @Query("SELECT * FROM weekly_adjustments WHERE weekStartDate = :weekStart")
    fun getAdjustmentsForWeek(weekStart: String): Flow<List<WeeklyAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: WeeklyAdjustment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAdjustments(adjustments: List<WeeklyAdjustment>)

    // Global Summary
    @Query("SELECT * FROM global_weekly_summaries")
    fun getAllGlobalSummaries(): Flow<List<GlobalWeeklySummary>>

    @Query("SELECT * FROM global_weekly_summaries WHERE weekStartDate = :weekStart")
    fun getGlobalSummary(weekStart: String): Flow<GlobalWeeklySummary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalSummary(summary: GlobalWeeklySummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGlobalSummaries(summaries: List<GlobalWeeklySummary>)

    // Flexible Kasbon Entries
    @Query("SELECT * FROM global_kasbon_entries")
    fun getAllGlobalKasbon(): Flow<List<GlobalKasbonEntry>>

    @Query("SELECT * FROM global_kasbon_entries WHERE weekStartDate = :weekStart")
    fun getGlobalKasbonEntries(weekStart: String): Flow<List<GlobalKasbonEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalKasbonEntry(entry: GlobalKasbonEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGlobalKasbon(entries: List<GlobalKasbonEntry>)

    @Delete
    suspend fun deleteGlobalKasbonEntry(entry: GlobalKasbonEntry)

    // Private Kasbon (Ledger)
    @Query("SELECT * FROM private_kasbon_entries")
    fun getAllPrivateKasbon(): Flow<List<PrivateKasbonEntry>>

    @Query("SELECT * FROM private_kasbon_entries WHERE workerId = :workerId ORDER BY date ASC")
    fun getPrivateKasbonEntries(workerId: String): Flow<List<PrivateKasbonEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateKasbonEntry(entry: PrivateKasbonEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPrivateKasbon(entries: List<PrivateKasbonEntry>)

    @Delete
    suspend fun deletePrivateKasbonEntry(entry: PrivateKasbonEntry)

    // Reset Week Data
    @Query("DELETE FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteAttendanceForRange(startDate: String, endDate: String)

    @Query("DELETE FROM weekly_adjustments WHERE weekStartDate = :weekStart")
    suspend fun deleteAdjustmentsForWeek(weekStart: String)

    @Query("DELETE FROM global_kasbon_entries WHERE weekStartDate = :weekStart")
    suspend fun deleteGlobalKasbonForWeek(weekStart: String)
}
