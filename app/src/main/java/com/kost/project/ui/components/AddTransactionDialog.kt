package com.kost.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kost.project.data.model.ProjectPeriod
import com.kost.project.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    periods: List<ProjectPeriod>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPeriodId by remember { mutableStateOf(periods.find { it.isActive }?.id ?: periods.firstOrNull()?.id ?: "") }
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tambah Transaksi Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(36.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFFF2F2F7)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", modifier = Modifier.size(20.dp), tint = Color.Gray)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Judul Transaksi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Contoh: Pembelian Semen") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA)
                        )
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jumlah Nominal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        placeholder = { Text("0") },
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih Periode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    periods.forEach { period ->
                        val isSelected = selectedPeriodId == period.id
                        Surface(
                            onClick = { selectedPeriodId = period.id },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF8F7FA),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPeriodId = period.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(period.name, fontWeight = FontWeight.Bold)
                                    Text(period.dateRange, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && amount.isNotBlank() && selectedPeriodId.isNotBlank()) {
                        onSave(
                            Transaction(
                                id = UUID.randomUUID().toString(),
                                periodId = selectedPeriodId,
                                title = title,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                date = dateFormat.format(Date())
                            )
                        )
                        onDismiss()
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
