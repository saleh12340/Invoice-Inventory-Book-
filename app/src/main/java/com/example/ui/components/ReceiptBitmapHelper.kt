package com.example.ui.components

import android.content.Context
import android.graphics.*
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.StoreConfigEntity
import com.example.ui.viewmodels.DualReceiptRow
import java.text.DecimalFormat

object ReceiptBitmapHelper {

    private val formatter = DecimalFormat("#,##0.##")

    /**
     * Renders a crisp, high-contrast 2-column receipt without heavy table boxes or gridlines.
     * Uses solid bold, thick black text optimized for 58mm / 80mm ESC/POS thermal printers.
     */
    fun createReceiptBitmap(
        context: Context,
        storeConfig: StoreConfigEntity,
        invoiceNumber: Int,
        dateString: String,
        customerName: String,
        paymentType: String,
        dualRows: List<DualReceiptRow>,
        widthPx: Int = 384 // Default 384px (58mm 1-to-1 native thermal resolution)
    ): Bitmap {
        // Filter out completely empty rows
        val activeRows = dualRows.filterNot { it.isCompletelyEmpty }.ifEmpty {
            listOf(DualReceiptRow(rightDescription = "صنف", rightQuantityStr = "1", rightTotalAmountStr = "0"))
        }

        val rightSubtotal = activeRows.sumOf { it.rightTotal }
        val leftSubtotal = activeRows.sumOf { it.leftTotal }
        val grandTotal = rightSubtotal + leftSubtotal

        // Dynamic height calculations
        val headerHeight = 90f
        val tableHeaderHeight = 26f
        val rowHeight = 24f
        val tableHeight = activeRows.size * rowHeight
        val subtotalsHeight = 24f
        val grandTotalHeight = 30f
        val footerHeight = 22f
        val padding = 8f
        val totalHeight = (headerHeight + tableHeaderHeight + tableHeight + subtotalsHeight + grandTotalHeight + footerHeight + 20f).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Ultra-Bold, Solid Black Paints (No gray, no alpha, maximum thermal heat transfer)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val heavyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val smallBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }

