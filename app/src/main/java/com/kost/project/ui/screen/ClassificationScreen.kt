package com.kost.project.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kost.project.R
import com.kost.project.ui.components.DateRangeFilter
import com.kost.project.ui.util.formatRupiah
import com.kost.project.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationScreen(
    periodId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCategoryClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods by viewModel.periods.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val filterStart by viewModel.filterStartDate.collectAsState()
    val filterEnd by viewModel.filterEndDate.collectAsState()
    
    val period = periods.find { it.id == periodId }
    val p4Transactions = transactions.filter { it.periodId == periodId }

    val bumimasBelanja = p4Transactions.filter { it.category == "Bumimas_Belanja" }.sumOf { it.amount }
    val bumimasLunas = p4Transactions.filter { it.category == "Bumimas_Lunas" }.sumOf { it.amount }
    
    val lainTotal = p4Transactions.filter { it.category == "Lain-lain" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(period?.name ?: "Periode 4", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Integrated Header Card (Matches Dashboard ActivePeriodCard style)
            period?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor(it.colorHex)),
                                    Color(android.graphics.Color.parseColor(it.colorHex)).copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.TopStart)) {
                        Text(
                            it.name,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            it.dateRange,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        formatRupiah(it.totalAmount),
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-20).dp, y = 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Bumimas Card (Always first)
            CategoryCard(
                title = "Belanja Matrial Bumimas",
                iconRes = R.drawable.ic_bumimas,
                content = {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tota Belanja", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(bumimasBelanja), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lunas", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(bumimasLunas), color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sisa Hutang", color = Color(0xFFF44336), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(bumimasBelanja - bumimasLunas), color = Color(0xFFF44336), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                onClick = { onCategoryClick("Bumimas") }
            )

            // 2. Jasa Tukang (Gondes) - Always second
            val gondesTotal = p4Transactions.filter { it.category == "Gondes" }.sumOf { it.amount }
            SimpleCategoryCard(
                title = "Jasa Tukang : Gondes",
                amount = gondesTotal,
                iconRes = R.drawable.ic_gondes,
                onClick = { onCategoryClick("Gondes") }
            )

            // 3. Buang Puing - Always third
            val puingTotal = p4Transactions.filter { it.category == "Puing" }.sumOf { it.amount }
            SimpleCategoryCard(
                title = "Buang Puing",
                amount = puingTotal,
                iconRes = R.drawable.icon_buang_puing,
                onClick = { onCategoryClick("Puing") }
            )

            // 4. Dynamic Custom Categories
            val fixedCategories = listOf("Bumimas_Belanja", "Bumimas_Lunas", "Gondes", "Puing", "Lain-lain")
            val customCategories = p4Transactions
                .mapNotNull { it.category }
                .filter { it !in fixedCategories }
                .distinct()
                .sorted()

            customCategories.forEach { cat ->
                val total = p4Transactions.filter { it.category == cat }.sumOf { it.amount }
                
                // Determine icon based on category name
                if (cat.contains("Kramik", ignoreCase = true)) {
                    SimpleCategoryCard(
                        title = cat,
                        amount = total,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF5474B3)
                            )
                        },
                        onClick = { onCategoryClick(cat) }
                    )
                } else if (cat.contains("AC", ignoreCase = true)) {
                    SimpleCategoryCard(
                        title = cat,
                        amount = total,
                        iconRes = R.drawable.icon_ac_baru,
                        onClick = { onCategoryClick(cat) }
                    )
                } else {
                    SimpleCategoryCard(
                        title = cat,
                        amount = total,
                        iconRes = R.drawable.icon_lain_lain,
                        onClick = { onCategoryClick(cat) }
                    )
                }
            }

            // 5. Other Categories (Lain-lain) - Always bottom
            SimpleCategoryCard(
                title = "Pengeluaran lainnya",
                amount = lainTotal,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF5474B3)
                    )
                },
                onClick = { onCategoryClick("Lain-lain") }
            )

            // Filter: Muncul jika transaksi >= 15 atau sedang ada filter aktif (Pindah ke Bawah)
            if (p4Transactions.size >= 15 || filterStart != null || filterEnd != null) {
                DateRangeFilter(
                    startDate = filterStart,
                    endDate = filterEnd,
                    onRangeSelected = { start, end -> viewModel.updateFilter(start, end) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    iconRes: Int,
    content: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0F5FA),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF5474B3)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SimpleCategoryCard(
    title: String,
    amount: Double,
    iconRes: Int? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F5FA),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        icon()
                    } else if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF5474B3)
                        )
                    }
                }
            }

            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                formatRupiah(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
