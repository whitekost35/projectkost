package com.kost.project.data.repository

import android.util.Log
import com.kost.project.SupabaseClientInstance
import com.kost.project.data.local.WorkerDao
import com.kost.project.data.model.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WorkerRepository(private val workerDao: WorkerDao) {

    // Workers
    fun getAllWorkers(): Flow<List<Worker>> = workerDao.getAllWorkers()

    suspend fun insertWorker(worker: Worker) {
        workerDao.insertWorker(worker)
        safeSync { SupabaseClientInstance.client.from("gondes_workers").upsert(worker) }
    }

    suspend fun updateWorker(worker: Worker) {
        workerDao.updateWorker(worker)
        safeSync { SupabaseClientInstance.client.from("gondes_workers").upsert(worker) }
    }

    suspend fun deleteWorker(worker: Worker) {
        workerDao.deleteWorker(worker)
        safeSync {
            SupabaseClientInstance.client.from("gondes_workers").delete {
                filter { eq("id", worker.id) }
            }
        }
    }

    // Attendance
    fun getAttendanceForRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>> =
        workerDao.getAttendanceForRange(startDate, endDate)

    suspend fun insertAttendance(record: AttendanceRecord) {
        val newId = workerDao.insertAttendance(record)
        safeSync { SupabaseClientInstance.client.from("gondes_attendance").upsert(record.copy(id = newId)) }
    }

    // Weekly Adjustments
    fun getAdjustmentsForWeek(weekStart: String): Flow<List<WeeklyAdjustment>> =
        workerDao.getAdjustmentsForWeek(weekStart)

    suspend fun insertAdjustment(adjustment: WeeklyAdjustment) {
        workerDao.insertAdjustment(adjustment)
        safeSync { SupabaseClientInstance.client.from("gondes_adjustments").upsert(adjustment) }
    }

    // Global Summary
    fun getGlobalSummary(weekStart: String): Flow<GlobalWeeklySummary?> =
        workerDao.getGlobalSummary(weekStart)

    suspend fun insertGlobalSummary(summary: GlobalWeeklySummary) {
        workerDao.insertGlobalSummary(summary)
        // Global summary can be synced to a specific table if needed, for now focusing on requested items
    }

    // Global Kasbon
    fun getGlobalKasbonEntries(weekStart: String): Flow<List<GlobalKasbonEntry>> =
        workerDao.getGlobalKasbonEntries(weekStart)

    suspend fun insertGlobalKasbonEntry(entry: GlobalKasbonEntry) {
        workerDao.insertGlobalKasbonEntry(entry)
        safeSync { SupabaseClientInstance.client.from("gondes_kasbon_global").upsert(entry) }
    }

    suspend fun deleteGlobalKasbonEntry(entry: GlobalKasbonEntry) {
        workerDao.deleteGlobalKasbonEntry(entry)
        safeSync {
            SupabaseClientInstance.client.from("gondes_kasbon_global").delete {
                filter { eq("id", entry.id) }
            }
        }
    }

    // Private Kasbon
    fun getPrivateKasbonEntries(workerId: String): Flow<List<PrivateKasbonEntry>> =
        workerDao.getPrivateKasbonEntries(workerId)

    suspend fun insertPrivateKasbonEntry(entry: PrivateKasbonEntry) {
        workerDao.insertPrivateKasbonEntry(entry)
        safeSync { SupabaseClientInstance.client.from("gondes_kasbon_private").upsert(entry) }
    }

    suspend fun deletePrivateKasbonEntry(entry: PrivateKasbonEntry) {
        workerDao.deletePrivateKasbonEntry(entry)
        safeSync {
            SupabaseClientInstance.client.from("gondes_kasbon_private").delete {
                filter { eq("id", entry.id) }
            }
        }
    }

    suspend fun fetchFromCloud() {
        val client = SupabaseClientInstance.client
        
        // Each sync is independent but will throw if any fails to let VM know
        try {
            val workers = client.from("gondes_workers").select().decodeList<Worker>()
            if (workers.isNotEmpty()) workers.forEach { workerDao.insertWorker(it) }

            val attendance = client.from("gondes_attendance").select().decodeList<AttendanceRecord>()
            if (attendance.isNotEmpty()) workerDao.insertAllAttendance(attendance)

            val adjustments = client.from("gondes_adjustments").select().decodeList<WeeklyAdjustment>()
            if (adjustments.isNotEmpty()) workerDao.insertAllAdjustments(adjustments)

            val globalKasbon = client.from("gondes_kasbon_global").select().decodeList<GlobalKasbonEntry>()
            if (globalKasbon.isNotEmpty()) workerDao.insertAllGlobalKasbon(globalKasbon)

            val privateKasbon = client.from("gondes_kasbon_private").select().decodeList<PrivateKasbonEntry>()
            if (privateKasbon.isNotEmpty()) workerDao.insertAllPrivateKasbon(privateKasbon)
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Fetch failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun pushLocalToCloud() {
        val client = SupabaseClientInstance.client

        // Sync each table independently
        runCatching {
            val data = workerDao.getAllWorkers().first()
            if (data.isNotEmpty()) client.from("gondes_workers").upsert(data)
        }.onFailure { Log.e("SupabaseSync", "Failed to push workers: ${it.message}") }

        runCatching {
            val data = workerDao.getAllAttendance().first()
            if (data.isNotEmpty()) client.from("gondes_attendance").upsert(data)
        }.onFailure { Log.e("SupabaseSync", "Failed to push attendance: ${it.message}") }

        runCatching {
            val data = workerDao.getAllAdjustments().first()
            if (data.isNotEmpty()) client.from("gondes_adjustments").upsert(data)
        }.onFailure { Log.e("SupabaseSync", "Failed to push adjustments: ${it.message}") }

        runCatching {
            val data = workerDao.getAllGlobalKasbon().first()
            if (data.isNotEmpty()) client.from("gondes_kasbon_global").upsert(data)
        }.onFailure { Log.e("SupabaseSync", "Failed to push global kasbon: ${it.message}") }

        runCatching {
            val data = workerDao.getAllPrivateKasbon().first()
            if (data.isNotEmpty()) client.from("gondes_kasbon_private").upsert(data)
        }.onFailure { Log.e("SupabaseSync", "Failed to push private kasbon: ${it.message}") }
    }

    suspend fun getWorkerCount(): Int {
        return workerDao.getAllWorkers().first().size
    }

    // Helper for safe synchronization
    private suspend fun safeSync(block: suspend () -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                block()
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Individual operation failed: ${e.message}", e)
            // Rethrow so caller knows sync failed
            throw e
        }
    }
}
