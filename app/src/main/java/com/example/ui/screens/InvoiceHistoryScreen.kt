package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InvoiceWithItems
import com.example.printer.PrinterConnectionState
import com.example.ui.components.InteractiveReceiptView
import com.example.ui.components.ReceiptBitmapHelper
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodels.DualReceiptRow
import com.example.ui.viewmodels.InvoiceViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    viewModel: InvoiceViewModel,
    onEditInvoice: (InvoiceWithItems) -> Unit,
    onNavigateToPrinterSetup: () -> Unit
) {
    val context = LocalContext.current
    val invoicesWithItems by viewModel.allInvoices.collectAsState()
    val storeConfigState by viewModel.storeConfig.collectAsState()
    val storeConfig = storeConfigState ?: com.example.data.local.StoreConfigEntity()
    val printerState by viewModel.printerState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedInvoiceForDetail by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val formatter = DecimalFormat("#,##0.##")

    // Invoices are already sorted newest first by database query (invoiceId DESC)
    val filteredList = remember(invoicesWithItems, searchQuery) {
        if (searchQuery.isBlank()) invoicesWithItems
        else invoicesWithItems.filter {
            it.invoice.customerName.contains(searchQuery, ignoreCase = true) ||
            it.invoice.invoiceNumber.toString().contains(searchQuery) ||
            it.invoice.paymentType.contains(searchQuery)
        }
    }

    val totalSalesSum = remember(filteredList) {
        filteredList.sumOf { it.invoice.grandTotal }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل الفواتير (مرتب حسب الأحدث)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم العميل أو رقم الفاتورة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "عدد الفواتير:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${filteredList.size} فاتورة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "إجمالي المبيعات:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${formatter.format(totalSalesSum)} ${storeConfig.currencySymbol}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد فواتير مسجلة في السجل حتى الآن.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.invoice.invoiceId }) { item ->
                        val inv = item.invoice
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInvoiceForDetail = item }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "#${inv.invoiceNumber}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = inv.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = "${formatter.format(inv.grandTotal)} ${storeConfig.currencySymbol}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "التاريخ: ${inv.dateString} | الدفع: ${inv.paymentType} (${item.items.size} أصناف)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Edit in Editor Button
                                        IconButton(
                                            onClick = { onEditInvoice(item) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Print Button
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val bitmap = ReceiptBitmapHelper.createReceiptBitmapFromItems(
                                                        context = context,
                                                        storeConfig = storeConfig,
                                                        invoiceNumber = inv.invoiceNumber,
                                                        dateString = inv.dateString,
                                                        customerName = inv.customerName,
                                                        paymentType = inv.paymentType,
                                                        items = item.items
                                                    )
                                                    if (printerState is PrinterConnectionState.Connected) {
                                                        viewModel.printInvoiceAsBitmap(bitmap)
                                                    } else {
                                                        Toast.makeText(context, "الرجاء الاتصال بالطابعة أولاً من شاشة الطابعة", Toast.LENGTH_SHORT).show()
                                                        onNavigateToPrinterSetup()
                                                    }
                                                } catch (e: Throwable) {
                                                    Toast.makeText(context, "خطأ في الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(AppIcons.Print, contentDescription = "طباعة", modifier = Modifier.size(17.dp))
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { viewModel.deleteInvoice(inv.invoiceId) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedInvoiceForDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedInvoiceForDetail = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val bitmap = ReceiptBitmapHelper.createReceiptBitmapFromItems(
                                context = context,
                                storeConfig = storeConfig,
                                invoiceNumber = detail.invoice.invoiceNumber,
                                dateString = detail.invoice.dateString,
                                customerName = detail.invoice.customerName,
                                paymentType = detail.invoice.paymentType,
                                items = detail.items
                            )
                            if (printerState is PrinterConnectionState.Connected) {
                                viewModel.printInvoiceAsBitmap(bitmap)
                            } else {
                                Toast.makeText(context, "الرجاء الاتصال بالطابعة أولاً من شاشة الطابعة", Toast.LENGTH_SHORT).show()
                                onNavigateToPrinterSetup()
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(context, "خطأ في الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة حرارية")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        try {
                            val bitmap = ReceiptBitmapHelper.createReceiptBitmapFromItems(
                                context = context,
                                storeConfig = storeConfig,
                                invoiceNumber = detail.invoice.invoiceNumber,
                                dateString = detail.invoice.dateString,
                                customerName = detail.invoice.customerName,
                                paymentType = detail.invoice.paymentType,
                                items = detail.items
                            )
                            val uri = viewModel.getShareableImageUri(bitmap)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة")
                }
            },
            title = { Text("فاتورة #${detail.invoice.invoiceNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val allItems = detail.items
                    val half = (allItems.size + 1) / 2
                    val rightItems = allItems.take(half)
                    val leftItems = allItems.drop(half)
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

                    InteractiveReceiptView(
                        storeConfig = storeConfig,
                        invoiceNumber = detail.invoice.invoiceNumber,
                        onInvoiceNumberChange = {},
                        dateString = detail.invoice.dateString,
                        onDateStringChange = {},
                        customerName = detail.invoice.customerName,
                        onCustomerNameChange = {},
                        paymentType = detail.invoice.paymentType,
                        onPaymentTypeChange = {},
                        dualRows = rows,
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
