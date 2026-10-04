package com.kost.project.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kost.project.ui.components.DateRangeFilter
import com.kost.project.ui.components.WorkerBottomNavigation
import com.kost.project.ui.components.WorkerTab
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.kost.project.R
import com.kost.project.ui.util.PdfReportGenerator
import com.kost.project.ui.util.ImageReportGenerator
import com.kost.project.ui.util.ThousandsSeparatorTransformation
import com.kost.project.ui.util.formatRupiah
import com.kost.project.data.model.PrivateKasbonEntry
import com.kost.project.data.model.Worker
import com.kost.project.ui.viewmodel.AttendanceViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateKasbonScreen(
    workerId: String?,
    onBack: () -> Unit,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToSalary: () -> Unit = {},
    viewModel: AttendanceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val workerData by viewModel.workerData.collectAsState()
    val selectedWorker = workerData.find { it.worker.id == workerId }?.worker 
        ?: workerData.firstOrNull()?.worker

    val filterStart by viewModel.filterStartDate.collectAsState()
    val filterEnd by viewModel.filterEndDate.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    if (selectedWorker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada data pekerja")
        }
        return
    }

    val entries by viewModel.getPrivateKasbonEntries(selectedWorker.id).collectAsState(initial = emptyList())
    var editingEntry by remember { mutableStateOf<PrivateKasbonEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Kasbon ${selectedWorker.name}", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    // Tombol PDF (Hanya Icon)
                    IconButton(onClick = {
                        PdfReportGenerator.generateKasbonPdf(context, selectedWorker.name, entries)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_pdf),
                            contentDescription = "Ekspor PDF",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }

                    // Tombol Kirim (JPG)
                    IconButton(onClick = {
                        ImageReportGenerator.generateAndShareKasbonImage(context, selectedWorker.name, entries)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_send_file),
                            contentDescription = "Kirim Gambar",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editingEntry = PrivateKasbonEntry(
                        workerId = selectedWorker.id,
                        date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                },
                containerColor = Color(0xFFE8F6EF),
                contentColor = Color(0xFF34C759),
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah", modifier = Modifier.size(32.dp))
            }
        },
        bottomBar = {
            WorkerBottomNavigation(
                selectedTab = WorkerTab.KASBON,
                onTabSelected = { tab ->
                    when (tab) {
                        WorkerTab.ABSENSI -> onNavigateToAttendance()
                        WorkerTab.GAJIAN -> onNavigateToSalary()
                        else -> {}
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card (Teal)
            item {
                val totalHutang = entries.sumOf { it.loanAmount - it.paymentAmount }
                HutangHeroCard(totalHutang)
            }

            item {
                Text(
                    "Riwayat Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Timeline Items
            var currentRunningBalance = 0.0
            items(entries) { entry ->
                currentRunningBalance += entry.loanAmount
                currentRunningBalance -= entry.paymentAmount
                TimelineTransactionItem(
                    entry = entry,
                    runningBalance = currentRunningBalance,
                    onClick = { editingEntry = entry }
                )
            }

            // Filter: Muncul jika transaksi >= 15 atau sedang ada filter aktif (Pindah ke Bawah)
            if (entries.size >= 15 || filterStart != null || filterEnd != null) {
                item {
                    DateRangeFilter(
                        startDate = filterStart,
                        endDate = filterEnd,
                        onRangeSelected = { start, end -> viewModel.updateFilter(start, end) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Edit Dialog
        editingEntry?.let { entry ->
            KasbonEditDialog(
                entry = entry,
                onDismiss = { editingEntry = null },
                onSave = { 
                    viewModel.updatePrivateKasbonEntry(it)
                    editingEntry = null
                },
                onDelete = {
                    viewModel.deletePrivateKasbonEntry(it)
                    editingEntry = null
                }
            )
        }
    }
}

@Composable
fun HutangHeroCard(
    totalHutang: Double
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF167C80), Color(0xFF006A6A))
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "Sisa Hutang",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatRupiah(totalHutang),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Per ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TimelineTransactionItem(
    entry: PrivateKasbonEntry,
    runningBalance: Double,
    onClick: () -> Unit
) {
    val isLoan = entry.loanAmount > 0
    val amount = if (isLoan) entry.loanAmount else entry.paymentAmount
    val color = if (isLoan) Color(0xFF34A853) else Color(0xFFFF3B30)
    val icon = if (isLoan) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
    val bgIcon = if (isLoan) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Metadata
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val date = LocalDate.parse(entry.date)
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    date.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("id", "ID"))),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Icon
            Surface(
                color = bgIcon,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isLoan) "Pinjaman Kasbon" else "Pembayaran",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (entry.note.isNotEmpty()) {
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Hutang: ${formatRupiah(runningBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Amount
            Text(
                text = "${if (isLoan) "+" else "-"} ${formatRupiah(amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonEditDialog(
    entry: PrivateKasbonEntry,
    onDismiss: () -> Unit,
    onSave: (PrivateKasbonEntry) -> Unit,
    onDelete: (PrivateKasbonEntry) -> Unit
) {
    var loanAmount by remember { mutableStateOf(if (entry.loanAmount == 0.0) "" else entry.loanAmount.toInt().toString()) }
    var paymentAmount by remember { mutableStateOf(if (entry.paymentAmount == 0.0) "" else entry.paymentAmount.toInt().toString()) }
    var note by remember { mutableStateOf(entry.note) }
    var date by remember { mutableStateOf(LocalDate.parse(entry.date)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val isNewEntry = entry.loanAmount == 0.0 && entry.paymentAmount == 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isNewEntry) "Tambah Transaksi" else "Edit Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFF2F2F7)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih Tanggal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F6EF)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))))
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pinjaman (Uang Masuk)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = loanAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) loanAmount = it },
                        placeholder = { Text("0") },
                        prefix = {
                            Surface(
                                modifier = Modifier.padding(end = 8.dp),
                                color = Color(0xFFE8F6EF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Rp", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        visualTransformation = ThousandsSeparatorTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pembayaran (Uang Keluar)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) paymentAmount = it },
                        placeholder = { Text("0") },
                        prefix = {
                            Surface(
                                modifier = Modifier.padding(end = 8.dp),
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Rp", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        visualTransformation = ThousandsSeparatorTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Catatan / Keterangan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Tulis catatan di sini...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                if (!isNewEntry) {
                    TextButton(
                        onClick = { 
                            onDelete(entry)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hapus Transaksi")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(entry.copy(
                        loanAmount = loanAmount.toDoubleOrNull() ?: 0.0,
                        paymentAmount = paymentAmount.toDoubleOrNull() ?: 0.0,
                        note = note,
                        date = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8F6EF),
                    contentColor = Color(0xFF34A853)
                )
            ) {
                Text(if (isNewEntry) "Simpan Transaksi" else "Simpan Perubahan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFFF3B30)
                )
            ) {
                Text("Batal", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

private fun formatRupiahKasbon(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
