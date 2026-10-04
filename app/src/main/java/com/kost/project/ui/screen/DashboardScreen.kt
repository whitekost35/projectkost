package com.kost.project.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.kost.project.R
import com.kost.project.data.model.ProjectPeriod
import com.kost.project.ui.components.GlassmorphicSurface
import com.kost.project.ui.components.ModernBarChart
import com.kost.project.ui.util.formatRupiah
import com.kost.project.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onPeriodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods by viewModel.periods.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.syncMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val rotation by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )
    
    val activePeriod = periods.find { it.isActive }
    val otherPeriods = periods.filter { !it.isActive }

    var showAddTransactionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
            )
        ),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF2F2F7), // Apple System Gray 6
                            tonalElevation = 1.dp
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.ic_total_spending),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Grand Total Pengeluaran",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatRupiah(grandTotal),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.syncData() }) {
                        Icon(
                            Icons.Rounded.Sync,
                            contentDescription = "Sinkronisasi",
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier
                        )
                    }
                    IconButton(onClick = { showAddTransactionDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "Tambah Cepat",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showAddTransactionDialog) {
            com.kost.project.ui.components.TransactionDialog(
                periods = periods,
                onDismiss = { showAddTransactionDialog = false },
                onSave = { viewModel.addTransaction(it) }
            )
        }
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Chart Section
            Text(
                "Visualisasi Pengeluaran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            GlassmorphicSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                cornerRadius = 28.dp
            ) {
                ModernBarChart(
                    periods = periods,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Active Period Spotlight
            activePeriod?.let {
                Text(
                    "Periode Aktif",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    onClick = { onPeriodClick(it.id) },
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent
                ) {
                    ActivePeriodCard(it)
                }
            }

            // Other Periods Grid
            Text(
                "Periode Lainnya",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                otherPeriods.forEach { period ->
                    Surface(
                        onClick = { onPeriodClick(period.id) },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent
                    ) {
                        OtherPeriodItem(period)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivePeriodCard(period: ProjectPeriod) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(android.graphics.Color.parseColor(period.colorHex)),
                        Color(android.graphics.Color.parseColor(period.colorHex)).copy(alpha = 0.7f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                period.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                period.dateRange,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            formatRupiah(period.totalAmount),
            modifier = Modifier.align(Alignment.BottomEnd),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        val iconData = getIconForPeriod(period.name)
        when (iconData) {
            is Int -> {
                Icon(
                    painter = painterResource(id = iconData),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-30).dp, y = 30.dp)
                )
            }
            is ImageVector -> {
                Icon(
                    imageVector = iconData,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 20.dp)
                )
            }
        }
    }
}

@Composable
fun OtherPeriodItem(period: ProjectPeriod) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconData = getIconForPeriod(period.name)
            val isCustomIcon = iconData is Int

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCustomIcon) Color.Transparent 
                        else Color(android.graphics.Color.parseColor(period.colorHex)).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (iconData) {
                    is Int -> {
                        Icon(
                            painter = painterResource(id = iconData),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    is ImageVector -> {
                        Icon(
                            imageVector = iconData,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(period.colorHex))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    period.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    period.dateRange,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                formatRupiah(period.totalAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun getIconForPeriod(name: String): Any {
    return when {
        name.contains("Periode 1", ignoreCase = true) -> R.drawable.icon_periode_1
        name.contains("Periode 2", ignoreCase = true) -> R.drawable.icon_periode_2
        name.contains("Periode 3", ignoreCase = true) -> R.drawable.icon_periode_3
        name.contains("Periode 4", ignoreCase = true) -> Icons.AutoMirrored.Rounded.TrendingUp
        name.contains("Mykost", ignoreCase = true) -> R.drawable.icon_mykost
        else -> Icons.Rounded.CalendarToday
    }
}
