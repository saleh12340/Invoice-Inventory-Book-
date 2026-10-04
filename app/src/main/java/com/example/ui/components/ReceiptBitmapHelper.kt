package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.StoreConfigEntity
import com.example.ui.viewmodels.DualReceiptRow

object ReceiptBitmapHelper {

    /**
     * Renders a 2-column paper invoice offscreen and generates a crisp Bitmap image.
     */
    fun createReceiptBitmap(
        context: Context,
        storeConfig: StoreConfigEntity,
        invoiceNumber: Int,
        dateString: String,
        customerName: String,
        paymentType: String,
        dualRows: List<DualReceiptRow>,
        widthPx: Int = 850
    ): Bitmap {
        // Exclude trailing completely empty rows when exporting/printing
        val rowsToPrint = dualRows.filterNot { it.isCompletelyEmpty }.ifEmpty {
            listOf(DualReceiptRow())
        }

        val composeView = ComposeView(context).apply {
            setContent {
                InteractiveReceiptView(
                    storeConfig = storeConfig,
                    invoiceNumber = invoiceNumber,
                    onInvoiceNumberChange = {},
                    dateString = dateString,
                    onDateStringChange = {},
                    customerName = customerName,
                    onCustomerNameChange = {},
                    paymentType = paymentType,
                    onPaymentTypeChange = {},
                    dualRows = rowsToPrint,
                    onUpdateRightDescription = { _, _ -> },
                    onUpdateRightQuantity = { _, _ -> },
                    onUpdateRightTotalAmount = { _, _ -> },
                    onUpdateLeftDescription = { _, _ -> },
                    onUpdateLeftQuantity = { _, _ -> },
                    onUpdateLeftTotalAmount = { _, _ -> },
                    onRemoveRow = {},
                    onAddManualRow = {},
                    isEditable = false
                )
            }
        }

        composeView.layoutParams = FrameLayout.LayoutParams(
            widthPx,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val specWidth = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val specHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        composeView.measure(specWidth, specHeight)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            composeView.measuredWidth,
            composeView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)

        return bitmap
    }

    /**
     * Overload for converting saved List<InvoiceItemEntity> into DualReceiptRow and creating Bitmap.
     */
    fun createReceiptBitmapFromItems(
        context: Context,
        storeConfig: StoreConfigEntity,
        invoiceNumber: Int,
        dateString: String,
        customerName: String,
        paymentType: String,
        items: List<InvoiceItemEntity>,
        widthPx: Int = 850
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
