package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreConfigEntity
import com.example.printer.PrinterConnectionState
import com.example.ui.components.InteractiveReceiptView
import com.example.ui.components.ReceiptBitmapHelper
import com.example.ui.theme.AppIcons
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
    val printerState by viewModel.printerState.collectAsState()

    var showProductPickerSheet by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val formatter = DecimalFormat("#,##0.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "فاتورة #${viewModel.invoiceNumber} - ${storeConfig.storeNameArabic.ifEmpty { "بقالة العزي" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "فاتورة ذكية تتوسع تلقائياً مع إدخال البيانات",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick Product Catalog Pick
                    IconButton(onClick = { showProductPickerSheet = true }) {
                        Icon(AppIcons.Category, contentDescription = "دليل الأصناف")
                    }

                    // Printer Status Indicator
                    IconButton(onClick = onNavigateToPrinterSetup) {
                        when (printerState) {
                            is PrinterConnectionState.Connected -> Icon(
                                AppIcons.Print,
                                contentDescription = "الطابعة متصلة",
                                tint = Color(0xFF16A34A)
                            )
                            is PrinterConnectionState.Connecting -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            else -> Icon(
                                AppIcons.PrintDisabled,
                                contentDescription = "غير متصل بالطابعة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Reset Form
                    IconButton(onClick = { viewModel.resetForm() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "فاتورة جديدة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Grand Total Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المبلغ الإجمالي الكلي:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${formatter.format(viewModel.calculateGrandTotal())} ${storeConfig.currencySymbol}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Save Button
                        Button(
                            onClick = { viewModel.saveInvoiceToDatabase() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Thermal Print Button
                        Button(
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
                            contentPadding = PaddingValues(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                        ) {
                            Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة حرارية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Save / Share Image
                        OutlinedButton(
                            onClick = { showPreviewDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صورة/واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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
            // MAIN DIRECT INPUT PAPER RECEIPT VIEW (DUAL COLUMN)
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
                    onUpdateRightDescription = { idx, desc -> viewModel.updateRightDescription(idx, desc) },
                    onUpdateRightQuantity = { idx, qty -> viewModel.updateRightQuantity(idx, qty) },
                    onUpdateRightTotalAmount = { idx, total -> viewModel.updateRightTotalAmount(idx, total) },
                    onUpdateLeftDescription = { idx, desc -> viewModel.updateLeftDescription(idx, desc) },
                    onUpdateLeftQuantity = { idx, qty -> viewModel.updateLeftQuantity(idx, qty) },
                    onUpdateLeftTotalAmount = { idx, total -> viewModel.updateLeftTotalAmount(idx, total) },
                    onRemoveRow = { idx -> viewModel.removeRow(idx) },
                    onAddManualRow = { viewModel.addManualRow() },
                    isEditable = true
                )
            }
        }
    }

    // MODAL 1: PICK FROM PRODUCTS CATALOG
    if (showProductPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showProductPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "اختر صنفاً لإدراجه المباشر في الفاتورة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (allProducts.isEmpty()) {
                    Text("لا توجد منتجات مسجلة في دليل الأصناف.", color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allProducts) { prod ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addCatalogProductToInvoice(prod.name, prod.defaultUnitPrice)
                                        showProductPickerSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${formatter.format(prod.defaultUnitPrice)} ${storeConfig.currencySymbol}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MODAL 2: FULL-SCREEN PREVIEW & SAVE/SHARE IMAGE DIALOG
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val bitmap = ReceiptBitmapHelper.createReceiptBitmap(
                            context = context,
                            storeConfig = storeConfig,
                            invoiceNumber = viewModel.invoiceNumber,
                            dateString = viewModel.dateString,
                            customerName = viewModel.customerName,
                            paymentType = viewModel.paymentType,
                            dualRows = viewModel.dualRows
                        )
                        viewModel.saveInvoiceImageToGallery(bitmap)
                    }
                ) {
                    Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ في الاستوديو")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val bitmap = ReceiptBitmapHelper.createReceiptBitmap(
                            context = context,
                            storeConfig = storeConfig,
                            invoiceNumber = viewModel.invoiceNumber,
                            dateString = viewModel.dateString,
                            customerName = viewModel.customerName,
                            paymentType = viewModel.paymentType,
                            dualRows = viewModel.dualRows
                        )
                        val shareUri = viewModel.getShareableImageUri(bitmap)
                        if (shareUri != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة صورة الفاتورة"))
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة واتساب")
                }
            },
            title = { Text("صورة الفاتورة الحرارية", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val rowsToPreview = viewModel.dualRows.filterNot { it.isCompletelyEmpty }.ifEmpty {
                        listOf(viewModel.dualRows.firstOrNull() ?: com.example.ui.viewmodels.DualReceiptRow())
                    }
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
                        dualRows = rowsToPreview,
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
