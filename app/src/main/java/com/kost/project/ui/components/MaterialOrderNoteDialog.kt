package com.kost.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class OrderItem(
    val name: String = "",
    val quantity: String = ""
)

@Composable
fun MaterialOrderNoteDialog(
    initialItems: List<OrderItem> = listOf(OrderItem(), OrderItem(), OrderItem(), OrderItem()),
    onDismiss: () -> Unit,
    onSave: (List<OrderItem>) -> Unit,
    onSend: (List<OrderItem>) -> Unit
) {
    var items by remember { mutableStateOf(initialItems.ifEmpty { listOf(OrderItem()) }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "List Order Matrial Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9))
                        .border(0.5.dp, Color.LightGray)
                ) {
                    Text(
                        "Nama Barang",
                        modifier = Modifier
                            .weight(2f)
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                    Text(
                        "Kuantitas",
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Table Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color.LightGray),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    items = items.toMutableList().apply {
                                        this[index] = this[index].copy(name = newName)
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start),
                                decorationBox = { innerTextField ->
                                    if (item.name.isEmpty()) Text("...", color = Color.LightGray)
                                    innerTextField()
                                }
                            )
                            
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))

                            BasicTextField(
                                value = item.quantity,
                                onValueChange = { newQty ->
                                    items = items.toMutableList().apply {
                                        this[index] = this[index].copy(quantity = newQty)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                decorationBox = { innerTextField ->
                                    if (item.quantity.isEmpty()) Text("...", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // Add/Remove Row Buttons
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (items.size > 1) {
                        IconButton(
                            onClick = { items = items.dropLast(1) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Kurangi Baris",
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { items = items + OrderItem() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Tambah Baris",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { 
                        onSave(items)
                        onSend(items) 
                    }) {
                        Text("Kirim", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom simple text field to avoid heavy decoration
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        decorationBox = decorationBox,
        singleLine = true
    )
}
