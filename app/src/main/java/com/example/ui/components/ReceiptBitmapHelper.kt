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
     * Renders a crisp 2-column paper invoice directly using Android's native Canvas.
     * 100% crash-free, requires no window attachment, and produces high-clarity ESC/POS thermal output.
     */
    fun createReceiptBitmap(
        context: Context,
        storeConfig: StoreConfigEntity,
        invoiceNumber: Int,
        dateString: String,
        customerName: String,
        paymentType: String,
        dualRows: List<DualReceiptRow>,
        widthPx: Int = 576 // Standard 80mm thermal width (576 dots)
    ): Bitmap {
        // Exclude completely empty rows when rendering for printing/saving
        val activeRows = dualRows.filterNot { it.isCompletelyEmpty }.ifEmpty {
            listOf(DualReceiptRow(rightDescription = "صنف", rightQuantityStr = "1", rightTotalAmountStr = "0"))
        }

        val rightSubtotal = activeRows.sumOf { it.rightTotal }
        val leftSubtotal = activeRows.sumOf { it.leftTotal }
        val grandTotal = rightSubtotal + leftSubtotal

        // Dynamic height calculation
        val rowHeight = 32f
        val headerHeight = 120f
        val tableHeaderHeight = 32f
        val tableHeight = activeRows.size * rowHeight
        val subtotalsHeight = 24f
        val grandTotalHeight = 28f
        val compactFooterHeight = 20f
        val totalHeight = (headerHeight + tableHeaderHeight + tableHeight + subtotalsHeight + grandTotalHeight + compactFooterHeight + 14f).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val inkNavy = Color.rgb(30, 58, 138)
        val inkRed = Color.rgb(220, 38, 38)
        val borderGray = Color.rgb(180, 190, 210)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkNavy
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkNavy
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkRed
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkNavy
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val lightStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val fillHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkNavy
            style = Paint.Style.FILL
        }

        val fillLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 245, 255)
            style = Paint.Style.FILL
        }

        val padding = 14f
        val contentWidth = widthPx - 2 * padding
        var currentY = padding

        // 1. Outer Border Frame
        canvas.drawRect(padding / 2, padding / 2, widthPx - padding / 2, totalHeight - padding / 2, strokePaint)

        // 2. Compact Top Store Banner
        val storeName = storeConfig.storeNameArabic.ifEmpty { "بقالة العزي" }
        titlePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(storeName, widthPx - padding - 4f, currentY + 22f, titlePaint)

        val phoneText = "جوال: ${storeConfig.phone1.ifEmpty { "772437314" }}"
        textPaint.textSize = 13f
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(phoneText, widthPx - padding - 4f, currentY + 42f, textPaint)

        // Center Payment Type Checkbox
        boldPaint.textSize = 14f
        boldPaint.textAlign = Paint.Align.CENTER
        val payText = if (paymentType == "نقداً") "[✓] نقداً   [ ] أجل" else "[ ] نقداً   [✓] أجل"
        canvas.drawText(payText, widthPx / 2f, currentY + 24f, boldPaint)

        // Left Invoice Number & Date
        redPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("الرقم: #$invoiceNumber", padding + 6f, currentY + 22f, redPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 13f
        canvas.drawText("التاريخ: $dateString", padding + 6f, currentY + 42f, textPaint)

        currentY += 50f
        canvas.drawLine(padding, currentY, widthPx - padding, currentY, lightStrokePaint)

        // 3. Customer Name Line ("المطلوب من الأخ")
        currentY += 8f
        boldPaint.textAlign = Paint.Align.RIGHT
        boldPaint.textSize = 14f
        val custText = "المطلوب من الأخ: ${customerName.ifEmpty { "عميل نقدي" }}"
        canvas.drawText(custText, widthPx - padding - 4f, currentY + 16f, boldPaint)

        currentY += 26f

        // 4. Two-Column Table Setup
        // Split contentWidth into 2 halves: Right Half & Left Half
        val halfWidth = contentWidth / 2f
        val rightStartX = padding + halfWidth
        val leftStartX = padding

        // Section Column Widths (RTL: Total Amount -> Qty -> Description)
        // Total (28%), Qty (18%), Description (54%)
        val colTotalW = halfWidth * 0.28f
        val colQtyW = halfWidth * 0.18f
        val colDescW = halfWidth * 0.54f

        // Table Header Background
        canvas.drawRect(padding, currentY, widthPx - padding, currentY + tableHeaderHeight, fillHeaderPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Draw Right Section Header (RTL)
        canvas.drawText("القيمة", rightStartX + halfWidth - (colTotalW / 2f), currentY + 21f, headerTextPaint)
        canvas.drawText("العدد", rightStartX + colDescW + (colQtyW / 2f), currentY + 21f, headerTextPaint)
        canvas.drawText("التفاصيل (البيان)", rightStartX + (colDescW / 2f), currentY + 21f, headerTextPaint)

        // Draw Left Section Header (RTL)
        canvas.drawText("القيمة", leftStartX + halfWidth - (colTotalW / 2f), currentY + 21f, headerTextPaint)
        canvas.drawText("العدد", leftStartX + colDescW + (colQtyW / 2f), currentY + 21f, headerTextPaint)
        canvas.drawText("التفاصيل (البيان)", leftStartX + (colDescW / 2f), currentY + 21f, headerTextPaint)

        // Center vertical divider in table header
        canvas.drawLine(padding + halfWidth, currentY, padding + halfWidth, currentY + tableHeaderHeight, lightStrokePaint)

        currentY += tableHeaderHeight

        // 5. Data Rows
        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val rowTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkNavy
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (row in activeRows) {
            val rowY = currentY

            // Draw horizontal row bottom line
            canvas.drawLine(padding, rowY + rowHeight, widthPx - padding, rowY + rowHeight, lightStrokePaint)

            // Right Section Cells (Total -> Qty -> Desc)
            rowTotalPaint.textAlign = Paint.Align.CENTER
            if (row.rightTotalAmountStr.isNotBlank()) {
                canvas.drawText(row.rightTotalAmountStr, rightStartX + halfWidth - (colTotalW / 2f), rowY + 21f, rowTotalPaint)
            }

            rowTextPaint.textAlign = Paint.Align.CENTER
            if (row.rightQuantityStr.isNotBlank()) {
                canvas.drawText(row.rightQuantityStr, rightStartX + colDescW + (colQtyW / 2f), rowY + 21f, rowTextPaint)
            }

            rowTextPaint.textAlign = Paint.Align.RIGHT
            if (row.rightDescription.isNotBlank()) {
                val desc = if (row.rightDescription.length > 18) row.rightDescription.take(17) + ".." else row.rightDescription
                canvas.drawText(desc, rightStartX + colDescW - 4f, rowY + 21f, rowTextPaint)
            }

            // Left Section Cells (Total -> Qty -> Desc)
            rowTotalPaint.textAlign = Paint.Align.CENTER
            if (row.leftTotalAmountStr.isNotBlank()) {
                canvas.drawText(row.leftTotalAmountStr, leftStartX + halfWidth - (colTotalW / 2f), rowY + 21f, rowTotalPaint)
            }

            rowTextPaint.textAlign = Paint.Align.CENTER
            if (row.leftQuantityStr.isNotBlank()) {
                canvas.drawText(row.leftQuantityStr, leftStartX + colDescW + (colQtyW / 2f), rowY + 21f, rowTextPaint)
            }

            rowTextPaint.textAlign = Paint.Align.RIGHT
            if (row.leftDescription.isNotBlank()) {
                val desc = if (row.leftDescription.length > 18) row.leftDescription.take(17) + ".." else row.leftDescription
                canvas.drawText(desc, leftStartX + colDescW - 4f, rowY + 21f, rowTextPaint)
            }

            // Vertical line dividing right and left sections
            canvas.drawLine(padding + halfWidth, rowY, padding + halfWidth, rowY + rowHeight, lightStrokePaint)

            currentY += rowHeight
        }

        // 6. Subtotals (كل شيء إجمالي لحاله)
        canvas.drawRect(padding, currentY, widthPx - padding, currentY + subtotalsHeight, fillLightPaint)
        canvas.drawLine(padding, currentY + subtotalsHeight, widthPx - padding, currentY + subtotalsHeight, strokePaint)
        canvas.drawLine(padding + halfWidth, currentY, padding + halfWidth, currentY + subtotalsHeight, strokePaint)

        boldPaint.textSize = 12f
        boldPaint.color = inkNavy
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("إجمالي اليمين: ${formatter.format(rightSubtotal)} ${storeConfig.currencySymbol}", rightStartX + (halfWidth / 2f), currentY + 16f, boldPaint)
        canvas.drawText("إجمالي اليسار: ${formatter.format(leftSubtotal)} ${storeConfig.currencySymbol}", leftStartX + (halfWidth / 2f), currentY + 16f, boldPaint)

        currentY += subtotalsHeight + 2f

        // 7. Grand Total Directly Underneath (وكذلك إجمالي عام للكل تحته)
        canvas.drawRect(padding, currentY, widthPx - padding, currentY + grandTotalHeight, fillHeaderPaint)

        val grandTotalLabel = "المبلغ الإجمالي العام (Grand Total):"
        val grandTotalVal = "${formatter.format(grandTotal)} ${storeConfig.currencySymbol}"
        val whiteGrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 240, 138)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val whiteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(grandTotalLabel, widthPx - padding - 8f, currentY + 19f, whiteLabelPaint)
        canvas.drawText(grandTotalVal, padding + 8f, currentY + 19f, whiteGrandPaint)

        currentY += grandTotalHeight + 3f

        // 8. Ultra-Compact Signatures (تقليص توقيع المشتري وتوقيع البائع)
        textPaint.textSize = 9.5f
        textPaint.color = Color.DKGRAY
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("ت.البائع: ......", widthPx - padding - 8f, currentY + 12f, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(storeConfig.defaultDisclaimerNote.take(30), widthPx / 2f, currentY + 12f, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ت.المشتري: ......", padding + 8f, currentY + 12f, textPaint)

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
        widthPx: Int = 576
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
