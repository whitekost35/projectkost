package com.kost.project.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.kost.project.R
import com.kost.project.ui.components.MaterialOrderNoteDialog
import com.kost.project.ui.components.OrderItem
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ChevronRight
import com.kost.project.ui.util.formatRupiah
import com.kost.project.ui.util.ImageReportGenerator
import com.kost.project.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BumimasClassificationScreen(
    periodId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderNoteItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    
    val p4Transactions = transactions.filter { it.periodId == periodId }
    val bumimasBelanjaList = p4Transactions.filter { it.category == "Bumimas_Belanja" }
    val bumimasLunasList = p4Transactions.filter { it.category == "Bumimas_Lunas" }
    
    val bumimasBelanja = bumimasBelanjaList.sumOf { it.amount }
    val bumimasLunas = bumimasLunasList.sumOf { it.amount }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Belanja Matrial Bumimas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            ImageReportGenerator.generateAndShareBumimasImage(
                                context, bumimasBelanjaList, bumimasBelanja, bumimasLunasList, bumimasLunas
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
            // Rincian Belanja Card
            Surface(
                onClick = { onNavigateToDetail("Bumimas_Belanja") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFEBF5FF),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(56.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFF007AFF)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rincian Transaksi Belanja",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            formatRupiah(bumimasBelanja),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            // Rincian Pembayaran Card
            Surface(
                onClick = { onNavigateToDetail("Bumimas_Lunas") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE8F5E9),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(56.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_bumimas_lunas),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rincian Transaksi Pembayaran",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            formatRupiah(bumimasLunas),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sisa Hutang Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sisa Hutang",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        formatRupiah(bumimasBelanja - bumimasLunas),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            IconButton(
                onClick = { showOrderDialog = true },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_list_oreder_matrial_baru),
                    contentDescription = "Catatan Pesanan",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(56.dp)
                )
            }

            if (showOrderDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                MaterialOrderNoteDialog(
                    initialItems = orderNoteItems,
                    onDismiss = { showOrderDialog = false },
                    onSave = { 
                        orderNoteItems = it
                    },
                    onSend = { items ->
                        ImageReportGenerator.generateAndShareOrderImage(context, items)
                        showOrderDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
