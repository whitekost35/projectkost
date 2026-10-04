package com.kost.project.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kost.project.R
import com.kost.project.data.model.Transaction
import com.kost.project.ui.components.DateRangeFilter
import com.kost.project.ui.util.PdfReportGenerator
import com.kost.project.ui.util.formatRupiah
import com.kost.project.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    periodId: String,
    category: String? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAttendanceClick: () -> Unit = {},
    onSalarySummaryClick: () -> Unit = {},
    onPrivateKasbonClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val periods by viewModel.periods.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val filterStart by viewModel.filterStartDate.collectAsState()
    val filterEnd by viewModel.filterEndDate.collectAsState()
    
    val period = periods.find { it.id == periodId }
    val filteredTransactions = transactions.filter { tx -> 
        if (tx.periodId != periodId) return@filter false
        category == null || tx.category?.contains(category, ignoreCase = true) == true
    }

    val displayTotal = if (category != null) {
        filteredTransactions.sumOf { it.amount }
    } else {
        period?.totalAmount ?: 0.0
    }
    
    val context = LocalContext.current
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAllTransactions by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8F7FA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (category != null) category.replace("_", " ") else (period?.name ?: "Detail Transaksi"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Kembali", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        PdfReportGenerator.generateTransactionPdf(
                            context = context,
                            reportTitle = if (category != null) category.replace("_", " ") else (period?.name ?: "Detail"),
                            dateRange = period?.dateRange ?: "",
                            transactions = filteredTransactions
                        )
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_pdf),
                            contentDescription = "Export PDF",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFE8F6EF),
                contentColor = Color(0xFF34C759),
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        if (showAddDialog) {
            com.kost.project.ui.components.TransactionDialog(
                periods = periods,
                onDismiss = { showAddDialog = false },
                onSave = { 
                    val tx = if (category != null && it.category == null) it.copy(category = category) else it
                    viewModel.addTransaction(tx) 
                },
                initialCategory = category
            )
        }
        
        selectedTransaction?.let { tx ->
            com.kost.project.ui.components.TransactionDialog(
                transactionToEdit = tx,
                periods = periods,
                onDismiss = { selectedTransaction = null },
                onSave = { viewModel.updateTransaction(it) },
                onDelete = { viewModel.deleteTransaction(it) }
            )
        }

        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CategoryHeroCard(
                    category = if (category != null) category else (period?.name ?: "Total"),
                    amount = displayTotal,
                    dateRange = period?.dateRange ?: ""
                )
            }

            // Section Pekerja (Khusus Gondes)
            if (category == "Gondes") {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            "Akses Cepat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WorkerMenuItem(
                                iconRes = R.drawable.icon_abseni_new,
                                label = "Absensi",
                                onClick = onAttendanceClick,
                                containerColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                                iconColor = Color(0xFF007AFF)
                            )
                            WorkerMenuItem(
                                iconRes = R.drawable.icon_gajian_new,
                                label = "Gajian",
                                onClick = onSalarySummaryClick,
                                containerColor = Color(0xFF34C759).copy(alpha = 0.1f),
                                iconColor = Color(0xFF34C759)
                            )
                            WorkerMenuItem(
                                iconRes = R.drawable.icon_kasbon_new,
                                label = "Kasbon",
                                onClick = onPrivateKasbonClick,
                                containerColor = Color(0xFFFF3B30).copy(alpha = 0.1f),
                                iconColor = Color(0xFFFF3B30)
                            )
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Daftar Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (filteredTransactions.size > 5) {
                        TextButton(onClick = { showAllTransactions = !showAllTransactions }) {
                            Text(
                                if (showAllTransactions) "Sembunyikan" else "Lihat semua",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
            }

            // Transaction List
            val displayedTransactions = if (showAllTransactions) filteredTransactions else filteredTransactions.take(5)
            items(displayedTransactions.size) { index ->
                val transaction = displayedTransactions[index]
                TransactionListItem(
                    transaction = transaction,
                    onClick = { selectedTransaction = transaction }
                )
            }
            
            // Filter: Muncul jika transaksi >= 15 atau sedang ada filter aktif (Pindah ke Bawah)
            if (filteredTransactions.size >= 15 || filterStart != null || filterEnd != null) {
                item {
                    DateRangeFilter(
                        startDate = filterStart,
                        endDate = filterEnd,
                        onRangeSelected = { start, end -> viewModel.updateFilter(start, end) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CategoryHeroCard(category: String, amount: Double, dateRange: String) {
    val (bgBrush, icon, iconBg) = when {
        category == "Bumimas_Belanja" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))),
            Icons.Rounded.TrendingUp,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Bumimas_Lunas" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF34A853), Color(0xFF2E7D32))),
            Icons.Rounded.CheckCircle,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Gondes" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF5856D6), Color(0xFF4845D2))),
            Icons.Rounded.Person,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Puing" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            R.drawable.icon_buang_puing,
            Color.White.copy(alpha = 0.2f)
        )
        category == "AC Baru" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF66BB6A), Color(0xFF388E3C))),
            R.drawable.icon_ac_baru,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Kramik" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFFE6BE8A), Color(0xFFC69C6D))),
            R.drawable.icon_keramik,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Lain-lain" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFFD4AF37), Color(0xFFC5A048))),
            R.drawable.icon_lain_lain,
            Color.White.copy(alpha = 0.2f)
        )
        category == "Periode 1" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))),
            R.drawable.icon_khusus_label_periode_1,
            Color.Transparent
        )
        category == "Periode 2" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF34A853), Color(0xFF2E7D32))),
            R.drawable.icon_khusus_label_periode_2,
            Color.Transparent
        )
        category == "Periode 3" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF00B4D8), Color(0xFF0077B6))),
            R.drawable.icon_khusus_label_periode_3,
            Color.Transparent
        )
        category == "Mykost" -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFFFF9F0A), Color(0xFFFF8E00))),
            R.drawable.icon_khusus_label_periode_mykost,
            Color.Transparent
        )
        else -> Triple(
            Brush.linearGradient(colors = listOf(Color(0xFF0077B6), Color(0xFF023E8A))),
            Icons.Rounded.MoreHoriz,
            Color.White.copy(alpha = 0.2f)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 60.dp)
            ) {
                Text(
                    "Total Pengeluaran",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    formatRupiah(amount),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Surface(
                modifier = Modifier.size(48.dp).align(Alignment.CenterEnd),
                shape = CircleShape,
                color = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (val currentIcon = icon) {
                        is ImageVector -> {
                            Icon(currentIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        is Int -> {
                            val useWhiteTint = category == "AC Baru" || category == "Puing"
                            val isPeriodIcon = category.contains("Periode") || category == "Mykost"
                            Icon(
                                painter = painterResource(id = currentIcon),
                                contentDescription = null,
                                tint = if (useWhiteTint) Color.White else Color.Unspecified,
                                modifier = Modifier.size(if (isPeriodIcon) 44.dp else 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerMenuItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(85.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp // Added shadow for "shadow background" effect
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor), // The tinted background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconRes, iconTint) = when {
                transaction.category == "Bumimas_Belanja" -> R.drawable.icon_bumimas_belanja to Color(0xFF007AFF)
                transaction.category == "Bumimas_Lunas" -> R.drawable.icon_bumimas_lunas to Color(0xFF34A853)
                transaction.category == "Gondes" -> R.drawable.ic_gondes to Color(0xFF5856D6)
                transaction.category == "Puing" -> R.drawable.icon_buang_puing to Color(0xFFF59E0B)
                transaction.category == "AC Baru" -> R.drawable.icon_ac_baru to Color(0xFF4CAF50)
                transaction.category == "Kramik" -> R.drawable.icon_keramik to Color(0xFFC69C6D)
                transaction.category == "Lain-lain" -> R.drawable.icon_lain_lain to Color(0xFFD4AF37)
                transaction.category == "P1" || transaction.periodId == "p1" -> R.drawable.icon_periode_1 to Color(0xFF007AFF)
                transaction.category == "P2" || transaction.periodId == "p2" -> R.drawable.icon_periode_2 to Color(0xFF34A853)
                transaction.category == "P3" || transaction.periodId == "p3" -> R.drawable.icon_periode_3 to Color(0xFF00B4D8)
                transaction.category == "Mykost" || transaction.periodId == "mykost" -> R.drawable.icon_mykost to Color(0xFFFF9F0A)
                else -> R.drawable.ic_others to Color(0xFFB8860B)
            }

            val isCustomIcon = transaction.category in listOf("Bumimas_Belanja", "Bumimas_Lunas", "Puing", "AC Baru", "Kramik", "Lain-lain", "P1", "P2", "P3", "Mykost") || 
                               transaction.periodId in listOf("p1", "p2", "p3", "mykost")
            
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isCustomIcon) Color.Transparent else iconTint.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (isCustomIcon) Color.Unspecified else iconTint,
                        modifier = Modifier.size(if (isCustomIcon) 40.dp else 24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    transaction.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Text(
                formatRupiah(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (transaction.category == "Bumimas_Lunas") Color(0xFF34A853) else Color.Black
            )
        }
    }
}
