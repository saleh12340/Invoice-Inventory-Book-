package com.example.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.*
import com.example.data.local.*
import com.example.data.repository.InvoiceRepository
import com.example.printer.BluetoothPrinterManager
import com.example.printer.PrinterConnectionState
import com.example.printer.ThermalImageConverter
import com.example.ui.components.InvoicePdfHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DualReceiptRow(
    val id: String = UUID.randomUUID().toString(),
    var rightDescription: String = "",
    var rightQuantityStr: String = "1",
    var rightTotalAmountStr: String = "",
    var leftDescription: String = "",
    var leftQuantityStr: String = "1",
    var leftTotalAmountStr: String = ""
) {
    val rightTotal: Double get() = rightTotalAmountStr.toDoubleOrNull() ?: 0.0
    val leftTotal: Double get() = leftTotalAmountStr.toDoubleOrNull() ?: 0.0
    val hasRightData: Boolean get() = rightDescription.isNotBlank() || rightTotalAmountStr.isNotBlank()
    val hasLeftData: Boolean get() = leftDescription.isNotBlank() || leftTotalAmountStr.isNotBlank()
    val isCompletelyEmpty: Boolean get() = !hasRightData && !hasLeftData
}

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = InvoiceRepository(db)
    val bluetoothPrinterManager = BluetoothPrinterManager(application)
    val backupManager = BackupManager(application, repository)

    val storeConfig = repository.storeConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StoreConfigEntity()
    )

    val allInvoices = repository.allInvoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allProducts = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val printerState: StateFlow<PrinterConnectionState> = bluetoothPrinterManager.connectionState

    // Form State for Current Invoice
    var invoiceNumber by mutableStateOf(1001)
    var dateString by mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date()))
    var customerName by mutableStateOf("")
    var paymentType by mutableStateOf("نقداً") // "نقداً" or "أجل"
    var notes by mutableStateOf("")

    // Smart expanding dual-column rows: Starts with EXACTLY ONE ROW
    val dualRows = mutableStateListOf<DualReceiptRow>()

    var searchQuery by mutableStateOf("")

    private val _uiEventMessage = MutableStateFlow<String?>(null)
    val uiEventMessage: StateFlow<String?> = _uiEventMessage.asStateFlow()

    init {
        viewModelScope.launch {
            invoiceNumber = repository.getNextInvoiceNumber()
            if (dualRows.isEmpty()) {
                dualRows.add(DualReceiptRow()) // Starts with 1 empty row only!
            }
        }
    }

    fun clearUiMessage() {
        _uiEventMessage.value = null
    }

    fun resetForm() {
        viewModelScope.launch {
            invoiceNumber = repository.getNextInvoiceNumber()
            dateString = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())
            customerName = ""
            paymentType = "نقداً"
            notes = ""
            dualRows.clear()
            dualRows.add(DualReceiptRow()) // Starts with 1 row only!
        }
    }

    private fun checkAutoExpand(editedIndex: Int) {
        // When typing in the last row and it now has data, automatically append the next empty row!
        if (editedIndex == dualRows.lastIndex) {
            val lastRow = dualRows.last()
            if (lastRow.hasRightData || lastRow.hasLeftData) {
                dualRows.add(DualReceiptRow())
            }
        }
    }

    fun updateRightDescription(index: Int, desc: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(rightDescription = desc)
            checkAutoExpand(index)
        }
    }

    fun updateRightQuantity(index: Int, qty: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(rightQuantityStr = qty)
            checkAutoExpand(index)
        }
    }

    fun updateRightTotalAmount(index: Int, total: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(rightTotalAmountStr = total)
            checkAutoExpand(index)
        }
    }

    fun updateLeftDescription(index: Int, desc: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(leftDescription = desc)
            checkAutoExpand(index)
        }
    }

    fun updateLeftQuantity(index: Int, qty: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(leftQuantityStr = qty)
            checkAutoExpand(index)
        }
    }

    fun updateLeftTotalAmount(index: Int, total: String) {
        if (index in dualRows.indices) {
            dualRows[index] = dualRows[index].copy(leftTotalAmountStr = total)
            checkAutoExpand(index)
        }
    }

    fun addManualRow() {
        dualRows.add(DualReceiptRow())
    }

    fun removeRow(index: Int) {
        if (dualRows.size > 1 && index in dualRows.indices) {
            dualRows.removeAt(index)
        }
    }

    fun calculateRightSubtotal(): Double = dualRows.sumOf { it.rightTotal }
    fun calculateLeftSubtotal(): Double = dualRows.sumOf { it.leftTotal }
    fun calculateGrandTotal(): Double = calculateRightSubtotal() + calculateLeftSubtotal()

    fun saveInvoiceToDatabase(onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (customerName.isBlank()) {
                customerName = "عميل نقدي"
            }
            val itemsToSave = mutableListOf<InvoiceItemEntity>()
            dualRows.forEach { row ->
                if (row.hasRightData) {
                    val q = row.rightQuantityStr.toDoubleOrNull() ?: 1.0
                    val tot = row.rightTotal
                    itemsToSave.add(
                        InvoiceItemEntity(
                            invoiceId = 0,
                            description = row.rightDescription.ifBlank { "صنف" },
                            quantity = q,
                            unitPrice = if (q > 0) tot / q else tot,
                            totalAmount = tot
                        )
                    )
                }
                if (row.hasLeftData) {
                    val q = row.leftQuantityStr.toDoubleOrNull() ?: 1.0
                    val tot = row.leftTotal
                    itemsToSave.add(
                        InvoiceItemEntity(
                            invoiceId = 0,
                            description = row.leftDescription.ifBlank { "صنف" },
                            quantity = q,
                            unitPrice = if (q > 0) tot / q else tot,
                            totalAmount = tot
                        )
                    )
                }
            }

            if (itemsToSave.isEmpty()) {
                _uiEventMessage.value = "يرجى كتابة أصناف الفاتورة أولاً"
                return@launch
            }

            try {
                val grandTotal = calculateGrandTotal()
                val currentInvoiceNum = invoiceNumber
                val invoiceEntity = InvoiceEntity(
                    invoiceNumber = currentInvoiceNum,
                    dateString = dateString,
                    customerName = customerName,
                    paymentType = paymentType,
                    subtotal = grandTotal,
                    grandTotal = grandTotal,
                    notes = notes
                )

                val savedId = repository.saveInvoice(invoiceEntity, itemsToSave)
                _uiEventMessage.value = "تم حفظ الفاتورة بنجاح برقم #$currentInvoiceNum وحفظها في السجل!"

                // Advance to next invoice number and prepare fresh row
                invoiceNumber = repository.getNextInvoiceNumber()
                customerName = ""
                dualRows.clear()
                dualRows.add(DualReceiptRow())

                onSuccess(savedId)
            } catch (e: Exception) {
                _uiEventMessage.value = "حدث خطأ أثناء حفظ الفاتورة: ${e.localizedMessage}"
            }
        }
    }

    fun loadInvoiceForEditing(invoiceWithItems: InvoiceWithItems) {
        val inv = invoiceWithItems.invoice
        invoiceNumber = inv.invoiceNumber
        dateString = inv.dateString
        customerName = inv.customerName
        paymentType = inv.paymentType
        notes = inv.notes

        dualRows.clear()
        val allItems = invoiceWithItems.items
        val half = (allItems.size + 1) / 2
        val rightItems = allItems.take(half)
        val leftItems = allItems.drop(half)

        val maxCount = maxOf(rightItems.size, leftItems.size)
        for (i in 0 until maxCount) {
            val r = rightItems.getOrNull(i)
            val l = leftItems.getOrNull(i)
            dualRows.add(
                DualReceiptRow(
                    rightDescription = r?.description ?: "",
                    rightQuantityStr = r?.quantity?.toString() ?: "1",
                    rightTotalAmountStr = r?.totalAmount?.let { if (it > 0) it.toString() else "" } ?: "",
                    leftDescription = l?.description ?: "",
                    leftQuantityStr = l?.quantity?.toString() ?: "1",
                    leftTotalAmountStr = l?.totalAmount?.let { if (it > 0) it.toString() else "" } ?: ""
                )
            )
        }
        // Add 1 trailing empty row
        dualRows.add(DualReceiptRow())
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
            _uiEventMessage.value = "تم حذف الفاتورة"
        }
    }

    fun printInvoiceAsBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = bluetoothPrinterManager.printReceiptBitmap(bitmap)
            result.onSuccess {
                _uiEventMessage.value = "تمت طباعة الفاتورة حرارياً بنجاح!"
            }.onFailure { e ->
                _uiEventMessage.value = e.message ?: "خطأ في الطباعة"
            }
        }
    }

    fun saveInvoiceImageToGallery(bitmap: Bitmap): Uri? {
        val uri = ThermalImageConverter.saveBitmapToGallery(
            context = getApplication(),
            bitmap = bitmap,
            fileName = "$invoiceNumber"
        )
        if (uri != null) {
            _uiEventMessage.value = "تم حفظ الفاتورة كصورة في الاستوديو!"
        } else {
            _uiEventMessage.value = "تعذر حفظ الصورة"
        }
        return uri
    }

    fun getShareableImageUri(bitmap: Bitmap): Uri? {
        return ThermalImageConverter.getShareableUri(getApplication(), bitmap)
    }

    fun generateInvoicePdf(): File? {
        val config = storeConfig.value ?: StoreConfigEntity()
        return InvoicePdfHelper.createInvoicePdf(
            context = getApplication(),
            storeConfig = config,
            invoiceNumber = invoiceNumber,
            dateString = dateString,
            customerName = customerName,
            paymentType = paymentType,
            dualRows = dualRows
        )
    }

    fun getShareableInvoicePdfUri(): Uri? {
        val file = generateInvoicePdf() ?: return null
        return InvoicePdfHelper.getShareablePdfUri(getApplication(), file)
    }

    fun saveInvoicePdfToDownloads(): Uri? {
        val file = generateInvoicePdf() ?: return null
        val uri = InvoicePdfHelper.savePdfToDownloads(getApplication(), file, "$invoiceNumber")
        if (uri != null) {
            _uiEventMessage.value = "تم حفظ ملف PDF في مجلد التنزيلات بنجاح!"
        } else {
            _uiEventMessage.value = "تعذر حفظ ملف PDF"
        }
        return uri
    }

    fun generateInvoicePdfFromItems(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): File? {
        val config = storeConfig.value ?: StoreConfigEntity()
        return InvoicePdfHelper.createInvoicePdfFromItems(
            context = getApplication(),
            storeConfig = config,
            invoiceNumber = invoice.invoiceNumber,
            dateString = invoice.dateString,
            customerName = invoice.customerName,
            paymentType = invoice.paymentType,
            items = items
        )
    }

    fun getShareablePdfUriForInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Uri? {
        val file = generateInvoicePdfFromItems(invoice, items) ?: return null
        return InvoicePdfHelper.getShareablePdfUri(getApplication(), file)
    }

    fun addCatalogProductToInvoice(name: String, price: Double) {
        val priceStr = if (price > 0) price.toString() else ""
        val lastIdx = dualRows.lastIndex
        if (lastIdx >= 0) {
            val last = dualRows[lastIdx]
            if (!last.hasRightData) {
                dualRows[lastIdx] = last.copy(rightDescription = name, rightTotalAmountStr = priceStr)
                dualRows.add(DualReceiptRow())
            } else if (!last.hasLeftData) {
                dualRows[lastIdx] = last.copy(leftDescription = name, leftTotalAmountStr = priceStr)
                dualRows.add(DualReceiptRow())
            } else {
                dualRows.add(DualReceiptRow(rightDescription = name, rightTotalAmountStr = priceStr))
                dualRows.add(DualReceiptRow())
            }
        } else {
            dualRows.add(DualReceiptRow(rightDescription = name, rightTotalAmountStr = priceStr))
            dualRows.add(DualReceiptRow())
        }
        _uiEventMessage.value = "تم إدراج الصنف في الفاتورة"
    }

    fun addCatalogProduct(name: String, price: Double) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.saveProduct(ProductCatalogEntity(name = name, defaultUnitPrice = price))
                _uiEventMessage.value = "تم إضافة الصنف إلى الدليل"
            }
        }
    }

    fun deleteCatalogProduct(product: ProductCatalogEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _uiEventMessage.value = "تم حذف الصنف"
        }
    }

    fun updateStoreConfig(config: StoreConfigEntity) {
        viewModelScope.launch {
            repository.saveStoreConfig(config)
            _uiEventMessage.value = "تم حفظ إعدادات المحل/التاجر بنجاح"
        }
    }

    fun createBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupManager.createBackupJson()
                onResult(json)
            } catch (e: Throwable) {
                _uiEventMessage.value = "فشل في إنشاء النسخة الاحتياطية: ${e.localizedMessage}"
            }
        }
    }

    fun restoreFromBackupJson(jsonString: String, onComplete: (BackupRestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreFromJson(jsonString)
            when (result) {
                is BackupRestoreResult.Success -> {
                    _uiEventMessage.value = "تم استعادة ${result.invoicesCount} فاتورة و ${result.productsCount} صنف بنجاح!"
                }
                is BackupRestoreResult.Error -> {
                    _uiEventMessage.value = "فشل في الاستعادة: ${result.message}"
                }
            }
            onComplete(result)
        }
    }
}
