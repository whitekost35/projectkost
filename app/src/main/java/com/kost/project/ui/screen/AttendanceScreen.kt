package com.kost.project.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kost.project.R
import com.kost.project.data.model.*
import com.kost.project.ui.components.WorkerBottomNavigation
import com.kost.project.ui.components.WorkerTab
import com.kost.project.ui.util.PdfReportGenerator
import com.kost.project.ui.util.ImageReportGenerator
import com.kost.project.ui.util.ThousandsSeparatorTransformation
import com.kost.project.ui.viewmodel.AttendanceViewModel
import com.kost.project.ui.viewmodel.WorkerWeeklyData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    periodId: String,
    onBack: () -> Unit,
    onNavigateToSalary: () -> Unit = {},
    onNavigateToKasbon: () -> Unit = {},
    viewModel: AttendanceViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val workerData by viewModel.workerData.collectAsState()

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var editingWorkerData by remember { mutableStateOf<WorkerWeeklyData?>(null) }
    
    // Bottom Sheet State
    var selectedForStatus by remember { mutableStateOf<Pair<String, LocalDate>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color(0xFFF8F9FB),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Absensi Pekerja", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold 
                        )
                        Text(
                            "Rekap Absensi Mingguan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Tombol PDF (Hanya Icon)
                    IconButton(
                        onClick = { PdfReportGenerator.generateAttendancePdf(context, weekStart, workerData) }
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
                            ImageReportGenerator.generateAndShareAttendanceImage(
                                context, weekStart, weekDates, workerData
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
                    
                    Surface(
                        onClick = { showAddWorkerDialog = true },
                        modifier = Modifier.padding(horizontal = 8.dp).size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F6EF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Tambah Pekerja",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF34C759)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WorkerBottomNavigation(
                selectedTab = WorkerTab.ABSENSI,
                onTabSelected = { tab ->
                    when (tab) {
                        WorkerTab.GAJIAN -> onNavigateToSalary()
                        WorkerTab.KASBON -> onNavigateToKasbon()
                        else -> {}
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Week Selector
            item {
                ModernWeekSelector(
                    weekStart = weekStart,
                    onPrev = { viewModel.prevWeek() },
                    onNext = { viewModel.nextWeek() }
                )
            }

            // Legend Section
            item {
                AttendanceLegendRow()
            }

            // Summary Card
            item {
                AttendanceSummaryCard(
                    totalUpah = workerData.sumOf { it.weeklyTotal }
                )
            }

            // Modern Attendance Table
            item {
                ModernAttendanceTable(
                    dates = weekDates,
                    workers = workerData,
                    onStatusClick = { workerId, date ->
                        selectedForStatus = workerId to date
                    },
                    onWorkerClick = { editingWorkerData = it }
                )
            }

        }

        // Status Selection Bottom Sheet
        if (selectedForStatus != null) {
            val (workerId, date) = selectedForStatus!!
            val currentStatus = workerData.find { it.worker.id == workerId }
                ?.attendance?.get(date.format(DateTimeFormatter.ISO_LOCAL_DATE)) ?: AttendanceStatus.OFF

            ModalBottomSheet(
                onDismissRequest = { selectedForStatus = null },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                StatusSelectionSheet(
                    workerName = workerData.find { it.worker.id == workerId }?.worker?.name ?: "",
                    date = date,
                    currentStatus = currentStatus,
                    onStatusSelected = { newStatus ->
                        viewModel.updateAttendance(workerId, date, newStatus)
                        selectedForStatus = null
                    }
                )
            }
        }
    }

    if (showAddWorkerDialog) {
        ModernWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onConfirm = { name, role, tarif, lembur ->
                viewModel.addWorker(name, role, tarif)
                showAddWorkerDialog = false
            }
        )
    }

    editingWorkerData?.let { data ->
        ModernWorkerDialog(
            worker = data.worker,
            initialOvertime = data.adjustment?.manualOvertime ?: 0.0,
            onDismiss = { editingWorkerData = null },
            onConfirm = { name, role, tarif, lembur ->
                viewModel.updateWorker(data.worker.copy(name = name, role = role, baseWage = tarif))
                viewModel.updateOvertime(data.worker.id, lembur)
                editingWorkerData = null
            },
            onDelete = {
                viewModel.deleteWorker(data.worker)
                editingWorkerData = null
            }
        )
    }
}

@Composable
fun AttendanceLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(AttendanceStatus.FULL, "Full", "(1 hari)")
        LegendItem(AttendanceStatus.HALF, "½ Hari", "(0,5 hari)")
        LegendItem(AttendanceStatus.LEMBUR_1, "Lembur 1", "(+50k)")
        LegendItem(AttendanceStatus.LEMBUR_2, "Lembur 2", "(+75k)")
        LegendItem(AttendanceStatus.LEMBUR_EKSTRA, "Ekstra", "(+Gaji)")
        LegendItem(AttendanceStatus.OFF, "Libur", "")
        LegendItem(null, "Tidak Absen", "")
    }
}

