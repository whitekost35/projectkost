package com.kost.project.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kost.project.ui.components.WorkerBottomNavigation
import com.kost.project.ui.components.WorkerTab
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.kost.project.R
import com.kost.project.ui.util.PdfReportGenerator
import com.kost.project.ui.util.ImageReportGenerator
import com.kost.project.ui.util.ThousandsSeparatorTransformation
import com.kost.project.ui.util.formatRupiah
import com.kost.project.data.model.GlobalKasbonEntry
import com.kost.project.ui.viewmodel.AttendanceViewModel
import com.kost.project.ui.viewmodel.WorkerWeeklyData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySummaryScreen(
    periodId: String,
    onBack: () -> Unit,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToKasbon: () -> Unit = {},
    viewModel: AttendanceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val workerData by viewModel.workerData.collectAsState()
    val globalKasbonEntries by viewModel.globalKasbonEntries.collectAsState()

    val payoutDate = weekStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    val totalUpah = workerData.sumOf { it.weeklyTotal }
    val totalPotongHutang = workerData.sumOf { it.adjustment?.privateKasbon ?: 0.0 }
    val totalKasbonGlobal = globalKasbonEntries.sumOf { it.amount }
    val netGajian = totalUpah - totalKasbonGlobal - totalPotongHutang

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddKasbonDialog by remember { mutableStateOf(false) }

    if (showAddKasbonDialog) {
        var amountText by remember { mutableStateOf("") }
        var selectedDate by remember { mutableStateOf(payoutDate) }
        var showDatePicker by remember { mutableStateOf(false) }
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = java.time.Instant.ofEpochMilli(it)
                                .atZone(java.time.ZoneId.systemDefault())
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

        AlertDialog(
            onDismissRequest = { showAddKasbonDialog = false },
            title = { 
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tambah Kasbon",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Surface(
                        onClick = { showAddKasbonDialog = false },
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
                                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Masukkan Nominal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                            placeholder = { Text("Contoh: 150000") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            prefix = {
                                Surface(
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = Color(0xFFE8F6EF),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Rp", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE5E5EA),
                                unfocusedBorderColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addGlobalKasbonWithTransaction(selectedDate, amount)
                            showAddKasbonDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Kasbon ditambahkan")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F6EF),
                        contentColor = Color(0xFF34A853)
                    )
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddKasbonDialog = false },
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

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Konfirmasi Pembayaran") },
            text = { Text("Gaji minggu ini sudah di bayar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markAsPaid(netGajian, payoutDate)
                    showConfirmDialog = false
                }) {
                    Text("Ya", color = Color(0xFF34A853), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Ringkasan Gajian", 
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
                    IconButton(
                        onClick = { PdfReportGenerator.generateSalaryPdf(context, payoutDate, workerData, globalKasbonEntries) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_pdf),
                            contentDescription = "Export PDF",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }

                    // Tombol Kirim (JPG)
                    IconButton(
                        onClick = { 
                            ImageReportGenerator.generateAndShareSalaryImage(
                                context, payoutDate, workerData, globalKasbonEntries
                            ) 
                        }
                    ) {
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
        bottomBar = {
            WorkerBottomNavigation(
                selectedTab = WorkerTab.GAJIAN,
                onTabSelected = { tab ->
                    when (tab) {
                        WorkerTab.ABSENSI -> onNavigateToAttendance()
                        WorkerTab.KASBON -> onNavigateToKasbon()
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Week Selector
            item {
                ModernWeekSelectorHeader(
                    payoutDate = payoutDate,
                    onPrev = { viewModel.prevWeek() },
                    onNext = { viewModel.nextWeek() }
                )
            }

            // Hero Card
            item {
                PayrollHeroCard(
                    totalGajian = netGajian,
                    workerCount = workerData.size
                )
            }

            // Pekerja Section
            item {
                SectionTitle("Rincian Pekerja")
            }

            items(workerData) { data ->
                PayrollWorkerItem(
                    data = data,
                    onDebtPaymentChange = { viewModel.updateWeeklyDebtPayment(data.worker.id, it) }
                )
            }

            // Potongan Section
            item {
                SectionTitle("Potongan & Kasbon")
            }

            items(globalKasbonEntries) { entry ->
                ModernKasbonItem(
                    entry = entry,
                    onDelete = { viewModel.deleteGlobalKasbonEntry(it) },
                    onUpdate = { viewModel.updateGlobalKasbonEntry(it) }
                )
            }

            item {
                TextButton(
                    onClick = { showAddKasbonDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Kasbon")
                }
            }

            // Final Summary Bar
            item {
                PayrollFinalSummary(
                    totalUpah = totalUpah,
                    totalPotong = totalPotongHutang + totalKasbonGlobal,
                    netGajian = netGajian
                )
            }

            item {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lunas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModernWeekSelectorHeader(
    payoutDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Minggu ini",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    payoutDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun PayrollHeroCard(
    totalGajian: Double,
    workerCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF34A853).copy(alpha = 0.2f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Total Gajian Minggu ini",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatRupiah(totalGajian),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    "$workerCount pekerja · Minggu ini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Surface(
                color = Color(0xFF34A853).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun PayrollWorkerItem(
    data: WorkerWeeklyData,
    onDebtPaymentChange: (Double) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        data.worker.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${data.worker.role.name.lowercase().replaceFirstChar { it.uppercase() }} · ${String.format(Locale.getDefault(), "%.1f", data.totalDays)} hari",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    formatRupiah(data.weeklyTotal),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Potong Hutang",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Inline Numeric Input
                var textValue by remember(data.adjustment?.privateKasbon) {
                    val amount = data.adjustment?.privateKasbon ?: 0.0
                    mutableStateOf(if (amount == 0.0) "" else amount.toLong().toString())
                }
                
                BasicTextField(
                    value = textValue,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            textValue = it
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onDebtPaymentChange(textValue.toDoubleOrNull() ?: 0.0)
                        }
                    ),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (textValue.isEmpty()) Text("Rp 0", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
                            else Row {
                                Text("Rp ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernKasbonItem(
    entry: GlobalKasbonEntry,
    onDelete: (GlobalKasbonEntry) -> Unit,
    onUpdate: (GlobalKasbonEntry) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onDelete(entry) },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                var descValue by remember(entry.description) { mutableStateOf(entry.description) }
                BasicTextField(
                    value = descValue,
                    onValueChange = { 
                        descValue = it
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onUpdate(entry.copy(description = descValue))
                    }),
                    decorationBox = { innerTextField ->
                        if (descValue.isEmpty()) Text("Keterangan", color = Color.LightGray)
                        innerTextField()
                    }
                )
                Text(
                    entry.date.split("-").reversed().joinToString("/"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            var amountValue by remember(entry.amount) {
                mutableStateOf(if (entry.amount == 0.0) "" else entry.amount.toLong().toString())
            }
            BasicTextField(
                value = amountValue,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        amountValue = it
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onUpdate(entry.copy(amount = amountValue.toDoubleOrNull() ?: 0.0))
                }),
                visualTransformation = ThousandsSeparatorTransformation(),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (amountValue.isEmpty()) Text("Rp 0", color = Color.LightGray)
                        else innerTextField()
                    }
                }
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PayrollFinalSummary(
    totalUpah: Double,
    totalPotong: Double,
    netGajian: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF34A853).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Total Upah", 
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatRupiah(totalUpah), 
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Total Potongan", 
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("- ${formatRupiah(totalPotong)}", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gajian Minggu ini", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatRupiah(netGajian),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34A853)
                )
            }
        }
    }
}