        val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        }

        val solidBlackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val whiteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val contentWidth = widthPx - (2 * padding)
        var currentY = padding

        // 1. TOP HEADER (اسم المحل، الهاتف، نوع الدفع، الرقم والتاريخ)
        val storeName = storeConfig.storeNameArabic.ifEmpty { "بقالة العزي" }
        titlePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(storeName, widthPx - padding, currentY + 20f, titlePaint)

        // Store phone
        val phoneText = "ج: ${storeConfig.phone1.ifEmpty { "772437314" }}"
        smallBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(phoneText, widthPx - padding, currentY + 38f, smallBoldPaint)

        // Payment type (Center)
        boldPaint.textAlign = Paint.Align.CENTER
        val payText = if (paymentType == "نقداً") "[✓] نقداً   [ ] أجل" else "[ ] نقداً   [✓] أجل"
        canvas.drawText(payText, widthPx / 2f, currentY + 20f, boldPaint)

        // Invoice Number & Date (Left)
        heavyBoldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("الرقم: #$invoiceNumber", padding, currentY + 20f, heavyBoldPaint)

        smallBoldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("التاريخ: $dateString", padding, currentY + 38f, smallBoldPaint)

        currentY += 46f

        // Separator line under store info
        canvas.drawLine(padding, currentY, widthPx - padding, currentY, linePaint)
        currentY += 6f

        // 2. CUSTOMER NAME LINE ("المطلوب من الأخ")
        boldPaint.textAlign = Paint.Align.RIGHT
        val custText = "المطلوب من الأخ: ${customerName.ifEmpty { "عميل نقدي" }}"
        canvas.drawText(custText, widthPx - padding, currentY + 14f, boldPaint)

        currentY += 22f

        // 3. TWO-COLUMN TABLE LAYOUT (WITHOUT CELL BOXES OR SQUARES)
        // Split contentWidth into 2 halves: Right Half (50%) & Left Half (50%)
        val halfWidth = contentWidth / 2f
        val rightStartX = padding + halfWidth
        val leftStartX = padding

        // Sub-column Widths (RTL: Total Amount 30%, Qty 18%, Description 52%)
        val colTotalW = halfWidth * 0.30f
        val colQtyW = halfWidth * 0.18f
        val colDescW = halfWidth * 0.52f

        // Top line for column headers
        canvas.drawLine(padding, currentY, widthPx - padding, currentY, linePaint)

        // Draw Column Headers in Bold Black
        smallBoldPaint.textAlign = Paint.Align.CENTER

        // Right Half Headers (RTL)
        canvas.drawText("القيمة", rightStartX + halfWidth - (colTotalW / 2f), currentY + 17f, smallBoldPaint)
        canvas.drawText("العدد", rightStartX + colDescW + (colQtyW / 2f), currentY + 17f, smallBoldPaint)
        smallBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("البيان", rightStartX + colDescW - 2f, currentY + 17f, smallBoldPaint)

        // Left Half Headers (RTL)
        smallBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("القيمة", leftStartX + halfWidth - (colTotalW / 2f), currentY + 17f, smallBoldPaint)
        canvas.drawText("العدد", leftStartX + colDescW + (colQtyW / 2f), currentY + 17f, smallBoldPaint)
        smallBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("البيان", leftStartX + colDescW - 2f, currentY + 17f, smallBoldPaint)

        // Vertical divider between right and left columns in the header
        canvas.drawLine(padding + halfWidth, currentY, padding + halfWidth, currentY + tableHeaderHeight, thinLinePaint)

        currentY += tableHeaderHeight

        // Bottom line under headers
        canvas.drawLine(padding, currentY, widthPx - padding, currentY, linePaint)

        // 4. DATA ROWS (Clean text alignment without any boxes or rectangles)
        val rowDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val rowTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        for (row in activeRows) {
            val rowY = currentY

            // Right Section Cells (Total -> Qty -> Desc)
            rowTotalPaint.textAlign = Paint.Align.CENTER
            if (row.rightTotalAmountStr.isNotBlank()) {
                canvas.drawText(row.rightTotalAmountStr, rightStartX + halfWidth - (colTotalW / 2f), rowY + 17f, rowTotalPaint)
            }

            rowDescPaint.textAlign = Paint.Align.CENTER
            if (row.rightQuantityStr.isNotBlank()) {
                canvas.drawText(row.rightQuantityStr, rightStartX + colDescW + (colQtyW / 2f), rowY + 17f, rowDescPaint)
            }

            rowDescPaint.textAlign = Paint.Align.RIGHT
            if (row.rightDescription.isNotBlank()) {
                val desc = if (row.rightDescription.length > 16) row.rightDescription.take(15) + ".." else row.rightDescription
                canvas.drawText(desc, rightStartX + colDescW - 2f, rowY + 17f, rowDescPaint)
            }

            // Left Section Cells (Total -> Qty -> Desc)
            rowTotalPaint.textAlign = Paint.Align.CENTER
            if (row.leftTotalAmountStr.isNotBlank()) {
                canvas.drawText(row.leftTotalAmountStr, leftStartX + halfWidth - (colTotalW / 2f), rowY + 17f, rowTotalPaint)
            }

            rowDescPaint.textAlign = Paint.Align.CENTER
            if (row.leftQuantityStr.isNotBlank()) {
                canvas.drawText(row.leftQuantityStr, leftStartX + colDescW + (colQtyW / 2f), rowY + 17f, rowDescPaint)
            }

            rowDescPaint.textAlign = Paint.Align.RIGHT
            if (row.leftDescription.isNotBlank()) {
                val desc = if (row.leftDescription.length > 16) row.leftDescription.take(15) + ".." else row.leftDescription
                canvas.drawText(desc, leftStartX + colDescW - 2f, rowY + 17f, rowDescPaint)
            }

            // Central vertical divider between columns
            canvas.drawLine(padding + halfWidth, rowY, padding + halfWidth, rowY + rowHeight, thinLinePaint)

            // Dotted/light horizontal row separator
            canvas.drawLine(padding, rowY + rowHeight, widthPx - padding, rowY + rowHeight, thinLinePaint)

            currentY += rowHeight
        }

        currentY += 2f

        // 5. SUBTOTALS (كل شيء إجمالي لحاله)
        boldPaint.textSize = 12.5f
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("إجمالي اليمين: ${formatter.format(rightSubtotal)} ${storeConfig.currencySymbol}", rightStartX + (halfWidth / 2f), currentY + 16f, boldPaint)
        canvas.drawText("إجمالي اليسار: ${formatter.format(leftSubtotal)} ${storeConfig.currencySymbol}", leftStartX + (halfWidth / 2f), currentY + 16f, boldPaint)

        // Central divider in subtotals
        canvas.drawLine(padding + halfWidth, currentY, padding + halfWidth, currentY + subtotalsHeight, thinLinePaint)
        currentY += subtotalsHeight

        // Dividing line before Grand Total
        canvas.drawLine(padding, currentY, widthPx - padding, currentY, linePaint)
        currentY += 3f

        // 6. GRAND TOTAL DIRECTLY UNDERNEATH (المبلغ الإجمالي العام)
        // Solid black bar for ultra-high contrast on thermal paper
        canvas.drawRect(padding, currentY, widthPx - padding, currentY + grandTotalHeight, solidBlackFill)

        whiteTextPaint.textAlign = Paint.Align.RIGHT
        whiteTextPaint.textSize = 13.5f
        canvas.drawText("المبلغ الإجمالي العام:", widthPx - padding - 6f, currentY + 20f, whiteTextPaint)

        whiteTextPaint.textAlign = Paint.Align.LEFT
        whiteTextPaint.textSize = 16f
        canvas.drawText("${formatter.format(grandTotal)} ${storeConfig.currencySymbol}", padding + 6f, currentY + 20f, whiteTextPaint)

        currentY += grandTotalHeight + 4f

        // 7. ULTRA-COMPACT SIGNATURES & DISCLAIMER
        smallBoldPaint.textSize = 10f
        smallBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("ت.البائع: ......", widthPx - padding, currentY + 14f, smallBoldPaint)

        smallBoldPaint.textAlign = Paint.Align.CENTER
        val note = storeConfig.defaultDisclaimerNote.take(28)
        canvas.drawText(note, widthPx / 2f, currentY + 14f, smallBoldPaint)

        smallBoldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ت.المشتري: ......", padding, currentY + 14f, smallBoldPaint)

        return bitmap
    }

    /**
     * Converts a list of InvoiceItemEntity into DualReceiptRow and renders the Bitmap.
     */
    fun createReceiptBitmapFromItems(
        context: Context,
        storeConfig: StoreConfigEntity,
        invoiceNumber: Int,
        dateString: String,
        customerName: String,
        paymentType: String,
        items: List<InvoiceItemEntity>,
        widthPx: Int = 384
    ): Bitmap {
        val half = (items.size + 1) / 2
        val rightItems = items.take(half)
        val leftItems = items.drop(half)

        val maxCount = maxOf(rightItems.size, leftItems.size).coerceAtLeast(1)
        val rows = (0 until maxCount).map { i ->
            val r = rightItems.getOrNull(i)
            val l = leftItems.getOrNull(i)
            DualReceiptRow(
                rightDescription = r?.description ?: "",
                rightQuantityStr = r?.quantity?.let { if (it > 0) it.toString() else "1" } ?: "1",
                rightTotalAmountStr = r?.totalAmount?.let { if (it > 0) it.toString() else "" } ?: "",
                leftDescription = l?.description ?: "",
                leftQuantityStr = l?.quantity?.let { if (it > 0) it.toString() else "1" } ?: "1",
                leftTotalAmountStr = l?.totalAmount?.let { if (it > 0) it.toString() else "" } ?: ""
            )
        }

        return createReceiptBitmap(
            context = context,
            storeConfig = storeConfig,
            invoiceNumber = invoiceNumber,
            dateString = dateString,
            customerName = customerName,
            paymentType = paymentType,
            dualRows = rows,
            widthPx = widthPx
        )
    }
}
