package com.kost.project.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kost.project.data.local.AppDatabase
import com.kost.project.data.model.ProjectPeriod
import com.kost.project.data.model.Transaction
import com.kost.project.data.repository.TransactionRepository
import com.kost.project.data.repository.WorkerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(db.transactionDao())
    private val workerRepository = WorkerRepository(db.workerDao())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    // Filter States
    private val _filterStartDate = MutableStateFlow<LocalDate?>(null)
    val filterStartDate: StateFlow<LocalDate?> = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<LocalDate?>(null)
    val filterEndDate: StateFlow<LocalDate?> = _filterEndDate.asStateFlow()

    fun updateFilter(start: LocalDate?, end: LocalDate?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    // StateFlow yang mengamati Room Database sebagai source of truth dengan Sorting dan Filtering
    val transactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _filterStartDate,
        _filterEndDate
    ) { txs, start, end ->
        txs.filter { tx ->
            val date = parseTransactionDate(tx.date)
            val matchesStart = start == null || date == null || !date.isBefore(start)
            val matchesEnd = end == null || date == null || !date.isAfter(end)
            matchesStart && matchesEnd
        }.sortedWith(compareByDescending { parseTransactionDate(it.date) ?: LocalDate.MIN })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun parseTransactionDate(dateStr: String): LocalDate? {
        return try {
            if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    val day = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year = parts[2]
                    LocalDate.parse("$year-$month-$day", DateTimeFormatter.ISO_LOCAL_DATE)
                } else null
            } else {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            }
        } catch (e: Exception) {
            null
        }
    }

    private val _periodDefinitions = MutableStateFlow(
        listOf(
            ProjectPeriod(id = "p1", name = "Periode 1", dateRange = "06 Agt 2024 - 20 Feb 2025", totalAmount = 0.0, colorHex = "#007AFF"),
            ProjectPeriod(id = "p2", name = "Periode 2", dateRange = "21 Feb - 05 Jul 2025", totalAmount = 0.0, colorHex = "#34A853"),
            ProjectPeriod(id = "p3", name = "Periode 3", dateRange = "05 Jul 2025 - 04 Apr 2026", totalAmount = 0.0, colorHex = "#00B4D8"),
            ProjectPeriod(id = "p4", name = "Periode 4", dateRange = "13 Mei 2026 - Saat Ini", totalAmount = 0.0, isActive = true, colorHex = "#0077B6"),
            ProjectPeriod(id = "mykost", name = "Mykost", dateRange = "03 Jul 2025 - 12 Mei 2026", totalAmount = 0.0, colorHex = "#FF9F0A")
        )
    )

    val periods: StateFlow<List<ProjectPeriod>> = combine(transactions, _periodDefinitions) { txs, defs ->
        defs.map { period ->
            // Menghitung total pengeluaran per periode.
            // Transaksi "Bumimas_Belanja" dikecualikan karena itu adalah pencatatan hutang material,
            // sedangkan "Bumimas_Lunas" adalah pembayaran aktual yang mengurangi saldo/pengeluaran nyata.
            val total = txs.filter { it.periodId == period.id && it.category != "Bumimas_Belanja" }
                .sumOf { it.amount }
            period.copy(totalAmount = total)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val grandTotal: StateFlow<Double> = periods.map { list ->
        list.sumOf { it.totalAmount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        syncData()
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // 1. Sync Transactions (Existing)
                repository.fetchFromCloud()
                if (repository.getCount() == 0) {
                    val initialData = getAllInitialTransactions()
                    repository.insertAll(initialData)
                } else {
                    repository.pushLocalToCloud()
                }

                // 2. Sync Worker Data (New)
                workerRepository.fetchFromCloud()
                workerRepository.pushLocalToCloud()

                _syncMessage.emit("Semua Data Berhasil Disinkronkan")
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Global Sync failed: ${e.message}", e)
                _syncMessage.emit("Gagal Sinkronisasi: Periksa Koneksi Internet")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun updateTransaction(updatedTx: Transaction) {
        viewModelScope.launch {
            repository.update(updatedTx)
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.delete(transactionId)
        }
    }

    private fun getAllInitialTransactions(): List<Transaction> {
        val txs = mutableListOf<Transaction>()
        
        // --- PERIODE 1 ---
        txs.add(Transaction("p1_1", "p1", "JAMAL", 490000000.0, "P1"))
        txs.add(Transaction("p1_2", "p1", "RENDI", 128100000.0, "P1"))
        txs.add(Transaction("p1_3", "p1", "TAUL : INSTALASI LISTRIK", 1500000.0, "P1"))
        txs.add(Transaction("p1_4", "p1", "DIPAN", 11200000.0, "P1"))
        txs.add(Transaction("p1_5", "p1", "JAELANI : INSTALASI LISTRIK PLN", 5500000.0, "P1"))
        txs.add(Transaction("p1_6", "p1", "Kwh PLN", 29421192.0, "P1"))
        txs.add(Transaction("p1_7", "p1", "AGUNG : Perbaiki listrik mati lampu", 1620000.0, "P1"))
        txs.add(Transaction("p1_8", "p1", "MUI : BOR SUMUR BARU", 9000000.0, "P1"))
        txs.add(Transaction("p1_9", "p1", "DARTO : Gali Septiktank", 4000000.0, "P1"))
        txs.add(Transaction("p1_10", "p1", "ROBI : ANGKUT PUING", 19500000.0, "P1"))
        txs.add(Transaction("p1_11", "p1", "Kurir Toko Bangunan : Uang Tip", 600000.0, "P1"))
        txs.add(Transaction("p1_12", "p1", "TOKO BANGUNAN : SENTEBANG", 102126900.0, "P1"))
        txs.add(Transaction("p1_13", "p1", "TOKO BANGUNAN : BUMI MAS", 479656000.0, "P1"))
        txs.add(Transaction("p1_14", "p1", "SINAR BARU JAYA KRAMIK", 68087000.0, "P1"))
        txs.add(Transaction("p1_15", "p1", "BELANJA LANGSUNG RENDI", 21452569.0, "P1"))
        txs.add(Transaction("p1_16", "p1", "BELI MESIN MOLEN", 6500000.0, "P1"))
        txs.add(Transaction("p1_17", "p1", "BELI MESIN POMPA SUBMERSIBLE", 3200000.0, "P1"))
        txs.add(Transaction("p1_18", "p1", "PINTU ALUMUNIUM", 50000000.0, "P1"))
        txs.add(Transaction("p1_19", "p1", "PINTU BAJA", 30000000.0, "P1"))
        txs.add(Transaction("p1_20", "p1", "PVC PLAFON", 26526000.0, "P1"))
        txs.add(Transaction("p1_21", "p1", "LAMPU INBOW LED 12 Watt & 9 Watt", 4131500.0, "P1"))
        txs.add(Transaction("p1_22", "p1", "PAGAR TRALIS BESI", 47300000.0, "P1"))
        txs.add(Transaction("p1_23", "p1", "Saklar, Stop kontak, MCB", 11121342.0, "P1"))
        txs.add(Transaction("p1_24", "p1", "AC : 13 Unit", 34450000.0, "P1"))
        txs.add(Transaction("p1_25", "p1", "Pasang AC 650.000 x 13 unit", 8450000.0, "P1"))
        txs.add(Transaction("p1_26", "p1", "Water Heater : 15 Unit", 18750000.0, "P1"))
        txs.add(Transaction("p1_27", "p1", "Keran Mix Water heater : 15 pcs", 1500000.0, "P1"))
        txs.add(Transaction("p1_28", "p1", "Pasang Water Heater 15 unit", 750000.0, "P1"))
        txs.add(Transaction("p1_29", "p1", "Shower Mandi : 24 Set", 1392000.0, "P1"))
        txs.add(Transaction("p1_30", "p1", "Meja : 24 pcs", 6914000.0, "P1"))
        txs.add(Transaction("p1_31", "p1", "Bangku : 15 pcs", 3735000.0, "P1"))
        txs.add(Transaction("p1_32", "p1", "Lemari : 24 pcs", 24000000.0, "P1"))
        txs.add(Transaction("p1_33", "p1", "Kasur : 12 Unit", 9468000.0, "P1"))
        txs.add(Transaction("p1_34", "p1", "Sprei : 15 Set", 1000000.0, "P1"))
        txs.add(Transaction("p1_35", "p1", "Bantal & Guling : 15 set", 1000000.0, "P1"))
        txs.add(Transaction("p1_36", "p1", "Tp link modem induk 2 pcs", 1900000.0, "P1"))
        txs.add(Transaction("p1_37", "p1", "Tp link modem Anak 14 pcs", 5600000.0, "P1"))
        txs.add(Transaction("p1_38", "p1", "Makan, Minum, kopi Tukang (199 Hari)", 15947000.0, "P1"))
        txs.add(Transaction("p1_39", "p1", "Makan Bersama Seafood 69", 2700000.0, "P1"))

        // --- PERIODE 2 ---
        txs.add(Transaction("p2_1", "p2", "Jasa Tukang dan Rendi", 337423000.0, "P2"))
        txs.add(Transaction("p2_2", "p2", "THR Tukang 13 x 100.000/Orang", 1300000.0, "P2"))
        txs.add(Transaction("p2_3", "p2", "Taul : Kasbon", 8000000.0, "P2"))
        txs.add(Transaction("p2_4", "p2", "Taul : Jasa pasang AC 8 Unit", 5560000.0, "P2"))
        txs.add(Transaction("p2_5", "p2", "ROBI : ANGKUT PUING", 11700000.0, "P2"))
        txs.add(Transaction("p2_6", "p2", "TOKO BANGUNAN : BUMI MAS", 348994700.0, "P2"))
        txs.add(Transaction("p2_7", "p2", "Kasur 5 Pcs 3.485 + Ongkir 150", 3995000.0, "P2"))
        txs.add(Transaction("p2_8", "p2", "Kasur 3 pcs 2.367 + Ongkir 150", 2517000.0, "P2"))
        txs.add(Transaction("p2_9", "p2", "Kasur 7 pcs", 5523000.0, "P2"))
        txs.add(Transaction("p2_10", "p2", "Dipan Besi putih 11 pcs", 10989000.0, "P2"))
        txs.add(Transaction("p2_11", "p2", "Kursi lipat 9 pcs", 1075305.0, "P2"))
        txs.add(Transaction("p2_12", "p2", "Water heater midea terbaru 8 unit", 10571200.0, "P2"))
        txs.add(Transaction("p2_13", "p2", "Mesin Bor baterai APR 34Pro", 499000.0, "P2"))
        txs.add(Transaction("p2_14", "p2", "Mesin Gerinda Makita M90", 469000.0, "P2"))
        txs.add(Transaction("p2_15", "p2", "List Siku Profil motif kayu 20 batang", 725000.0, "P2"))
        txs.add(Transaction("p2_16", "p2", "AC Sharp 9 Unit x 2.650.000/unit", 24100000.0, "P2"))
        txs.add(Transaction("p2_17", "p2", "Kramik WC 3D 35 Dus", 2187500.0, "P2"))
        txs.add(Transaction("p2_18", "p2", "Modem Wifi 4 Unit x 300.000", 1200000.0, "P2"))
        txs.add(Transaction("p2_19", "p2", "Keran mix water heater 8pcs", 644000.0, "P2"))
        txs.add(Transaction("p2_20", "p2", "8 set shower", 382500.0, "P2"))
        txs.add(Transaction("p2_21", "p2", "Bantal guling 8 set", 480000.0, "P2"))
        txs.add(Transaction("p2_22", "p2", "Sprei 8 set", 400000.0, "P2"))
        txs.add(Transaction("p2_23", "p2", "Cermin 10 pcs", 850000.0, "P2"))
        txs.add(Transaction("p2_24", "p2", "Arifin Pagar depan", 10000000.0, "P2"))
        txs.add(Transaction("p2_25", "p2", "Philips lampu Inbow 14watt", 791644.0, "P2"))
        txs.add(Transaction("p2_26", "p2", "RENDI / ENDI Belanja Langsung", 5254845.0, "P2"))
        txs.add(Transaction("p2_27", "p2", "Makan Bersama Perpisahan : 24/03/25", 1000000.0, "P2"))
        txs.add(Transaction("p2_28", "p2", "Makan, Minum, kopi Tukang ( 120 Hari )", 8191000.0, "P2"))

        // --- PERIODE 3 ---
        txs.add(Transaction("p3_1", "p3", "Jasa Tukang & Rendi", 104600000.0, "P3"))
        txs.add(Transaction("p3_2", "p3", "Gondes", 104020000.0, "P3"))
        txs.add(Transaction("p3_3", "p3", "ROBI : ANGKUT PUING", 2400000.0, "P3"))
        txs.add(Transaction("p3_4", "p3", "TOKO BANGUNAN : BUMI MAS", 202662500.0, "P3"))
        txs.add(Transaction("p3_5", "p3", "Kramik putih & Granit", 8118600.0, "P3"))
        txs.add(Transaction("p3_6", "p3", "Kramik ( 13/09/2025 )", 6250000.0, "P3"))
        txs.add(Transaction("p3_7", "p3", "Dipan Besi putih 3 Unit", 3000000.0, "P3"))
        txs.add(Transaction("p3_8", "p3", "Dipan Besi putih 18 Unit", 18000000.0, "P3"))
        txs.add(Transaction("p3_9", "p3", "AC Sharp 3 Unit : D9, D10, D11", 7950000.0, "P3"))
        txs.add(Transaction("p3_10", "p3", "Lemari dan kasur D9", 1500000.0, "P3"))
        txs.add(Transaction("p3_11", "p3", "WPC Wall Panel Profil 25 batang", 1747500.0, "P3"))
        txs.add(Transaction("p3_12", "p3", "List PVC", 400000.0, "P3"))
        txs.add(Transaction("p3_13", "p3", "Lampu LED", 205000.0, "P3"))
        txs.add(Transaction("p3_14", "p3", "Taul", 5000000.0, "P3"))
        txs.add(Transaction("p3_15", "p3", "Belanja Online 20 - 29 September", 2950000.0, "P3"))
        txs.add(Transaction("p3_16", "p3", "Belanja Online 1 - 8 Okt", 6064000.0, "P3"))
        txs.add(Transaction("p3_17", "p3", "Belanja Online 10-14 Okt", 2527000.0, "P3"))
        txs.add(Transaction("p3_18", "p3", "Belanja Online 14 Okt - 14 Desember", 11325000.0, "P3"))
        txs.add(Transaction("p3_19", "p3", "MARTINUS", 85302433.0, "P3"))
        txs.add(Transaction("p3_20", "p3", "ENDI Belanja Langsung", 4965000.0, "P3"))
        txs.add(Transaction("p3_21", "p3", "Makan, Minum, kopi Tukang ( 35 Hari )", 4482000.0, "P3"))

        // --- MYKOST ---
        txs.add(Transaction("mk_1", "mykost", "Gondes", 139625000.0, "Mykost"))
        txs.add(Transaction("mk_2", "mykost", "Taul Jasa Listrik", 18480000.0, "Mykost"))
        txs.add(Transaction("mk_3", "mykost", "Syamsul ( Jasa PLN )", 1000000.0, "Mykost"))
        txs.add(Transaction("mk_4", "mykost", "Buang Puing", 9900000.0, "Mykost"))
        txs.add(Transaction("mk_5", "mykost", "Toko Matrial", 183257000.0, "Mykost"))
        txs.add(Transaction("mk_6", "mykost", "Kompor Gas Set", 604505.0, "Mykost"))
        txs.add(Transaction("mk_7", "mykost", "Karung 1500pcs", 1500000.0, "Mykost"))
        txs.add(Transaction("mk_8", "mykost", "Saklar dan stop kontak 60pcs", 1212000.0, "Mykost"))
        txs.add(Transaction("mk_9", "mykost", "Lampu bohlanp 12pcs", 275200.0, "Mykost"))
        txs.add(Transaction("mk_10", "mykost", "Philips lampu Inbow plafon 40pcs", 2000000.0, "Mykost"))
        txs.add(Transaction("mk_11", "mykost", "Gerobak besi + Ongkir", 1678000.0, "Mykost"))
        txs.add(Transaction("mk_12", "mykost", "Tabung Gas + Isi", 480000.0, "Mykost"))
        txs.add(Transaction("mk_13", "mykost", "Kramik 08/07/2025", 21425000.0, "Mykost"))
        txs.add(Transaction("mk_14", "mykost", "Kramik 15/08/2025", 2950000.0, "Mykost"))
        txs.add(Transaction("mk_15", "mykost", "Kramik 03/10/2025", 6500000.0, "Mykost"))
        txs.add(Transaction("mk_16", "mykost", "Pintu alumunium ( Rahmadi)", 38700000.0, "Mykost"))
        txs.add(Transaction("mk_17", "mykost", "Mesin Bor", 621000.0, "Mykost"))
        txs.add(Transaction("mk_18", "mykost", "Mesin Drill Makita", 1147500.0, "Mykost"))
        txs.add(Transaction("mk_19", "mykost", "Kabel ( Taul )", 4600000.0, "Mykost"))
        txs.add(Transaction("mk_20", "mykost", "Token Listrik", 580000.0, "Mykost"))
        txs.add(Transaction("mk_21", "mykost", "PVC ( Martinus )", 44787000.0, "Mykost"))
        txs.add(Transaction("mk_22", "mykost", "Sealent 2 Box isi 48", 1055000.0, "Mykost"))
        txs.add(Transaction("mk_23", "mykost", "Meteran Listrik", 10040000.0, "Mykost"))
        txs.add(Transaction("mk_24", "mykost", "Belanja Online 20 - 23 agustus", 9500000.0, "Mykost"))
        txs.add(Transaction("mk_25", "mykost", "Belanja Online 24/08 - 5/09", 11618780.0, "Mykost"))
        txs.add(Transaction("mk_26", "mykost", "Belanja Online 05 - 20 September", 8370000.0, "Mykost"))
        txs.add(Transaction("mk_27", "mykost", "Belanja Online 20 - 29 September", 4955399.0, "Mykost"))
        txs.add(Transaction("mk_28", "mykost", "Belanja Online 20 - 29 Oktober", 1529000.0, "Mykost"))
        txs.add(Transaction("mk_29", "mykost", "Belanja Online 1 Nov - 13 Des", 7250000.0, "Mykost"))
        txs.add(Transaction("mk_30", "mykost", "Floordrain, saklar, spray gun dll", 500000.0, "Mykost"))

        txs.addAll(getPeriode4Transactions())

        return txs
    }

    private fun getPeriode4Transactions(): List<Transaction> {
        val txs = mutableListOf<Transaction>()
        val csvContent = """
            ;MATRIAL BUMIMAS;;;JASA TUKANG : GONDES;;;;Biaya Buang Puing;;;Lain-lain;;
            ;Tanggal ;Harga;;Tanggaal ;Di Bayar;;;Tanggaal ;Lunas;;Tanggaal ;Kebutuhan; Harga 
            ;08/05/2026; Rp151.000 ;;12/05/2026; Rp500.000 ;;;31/05/2026; Rp300.000 ;;03/07/2026;Gondes kasbon; Rp100.000 
            ;12/05/2026; Rp2.417.000 ;;14/05/2026; Rp300.000 ;;;03/07/2026; Rp600.000 ;;05/07/2026;Gondes uang lembur cor balok 4 orang; Rp200.000 
            ;15/05/2026; Rp690.000 ;;16/05/2026; Rp500.000 ;;;28/07/2026; Rp300.000 ;;06/07/2026;Es kelapa dan gorengan tukang; Rp75.000 
            ;15/05/2026; Rp90.000 ;;16/05/2026; Rp1.240.000 ;;;05/08/2026; Rp300.000 ;;06/07/2026;Galon tukang; Rp43.000 
            ;16/05/2026; Rp300.000 ;;18/05/2026; Rp1.000.000 ;;;;;;06/07/2026;Nasi padang 6 orang; Rp96.000 
            ;17/05/2026; Rp3.180.000 ;;23/05/2026; Rp1.425.000 ;;;;;;07/07/2026;Handle pintu D11; Rp62.000 
            ;17/05/2026; Rp186.000 ;;24/05/2026; Rp300.000 ;;;;;;07/07/2026;Silinder kunci Nainai Soligen; Rp95.000 
            ;18/05/2026; Rp1.316.000 ;;25/05/2026; Rp3.000.000 ;;;;;;09/07/2026;Philips lampu downlight 3 dus isi 12 pcs; Rp601.500 
            ;18/05/2026; Rp695.000 ;;30/05/2026; Rp2.600.000 ;;;;;;10/07/2026;Prostex 1 Liter; Rp24.600 
            ;18/05/2026; Rp220.000 ;;01/06/2026; Rp1.000.000 ;;;; Rp1.500.000 ;;11/07/2026;Air minum tukang 3 galon; Rp21.000 
            ;19/05/2026; Rp1.503.500 ;;02/06/2026; Rp1.000.000 ;;;;;;13/07/2026;Galon Aqua tukang 2 galon; Rp44.000 
            ;20/05/2026; Rp5.060.000 ;;06/06/2026; Rp2.700.000 ;;;;;;14/07/2026;Sealant Netral 24 botol; Rp573.000 
            ;22/05/2026; Rp150.000 ;;09/06/2026; Rp1.000.000 ;;;;;;16/07/2026;MCB saklar kelistrikan D8; Rp260.000 
            ;22/05/2026; Rp1.942.000 ;;11/06/2026; Rp300.000 ;;;;;;17/07/2026;Galon air minum tukang; Rp21.000 
            ;23/05/2026; Rp2.774.000 ;;13/06/2026; Rp2.350.000 ;;;;;;17/07/2026;Kasbon Gondes; Rp300.000 
            ;27/05/2026; Rp2.452.000 ;;16/06/2026; Rp1.000.000 ;;;;;;18/07/2026;Tip kurir toko bangunan; Rp100.000 
            ;27/05/2026; Rp170.000 ;;18/06/2026; Rp300.000 ;;;;;;20/07/2026;Galon tukang 2 pcs + galon kung 1 pcs; Rp36.000 
            ;28/05/2026; Rp2.230.000 ;;20/06/2026; Rp1.050.000 ;;;;;;20/07/2026;Token listrik kamar C4 : tukang; Rp50.000 
            ;30/05/2026; Rp534.000 ;;21/06/2026; Rp200.000 ;;;;;;22/07/2026;Listrik kamar C5; Rp20.825 
            ;31/05/2026; Rp1.118.000 ;;22/06/2026; Rp1.000.000 ;;;;;;23/07/2026;Floor drain A3 dan D9; Rp54.294 
            ;01/06/2026; Rp1.280.000 ;;27/06/2026; Rp3.500.000 ;;;;;;24/07/2026;Galon tukang 4 pcs; Rp28.000 
            ;04/06/2026; Rp880.000 ;;29/06/2026; Rp1.200.000 ;;;;;;24/07/2026;Gergaji besi dan spidol; Rp35.000 
            ;05/06/2026; Rp2.340.000 ;;02/07/2026; Rp500.000 ;;;;;;25/07/2026;Selang Flexible Washtafel; Rp32.200 
            ;05/06/2026; Rp2.340.000 ;;03/07/2026; Rp100.000 ;;;;;;25/07/2026;Sticker kaca Blur; Rp35.389 
            ;08/06/2026; Rp543.500 ;;04/07/2026; Rp3.150.000 ;;;Logistik;;25/07/2026;Belanja sanitary D8; Rp365.000 
            ;08/06/2026; Rp3.670.000 ;;06/07/2026; Rp2.000.000 ;;;;;;26/07/2026;Selang Shower 2 pcs; Rp36.810 
            ;09/06/2026; Rp3.968.000 ;;08/07/2026; Rp500.000 ;;;;;;27/07/2026;Sharp AC kamar D8; Rp3.088.645 
            ;10/06/2026; Rp100.000 ;;11/07/2026; Rp4.475.000 ;;;;;;27/07/2026;Skrup gypsum; Rp175.000 
            ;10/06/2026; Rp346.000 ;;14/07/2026; Rp2.000.000 ;;;;;;27/07/2026;Prostex Jumbo 4 Liter; Rp79.500 
            ;11/06/2026; Rp2.552.000 ;;17/07/2026; Rp300.000 ;;;;;;31/07/2026;Gondes kasbon; Rp200.000 
            ;13/06/2026; Rp700.000 ;;18/07/2026; Rp3.500.000 ;;;;;;01/08/2026;Galon tukang 2 pcs + galon kung 1 pcs; Rp36.000 
            ;15/06/2026; Rp961.000 ;;19/07/2026; Rp300.000 ;;;;;;02/08/2026;Gorengan dan Es Kelapa; Rp100.000 
            ;16/06/2026; Rp499.000 ;;20/07/2026; Rp2.000.000 ;;;;;;02/08/2026;Gondes Kasbon; Rp300.000 
            ;17/06/2026; Rp1.545.000 ;;23/07/2026; Rp200.000 ;;;;;;;;
            ;18/06/2026; Rp600.000 ;;25/07/2026; Rp3.600.000 ;;;;;;Total;; Rp7.288.763 
            ;19/06/2026; Rp132.000 ;;26/07/2026; Rp300.000 ;;;;;;;;
            ;19/06/2026; Rp55.000 ;;28/07/2026; Rp2.000.000 ;;;;;;;;
            ;20/06/2026; Rp925.000 ;;31/07/2026; Rp300.000 ;;;;;;;;
            ;21/06/2026; Rp1.748.000 ;;01/08/2026; Rp3.250.000 ;;;;;;;;
            ;21/06/2026; Rp4.000.000 ;;01/08/2026; Rp500.000 ;;;;;;;;
            ;23/06/2026; Rp202.000 ;;02/08/2026; Rp300.000 ;;;;;;;;
            ;25/06/2026; Rp1.736.000 ;;03/08/2026; Rp2.000.000 ;;;;;;;;
            ;25/06/2026; Rp1.456.000 ;;05/08/2026; Rp500.000 ;;;;;;;;
            ;26/06/2026; Rp462.000 ;;08/08/2026; Rp3.850.000 ;;;;;;;;
            ;27/06/2026; Rp220.000 ;;09/08/2026; Rp300.000 ;;;;;;;;
            ;28/06/2026; Rp2.178.000 ;;10/08/2026; Rp2.000.000 ;;;;;;;;
            ;29/06/2026; Rp814.000 ;;;;;;;;;;;
            ;29/06/2026; Rp90.000 ;;;;;;;;;;;
            ;01/07/2026; Rp3.632.000 ;;;;;;;;;;;
            ;02/07/2026; Rp550.000 ;;;;;;;;;;;
            ;03/07/2026; Rp6.345.000 ;;;;;;;;;;;
            Bayar;;;;Total; Rp65.390.000 ;;;;;;;;
            ;04/07/2026; Rp651.000 ;;;;;;;;;;;
            ;04/07/2026; Rp6.420.000 ;;;;;;;;;;;
            ;06/07/2026; Rp46.000 ;;;;;;;;;;;
            ;06/07/2026; Rp700.000 ;;;;;;;;;;;
            ;07/07/2026; Rp3.118.000 ;;;;;;;;;;;
            ;07/07/2026; Rp134.000 ;;;;;;;;;;;
            ;07/07/2026; Rp220.000 ;;;;;;;;;;;
            ;08/07/2026; Rp4.144.000 ;;;;;;;;;;;
            ;09/07/2026; Rp2.145.000 ;;;;;;;;;;;
            ;09/07/2026; Rp1.885.000 ;;;;;;;;;;;
            ;09/07/2026; Rp55.000 ;;;;;;;;;;;
            ;10/07/2026; Rp227.000 ;;;;;;;;;;;
            ;10/07/2026; Rp50.000 ;;;;;;;;;;;
            ;11/07/2026; Rp1.310.000 ;;;;;;;;;;;
            ;12/07/2026; Rp1.152.000 ;;;;;;;;;;;
            ;13/07/2026; Rp696.000 ;;;;;;;;;;;
            ;14/07/2026; Rp660.000 ;;;;;;;;;;;
            ;14/07/2026; Rp100.000 ;;;;;;;;;;;
            ;14/07/2026; Rp945.000 ;;;;;;;;;;;
            ;15/07/2026; Rp869.000 ;;;;;;;;;;;
            ;15/07/2026; Rp167.000 ;;;;;;;;;;;
            ;16/07/2026; Rp5.362.000 ;;;;;;;;;;;
            ;17/07/2026; Rp6.000.000 ;;;;;;;;;;;
            ;17/07/2026; Rp5.590.000 ;;;;;;;;;;;
            ;18/07/2026; Rp7.731.000 ;;;;;;;;;;;
            ;18/07/2026; Rp800.000 ;;;;;;;;;;;
            ;20/07/2026; Rp1.463.000 ;;;;;;;;;;;
            ;21/07/2026; Rp648.000 ;;;;;;;;;;;
            ;21/07/2026; Rp1.100.000 ;;;;;;;;;;;
            ;23/07/2026; Rp36.000 ;;;;;;;;;;;
            ;24/07/2026; Rp117.000 ;;;;;;;;;;;
            ;25/07/2026; Rp925.000 ;;;;;;;;;;;
            ;25/07/2026; Rp115.000 ;;;;;;;;;;;
            ;25/07/2026; Rp90.000 ;;;;;;;;;;;
            ;26/07/2026; Rp281.500 ;;;;;;;;;;;
            ;28/07/2026; Rp1.835.000 ;;;;;;;;;;;
            ;29/07/2026; Rp450.000 ;;;;;;;;;;;
            ;30/07/2026; Rp135.000 ;;;;;;;;;;;
            Bayar;30/07/2026; Rp1.660.000 ;;;;;;;;;;;
            ;;;;;;;;;;;;;
            ;31/07/2026; Rp7.070.000 ;;;;;;;;;;;
            ;31/07/2026; Rp650.000 ;;;;;;;;;;;
            ;01/08/2026; Rp674.000 ;;;;;;;;;;;
            ;02/08/2026; Rp3.884.000 ;;;;;;;;;;;
            ;02/08/2026; Rp11.657.000 ;;;;;;;;;;;
            ;03/08/2026; Rp5.540.000 ;;;;;;;;;;;
            ;04/08/2026; Rp203.000 ;;;;;;;;;;;
            ;04/08/2026; Rp1.400.000 ;;;;;;;;;;;
            ;06/08/2026; Rp4.795.000 ;;;;;;;;;;;
            ;07/08/2026; Rp2.540.000 ;;;;;;;;;;;
            ;08/08/2026; Rp2.950.000 ;;;;;;;;;;;
            Bayar;10/08/2026; Rp2.468.000 ;;;;;;;;;;;
            ;;;;;;;;;;;;;
            ;Total Belanja; Rp183.959.500 ;;;;;;;;;;;
            ;Lunas 04 Juli 2026; Rp40.000.000 ;;;;;;;;;;;
            ;Lunas 30 Juli 2026; Rp30.000.000 ;;;;;;;;;;;
            ;Lunas 10 Agustus 2026; Rp40.000.000 ;;;;;;;;;;;
            ;Sisa Hutang; Rp73.959.500 ;;;;;;;;;;;
        """.trimIndent()

        val lines = csvContent.lines()
        
        fun parseRp(str: String): Double {
            return str.replace("Rp", "").replace(".", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
        }

        lines.forEachIndexed { index, line ->
            if (index < 2) return@forEachIndexed
            val parts = line.split(";")
            
            if (parts.size > 2) {
                val date = parts.getOrNull(1)?.trim() ?: ""
                val priceStr = parts.getOrNull(2)?.trim() ?: ""
                if (date.isNotEmpty() && priceStr.startsWith("Rp")) {
                    txs.add(Transaction("p4_bm_$index", "p4", "Belanja Bumimas", parseRp(priceStr), date, "Bumimas_Belanja"))
                }
            }
            
            if (line.contains("Lunas") && line.contains("Rp")) {
                val title = parts.getOrNull(1)?.trim() ?: "Lunas Bumimas"
                val amount = parseRp(parts.getOrNull(2) ?: "")
                txs.add(Transaction("p4_bm_pay_$index", "p4", title, amount, "Bayar", "Bumimas_Lunas"))
            }

            if (parts.size > 5) {
                val date = parts.getOrNull(4)?.trim() ?: ""
                val amountStr = parts.getOrNull(5)?.trim() ?: ""
                if (date.isNotEmpty() && amountStr.startsWith("Rp")) {
                    txs.add(Transaction("p4_gd_$index", "p4", "Upah Gondes", parseRp(amountStr), date, "Gondes"))
                }
            }

            if (parts.size > 9) {
                val date = parts.getOrNull(8)?.trim() ?: ""
                val amountStr = parts.getOrNull(9)?.trim() ?: ""
                if (date.isNotEmpty() && amountStr.startsWith("Rp")) {
                    txs.add(Transaction("p4_pg_$index", "p4", "Buang Puing", parseRp(amountStr), date, "Puing"))
                }
            }

            if (parts.size > 12) {
                val date = parts.getOrNull(11)?.trim() ?: ""
                val kebutuhan = parts.getOrNull(12)?.trim() ?: ""
                val amountStr = parts.getOrNull(13)?.trim() ?: ""
                if (date.isNotEmpty() && amountStr.startsWith("Rp")) {
                    txs.add(Transaction("p4_ll_$index", "p4", kebutuhan, parseRp(amountStr), date, "Lain-lain", null))
                }
            }
        }
        
        return txs
    }
}
