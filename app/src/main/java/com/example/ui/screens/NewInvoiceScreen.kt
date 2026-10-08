package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreConfigEntity
import com.example.printer.PrinterConnectionState
import com.example.ui.components.InteractiveReceiptView
import com.example.ui.components.AutoSelectBasicTextField
import com.example.ui.components.ReceiptBitmapHelper
import com.example.ui.theme.*
import com.example.ui.viewmodels.InvoiceViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    viewModel: InvoiceViewModel,
    onNavigateToPrinterSetup: () -> Unit
) {
    val context = LocalContext.current
    val storeConfigState by viewModel.storeConfig.collectAsState()
    val storeConfig = storeConfigState ?: StoreConfigEntity()
    val allProducts by viewModel.allProducts.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val printerState by viewModel.printerState.collectAsState()

    var showProductPickerSheet by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val formatter = DecimalFormat("#,##0.##")
    val customerSuggestions = remember(allInvoices, viewModel.customerName) {
        val q = viewModel.customerName.trim()
        if (q.isBlank()) emptyList() else allInvoices.asSequence()
            .map { it.invoice.customerName.trim() }
            .filter { it.isNotBlank() && it != "عميل نقدي" && it.contains(q, ignoreCase = true) }
            .distinct()
            .take(4)
            .toList()
    }

    Scaffold(
        containerColor = NeuBackground,
        topBar = {
            // Neumorphic Top Bar
            Surface(
                color = NeuSurfaceRaised,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "فاتورة #${viewModel.invoiceNumber} - ${storeConfig.storeNameArabic.ifEmpty { "بقالة العزي" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeuTextPrimary
                        )
                        Text(
                            text = "",
                            fontSize = 1.sp,
                            color = Color.Transparent
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Quick Product Catalog Pick
                        NeuCircleButton(
                            onClick = { showProductPickerSheet = true },
                            size = 38.dp
                        ) {
                            Icon(
                                AppIcons.Category,
                                contentDescription = "دليل الأصناف",
                                tint = NeuAccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. Printer Status Indicator
                        NeuCircleButton(
                            onClick = onNavigateToPrinterSetup,
                            size = 38.dp
                        ) {
                            when (printerState) {
                                is PrinterConnectionState.Connected -> Icon(
                                    AppIcons.Print,
                                    contentDescription = "الطابعة متصلة",
                                    tint = NeuSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                is PrinterConnectionState.Connecting -> CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NeuAccentBlue
                                )
                                else -> Icon(
                                    AppIcons.PrintDisabled,
                                    contentDescription = "غير متصل بالطابعة",
                                    tint = NeuError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 3. Reset Form
                        NeuCircleButton(
                            onClick = { viewModel.resetForm() },
                            size = 38.dp
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "فاتورة جديدة",
                                tint = NeuTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Neumorphic Bottom Action Card
            NeuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                // Grand Total Display (Sunken Neumorphic Box)
                NeuInsetBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المبلغ الإجمالي الكلي:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeuTextSecondary
                        )
                        Text(
                            text = "${formatter.format(viewModel.calculateGrandTotal())} ${storeConfig.currencySymbol}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeuAccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Neumorphic Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Save Button (Soft Raised)
                    NeuButton(
                        onClick = { viewModel.saveInvoiceToDatabase() },
                        modifier = Modifier.weight(1f),
                        isPrimary = false
                    ) {
                        Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeuTextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                    }

                    // 2. Thermal Print Button (Vibrant #0072ff Glow)
                    NeuButton(
                        onClick = {
                            try {
                                val bitmap = ReceiptBitmapHelper.createReceiptBitmap(
                                    context = context,
                                    storeConfig = storeConfig,
                                    invoiceNumber = viewModel.invoiceNumber,
                                    dateString = viewModel.dateString,
                                    customerName = viewModel.customerName,
                                    paymentType = viewModel.paymentType,
                                    dualRows = viewModel.dualRows
                                )
                                if (printerState is PrinterConnectionState.Connected) {
                                    viewModel.printInvoiceAsBitmap(bitmap)
                                } else {
                                    Toast.makeText(context, "الرجاء الاتصال بالطابعة أولاً من شاشة الطابعة", Toast.LENGTH_SHORT).show()
                                    onNavigateToPrinterSetup()
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(context, "تعذر تجهيز الفاتورة للطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        isPrimary = true
                    ) {
                        Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة حرارية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 3. Save / Share Image
                    NeuButton(
                        onClick = { showPreviewDialog = true },
                        modifier = Modifier.weight(1f),
                        isPrimary = false
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeuAccentBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. QUICK ITEM ENTRY & SMART UNIT PRICE CALCULATION CARD (AT TOP OF PAGE)
            item {
                QuickItemEntryCard(
                    storeConfig = storeConfig,
                    allProducts = allProducts,
                    onAddItem = { name, qty, tot ->
                        viewModel.addQuickItem(name, qty, tot)
                    }
                )
            }

            // 2. MAIN DIRECT INPUT PAPER RECEIPT VIEW (DUAL COLUMN)
            item {
                InteractiveReceiptView(
                    storeConfig = storeConfig,
                    invoiceNumber = viewModel.invoiceNumber,
                    onInvoiceNumberChange = { viewModel.invoiceNumber = it },
                    dateString = viewModel.dateString,
                    onDateStringChange = { viewModel.dateString = it },
                    customerName = viewModel.customerName,
                    onCustomerNameChange = { viewModel.customerName = it },
                    paymentType = viewModel.paymentType,
                    onPaymentTypeChange = { viewModel.paymentType = it },
                    dualRows = viewModel.dualRows,
                    onUpdateRightDescription = { idx, v -> viewModel.updateRightDescription(idx, v) },
                    onUpdateRightQuantity = { idx, v -> viewModel.updateRightQuantity(idx, v) },
                    onUpdateRightTotalAmount = { idx, v -> viewModel.updateRightTotalAmount(idx, v) },
                    onUpdateLeftDescription = { idx, v -> viewModel.updateLeftDescription(idx, v) },
                    onUpdateLeftQuantity = { idx, v -> viewModel.updateLeftQuantity(idx, v) },
                    onUpdateLeftTotalAmount = { idx, v -> viewModel.updateLeftTotalAmount(idx, v) },
                    onRemoveRow = { idx -> viewModel.removeRow(idx) },
                    onAddManualRow = { viewModel.addManualRow() },
                    customerSuggestions = customerSuggestions,
                    onCustomerSuggestionClick = { selected -> viewModel.customerName = selected }
                )
            }
        }
    }

    // Quick Product Picker Modal
    if (showProductPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProductPickerSheet = false },
            containerColor = NeuSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر صنفاً لإدراجه مباشرة في الفاتورة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NeuTextPrimary
                    )
                    IconButton(onClick = { showProductPickerSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = NeuTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var searchFilter by remember { mutableStateOf("") }
                NeuInsetBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NeuTextMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = searchFilter,
                            onValueChange = { searchFilter = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = NeuTextPrimary),
                            cursorBrush = SolidColor(NeuAccentBlue),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (searchFilter.isEmpty()) {
                                    Text("ابحث عن صنف...", fontSize = 13.sp, color = NeuTextMuted)
                                }
                                inner()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val availableProducts = remember(allProducts, searchFilter) {
                    if (searchFilter.isBlank()) allProducts
                    else allProducts.filter { it.name.contains(searchFilter, ignoreCase = true) }
                }

                if (availableProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (allProducts.isEmpty()) "لا توجد أصناف في الدليل بعد. أضف أصنافاً من تبويب 'الأصناف'." else "لم يتم العثور على نتائج.",
                            color = NeuTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableProducts) { product ->
                            NeuCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addCatalogProductToInvoice(product.name, product.defaultUnitPrice)
                                        showProductPickerSheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                elevation = 4.dp,
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeuTextPrimary)
                                        Text(product.category, fontSize = 11.sp, color = NeuTextSecondary)
                                    }
                                    Text(
                                        text = "${formatter.format(product.defaultUnitPrice)} ${storeConfig.currencySymbol}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NeuAccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice Image Preview & Share Dialog
    if (showPreviewDialog) {
        val invoiceBitmap = remember(viewModel.dualRows, storeConfig) {
            ReceiptBitmapHelper.createReceiptBitmap(
                context = context,
                storeConfig = storeConfig,
                invoiceNumber = viewModel.invoiceNumber,
                dateString = viewModel.dateString,
                customerName = viewModel.customerName,
                paymentType = viewModel.paymentType,
                dualRows = viewModel.dualRows,
                widthPx = 2160 // 4K Ultra-HD resolution
            )
        }

        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor = NeuSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeuBadge(size = 38.dp) {
                        Icon(AppIcons.Pdf, contentDescription = null, tint = NeuAccentBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "تصدير ومشاركة الفاتورة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeuTextPrimary
                        )
                        Text(
                            text = "مستند PDF عالي الجودة أو صورة PNG",
                            fontSize = 11.sp,
                            color = NeuTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Share as PDF (Primary Action)
                    NeuButton(
                        onClick = {
                            val pdfUri = viewModel.getShareableInvoicePdfUri()
                            if (pdfUri != null) {
                                val shareIntent = com.example.ui.components.InvoicePdfHelper.createSharePdfIntent(
                                    context = context,
                                    pdfUri = pdfUri,
                                    invoiceNumber = viewModel.invoiceNumber,
                                    customerName = viewModel.customerName
                                )
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة كملف PDF"))
                            } else {
                                Toast.makeText(context, "تعذر إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = true
                    ) {
                        Icon(AppIcons.Pdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة كملف PDF احترافي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // 2. Secondary Row: Save PDF / Share Image / Save Image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Save PDF to Downloads
                        NeuButton(
                            onClick = {
                                viewModel.saveInvoicePdfToDownloads()
                            },
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        ) {
                            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeuAccentBlue)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("حفظ PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                        }

                        // Share Image
                        NeuButton(
                            onClick = {
                                val uri = viewModel.getShareableImageUri(invoiceBitmap)
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة صورة الفاتورة"))
                                } else {
                                    Toast.makeText(context, "تعذر تجهيز الصورة للمشاركة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeuTextPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("مشاركة صورة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                        }

                        // Save Image to Gallery
                        NeuButton(
                            onClick = {
                                viewModel.saveInvoiceImageToGallery(invoiceBitmap)
                            },
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        ) {
                            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeuTextPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("حفظ صورة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("إغلاق", color = NeuTextSecondary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    InteractiveReceiptView(
                        storeConfig = storeConfig,
                        invoiceNumber = viewModel.invoiceNumber,
                        onInvoiceNumberChange = {},
                        dateString = viewModel.dateString,
                        onDateStringChange = {},
                        customerName = viewModel.customerName,
                        onCustomerNameChange = {},
                        paymentType = viewModel.paymentType,
                        onPaymentTypeChange = {},
                        dualRows = viewModel.dualRows.filterNot { it.isCompletelyEmpty }.ifEmpty {
                            listOf(com.example.ui.viewmodels.DualReceiptRow())
                        },
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
        )
    }
}

@Composable
fun QuickItemEntryCard(
    storeConfig: StoreConfigEntity,
    allProducts: List<com.example.data.local.ProductCatalogEntity>,
    onAddItem: (name: String, quantity: String, total: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DecimalFormat("#,##0.##") }

    var itemName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var totalAmountStr by remember { mutableStateOf("") }
    var unitPriceStr by remember { mutableStateOf("") }
    var lastEditedField by remember { mutableStateOf("TOTAL") }

    val currentQty = quantityStr.toDoubleOrNull() ?: 1.0

    val deducedUnitPrice = remember(quantityStr, totalAmountStr, unitPriceStr, lastEditedField) {
        if (lastEditedField == "UNIT") {
            unitPriceStr.toDoubleOrNull() ?: 0.0
        } else {
            val tot = totalAmountStr.toDoubleOrNull() ?: 0.0
            if (currentQty > 0.0) tot / currentQty else 0.0
        }
    }

    val deducedTotal = remember(quantityStr, totalAmountStr, unitPriceStr, lastEditedField) {
        if (lastEditedField == "UNIT") {
            val up = unitPriceStr.toDoubleOrNull() ?: 0.0
            up * currentQty
        } else {
            totalAmountStr.toDoubleOrNull() ?: 0.0
        }
    }

    NeuCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 6.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        // 1. Header with icon, title and deduced unit price pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeuAccentBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Receipt,
                        contentDescription = null,
                        tint = NeuAccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "إضافة صنف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = NeuTextPrimary
                    )
                    Text(
                        text = "",
                        fontSize = 1.sp,
                        color = Color.Transparent
                    )
                }
            }

            // Real-time Unit Price Display Badge
            if (deducedUnitPrice > 0.0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeuAccentBlue.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuAccentBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "سعر الواحدة:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeuTextSecondary
                        )
                        Text(
                            text = "${formatter.format(deducedUnitPrice)} ${storeConfig.currencySymbol}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = NeuAccentBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Invoice Input Boxes (Arranged RTL as in the invoice table)
        // [ البيان (اسم الصنف) ] [ العدد ] [ القيمة (المبلغ الإجمالي) ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Field 1: البيان (اسم الصنف) - 48% weight
            NeuInsetBox(
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Text(
                        text = "البيان (اسم الصنف)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeuTextSecondary
                    )
                    BasicTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeuTextPrimary
                        ),
                        cursorBrush = SolidColor(NeuAccentBlue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (itemName.isEmpty()) {
                                Text("اسم الصنف...", fontSize = 11.5.sp, color = NeuTextMuted)
                            }
                            inner()
                        }
                    )
                }
            }

            // Field 2: العدد (الكمية) - 22% weight
            NeuInsetBox(
                modifier = Modifier.weight(0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "العدد",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeuTextSecondary
                    )
                    BasicTextField(
                        value = quantityStr,
                        onValueChange = {
                            quantityStr = it
                            if (lastEditedField == "UNIT" && unitPriceStr.isNotBlank()) {
                                val up = unitPriceStr.toDoubleOrNull() ?: 0.0
                                val q = it.toDoubleOrNull() ?: 1.0
                                totalAmountStr = if (up * q > 0) formatter.format(up * q) else ""
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = NeuAccentBlue,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(NeuAccentBlue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (quantityStr.isEmpty()) {
                                Text("1", fontSize = 12.sp, color = NeuTextMuted, textAlign = TextAlign.Center)
                            }
                            inner()
                        }
                    )
                }
            }

            // Field 3: القيمة (المبلغ الإجمالي) - 30% weight
            NeuInsetBox(
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الإجمالي",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeuTextSecondary
                    )
                    BasicTextField(
                        value = totalAmountStr,
                        onValueChange = {
                            totalAmountStr = it
                            lastEditedField = "TOTAL"
                            val tot = it.toDoubleOrNull()
                            val q = quantityStr.toDoubleOrNull() ?: 1.0
                            if (tot != null && q > 0) {
                                unitPriceStr = formatter.format(tot / q)
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeuTextPrimary,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(NeuAccentBlue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (totalAmountStr.isEmpty()) {
                                Text("0.0", fontSize = 11.5.sp, color = NeuTextMuted, textAlign = TextAlign.End)
                            }
                            inner()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Optional row: Direct unit-price entry / quick calculation toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "",
                    fontSize = 1.sp,
                    color = Color.Transparent,
                    fontWeight = FontWeight.Medium
                )
                NeuInsetBox(
                    modifier = Modifier.width(95.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BasicTextField(
                        value = unitPriceStr,
                        onValueChange = {
                            unitPriceStr = it
                            lastEditedField = "UNIT"
                            val up = it.toDoubleOrNull()
                            val q = quantityStr.toDoubleOrNull() ?: 1.0
                            if (up != null && q > 0) {
                                totalAmountStr = formatter.format(up * q)
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeuAccentBlue,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(NeuAccentBlue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        decorationBox = { inner ->
                            if (unitPriceStr.isEmpty()) {
                                Text("سعر الواحدة", fontSize = 9.sp, color = NeuTextMuted, textAlign = TextAlign.Center)
                            }
                            inner()
                        }
                    )
                }
            }

            if (currentQty > 1.0 && deducedUnitPrice > 0.0) {
                Text(
                    text = "(${quantityStr} × ${formatter.format(deducedUnitPrice)} = ${formatter.format(deducedTotal)})",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. WIDE ADD BUTTON ("زر الاضافه بشكل عريض")
        val effectiveTotal = if (lastEditedField == "UNIT" && deducedTotal > 0.0) deducedTotal.toString() else totalAmountStr
        NeuButton(
            onClick = {
                val tot = if (lastEditedField == "UNIT" && deducedTotal > 0.0) deducedTotal.toString() else totalAmountStr
                if (itemName.isNotBlank() || tot.isNotBlank()) {
                    onAddItem(itemName, quantityStr, tot)
                    itemName = ""
                    quantityStr = "1"
                    totalAmountStr = ""
                    unitPriceStr = ""
                    lastEditedField = "TOTAL"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            isPrimary = true,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                Icons.Default.AddCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val btnLabel = if (deducedUnitPrice > 0.0) {
                "+ إضافة الصنف إلى الفاتورة (سعر الواحدة: ${formatter.format(deducedUnitPrice)} ${storeConfig.currencySymbol})"
            } else {
                "+ إضافة الصنف إلى الفاتورة"
            }
            Text(
                text = btnLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = Color.White
            )
        }
    }
}
