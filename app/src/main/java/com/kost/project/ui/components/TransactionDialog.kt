package com.kost.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kost.project.data.model.ProjectPeriod
import com.kost.project.data.model.Transaction
import com.kost.project.ui.util.ThousandsSeparatorTransformation
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    transactionToEdit: Transaction? = null,
    periods: List<ProjectPeriod>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    initialCategory: String? = null
) {
    var currentStep by remember { mutableStateOf(if (transactionToEdit != null || initialCategory != null) 1 else 0) }
    
    var selectedCategory by remember { 
        mutableStateOf(transactionToEdit?.category ?: initialCategory ?: "") 
    }
    
    var newCategoryName by remember { mutableStateOf("") }

    var title by remember { mutableStateOf(transactionToEdit?.title ?: "") }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toLong()?.toString() ?: "") }
    var selectedPeriodId by remember { mutableStateOf(transactionToEdit?.periodId ?: "p4") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val initialDate = transactionToEdit?.date?.let { 
        try { sdf.parse(it) } catch (_: Exception) { Date() }
    } ?: Date()
    
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Transaksi") },
            text = { Text("Apakah Anda yakin ingin menghapus transaksi ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToEdit?.id?.let { onDelete?.invoke(it) }
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Date(it)
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
        onDismissRequest = onDismiss,
        title = { 
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (currentStep == 0) "Pilih Kategori" else if (transactionToEdit == null) "Tambah Transaksi Baru" else "Edit Transaksi",
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
            if (currentStep == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryModernItem(
                        title = "Belanja Material",
                        subtitle = "Tambah belanja material baru",
                        icon = Icons.Rounded.ShoppingCart,
                        iconColor = Color(0xFF007AFF),
                        onClick = { 
                            selectedCategory = "Bumimas_Belanja"
                            title = "Belanja Bumimas"
                            currentStep = 1 
                        }
                    )
                    CategoryModernItem(
                        title = "Pembayaran",
                        subtitle = "Tambah pembayaran pelunasan",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconColor = Color(0xFF34A853),
                        onClick = { 
                            selectedCategory = "Bumimas_Lunas"
                            title = "Lunas Bumimas"
                            currentStep = 1 
                        }
                    )
                    CategoryModernItem(
                        title = "Kasbon Pekerja",
                        subtitle = "Tambah kasbon untuk pekerja",
                        icon = Icons.Rounded.Person,
                        iconColor = Color(0xFF5856D6),
                        onClick = { 
                            selectedCategory = "Gondes"
                            title = "Upah Gondes"
                            currentStep = 1 
                        }
                    )
                    CategoryModernItem(
                        title = "Lain-lain",
                        subtitle = "Tambah pengeluaran lain-lain",
                        icon = Icons.Rounded.MoreHoriz,
                        iconColor = Color(0xFFFF9500),
                        onClick = { 
                            selectedCategory = "Lain-lain"
                            currentStep = 1 
                        }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Nama Klasifikasi Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E5EA),
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            focusedContainerColor = Color(0xFFF8F7FA),
                            unfocusedContainerColor = Color(0xFFF8F7FA)
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Selected Category Preview
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8F7FA),
                        border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val catIcon = when {
                                selectedCategory == "Bumimas_Belanja" -> Icons.Rounded.ShoppingCart
                                selectedCategory == "Bumimas_Lunas" -> Icons.Rounded.AccountBalanceWallet
                                selectedCategory == "Gondes" -> Icons.Rounded.Person
                                else -> Icons.Rounded.FolderOpen
                            }
                            val catColor = when {
                                selectedCategory == "Bumimas_Belanja" -> Color(0xFF007AFF)
                                selectedCategory == "Bumimas_Lunas" -> Color(0xFF34A853)
                                selectedCategory == "Gondes" -> Color(0xFF5856D6)
                                else -> Color(0xFFFF9500)
                            }
                            
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = catColor.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Kategori", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(selectedCategory.replace("_", " "), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }

                    val isMainCategory = selectedCategory.startsWith("Bumimas") || 
                                         selectedCategory == "Gondes" || 
                                         selectedCategory == "Puing"
                    val isOthers = selectedCategory == "Lain-lain" || (!isMainCategory && selectedCategory.isNotEmpty())

                    if (isOthers) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Masukkan Judul", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("Contoh: Beli Semen") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE5E5EA),
                                    unfocusedBorderColor = Color(0xFFE5E5EA)
                                )
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pilih tanggal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                                    Text(sdf.format(selectedDate), style = MaterialTheme.typography.bodyLarge)
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Masukkan Nominal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                            placeholder = { Text("Contoh: 150000") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorTransformation(),
                            singleLine = true,
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
                    
                    if (transactionToEdit != null && onDelete != null) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus Transaksi")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 0) {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            selectedCategory = newCategoryName
                            title = newCategoryName
                            currentStep = 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F6EF),
                        contentColor = Color(0xFF34A853)
                    ),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Lanjutkan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = {
                        if (amountText.isNotBlank() && selectedPeriodId.isNotBlank()) {
                            onSave(
                                Transaction(
                                    id = transactionToEdit?.id ?: UUID.randomUUID().toString(),
                                    periodId = selectedPeriodId,
                                    title = if (title.isBlank()) selectedCategory.replace("_", " ") else title,
                                    amount = amountText.toDoubleOrNull() ?: 0.0,
                                    date = sdf.format(selectedDate),
                                    category = selectedCategory,
                                    note = note.ifBlank { null }
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
                    Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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
                Text("Batal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun CategoryModernItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8F7FA)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun CategorySelectButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Text(text, color = Color.Black, fontSize = 16.sp)
    }
}
