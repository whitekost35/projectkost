package com.kost.project.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kost.project.data.local.AppDatabase
import com.kost.project.data.model.*
import com.kost.project.data.repository.TransactionRepository
import com.kost.project.data.repository.WorkerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class WorkerWeeklyData(
    val worker: Worker,
    val attendance: Map<String, AttendanceStatus>, // Date -> Status
    val adjustment: WeeklyAdjustment?
) {
    val totalDays: Double
        get() = attendance.values.sumOf { it.value }

    val baseWageTotal: Double
        get() = totalDays * worker.baseWage
        
    private val overtimeBonus: Double
        get() {
            val countLembur1 = attendance.values.count { it == AttendanceStatus.LEMBUR_1 }
            val countLembur2 = attendance.values.count { it == AttendanceStatus.LEMBUR_2 }
            val countLemburEkstra = attendance.values.count { it == AttendanceStatus.LEMBUR_EKSTRA }
            return (countLembur1 * 50000.0) + (countLembur2 * 75000.0) + (countLemburEkstra * worker.baseWage)
        }

    val weeklyTotal: Double
        get() = baseWageTotal + overtimeBonus + (adjustment?.manualOvertime ?: 0.0)

    val netWeeklyTotal: Double
        get() = weeklyTotal - (adjustment?.privateKasbon ?: 0.0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val workerDao = db.workerDao()
    private val workerRepository = WorkerRepository(workerDao)
    private val transactionRepository = TransactionRepository(db.transactionDao())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Filter States
    private val _filterStartDate = MutableStateFlow<LocalDate?>(null)
    val filterStartDate: StateFlow<LocalDate?> = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<LocalDate?>(null)
    val filterEndDate: StateFlow<LocalDate?> = _filterEndDate.asStateFlow()

    fun updateFilter(start: LocalDate?, end: LocalDate?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    val weekDates: StateFlow<List<LocalDate>> = _currentWeekStart.map { start ->
        (0..6).map { start.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val globalSummary: StateFlow<GlobalWeeklySummary?> = _currentWeekStart.flatMapLatest { start ->
        workerRepository.getGlobalSummary(start.format(dateFormatter))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val globalKasbonEntries: StateFlow<List<GlobalKasbonEntry>> = _currentWeekStart.flatMapLatest { start ->
        workerRepository.getGlobalKasbonEntries(start.format(dateFormatter))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerData: StateFlow<List<WorkerWeeklyData>> = combine(
        workerRepository.getAllWorkers(),
        _currentWeekStart.flatMapLatest { start ->
            val end = start.plusDays(6)
            workerRepository.getAttendanceForRange(start.format(dateFormatter), end.format(dateFormatter))
        },
        _currentWeekStart.flatMapLatest { start ->
            workerRepository.getAdjustmentsForWeek(start.format(dateFormatter))
        }
    ) { workers, attendance, adjustments ->
        workers.map { worker ->
            val workerAttendance = attendance.filter { it.workerId == worker.id }
                .associate { it.date to it.status }
            val adjustment = adjustments.find { it.workerId == worker.id }
            WorkerWeeklyData(worker, workerAttendance, adjustment)
        }.sortedWith(
            compareBy<WorkerWeeklyData> { 
                if (it.worker.name.equals("GONDES", ignoreCase = true)) 0 else 1 
            }.thenBy { 
                when (it.worker.role) {
                    WorkerRole.TUKANG -> 0
                    WorkerRole.KENEK -> 1
                }
            }.thenBy { it.worker.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncData()
        checkAndInitializeWorkers()
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // 1. Fetch from Cloud
                workerRepository.fetchFromCloud()
                
                // 2. Check if we have workers, if not initialize
                checkAndInitializeWorkersSync()

                // 3. Push Local to Cloud
                workerRepository.pushLocalToCloud()
                
                _syncMessage.emit("Sinkronisasi Cloud Berhasil")
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Sync failed: ${e.message}", e)
                _syncMessage.emit("Gagal Sinkronisasi: ${e.message ?: "Periksa Koneksi"}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun checkAndInitializeWorkersSync() {
        val workers = workerRepository.getAllWorkers().first()
        if (workers.isEmpty()) {
            val initialWorkers = listOf(
                Worker(name = "GONDES", role = WorkerRole.TUKANG, baseWage = WorkerRole.TUKANG.defaultWage),
                Worker(name = "ERWIN", role = WorkerRole.TUKANG, baseWage = WorkerRole.TUKANG.defaultWage),
                Worker(name = "HENDRIK", role = WorkerRole.TUKANG, baseWage = WorkerRole.TUKANG.defaultWage),
                Worker(name = "ADI", role = WorkerRole.KENEK, baseWage = WorkerRole.KENEK.defaultWage),
                Worker(name = "IWAN", role = WorkerRole.KENEK, baseWage = WorkerRole.KENEK.defaultWage)
            )
            initialWorkers.forEach { w -> workerRepository.insertWorker(w) }
        }
    }

    private fun checkAndInitializeWorkers() {
        viewModelScope.launch {
            workerRepository.getAllWorkers().first().let {
                if (it.isEmpty()) {
                    val initialWorkers = listOf(
                        Worker(name = "GONDES", role = WorkerRole.TUKANG),
                        Worker(name = "ERWIN", role = WorkerRole.TUKANG),
                        Worker(name = "HENDRIK", role = WorkerRole.TUKANG),
                        Worker(name = "ADI", role = WorkerRole.KENEK),
                        Worker(name = "IWAN", role = WorkerRole.KENEK)
                    )
                    initialWorkers.forEach { w -> workerRepository.insertWorker(w) }
                }
            }
        }
    }

    fun updateAttendance(workerId: String, date: LocalDate, status: AttendanceStatus) {
        viewModelScope.launch {
            try {
                workerRepository.insertAttendance(
                    AttendanceRecord(
                        workerId = workerId,
                        date = date.format(dateFormatter),
                        status = status
                    )
                )
            } catch (e: Exception) {
                Log.e("AttendanceVM", "Gagal update absen: ${e.message}")
                _syncMessage.emit("Gagal simpan absen ke cloud")
            }
        }
    }

    fun updateOvertime(workerId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val weekStart = _currentWeekStart.value.format(dateFormatter)
                val existing = workerData.value.find { it.worker.id == workerId }?.adjustment
                workerRepository.insertAdjustment(
                    (existing ?: WeeklyAdjustment(id = "${workerId}_$weekStart", workerId = workerId, weekStartDate = weekStart))
                        .copy(manualOvertime = amount)
                )
            } catch (e: Exception) {
                _syncMessage.emit("Gagal simpan lembur ke cloud")
            }
        }
    }

    fun updateWeeklyDebtPayment(workerId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val weekStart = _currentWeekStart.value.format(dateFormatter)
                val weekEnd = _currentWeekStart.value.plusDays(6).format(dateFormatter)
                val existing = workerData.value.find { it.worker.id == workerId }?.adjustment
                
                // 1. Update WeeklyAdjustment
                workerRepository.insertAdjustment(
                    (existing ?: WeeklyAdjustment(id = "${workerId}_$weekStart", workerId = workerId, weekStartDate = weekStart))
                        .copy(privateKasbon = amount)
                )

                // 2. Sync to PrivateKasbonEntry
                val autoId = "auto_${workerId}_$weekStart"
                workerRepository.insertPrivateKasbonEntry(
                    PrivateKasbonEntry(
                        id = autoId,
                        workerId = workerId,
                        date = weekEnd,
                        description = "Bayar hutang pribadi",
                        paymentAmount = amount,
                        note = if (amount == 0.0) "Tidak bayar" else ""
                    )
                )
            } catch (e: Exception) {
                _syncMessage.emit("Gagal simpan bayar hutang ke cloud")
            }
        }
    }

    fun updateGlobalKasbon(amount: Double) {
        viewModelScope.launch {
            try {
                val weekStart = _currentWeekStart.value.format(dateFormatter)
                workerRepository.insertGlobalSummary(
                    GlobalWeeklySummary(weekStartDate = weekStart, globalKasbon = amount)
                )
            } catch (e: Exception) {
                _syncMessage.emit("Gagal update ringkasan")
            }
        }
    }

    fun addGlobalKasbonEntry(date: LocalDate, amount: Double) {
        viewModelScope.launch {
            try {
                val weekStart = _currentWeekStart.value.format(dateFormatter)
                workerRepository.insertGlobalKasbonEntry(
                    GlobalKasbonEntry(
                        weekStartDate = weekStart,
                        date = date.format(dateFormatter),
                        amount = amount
                    )
                )
            } catch (e: Exception) {
                _syncMessage.emit("Gagal simpan kasbon")
            }
        }
    }

    fun addGlobalKasbonWithTransaction(date: LocalDate, amount: Double, description: String = "Kasbon uang makan") {
        viewModelScope.launch {
            try {
                val weekStart = _currentWeekStart.value.format(dateFormatter)
                val entry = GlobalKasbonEntry(
                    weekStartDate = weekStart,
                    date = date.format(dateFormatter),
                    description = description,
                    amount = amount
                )
                
                // 1. Insert into Kasbon Ledger
                workerRepository.insertGlobalKasbonEntry(entry)
                
                // 2. Create and insert Transaction
                val transaction = Transaction(
                    id = "p4_gd_kasbon_${System.currentTimeMillis()}",
                    periodId = "p4",
                    title = description,
                    amount = amount,
                    date = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    category = "Gondes",
                    note = "Kasbon Global"
                )
                transactionRepository.insert(transaction)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal simpan kasbon")
            }
        }
    }

    fun updateGlobalKasbonEntry(entry: GlobalKasbonEntry) {
        viewModelScope.launch {
            try {
                workerRepository.insertGlobalKasbonEntry(entry)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal update kasbon")
            }
        }
    }

    fun deleteGlobalKasbonEntry(entry: GlobalKasbonEntry) {
        viewModelScope.launch {
            try {
                workerRepository.deleteGlobalKasbonEntry(entry)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal hapus kasbon")
            }
        }
    }

    // Private Kasbon (Ledger)
    fun getPrivateKasbonEntries(workerId: String): Flow<List<PrivateKasbonEntry>> {
        return combine(
            workerRepository.getPrivateKasbonEntries(workerId),
            _filterStartDate,
            _filterEndDate
        ) { entries, start, end ->
            entries.filter { entry ->
                val date = try { LocalDate.parse(entry.date) } catch (e: Exception) { null }
                val matchesStart = start == null || date == null || !date.isBefore(start)
                val matchesEnd = end == null || date == null || !date.isAfter(end)
                matchesStart && matchesEnd
            }.sortedByDescending { it.date }
        }
    }

    fun addPrivateKasbonEntry(workerId: String, date: LocalDate) {
        viewModelScope.launch {
            try {
                workerRepository.insertPrivateKasbonEntry(
                    PrivateKasbonEntry(
                        workerId = workerId,
                        date = date.format(dateFormatter)
                    )
                )
            } catch (e: Exception) {
                _syncMessage.emit("Gagal tambah kasbon")
            }
        }
    }

    fun updatePrivateKasbonEntry(entry: PrivateKasbonEntry) {
        viewModelScope.launch {
            try {
                workerRepository.insertPrivateKasbonEntry(entry)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal update kasbon")
            }
        }
    }

    fun deletePrivateKasbonEntry(entry: PrivateKasbonEntry) {
        viewModelScope.launch {
            try {
                workerRepository.deletePrivateKasbonEntry(entry)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal hapus kasbon")
            }
        }
    }

    fun addWorker(name: String, role: WorkerRole, baseWage: Double) {
        viewModelScope.launch {
            try {
                workerRepository.insertWorker(Worker(name = name, role = role, baseWage = baseWage))
            } catch (e: Exception) {
                _syncMessage.emit("Gagal tambah pekerja")
            }
        }
    }
    
    fun updateWorker(worker: Worker) {
        viewModelScope.launch {
            try {
                workerRepository.updateWorker(worker)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal update pekerja")
            }
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            try {
                workerRepository.deleteWorker(worker)
            } catch (e: Exception) {
                _syncMessage.emit("Gagal hapus pekerja")
            }
        }
    }

    fun nextWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plusWeeks(1)
    }

    fun prevWeek() {
        _currentWeekStart.value = _currentWeekStart.value.minusWeeks(1)
    }

    fun markAsPaid(amount: Double, payoutDate: LocalDate) {
        viewModelScope.launch {
            val start = _currentWeekStart.value
            val weekStartStr = start.format(dateFormatter)
            val weekEndStr = start.plusDays(6).format(dateFormatter)

            // 1. Create Transaction
            val transaction = Transaction(
                id = "p4_gd_paid_${System.currentTimeMillis()}",
                periodId = "p4",
                title = "Upah Gondes",
                amount = amount,
                date = payoutDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                category = "Gondes",
                note = "Gajian minggu $weekStartStr s/d $weekEndStr"
            )

            // 2. Save Transaction
            transactionRepository.insert(transaction)

            // 3. Reset Data for the week
            workerDao.deleteAttendanceForRange(weekStartStr, weekEndStr)
            workerDao.deleteAdjustmentsForWeek(weekStartStr)
            workerDao.deleteGlobalKasbonForWeek(weekStartStr)
        }
    }
}