@Composable
fun LegendItem(status: AttendanceStatus?, label: String, subLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(status = status ?: AttendanceStatus.OFF, isPlaceholder = status == null, size = 20.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (subLabel.isNotEmpty()) {
                Text(subLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AttendanceSummaryCard(totalUpah: Double) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Total Upah Seluruhnya", 
                    style = MaterialTheme.typography.labelLarge, 
                    color = Color(0xFF34A853),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    formatRupiah(totalUpah),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34A853)
                )
            }
        }
    }
}

@Composable
fun ModernAttendanceTable(
    dates: List<LocalDate>,
    workers: List<WorkerWeeklyData>,
    onStatusClick: (String, LocalDate) -> Unit,
    onWorkerClick: (WorkerWeeklyData) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            // Table Header
            Row(
                modifier = Modifier
                    .background(Color(0xFFE3F2FD))
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell("No.", 45.dp)
                TableHeaderCell("Pekerja", 150.dp)
                TableHeaderCell("Upah/Hari", 115.dp)
                
                val dayNames = listOf("MIN", "SEN", "SEL", "RAB", "KAM", "JUM", "SAB")
                dates.forEachIndexed { index, date ->
                    Column(
                        modifier = Modifier.width(65.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dayNames[index], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            date.format(DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                TableHeaderCell("Total Hari", 90.dp)
                TableHeaderCell("Total Upah", 170.dp)
            }

            // Table Rows
            workers.forEachIndexed { index, data ->
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 1.dp)
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // No.
                    Text(
                        (index + 1).toString(),
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    // Pekerja
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onWorkerClick(data) }
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(data.worker.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            data.worker.role.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    // Upah/Hari
                    Text(
                        formatRupiah(data.worker.baseWage),
                        modifier = Modifier.width(115.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    // Attendance Status Icons
                    dates.forEach { date ->
                        val status = data.attendance[date.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: AttendanceStatus.OFF
                        val isNoAbsen = !data.attendance.containsKey(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        
                        Box(
                            modifier = Modifier
                                .width(65.dp)
                                .clickable { onStatusClick(data.worker.id, date) },
                            contentAlignment = Alignment.Center
                        ) {
                            StatusIcon(status = status, isPlaceholder = isNoAbsen)
                        }
                    }

                    // Total Hari
                    Text(
                        if (data.totalDays % 1.0 == 0.0) data.totalDays.toInt().toString() else data.totalDays.toString(),
                        modifier = Modifier.width(90.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Total Upah
                    Text(
                        formatRupiah(data.weeklyTotal),
                        modifier = Modifier.width(170.dp).padding(end = 16.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Table Footer Row (Total Per Hari)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .background(Color(0xFFE3F2FD))
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(310.dp).padding(start = 16.dp)) {
                    Text(
                        "Total Per Hari",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("(Orang)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                dates.forEach { date ->
                    val fractionalCount = workers.sumOf { 
                        when(it.attendance[date.format(DateTimeFormatter.ISO_LOCAL_DATE)]) {
                            AttendanceStatus.FULL, AttendanceStatus.LEMBUR_1, AttendanceStatus.LEMBUR_2 -> 1.0
                            AttendanceStatus.HALF -> 0.5
                            else -> 0.0
                        }
                    }
                    Text(
                        if (fractionalCount % 1.0 == 0.0) fractionalCount.toInt().toString() else fractionalCount.toString(),
                        modifier = Modifier.width(65.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853)
                    )
                }

                // Final Footer Summary (Fixed alignment with columns)
                Row(
                    modifier = Modifier.width(260.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Align with "Total Hari" column (90.dp)
                    Column(
                        modifier = Modifier.width(90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            workers.sumOf { it.totalDays }.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Align with "Total Upah" column (170.dp)
                    Column(
                        modifier = Modifier.width(170.dp).padding(end = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Total Upah", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            formatRupiah(workers.sumOf { it.weeklyTotal }),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34A853),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        modifier = Modifier.width(width),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

@Composable
fun StatusIcon(status: AttendanceStatus, isPlaceholder: Boolean, size: androidx.compose.ui.unit.Dp = 32.dp) {
    if (isPlaceholder) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center
        ) {
            Text("—", color = Color.LightGray, fontWeight = FontWeight.Bold)
        }
        return
    }

    val (bgColor, iconColor, icon) = when (status) {
        AttendanceStatus.FULL -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Rounded.Check)
        AttendanceStatus.HALF -> Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), null)
        AttendanceStatus.LEMBUR_1 -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Rounded.Schedule)
        AttendanceStatus.LEMBUR_2 -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), Icons.Rounded.Schedule)
        AttendanceStatus.LEMBUR_EKSTRA -> Triple(Color(0xFFFFFDE7), Color(0xFFFBC02D), Icons.Rounded.Star)
        AttendanceStatus.OFF -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Rounded.RemoveCircle)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (status == AttendanceStatus.HALF) {
            Text("½", color = iconColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else {
            icon?.let { Icon(it, contentDescription = null, tint = iconColor, modifier = Modifier.size(size * 0.6f)) }
        }
    }
}

@Composable
fun ModernWeekSelector(
    weekStart: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val weekEnd = weekStart.plusDays(6)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp
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
                    weekEnd.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))),
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
fun StatusSelectionSheet(
    workerName: String,
    date: LocalDate,
    currentStatus: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale("id", "ID"))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Status Kehadiran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            "$workerName · ${date.format(formatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        StatusOptionItem(
            label = "Full",
            desc = "Bekerja satu hari penuh",
            status = AttendanceStatus.FULL,
            isSelected = currentStatus == AttendanceStatus.FULL,
            onClick = { onStatusSelected(AttendanceStatus.FULL) }
        )
        StatusOptionItem(
            label = "Half",
            desc = "Bekerja setengah hari",
            status = AttendanceStatus.HALF,
            isSelected = currentStatus == AttendanceStatus.HALF,
            onClick = { onStatusSelected(AttendanceStatus.HALF) }
        )
        StatusOptionItem(
            label = "Lembur 1 (Biru)",
            desc = "Bonus Rp 50.000",
            status = AttendanceStatus.LEMBUR_1,
            isSelected = currentStatus == AttendanceStatus.LEMBUR_1,
            onClick = { onStatusSelected(AttendanceStatus.LEMBUR_1) }
        )
        StatusOptionItem(
            label = "Lembur 2 (Ungu)",
            desc = "Bonus Rp 75.000",
            status = AttendanceStatus.LEMBUR_2,
            isSelected = currentStatus == AttendanceStatus.LEMBUR_2,
            onClick = { onStatusSelected(AttendanceStatus.LEMBUR_2) }
        )
        StatusOptionItem(
            label = "Lembur Ekstra (Emas)",
            desc = "Bonus 1x Gaji Harian",
            status = AttendanceStatus.LEMBUR_EKSTRA,
            isSelected = currentStatus == AttendanceStatus.LEMBUR_EKSTRA,
            onClick = { onStatusSelected(AttendanceStatus.LEMBUR_EKSTRA) }
        )
        StatusOptionItem(
            label = "Off",
            desc = "Tidak bekerja",
            status = AttendanceStatus.OFF,
            isSelected = currentStatus == AttendanceStatus.OFF,
            onClick = { onStatusSelected(AttendanceStatus.OFF) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onStatusSelected(currentStatus) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFFF3B30)
            )
        ) {
            Text("Batal", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusOptionItem(
    label: String,
    desc: String,
    status: AttendanceStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, iconColor, icon) = when (status) {
        AttendanceStatus.FULL -> Triple(Color(0xFFE8F5E9), Color(0xFF34A853), Icons.Rounded.Check)
        AttendanceStatus.HALF -> Triple(Color(0xFFFFF3E0), Color(0xFFF5A623), null)
        AttendanceStatus.LEMBUR_1 -> Triple(Color(0xFFE3F2FD), Color(0xFF007AFF), Icons.Rounded.Schedule)
        AttendanceStatus.LEMBUR_2 -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), Icons.Rounded.Schedule)
        AttendanceStatus.LEMBUR_EKSTRA -> Triple(Color(0xFFFFFDE7), Color(0xFFFBC02D), Icons.Rounded.Star)
        AttendanceStatus.OFF -> Triple(Color(0xFFFFEBEE), Color(0xFF8E8E93), Icons.Rounded.RemoveCircle)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) bgColor.copy(alpha = 0.5f) else Color(0xFFF8F7FA),
        border = if (isSelected) BorderStroke(1.dp, iconColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (status == AttendanceStatus.HALF) {
                        Text("½", color = iconColor, fontWeight = FontWeight.Bold)
                    } else {
                        icon?.let { Icon(it, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun ModernWorkerDialog(
    worker: Worker? = null,
    initialOvertime: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (String, WorkerRole, Double, Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var role by remember { mutableStateOf(worker?.role ?: WorkerRole.TUKANG) }
    var tarif by remember { mutableStateOf(if (worker?.baseWage == 0.0 || worker == null) "" else worker.baseWage.toInt().toString()) }
    var lembur by remember { mutableStateOf(if (initialOvertime == 0.0) "" else initialOvertime.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (worker == null) "Tambah Pekerja" else "Edit Detail Pekerja",
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama Lengkap", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Nama Pekerja") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tarif (Rp)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = tarif,
                            onValueChange = { if (it.all { char -> char.isDigit() }) tarif = it },
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            visualTransformation = ThousandsSeparatorTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            prefix = {
                                Surface(
                                    modifier = Modifier.padding(end = 4.dp),
                                    color = Color(0xFFE8F6EF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Rp", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color(0xFF34C759), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE5E5EA),
                                unfocusedBorderColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lembur (Rp)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = lembur,
                            onValueChange = { if (it.all { char -> char.isDigit() }) lembur = it },
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            visualTransformation = ThousandsSeparatorTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            prefix = {
                                Surface(
                                    modifier = Modifier.padding(end = 4.dp),
                                    color = Color(0xFFE8F6EF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Rp", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = Color(0xFF34C759), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE5E5EA),
                                unfocusedBorderColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                }
                
                Column {
                    Text("Jabatan / Peran", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WorkerRole.entries.forEach { r ->
                            val isSelected = role == r
                            Surface(
                                onClick = { role = r },
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF8F7FA),
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f)) else null
                            ) {
                                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        r.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF007AFF) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                if (worker != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hapus Pekerja")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        name, 
                        role, 
                        tarif.toDoubleOrNull() ?: 0.0,
                        lembur.toDoubleOrNull() ?: 0.0
                    ) 
                },
                enabled = name.isNotBlank(),
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

// Utility functions for formatting
private fun formatRupiah(amount: Double): String {
    val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
