package com.kost.project.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kost.project.data.model.AttendanceStatus
import com.kost.project.data.model.GlobalKasbonEntry
import com.kost.project.data.model.PrivateKasbonEntry
import com.kost.project.data.model.Transaction
import com.kost.project.ui.components.OrderItem
import com.kost.project.ui.viewmodel.WorkerWeeklyData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object ImageReportGenerator {

    private val idLocale = Locale("id", "ID")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", idLocale)
    private val dayNameFormatter = DateTimeFormatter.ofPattern("EEE", idLocale)

    fun generateAndShareOrderImage(context: Context, items: List<OrderItem>) {
        try {
            val validItems = items.filter { it.name.isNotBlank() }
            if (validItems.isEmpty()) {
                Toast.makeText(context, "Daftar pesanan kosong", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = createOrderBitmap(validItems)
            saveAndShareBitmap(context, bitmap, "Nota_Order")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndShareAttendanceImage(
        context: Context,
        weekStart: LocalDate,
        weekDates: List<LocalDate>,
        workerData: List<WorkerWeeklyData>
    ) {
        try {
            if (workerData.isEmpty()) {
                Toast.makeText(context, "Data absensi kosong", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = createAttendanceBitmap(weekStart, weekDates, workerData)
            saveAndShareBitmap(context, bitmap, "Rekap_Absensi")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndShareSalaryImage(
        context: Context,
        payoutDate: LocalDate,
        workerData: List<WorkerWeeklyData>,
        globalKasbon: List<GlobalKasbonEntry>
    ) {
        try {
            if (workerData.isEmpty()) {
                Toast.makeText(context, "Data gajian kosong", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = createSalaryBitmap(payoutDate, workerData, globalKasbon)
            saveAndShareBitmap(context, bitmap, "Rekap_Gaji")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndShareKasbonImage(
        context: Context,
        workerName: String,
        entries: List<PrivateKasbonEntry>
    ) {
        try {
            if (entries.isEmpty()) {
                Toast.makeText(context, "Riwayat kasbon kosong", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = createKasbonBitmap(workerName, entries)
            saveAndShareBitmap(context, bitmap, "Riwayat_Kasbon_${workerName.replace(" ", "_")}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndShareBumimasImage(
        context: Context,
        belanjaList: List<Transaction>,
        totalBelanja: Double,
        lunasList: List<Transaction>,
        totalLunas: Double
    ) {
        try {
            val bitmap = createBumimasBitmap(belanjaList, totalBelanja, lunasList, totalLunas)
            saveAndShareBitmap(context, bitmap, "Rekap_Bumimas")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOrderBitmap(items: List<OrderItem>): Bitmap {
        val padding = 40f
        val rowHeight = 60f
        val headerHeight = 100f
        val titleHeight = 80f
        
        val width = 800
        val height = (titleHeight + headerHeight + (items.size * rowHeight) + (padding * 2)).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            isFakeBoldText = true
        }
        val tableLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }

        var currentY = padding

        // Title
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("List Order Matrial Baru", width / 2f, currentY + 40f, headerPaint)
        currentY += titleHeight

        val col1Width = 500f
        val col2Width = width - (padding * 2) - col1Width
        val startX = padding

        // Table Header Background
        canvas.drawRect(startX, currentY, width - padding, currentY + headerHeight, headerBgPaint)
        
        // Table Header Borders
        canvas.drawRect(startX, currentY, width - padding, currentY + headerHeight, tableLinePaint)
        canvas.drawLine(startX + col1Width, currentY, startX + col1Width, currentY + headerHeight, tableLinePaint)

        // Header Text
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Nama Barang", startX + (col1Width / 2f), currentY + (headerHeight / 2f) + 10f, textPaint)
        canvas.drawText("Kuantitas", startX + col1Width + (col2Width / 2f), currentY + (headerHeight / 2f) + 10f, textPaint)
        
        currentY += headerHeight

        // Items
        textPaint.isFakeBoldText = false
        items.forEach { item ->
            // Cell Border
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, tableLinePaint)
            canvas.drawLine(startX + col1Width, currentY, startX + col1Width, currentY + rowHeight, tableLinePaint)

            // Name (Left Align)
            textPaint.textAlign = Paint.Align.LEFT
            val nameText = if (item.name.length > 30) item.name.substring(0, 27) + "..." else item.name
            canvas.drawText(nameText, startX + 20f, currentY + (rowHeight / 2f) + 10f, textPaint)

            // Quantity (Center Align)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.quantity, startX + col1Width + (col2Width / 2f), currentY + (rowHeight / 2f) + 10f, textPaint)

            currentY += rowHeight
        }

        return bitmap
    }

    private fun createAttendanceBitmap(
        weekStart: LocalDate,
        weekDates: List<LocalDate>,
        workerData: List<WorkerWeeklyData>
    ): Bitmap {
        val width = 1200
        val padding = 40f
        val headerHeight = 120f
        val summaryCardHeight = 180f
        val tableHeaderHeight = 80f
        val rowHeight = 70f
        val footerHeight = 100f
        val legendHeight = 120f
        
        val totalHeight = (padding + headerHeight + summaryCardHeight + 40f + tableHeaderHeight + (workerData.size * rowHeight) + footerHeight + legendHeight + padding).toInt()
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 38f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        val summaryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34A853")
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val summaryAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34A853")
            textSize = 54f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val workerNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEEEEE")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        var currentY = padding

        // 1. Header
        canvas.drawText("REKAP ABSENSI MINGGUAN", width / 2f, currentY + 40f, titlePaint)
        val weekEnd = weekStart.plusDays(6)
        val weekRange = "${weekStart.format(dateFormatter)} - ${weekEnd.format(dateFormatter)}"
        canvas.drawText(weekRange, width / 2f, currentY + 85f, datePaint)
        currentY += headerHeight

        // 2. Summary Card
        val cardRect = RectF(padding, currentY, width - padding, currentY + summaryCardHeight)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)
        
        val totalUpah = workerData.sumOf { it.weeklyTotal }
        canvas.drawText("Total Upah Seluruhnya", width / 2f, currentY + 70f, summaryLabelPaint)
        canvas.drawText(formatRupiah(totalUpah), width / 2f, currentY + 140f, summaryAmountPaint)
        currentY += summaryCardHeight + 40f

        // 3. Table Headers
        val startX = padding
        val colNoWidth = 45f
        val colWorkerWidth = 220f
        val colDayWidth = 80f
        val colTotalHariWidth = 85f
        val colTotalUpahWidth = 190f
        
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E3F2FD") // Biru Soft Elegan
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, paint)
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, linePaint)

        var cx = startX
        canvas.drawText("No", cx + (colNoWidth / 2f), currentY + 50f, tableHeaderPaint)
        cx += colNoWidth
        canvas.drawText("Pekerja", cx + (colWorkerWidth / 2f), currentY + 50f, tableHeaderPaint)
        cx += colWorkerWidth
        
        weekDates.forEach { date ->
            val dayName = date.format(dayNameFormatter).uppercase()
            canvas.drawText(dayName, cx + (colDayWidth / 2f), currentY + 35f, tableHeaderPaint)
            val dayDate = date.format(DateTimeFormatter.ofPattern("dd MMM", idLocale))
            canvas.drawText(dayDate, cx + (colDayWidth / 2f), currentY + 65f, datePaint.apply { textSize = 16f })
            cx += colDayWidth
        }
        
        canvas.drawText("Hari", cx + (colTotalHariWidth / 2f), currentY + 50f, tableHeaderPaint)
        cx += colTotalHariWidth
        canvas.drawText("Total Upah", cx + (colTotalUpahWidth / 2f), currentY + 50f, tableHeaderPaint)
        
        currentY += tableHeaderHeight

        // 4. Table Rows
        workerData.forEachIndexed { index, data ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            
            var rowCx = startX
            // No
            canvas.drawText((index + 1).toString(), rowCx + (colNoWidth / 2f), currentY + 45f, tableTextPaint.apply { color = Color.GRAY })
            rowCx += colNoWidth
            
            // Pekerja
            canvas.drawText(data.worker.name, rowCx + 15f, currentY + 35f, workerNamePaint)
            canvas.drawText(data.worker.role.name.lowercase().replaceFirstChar { it.uppercase() }, rowCx + 15f, currentY + 60f, tableTextPaint.apply { 
                textAlign = Paint.Align.LEFT
                textSize = 16f
                color = Color.GRAY
            })
            rowCx += colWorkerWidth
            
            // Statuses
            weekDates.forEach { date ->
                val status = data.attendance[date.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: AttendanceStatus.OFF
                drawStatusInImage(canvas, rowCx, currentY, colDayWidth, rowHeight, status)
                rowCx += colDayWidth
            }
            
            // Total Hari
            val totalDaysStr = if (data.totalDays % 1.0 == 0.0) data.totalDays.toInt().toString() else data.totalDays.toString()
            canvas.drawText(totalDaysStr, rowCx + (colTotalHariWidth / 2f), currentY + 45f, tableTextPaint.apply { 
                textAlign = Paint.Align.CENTER
                textSize = 22f
                isFakeBoldText = true
                color = Color.BLACK
            })
            rowCx += colTotalHariWidth
            
            // Total Upah
            canvas.drawText(formatRupiah(data.weeklyTotal), rowCx + (colTotalUpahWidth / 2f), currentY + 45f, tableTextPaint.apply { 
                color = Color.parseColor("#34A853")
            })
            
            currentY += rowHeight
        }

        // 5. Footer Row
        canvas.drawRect(startX, currentY, width - padding, currentY + footerHeight, paint.apply { color = Color.parseColor("#E3F2FD") })
        canvas.drawRect(startX, currentY, width - padding, currentY + footerHeight, linePaint)
        
        canvas.drawText("Total Per Hari (Orang)", startX + 20f, currentY + 60f, tableHeaderPaint.apply { textAlign = Paint.Align.LEFT })
        
        var footerCx = startX + colNoWidth + colWorkerWidth
        weekDates.forEach { date ->
            val count = workerData.sumOf { 
                when(it.attendance[date.format(DateTimeFormatter.ISO_LOCAL_DATE)]) {
                    AttendanceStatus.FULL, AttendanceStatus.LEMBUR_1, AttendanceStatus.LEMBUR_2 -> 1.0
                    AttendanceStatus.HALF -> 0.5
                    else -> 0.0
                }
            }
            val countStr = if (count % 1.0 == 0.0) count.toInt().toString() else count.toString()
            canvas.drawText(countStr, footerCx + (colDayWidth / 2f), currentY + 60f, tableHeaderPaint.apply { 
                textAlign = Paint.Align.CENTER
                color = Color.parseColor("#34A853")
            })
            footerCx += colDayWidth
        }
        
        val totalDaysAll = workerData.sumOf { it.totalDays }
        val totalDaysAllStr = if (totalDaysAll % 1.0 == 0.0) totalDaysAll.toInt().toString() else totalDaysAll.toString()
        canvas.drawText(totalDaysAllStr, footerCx + (colTotalHariWidth / 2f), currentY + 60f, tableHeaderPaint)
        footerCx += colTotalHariWidth
        
        canvas.drawText(formatRupiah(totalUpah), footerCx + (colTotalUpahWidth / 2f), currentY + 60f, tableHeaderPaint)
        
        currentY += footerHeight

        // 6. Visual Legend
        val legendStartY = currentY + 40f
        var lx = padding + 20f
        val itemSpacing = 160f
        
        drawLegendItemInImage(canvas, lx, legendStartY, "Full", "(1 hari)", AttendanceStatus.FULL)
        lx += itemSpacing
        drawLegendItemInImage(canvas, lx, legendStartY, "½ Hari", "(0,5 hari)", AttendanceStatus.HALF)
        lx += itemSpacing + 10f
        drawLegendItemInImage(canvas, lx, legendStartY, "Lembur 1", "(+50k)", AttendanceStatus.LEMBUR_1)
        lx += itemSpacing + 20f
        drawLegendItemInImage(canvas, lx, legendStartY, "Lembur 2", "(+75k)", AttendanceStatus.LEMBUR_2)
        lx += itemSpacing + 20f
        drawLegendItemInImage(canvas, lx, legendStartY, "Ekstra", "(+Gaji)", AttendanceStatus.LEMBUR_EKSTRA)
        lx += itemSpacing + 20f
        drawLegendItemInImage(canvas, lx, legendStartY, "Libur", "", AttendanceStatus.OFF)
        lx += itemSpacing - 30f
        drawLegendItemInImage(canvas, lx, legendStartY, "Tidak Absen", "", null)

        return bitmap
    }

    private fun drawLegendItemInImage(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        subLabel: String,
        status: AttendanceStatus?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val iconSize = 40f
        
        if (status == null) {
            paint.color = Color.parseColor("#F2F2F7")
            canvas.drawRoundRect(RectF(x, y, x + iconSize, y + iconSize), 8f, 8f, paint)
            paint.color = Color.LTGRAY
            paint.textSize = 20f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("—", x + iconSize / 2f, y + iconSize / 2f + 8f, paint)
        } else {
            val (bgColor, iconColor, symbol) = when (status) {
                AttendanceStatus.FULL -> Triple("#E8F5E9", "#34A853", "✓")
                AttendanceStatus.HALF -> Triple("#FFF3E0", "#EF6C00", "½")
                AttendanceStatus.LEMBUR_1 -> Triple("#E3F2FD", "#007AFF", "🕒")
                AttendanceStatus.LEMBUR_2 -> Triple("#F3E5F5", "#7B1FA2", "🕒")
                AttendanceStatus.LEMBUR_EKSTRA -> Triple("#FFFDE7", "#FBC02D", "⭐")
                AttendanceStatus.OFF -> Triple("#FFEBEE", "#D32F2F", "—")
            }
            
            paint.color = Color.parseColor(bgColor)
            canvas.drawRoundRect(RectF(x, y, x + iconSize, y + iconSize), 8f, 8f, paint)
            
            paint.color = Color.parseColor(iconColor)
            paint.textSize = if (symbol == "✓") 28f else 22f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(symbol, x + iconSize / 2f, y + iconSize / 2f + 8f, paint)
        }
        
        val textStartX = x + iconSize + 12f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(label, textStartX, y + 18f, paint)
        
        if (subLabel.isNotEmpty()) {
            paint.isFakeBoldText = false
            paint.color = Color.GRAY
            paint.textSize = 16f
            canvas.drawText(subLabel, textStartX, y + 38f, paint)
        }
    }

    private fun createSalaryBitmap(
        payoutDate: LocalDate,
        workerData: List<WorkerWeeklyData>,
        globalKasbon: List<GlobalKasbonEntry>
    ): Bitmap {
        val width = 1000
        val padding = 40f
        val headerHeight = 100f
        val tableHeaderHeight = 80f
        val rowHeight = 70f
        val sectionTitleHeight = 60f
        
        val hasPrivateKasbon = workerData.any { (it.adjustment?.privateKasbon ?: 0.0) > 0.0 }
        val potongRowsCount = (if (hasPrivateKasbon) 1 else 0) + globalKasbon.size
        
        val totalHeight = (padding + headerHeight + tableHeaderHeight + (workerData.size * rowHeight) + rowHeight + 40f + sectionTitleHeight + (potongRowsCount * rowHeight) + 40f + 100f + padding).toInt()
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        val deductionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D32F2F")
            textSize = 22f
            textAlign = Paint.Align.LEFT
        }

        var currentY = padding

        // 1. Header
        canvas.drawText("Rekap Gaji Mingguan: ${payoutDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", idLocale))}", width / 2f, currentY + 40f, titlePaint)
        currentY += headerHeight

        // 2. Salary Table
        val startX = padding
        val colWidths = floatArrayOf(300f, 180f, 220f, 220f)
        val headers = listOf("Nama", "Hari Kerja", "Upah Harian", "Total Upah")
        
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, linePaint)
        
        var cx = startX
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, cx + (colWidths[i] / 2f), currentY + 50f, tableHeaderPaint)
            if (i > 0) canvas.drawLine(cx, currentY, cx, currentY + tableHeaderHeight, linePaint)
            cx += colWidths[i]
        }
        currentY += tableHeaderHeight

        workerData.forEach { data ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            var rowCx = startX
            
            tableTextPaint.textAlign = Paint.Align.LEFT
            tableTextPaint.isFakeBoldText = true
            canvas.drawText(data.worker.name.uppercase(), rowCx + 20f, currentY + 45f, tableTextPaint)
            rowCx += colWidths[0]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.textAlign = Paint.Align.CENTER
            tableTextPaint.isFakeBoldText = false
            val totalDaysStr = if (data.totalDays % 1.0 == 0.0) "${data.totalDays.toInt()} Hari" else "${data.totalDays} Hari"
            canvas.drawText(totalDaysStr, rowCx + (colWidths[1] / 2f), currentY + 45f, tableTextPaint)
            rowCx += colWidths[1]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatRupiah(data.worker.baseWage), rowCx + colWidths[2] - 20f, currentY + 45f, tableTextPaint)
            rowCx += colWidths[2]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.isFakeBoldText = true
            canvas.drawText(formatRupiah(data.weeklyTotal), width - padding - 20f, currentY + 45f, tableTextPaint)
            
            currentY += rowHeight
        }

        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
        val combinedWidth = colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawText("TOTAL UPAH KOTOR", startX + (combinedWidth / 2f), currentY + 45f, tableHeaderPaint)
        canvas.drawLine(startX + combinedWidth, currentY, startX + combinedWidth, currentY + rowHeight, linePaint)
        canvas.drawText(formatRupiah(workerData.sumOf { it.weeklyTotal }), width - padding - 20f, currentY + 45f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })
        
        currentY += rowHeight + 40f

        // 3. Potongan Section
        deductionPaint.color = Color.BLACK
        deductionPaint.isFakeBoldText = true
        deductionPaint.textSize = 26f
        canvas.drawText("RINCIAN POTONGAN & KASBON", startX, currentY + 30f, deductionPaint)
        currentY += sectionTitleHeight
        
        val colPotongWidth = 620f
        
        val totalPotongHutang = workerData.sumOf { it.adjustment?.privateKasbon ?: 0.0 }
        if (totalPotongHutang > 0 || hasPrivateKasbon) {
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            deductionPaint.color = Color.parseColor("#D32F2F")
            deductionPaint.textSize = 22f
            deductionPaint.isFakeBoldText = false
            deductionPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Potongan Hutang (Pribadi)", startX + 20f, currentY + 45f, deductionPaint)
            
            deductionPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(parseDateSafely(null, "Potongan").format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), startX + colPotongWidth - 40f, currentY + 45f, deductionPaint)
            
            canvas.drawLine(startX + colPotongWidth, currentY, startX + colPotongWidth, currentY + rowHeight, linePaint)
            canvas.drawText(formatRupiah(totalPotongHutang), width - padding - 20f, currentY + 45f, deductionPaint)
            currentY += rowHeight
        }
        
        globalKasbon.forEach { entry ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            deductionPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(entry.description, startX + 20f, currentY + 45f, deductionPaint)
            
            deductionPaint.textAlign = Paint.Align.RIGHT
            val entryDate = parseDateSafely(entry.date, entry.description)
            canvas.drawText(entryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), startX + colPotongWidth - 40f, currentY + 45f, deductionPaint)
            
            canvas.drawLine(startX + colPotongWidth, currentY, startX + colPotongWidth, currentY + rowHeight, linePaint)
            canvas.drawText(formatRupiah(entry.amount), width - padding - 20f, currentY + 45f, deductionPaint)
            currentY += rowHeight
        }
        
        currentY += 40f

        // 4. Final Footer
        val totalKasbonGlobal = globalKasbon.sumOf { it.amount }
        val netGajian = workerData.sumOf { it.weeklyTotal } - totalKasbonGlobal - totalPotongHutang
        
        canvas.drawRect(startX, currentY, width - padding, currentY + 100f, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + 100f, linePaint)
        
        val footerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GAJIAN BERSIH MINGGU INI", startX + (colPotongWidth / 2f), currentY + 65f, footerLabelPaint)
        canvas.drawLine(startX + colPotongWidth, currentY, startX + colPotongWidth, currentY + 100f, linePaint)
        
        val footerValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (netGajian >= 0) Color.parseColor("#34A853") else Color.parseColor("#D32F2F")
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(formatRupiah(netGajian), width - padding - 20f, currentY + 65f, footerValuePaint)

        return bitmap
    }

    private fun createKasbonBitmap(
        workerName: String,
        entries: List<PrivateKasbonEntry>
    ): Bitmap {
        val width = 1000
        val padding = 40f
        val headerHeight = 100f
        val tableHeaderHeight = 80f
        val rowHeight = 70f
        
        val sortedEntries = entries.sortedBy { it.date }
        var currentBalance = 0.0
        val entriesWithBalance = sortedEntries.map { entry ->
            currentBalance += entry.loanAmount
            currentBalance -= entry.paymentAmount
            entry to currentBalance
        }
        
        val displayEntries = if (entriesWithBalance.size > 15) {
            entriesWithBalance.takeLast(15)
        } else {
            entriesWithBalance
        }

        val totalHeight = (padding + headerHeight + tableHeaderHeight + (displayEntries.size * rowHeight) + rowHeight + padding).toInt()
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCDD2") // Merah Soft Elegan
            style = Paint.Style.FILL
        }

        var currentY = padding

        // 1. Header
        canvas.drawText("Riwayat Kasbon: $workerName", width / 2f, currentY + 40f, titlePaint)
        currentY += headerHeight

        // 2. Table Headers
        val startX = padding
        val colWidths = floatArrayOf(280f, 210f, 210f, 220f)
        val headers = listOf("Tanggal", "Pinjam (+)", "Bayar (-)", "Saldo Hutang")
        
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, linePaint)
        var cx = startX
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, cx + (colWidths[i] / 2f), currentY + 50f, tableHeaderPaint)
            if (i > 0) canvas.drawLine(cx, currentY, cx, currentY + tableHeaderHeight, linePaint)
            cx += colWidths[i]
        }
        currentY += tableHeaderHeight

        // 3. Rows
        displayEntries.forEach { (entry, balance) ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            var rowCx = startX
            
            val date = parseDateSafely(entry.date, entry.description)
            val dateStr = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", idLocale))
            tableTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(dateStr, rowCx + 20f, currentY + 45f, tableTextPaint)
            rowCx += colWidths[0]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.textAlign = Paint.Align.RIGHT
            if (entry.loanAmount > 0) {
                canvas.drawText(formatRupiah(entry.loanAmount), rowCx + colWidths[1] - 20f, currentY + 45f, tableTextPaint)
            }
            rowCx += colWidths[1]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            if (entry.paymentAmount > 0) {
                tableTextPaint.color = Color.parseColor("#D32F2F")
                canvas.drawText("-${formatRupiah(entry.paymentAmount)}", rowCx + colWidths[2] - 20f, currentY + 45f, tableTextPaint)
                tableTextPaint.color = Color.BLACK
            }
            rowCx += colWidths[2]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.isFakeBoldText = true
            canvas.drawText(formatRupiah(balance), width - padding - 20f, currentY + 45f, tableTextPaint)
            tableTextPaint.isFakeBoldText = false
            
            currentY += rowHeight
        }

        // 4. Footer Row
        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
        val combinedWidth = colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawText("TOTAL SISA HUTANG", startX + (combinedWidth / 2f), currentY + 45f, tableHeaderPaint)
        canvas.drawLine(startX + combinedWidth, currentY, startX + combinedWidth, currentY + rowHeight, linePaint)
        
        canvas.drawText(formatRupiah(currentBalance), width - padding - 20f, currentY + 45f, tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT })

        return bitmap
    }

    private fun createBumimasBitmap(
        belanjaList: List<Transaction>,
        totalBelanja: Double,
        lunasList: List<Transaction>,
        totalLunas: Double
    ): Bitmap {
        val width = 1000
        val padding = 40f
        val headerHeight = 100f
        val subHeaderHeight = 60f
        val tableHeaderHeight = 70f
        val rowHeight = 60f
        
        val latestBelanja = belanjaList.sortedByDescending { parseDateSafely(it.date, it.title) }.take(10).reversed()
        val totalBelanjaAll = totalBelanja
        
        val section1Height = subHeaderHeight + tableHeaderHeight + (latestBelanja.size * rowHeight) + rowHeight
        val section2Height = (lunasList.size * rowHeight) + rowHeight
        val finalFooterHeight = 100f
        
        val totalHeight = (padding + headerHeight + section1Height + 40f + section2Height + 40f + finalFooterHeight + padding).toInt()
        
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 38f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCDD2") // Merah Soft Elegan
            style = Paint.Style.FILL
        }

        var currentY = padding

        // 1. Title
        canvas.drawText("Belanja Matrial Bumimas", width / 2f, currentY + 45f, titlePaint)
        currentY += headerHeight

        // 2. Table 1 (Belanja)
        val startX = padding
        val colWidths = floatArrayOf(250f, 350f, 320f)
        val combinedCol12 = colWidths[0] + colWidths[1]
        
        // Sub-header
        canvas.drawRect(startX, currentY, width - padding, currentY + subHeaderHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + subHeaderHeight, linePaint)
        canvas.drawText("Ringkasan 10 Transaksi Terakhir", width / 2f, currentY + 40f, subHeaderPaint)
        currentY += subHeaderHeight
        
        // Column Headers
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, headerBgPaint)
        canvas.drawRect(startX, currentY, width - padding, currentY + tableHeaderHeight, linePaint)
        var cx = startX
        val headers = listOf("Tanggal", "Keterangan", "Nominal Transaksi")
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, cx + (colWidths[i] / 2f), currentY + 45f, tableHeaderPaint)
            if (i > 0) canvas.drawLine(cx, currentY, cx, currentY + tableHeaderHeight, linePaint)
            cx += colWidths[i]
        }
        currentY += tableHeaderHeight
        
        // Rows (Belanja)
        latestBelanja.forEach { tx ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            var rowCx = startX
            
            // Tanggal
            tableTextPaint.textAlign = Paint.Align.CENTER
            val date = parseDateSafely(tx.date, tx.title)
            canvas.drawText(date.format(DateTimeFormatter.ofPattern("d/M/yyyy")), rowCx + (colWidths[0] / 2f), currentY + 38f, tableTextPaint)
            rowCx += colWidths[0]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            // Keterangan
            tableTextPaint.textAlign = Paint.Align.LEFT
            val titleText = if (tx.title.length > 28) tx.title.substring(0, 25) + "..." else tx.title
            canvas.drawText(titleText, rowCx + 20f, currentY + 38f, tableTextPaint)
            rowCx += colWidths[1]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            // Nominal
            tableTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatRupiah(tx.amount), width - padding - 20f, currentY + 38f, tableTextPaint)
            
            currentY += rowHeight
        }
        
        // Footer (Total Belanja All)
        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
        canvas.drawText("Total Seluruh Transaksi", startX + (combinedCol12 / 2f), currentY + 42f, subHeaderPaint.apply { 
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#D32F2F") 
        })
        canvas.drawLine(startX + combinedCol12, currentY, startX + combinedCol12, currentY + rowHeight, linePaint)
        canvas.drawText(formatRupiah(totalBelanjaAll), width - padding - 20f, currentY + 42f, subHeaderPaint.apply { 
            textAlign = Paint.Align.RIGHT 
        })
        currentY += rowHeight + 40f
        
        // 3. Table 2 (Lunas)
        lunasList.forEach { tx ->
            canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
            var rowCx = startX
            
            // Tanggal
            tableTextPaint.color = Color.BLACK
            tableTextPaint.textAlign = Paint.Align.CENTER
            val date = parseDateSafely(tx.date, tx.title)
            canvas.drawText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), rowCx + (colWidths[0] / 2f), currentY + 38f, tableTextPaint)
            rowCx += colWidths[0]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            canvas.drawText("LUNAS", rowCx + (colWidths[1] / 2f), currentY + 38f, subHeaderPaint.apply { color = Color.BLACK })
            rowCx += colWidths[1]
            canvas.drawLine(rowCx, currentY, rowCx, currentY + rowHeight, linePaint)
            
            tableTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatRupiah(tx.amount), width - padding - 20f, currentY + 38f, tableTextPaint)
            
            currentY += rowHeight
        }
        
        // Footer (Total Lunas)
        canvas.drawRect(startX, currentY, width - padding, currentY + rowHeight, linePaint)
        canvas.drawText("Total Lunas", startX + (combinedCol12 / 2f), currentY + 42f, subHeaderPaint.apply { 
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#34A853") 
        })
        canvas.drawLine(startX + combinedCol12, currentY, startX + combinedCol12, currentY + rowHeight, linePaint)
        canvas.drawText(formatRupiah(totalLunas), width - padding - 20f, currentY + 42f, subHeaderPaint.apply { 
            textAlign = Paint.Align.RIGHT 
            color = Color.parseColor("#34A853")
        })
        currentY += rowHeight + 40f
        
        // 4. Final Footer (Sisa Hutang)
        val sisaHutang = totalBelanjaAll - totalLunas
        canvas.drawRect(startX, currentY, width - padding, currentY + finalFooterHeight, linePaint)
        canvas.drawText("Sisa Hutang saat ini", startX + (combinedCol12 / 2f), currentY + 65f, titlePaint.apply { 
            textAlign = Paint.Align.CENTER
            textSize = 32f
            color = Color.BLACK 
        })
        canvas.drawLine(startX + combinedCol12, currentY, startX + combinedCol12, currentY + finalFooterHeight, linePaint)
        canvas.drawText(formatRupiah(sisaHutang), width - padding - 20f, currentY + 65f, titlePaint.apply { 
            textAlign = Paint.Align.RIGHT
            textSize = 32f
            color = Color.BLACK
        })

        return bitmap
    }

    private fun parseDateSafely(dateStr: String?, title: String? = null): LocalDate {
        if (dateStr == null) return LocalDate.now()
        
        // 1. ISO Format (yyyy-MM-dd)
        try { return LocalDate.parse(dateStr) } catch (e: Exception) {}
        
        // 2. ID/CSV Format (dd/MM/yyyy)
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            return LocalDate.parse(dateStr, formatter)
        } catch (e: Exception) {}
        
        // 3. Fallback: Check title for date (e.g. "Lunas 04 Juli 2026")
        if (title != null && title.contains("Lunas")) {
            try {
                val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                val words = title.split(" ")
                val day = words.find { it.toIntOrNull() != null }?.toInt()
                val monthName = words.find { months.contains(it) }
                val month = if (monthName != null) months.indexOf(monthName) + 1 else null
                val year = words.find { it.length == 4 && it.toIntOrNull() != null }?.toInt()
                
                if (day != null && month != null && year != null) {
                    return LocalDate.of(year, month, day)
                }
            } catch (e: Exception) {}
        }
        
        return LocalDate.now()
    }

    private fun drawStatusInImage(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, status: AttendanceStatus) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val centerX = x + (w / 2f)
        val centerY = y + (h / 2f)
        
        when (status) {
            AttendanceStatus.FULL -> {
                paint.color = Color.parseColor("#34A853")
                paint.textSize = 30f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("✓", centerX, centerY + 10f, paint)
            }
            AttendanceStatus.HALF -> {
                paint.color = Color.parseColor("#EF6C00")
                paint.textSize = 24f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("½", centerX, centerY + 10f, paint)
            }
            AttendanceStatus.LEMBUR_1, AttendanceStatus.LEMBUR_2, AttendanceStatus.LEMBUR_EKSTRA -> {
                paint.color = when(status) {
                    AttendanceStatus.LEMBUR_1 -> Color.parseColor("#007AFF")
                    AttendanceStatus.LEMBUR_2 -> Color.parseColor("#7B1FA2")
                    else -> Color.parseColor("#FBC02D")
                }
                paint.textSize = 24f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(if(status == AttendanceStatus.LEMBUR_EKSTRA) "⭐" else "🕒", centerX, centerY + 10f, paint)
            }
            AttendanceStatus.OFF -> {
                paint.color = Color.LTGRAY
                paint.textSize = 24f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("—", centerX, centerY + 10f, paint)
            }
        }
    }

    private fun formatRupiah(amount: Double): String {
        val formatter = java.text.NumberFormat.getCurrencyInstance(idLocale)
        return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    private fun saveAndShareBitmap(context: Context, bitmap: Bitmap, prefix: String) {
        try {
            val fileName = "${prefix}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.jpg"
            val file = File(context.cacheDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Laporan Ke..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengirim gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
