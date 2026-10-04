package com.kost.project.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kost.project.data.model.AttendanceStatus
import com.kost.project.data.model.GlobalKasbonEntry
import com.kost.project.data.model.PrivateKasbonEntry
import com.kost.project.data.model.Transaction
import com.kost.project.ui.viewmodel.WorkerWeeklyData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object PdfReportGenerator {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private const val PAGE_WIDTH_PORTRAIT = 595f
    private const val PAGE_HEIGHT_PORTRAIT = 842f
    private const val PAGE_WIDTH_LANDSCAPE = 842f
    private const val PAGE_HEIGHT_LANDSCAPE = 595f
    
    private const val MARGIN_BOTTOM_PORTRAIT = 800f
    private const val MARGIN_BOTTOM_LANDSCAPE = 550f

    fun generateAttendancePdf(
        context: Context,
        weekStart: LocalDate,
        workerData: List<WorkerWeeklyData>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        }
        
        val weekEnd = weekStart.plusDays(6)
        val title = "Absensi Mingguan: ${weekStart.format(dateFormatter)} s/d ${weekEnd.format(dateFormatter)}"
        
        val startX = 30f
        val rowHeight = 35f
        val nameWidth = 150f
        val dayWidth = 60f
        val totalWidth = 80f
        val lemburWidth = 85f
        val dayNames = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu")

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE.toInt(), PAGE_HEIGHT_LANDSCAPE.toInt(), pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        fun drawHeader() {
            paint.textSize = 18f
            paint.isFakeBoldText = true
            drawTextInCell(canvas, title, 0f, 20f, PAGE_WIDTH_LANDSCAPE, 30f, paint, Paint.Align.CENTER)
            
            val startY = 60f
            paint.textSize = 11f
            paint.isFakeBoldText = true
            
            // NAMA
            canvas.drawRect(startX, startY, startX + nameWidth, startY + 70f, linePaint)
            drawTextInCell(canvas, "NAMA", startX, startY, nameWidth, 70f, paint, Paint.Align.CENTER)
            
            // HARI
            var cx = startX + nameWidth
            canvas.drawRect(cx, startY, cx + (7 * dayWidth), startY + 35f, linePaint)
            drawTextInCell(canvas, "HARI", cx, startY, 7 * dayWidth, 35f, paint, Paint.Align.CENTER)
            
            // Day Names
            paint.textSize = 10f
            for (i in 0..6) {
                val dx = cx + (i * dayWidth)
                canvas.drawRect(dx, startY + 35f, dx + dayWidth, startY + 70f, linePaint)
                drawTextInCell(canvas, dayNames[i], dx, startY + 35f, dayWidth, 35f, paint, Paint.Align.CENTER)
            }
            
            // Total Hari Kerja
            cx += (7 * dayWidth)
            canvas.drawRect(cx, startY, cx + totalWidth, startY + 70f, linePaint)
            paint.textSize = 10f
            drawTextInCell(canvas, "Total Hari\nKerja", cx, startY, totalWidth, 70f, paint, Paint.Align.CENTER)
            
            // Lembur
            cx += totalWidth
            canvas.drawRect(cx, startY, cx + lemburWidth, startY + 35f, linePaint)
            paint.textSize = 10f
            drawTextInCell(canvas, "Lembur 1", cx, startY, lemburWidth, 35f, paint, Paint.Align.CENTER)
            canvas.drawRect(cx, startY + 35f, cx + lemburWidth, startY + 70f, linePaint)
            paint.textSize = 7f
            paint.isFakeBoldText = false
            drawTextInCell(canvas, "Biru (+50k)", cx, startY + 35f, lemburWidth, 35f, paint, Paint.Align.CENTER)
            
            cx += lemburWidth
            paint.isFakeBoldText = true
            canvas.drawRect(cx, startY, cx + lemburWidth, startY + 35f, linePaint)
            paint.textSize = 10f
            drawTextInCell(canvas, "Lembur 2", cx, startY, lemburWidth, 35f, paint, Paint.Align.CENTER)
            canvas.drawRect(cx, startY + 35f, cx + lemburWidth, startY + 70f, linePaint)
            paint.textSize = 7f
            paint.isFakeBoldText = false
            drawTextInCell(canvas, "Ungu (+75k)", cx, startY + 35f, lemburWidth, 35f, paint, Paint.Align.CENTER)
            
            currentY = startY + 70f
        }

        drawHeader()
        paint.textSize = 11f
        
        workerData.forEach { data ->
            if (currentY > MARGIN_BOTTOM_LANDSCAPE) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_LANDSCAPE.toInt(), PAGE_HEIGHT_LANDSCAPE.toInt(), pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            var cx = startX
            // Name
            canvas.drawRect(cx, currentY, cx + nameWidth, currentY + rowHeight, linePaint)
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            drawTextInCell(canvas, data.worker.name.uppercase(), cx, currentY, nameWidth, rowHeight, paint, Paint.Align.LEFT, padding = 8f)
            
            cx += nameWidth
            
            // Days Status
            paint.isFakeBoldText = false
            for (i in 0..6) {
                val dateStr = weekStart.plusDays(i.toLong()).format(isoFormatter)
                val status = data.attendance[dateStr] ?: AttendanceStatus.OFF
                
                val bgColor = when(status) {
                    AttendanceStatus.FULL -> Color.parseColor("#4CAF50")
                    AttendanceStatus.HALF -> Color.parseColor("#C8E6C9")
                    AttendanceStatus.LEMBUR_1 -> Color.parseColor("#007AFF") // Blue
                    AttendanceStatus.LEMBUR_2 -> Color.parseColor("#7B1FA2") // Purple
                    AttendanceStatus.LEMBUR_EKSTRA -> Color.parseColor("#FFD700") // Gold
                    AttendanceStatus.OFF -> Color.WHITE
                }
                
                if (status != AttendanceStatus.OFF || i == 0) {
                    paint.style = Paint.Style.FILL
                    paint.color = if (i == 0 && status == AttendanceStatus.OFF) Color.WHITE else bgColor
                    canvas.drawRect(cx + 0.5f, currentY + 0.5f, cx + dayWidth - 0.5f, currentY + rowHeight - 0.5f, paint)
                }
                
                canvas.drawRect(cx, currentY, cx + dayWidth, currentY + rowHeight, linePaint)
                
                val label = when(status) {
                    AttendanceStatus.FULL -> "Full"
                    AttendanceStatus.HALF -> "½ Hari"
                    AttendanceStatus.LEMBUR_1 -> "Lmbr 1"
                    AttendanceStatus.LEMBUR_2 -> "Lmbr 2"
                    AttendanceStatus.LEMBUR_EKSTRA -> "Ekstra"
                    AttendanceStatus.OFF -> if (i == 0) "Libur" else ""
                }
                
                paint.style = Paint.Style.FILL
                paint.textSize = 9.5f
                if (status == AttendanceStatus.FULL || status == AttendanceStatus.LEMBUR_1 || status == AttendanceStatus.LEMBUR_2) paint.color = Color.WHITE
                else if (i == 0 && status == AttendanceStatus.OFF) paint.color = Color.RED
                else paint.color = Color.BLACK
                
                drawTextInCell(canvas, label, cx, currentY, dayWidth, rowHeight, paint, Paint.Align.CENTER)
                cx += dayWidth
            }
            
            // Total Hari Kerja
            paint.color = Color.BLACK
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawRect(cx, currentY, cx + totalWidth, currentY + rowHeight, linePaint)
            val totalDaysStr = if (data.totalDays % 1.0 == 0.0) "${data.totalDays.toInt()} Hari" else "${data.totalDays} Hari"
            drawTextInCell(canvas, totalDaysStr, cx, currentY, totalWidth, rowHeight, paint, Paint.Align.CENTER)
            
            // Lembur counts
            cx += totalWidth
            val countLembur1 = data.attendance.values.count { it == AttendanceStatus.LEMBUR_1 }
            canvas.drawRect(cx, currentY, cx + lemburWidth, currentY + rowHeight, linePaint)
            drawTextInCell(canvas, countLembur1.toString(), cx, currentY, lemburWidth, rowHeight, paint, Paint.Align.CENTER)
            
            cx += lemburWidth
            val countLembur2 = data.attendance.values.count { it == AttendanceStatus.LEMBUR_2 }
            canvas.drawRect(cx, currentY, cx + lemburWidth, currentY + rowHeight, linePaint)
            drawTextInCell(canvas, countLembur2.toString(), cx, currentY, lemburWidth, rowHeight, paint, Paint.Align.CENTER)
            
            currentY += rowHeight
        }
        
        pdfDocument.finishPage(page)
        saveAndShare(context, pdfDocument, "Laporan_Absensi_${weekStart}")
    }

    fun generateSalaryPdf(
        context: Context,
        payoutDate: LocalDate,
        workerData: List<WorkerWeeklyData>,
        globalKasbon: List<GlobalKasbonEntry>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        }
        
        val title = "Rekap Gaji Mingguan: ${payoutDate.format(dateFormatter)}"
        val startX = 40f
        val colWidths = floatArrayOf(160f, 85f, 120f, 150f)
        val headers = listOf("Nama", "Hari Kerja", "Upah Harian", "Total Upah")

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        fun drawHeader() {
            paint.textSize = 18f
            paint.isFakeBoldText = true
            drawTextInCell(canvas, title, 0f, 30f, PAGE_WIDTH_PORTRAIT, 30f, paint, Paint.Align.CENTER)
            
            val startY = 80f
            paint.textSize = 11f
            var cx = startX
            headers.forEachIndexed { i, h ->
                canvas.drawRect(cx, startY, cx + colWidths[i], startY + 35f, linePaint)
                drawTextInCell(canvas, h, cx, startY, colWidths[i], 35f, paint, Paint.Align.CENTER)
                cx += colWidths[i]
            }
            currentY = startY + 35f
        }

        drawHeader()
        paint.textSize = 11f
        
        workerData.forEach { data ->
            if (currentY > MARGIN_BOTTOM_PORTRAIT) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            var cx = startX
            // Name
            canvas.drawRect(cx, currentY, cx + colWidths[0], currentY + 30f, linePaint)
            paint.isFakeBoldText = true
            drawTextInCell(canvas, data.worker.name.uppercase(), cx, currentY, colWidths[0], 30f, paint, Paint.Align.LEFT, padding = 8f)
            
            // Hari Kerja
            cx += colWidths[0]
            canvas.drawRect(cx, currentY, cx + colWidths[1], currentY + 30f, linePaint)
            paint.isFakeBoldText = false
            val totalDaysStr = if (data.totalDays % 1.0 == 0.0) "${data.totalDays.toInt()} Hari" else "${data.totalDays} Hari"
            drawTextInCell(canvas, totalDaysStr, cx, currentY, colWidths[1], 30f, paint, Paint.Align.CENTER)
            
            // Upah Harian
            cx += colWidths[1]
            canvas.drawRect(cx, currentY, cx + colWidths[2], currentY + 30f, linePaint)
            drawTextInCell(canvas, formatRupiah(data.worker.baseWage), cx, currentY, colWidths[2], 30f, paint, Paint.Align.RIGHT, padding = 8f)
            
            // Total Upah
            cx += colWidths[2]
            canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 30f, linePaint)
            paint.isFakeBoldText = true
            drawTextInCell(canvas, formatRupiah(data.weeklyTotal), cx, currentY, colWidths[3], 30f, paint, Paint.Align.RIGHT, padding = 8f)
            
            currentY += 30f
        }
        
        // --- Total & Deductions (Always ensure enough space or start new page) ---
        if (currentY + 200f > PAGE_HEIGHT_PORTRAIT) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        // Total Upah Row
        paint.isFakeBoldText = true
        canvas.drawRect(startX, currentY, startX + colWidths[0] + colWidths[1] + colWidths[2], currentY + 30f, linePaint)
        drawTextInCell(canvas, "TOTAL UPAH KOTOR", startX, currentY, colWidths[0] + colWidths[1] + colWidths[2], 30f, paint, Paint.Align.CENTER)
        
        var cx = startX + colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 30f, linePaint)
        drawTextInCell(canvas, formatRupiah(workerData.sumOf { it.weeklyTotal }), cx, currentY, colWidths[3], 30f, paint, Paint.Align.RIGHT, padding = 8f)
        
        currentY += 45f
        
        // Deductions
        paint.textSize = 12f
        drawTextInCell(canvas, "RINCIAN POTONGAN & KASBON", startX, currentY, 515f, 25f, paint, Paint.Align.LEFT)
        currentY += 25f
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val totalPotongHutang = workerData.sumOf { it.adjustment?.privateKasbon ?: 0.0 }
        val tableBottomWidth = 365f
        val valueColWidth = 150f
        
        // Private Hutang
        canvas.drawRect(startX, currentY, startX + tableBottomWidth, currentY + 30f, linePaint)
        drawTextInCell(canvas, "Potongan Hutang (Pribadi)", startX, currentY, tableBottomWidth, 30f, paint, Paint.Align.LEFT, padding = 8f)
        drawTextInCell(canvas, payoutDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), startX, currentY, tableBottomWidth, 30f, paint, Paint.Align.RIGHT, padding = 40f)
        
        canvas.drawRect(startX + tableBottomWidth, currentY, startX + tableBottomWidth + valueColWidth, currentY + 30f, linePaint)
        drawTextInCell(canvas, formatRupiah(totalPotongHutang), startX + tableBottomWidth, currentY, valueColWidth, 30f, paint, Paint.Align.RIGHT, padding = 8f)
        currentY += 30f
        
        // Global Kasbon
        val sortedGlobalKasbon = globalKasbon.sortedBy { it.date }
        sortedGlobalKasbon.forEach { entry ->
            canvas.drawRect(startX, currentY, startX + tableBottomWidth, currentY + 30f, linePaint)
            drawTextInCell(canvas, "Kasbon Uang Makan / Lainnya", startX, currentY, tableBottomWidth, 30f, paint, Paint.Align.LEFT, padding = 8f)
            val entryDate = LocalDate.parse(entry.date)
            drawTextInCell(canvas, entryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), startX, currentY, tableBottomWidth, 30f, paint, Paint.Align.RIGHT, padding = 40f)
            
            canvas.drawRect(startX + tableBottomWidth, currentY, startX + tableBottomWidth + valueColWidth, currentY + 30f, linePaint)
            drawTextInCell(canvas, formatRupiah(entry.amount), startX + tableBottomWidth, currentY, valueColWidth, 30f, paint, Paint.Align.RIGHT, padding = 8f)
            currentY += 30f
        }
        
        // Final Row
        val netGajian = workerData.sumOf { it.weeklyTotal } - totalPotongHutang - globalKasbon.sumOf { it.amount }
        currentY += 15f
        
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawRect(startX, currentY, startX + tableBottomWidth, currentY + 45f, linePaint)
        drawTextInCell(canvas, "GAJIAN BERSIH MINGGU INI", startX, currentY, tableBottomWidth, 45f, paint, Paint.Align.CENTER)
        
        canvas.drawRect(startX + tableBottomWidth, currentY, startX + tableBottomWidth + valueColWidth, currentY + 45f, linePaint)
        paint.color = Color.parseColor("#1B5E20")
        drawTextInCell(canvas, formatRupiah(netGajian), startX + tableBottomWidth, currentY, valueColWidth, 45f, paint, Paint.Align.RIGHT, padding = 8f)
        
        pdfDocument.finishPage(page)
        saveAndShare(context, pdfDocument, "Laporan_Gajian_${payoutDate}")
    }

    fun generateKasbonPdf(
        context: Context,
        workerName: String,
        entries: List<PrivateKasbonEntry>
    ) {
        // Ensure chronological order for PDF
        val sortedEntries = entries.sortedBy { it.date }
        
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        }
        
        val startX = 40f
        val colWidths = floatArrayOf(155f, 110f, 110f, 140f)
        val headers = listOf("Tanggal", "Pinjam (+)", "Bayar (-)", "Saldo Hutang")

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        fun drawHeader() {
            paint.textSize = 20f
            paint.isFakeBoldText = true
            drawTextInCell(canvas, "Riwayat Kasbon: $workerName", 0f, 30f, PAGE_WIDTH_PORTRAIT, 30f, paint, Paint.Align.CENTER)
            
            val startY = 80f
            paint.textSize = 12f
            var cx = startX
            headers.forEachIndexed { i, h ->
                canvas.drawRect(cx, startY, cx + colWidths[i], startY + 35f, linePaint)
                drawTextInCell(canvas, h, cx, startY, colWidths[i], 35f, paint, Paint.Align.CENTER)
                cx += colWidths[i]
            }
            currentY = startY + 35f
        }

        drawHeader()
        paint.isFakeBoldText = false
        paint.textSize = 12f
        var runningBalance = 0.0
        
        sortedEntries.forEach { entry ->
            if (currentY > MARGIN_BOTTOM_PORTRAIT) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            runningBalance += entry.loanAmount
            runningBalance -= entry.paymentAmount
            
            var cx = startX
            // Date
            canvas.drawRect(cx, currentY, cx + colWidths[0], currentY + 30f, linePaint)
            val date = LocalDate.parse(entry.date)
            drawTextInCell(canvas, date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale("id", "ID"))), cx, currentY, colWidths[0], 30f, paint, Paint.Align.LEFT, padding = 8f)
            
            // Pinjam
            cx += colWidths[0]
            canvas.drawRect(cx, currentY, cx + colWidths[1], currentY + 30f, linePaint)
            if (entry.loanAmount > 0) {
                drawTextInCell(canvas, formatRupiah(entry.loanAmount), cx, currentY, colWidths[1], 30f, paint, Paint.Align.RIGHT, padding = 8f)
            }
            
            // Bayar
            cx += colWidths[1]
            canvas.drawRect(cx, currentY, cx + colWidths[2], currentY + 30f, linePaint)
            if (entry.paymentAmount > 0) {
                paint.color = Color.RED
                drawTextInCell(canvas, "-${formatRupiah(entry.paymentAmount)}", cx, currentY, colWidths[2], 30f, paint, Paint.Align.RIGHT, padding = 8f)
                paint.color = Color.BLACK
            } else if (entry.note == "Tidak di potong") {
                paint.color = Color.RED
                paint.textSize = 9f
                drawTextInCell(canvas, "Tidak di potong", cx, currentY, colWidths[2], 30f, paint, Paint.Align.CENTER)
                paint.textSize = 12f
                paint.color = Color.BLACK
            }
            
            // Saldo
            cx += colWidths[2]
            canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 30f, linePaint)
            paint.isFakeBoldText = true
            drawTextInCell(canvas, formatRupiah(runningBalance), cx, currentY, colWidths[3], 30f, paint, Paint.Align.RIGHT, padding = 8f)
            paint.isFakeBoldText = false
            
            currentY += 30f
        }
        
        // --- Footer ---
        if (currentY + 50f > PAGE_HEIGHT_PORTRAIT) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        paint.isFakeBoldText = true
        canvas.drawRect(startX, currentY, startX + colWidths[0] + colWidths[1] + colWidths[2], currentY + 45f, linePaint)
        drawTextInCell(canvas, "TOTAL SISA HUTANG", startX, currentY, colWidths[0] + colWidths[1] + colWidths[2], 45f, paint, Paint.Align.CENTER)
        
        val cx = startX + colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 45f, linePaint)
        drawTextInCell(canvas, formatRupiah(runningBalance), cx, currentY, colWidths[3], 45f, paint, Paint.Align.RIGHT, padding = 8f)
        
        pdfDocument.finishPage(page)
        saveAndShare(context, pdfDocument, "Laporan_Kasbon_${workerName.replace(" ", "_")}")
    }

    fun generateTransactionPdf(
        context: Context,
        reportTitle: String,
        dateRange: String,
        transactions: List<Transaction>
    ) {
        // Ensure chronological order for PDF (handles both formats)
        val sortedTransactions = transactions.sortedWith(compareBy { tx ->
            try {
                if (tx.date.contains("/")) {
                    val parts = tx.date.split("/")
                    if (parts.size == 3) {
                        val day = parts[0].padStart(2, '0')
                        val month = parts[1].padStart(2, '0')
                        val year = parts[2]
                        "$year$month$day"
                    } else tx.date
                } else tx.date
            } catch (e: Exception) { tx.date }
        })

        val pdfDocument = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        }
        
        val startX = 40f
        val colWidths = floatArrayOf(35f, 100f, 235f, 145f)
        val headers = listOf("No", "Tanggal", "Keterangan", "Nominal (Rp)")

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        fun drawHeader() {
            paint.textSize = 18f
            paint.isFakeBoldText = true
            drawTextInCell(canvas, "Laporan Transaksi: $reportTitle", 0f, 30f, PAGE_WIDTH_PORTRAIT, 30f, paint, Paint.Align.CENTER)
            
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.color = Color.GRAY
            drawTextInCell(canvas, dateRange, 0f, 55f, PAGE_WIDTH_PORTRAIT, 20f, paint, Paint.Align.CENTER)
            
            val startY = 90f
            paint.textSize = 11f
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            var cx = startX
            headers.forEachIndexed { i, h ->
                canvas.drawRect(cx, startY, cx + colWidths[i], startY + 35f, linePaint)
                drawTextInCell(canvas, h, cx, startY, colWidths[i], 35f, paint, Paint.Align.CENTER)
                cx += colWidths[i]
            }
            currentY = startY + 35f
        }

        drawHeader()
        paint.isFakeBoldText = false
        
        sortedTransactions.forEachIndexed { index, tx ->
            if (currentY > MARGIN_BOTTOM_PORTRAIT) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            var cx = startX
            // No
            canvas.drawRect(cx, currentY, cx + colWidths[0], currentY + 30f, linePaint)
            drawTextInCell(canvas, (index + 1).toString(), cx, currentY, colWidths[0], 30f, paint, Paint.Align.CENTER)
            cx += colWidths[0]
            
            // Tanggal
            canvas.drawRect(cx, currentY, cx + colWidths[1], currentY + 30f, linePaint)
            drawTextInCell(canvas, tx.date, cx, currentY, colWidths[1], 30f, paint, Paint.Align.CENTER)
            cx += colWidths[1]
            
            // Keterangan
            canvas.drawRect(cx, currentY, cx + colWidths[2], currentY + 30f, linePaint)
            drawTextInCell(canvas, tx.title, cx, currentY, colWidths[2], 30f, paint, Paint.Align.LEFT, padding = 8f)
            cx += colWidths[2]
            
            // Nominal
            canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 30f, linePaint)
            drawTextInCell(canvas, formatRupiah(tx.amount), cx, currentY, colWidths[3], 30f, paint, Paint.Align.RIGHT, padding = 8f)
            
            currentY += 30f
        }
        
        // Footer Total
        if (currentY + 50f > PAGE_HEIGHT_PORTRAIT) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PORTRAIT.toInt(), PAGE_HEIGHT_PORTRAIT.toInt(), pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        paint.isFakeBoldText = true
        val totalAmount = transactions.sumOf { it.amount }
        canvas.drawRect(startX, currentY, startX + colWidths[0] + colWidths[1] + colWidths[2], currentY + 40f, linePaint)
        drawTextInCell(canvas, "TOTAL PENGELUARAN", startX, currentY, colWidths[0] + colWidths[1] + colWidths[2], 40f, paint, Paint.Align.CENTER)
        
        val cx = startX + colWidths[0] + colWidths[1] + colWidths[2]
        canvas.drawRect(cx, currentY, cx + colWidths[3], currentY + 40f, linePaint)
        drawTextInCell(canvas, formatRupiah(totalAmount), cx, currentY, colWidths[3], 40f, paint, Paint.Align.RIGHT, padding = 8f)
        
        pdfDocument.finishPage(page)
        saveAndShare(context, pdfDocument, "Laporan_Transaksi_${reportTitle.replace(" ", "_")}")
    }

    /**
     * Helper to draw text within a defined cell rectangle with alignment and clipping.
     */
    private fun drawTextInCell(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: Paint,
        align: Paint.Align,
        padding: Float = 0f
    ) {
        canvas.save()
        canvas.clipRect(x + 1f, y + 1f, x + width - 1f, y + height - 1f)
        
        val originalAlign = paint.textAlign
        paint.textAlign = align
        
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val textY = y + (height - textHeight) / 2f - fontMetrics.ascent
        
        val textX = when (align) {
            Paint.Align.LEFT -> x + padding
            Paint.Align.CENTER -> x + (width / 2f)
            Paint.Align.RIGHT -> x + width - padding
        }
        
        // Multi-line support (simple)
        if (text.contains("\n")) {
            val lines = text.split("\n")
            val totalH = lines.size * textHeight
            var startLY = y + (height - totalH) / 2f - fontMetrics.ascent
            lines.forEach { line ->
                canvas.drawText(line, textX, startLY, paint)
                startLY += textHeight
            }
        } else {
            canvas.drawText(text, textX, textY, paint)
        }
        
        paint.textAlign = originalAlign
        canvas.restore()
    }

    private fun saveAndShare(context: Context, pdfDocument: PdfDocument, filename: String) {
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$filename.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(intent, "Buka PDF"))
            Toast.makeText(context, "PDF Berhasil disimpan di Downloads", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
