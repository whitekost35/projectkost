package com.kost.project.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kost.project.data.model.ProjectPeriod
import java.util.Locale

@Composable
fun ModernBarChart(
    periods: List<ProjectPeriod>,
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty()) return

    val maxAmount = periods.maxOfOrNull { it.totalAmount } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth / (periods.size * 2f)
                val spacing = barWidth

                periods.forEachIndexed { index, period ->
                    // Sisakan ruang 20% di atas untuk teks nominal
                    val barHeight = (period.totalAmount / maxAmount) * canvasHeight * 0.75f
                    val left = index * (barWidth + spacing) + spacing / 2f
                    val top = (canvasHeight - barHeight).toFloat()

                    val barColor = runCatching {
                        Color(android.graphics.Color.parseColor(period.colorHex))
                    }.getOrDefault(primaryColor)

                    // Gambar Batang
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight.toFloat()),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Gambar Teks Nominal di atas batang
                    val abbreviatedText = formatAbbreviatedAmount(period.totalAmount)
                    val textLayoutResult = textMeasurer.measure(abbreviatedText, labelStyle)
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = left + (barWidth - textLayoutResult.size.width) / 2f,
                            y = top - textLayoutResult.size.height - 8f // Jarak 8px dari atas batang
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label Singkat di Bawah Grafik (Sejajar sempurna dengan batang)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            periods.forEach { period ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.name.replace("Periode ", "Per "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatAbbreviatedAmount(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> {
            val billions = amount / 1_000_000_000.0
            String.format(Locale.forLanguageTag("id-ID"), "%.1f M", billions)
        }
        amount >= 1_000_000 -> {
            val millions = (amount / 1_000_000).toLong()
            "$millions juta"
        }
        else -> {
            amount.toLong().toString()
        }
    }
}
