package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*
import com.example.ui.viewmodels.InvoiceViewModel
import java.text.DecimalFormat

@Composable
fun CustomersScreen(viewModel: InvoiceViewModel) {
    val invoices by viewModel.allInvoices.collectAsState()
    val config by viewModel.storeConfig.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<String?>(null) }
    val formatter = remember { DecimalFormat("#,##0.##") }

    val customers = remember(invoices, query) {
        invoices
            .filter { it.invoice.customerName.isNotBlank() && it.invoice.customerName != "عميل نقدي" }
            .groupBy { it.invoice.customerName.trim() }
            .map { (name, list) -> name to list.sortedByDescending { it.invoice.invoiceId } }
            .filter { query.isBlank() || it.first.contains(query, true) }
            .sortedByDescending { it.second.firstOrNull()?.invoice?.invoiceId ?: 0L }
    }

    Scaffold(containerColor = NeuBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("العملاء", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NeuTextPrimary)
            Surface(shape = RoundedCornerShape(14.dp), color = NeuSurfaceRaised) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = NeuTextMuted)
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ابحث عن العميل") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(customers, key = { it.first }) { pair ->
                    val name = pair.first
                    val list = pair.second
                    val total = list.sumOf { it.invoice.grandTotal }
                    NeuCard(Modifier.fillMaxWidth().clickable { selectedCustomer = name }, RoundedCornerShape(16.dp), contentPadding = PaddingValues(13.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                                Text(list.size.toString() + " فاتورة • آخر فاتورة #" + list.first().invoice.invoiceNumber, fontSize = 11.sp, color = NeuTextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatter.format(total), fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeuAccentBlue)
                                Text(config?.currencySymbol ?: "ر.ي", fontSize = 10.sp, color = NeuTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCustomer?.let { name ->
        val customerInvoices = invoices.filter { it.invoice.customerName.trim() == name }.sortedByDescending { it.invoice.invoiceId }
        AlertDialog(
            onDismissRequest = { selectedCustomer = null },
            containerColor = NeuSurface,
            title = { Text("فواتير العميل: " + name, fontWeight = FontWeight.Black, color = NeuTextPrimary) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customerInvoices, key = { it.invoice.invoiceId }) { item ->
                            Surface(shape = RoundedCornerShape(10.dp), color = NeuInsetBg, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("#" + item.invoice.invoiceNumber, fontWeight = FontWeight.Bold, color = NeuAccentBlue)
                                        Text(item.invoice.dateString, fontSize = 11.sp, color = NeuTextSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatter.format(item.invoice.grandTotal), fontWeight = FontWeight.Bold, color = NeuTextPrimary)
                                        Text(item.items.size.toString() + " أصناف", fontSize = 10.sp, color = NeuTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedCustomer = null }) { Text("إغلاق") } }
        )
    }
}
