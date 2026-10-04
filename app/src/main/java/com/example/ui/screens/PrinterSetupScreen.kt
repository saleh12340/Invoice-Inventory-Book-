package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.StoreConfigEntity
import com.example.printer.PrinterConnectionState
import com.example.ui.components.ReceiptBitmapHelper
import com.example.ui.viewmodels.InvoiceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun PrinterSetupScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val printerState by viewModel.printerState.collectAsState()
    val storeConfigState by viewModel.storeConfig.collectAsState()
    val config = storeConfigState ?: StoreConfigEntity()

    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedPaperWidthDots by remember { mutableStateOf(384) } // 384 = 58mm POS, 576 = 80mm POS

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pairedDevices = viewModel.bluetoothPrinterManager.getPairedDevices()
        } else {
            Toast.makeText(context, "تتطلب الطباعة منح صلاحية البلوتوث", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(missing.toTypedArray())
        } else {
            pairedDevices = viewModel.bluetoothPrinterManager.getPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات طابعة الفواتير (بلوتوث)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // STATUS BANNER
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (printerState) {
                        is PrinterConnectionState.Connected -> Color(0xFFDCFCE7)
                        is PrinterConnectionState.Connecting -> Color(0xFFFEF9C3)
                        is PrinterConnectionState.Error -> Color(0xFFFEE2E2)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حالة الاتصال بالطابعة:",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (val state = printerState) {
                                is PrinterConnectionState.Connected -> "متصل بالطابعة: ${state.deviceName}"
                                is PrinterConnectionState.Connecting -> "جاري الاتصال بالطابعة..."
                                is PrinterConnectionState.Error -> state.message
                                else -> "غير متصل بالطابعة"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = when (printerState) {
                                is PrinterConnectionState.Connected -> Color(0xFF15803D)
                                is PrinterConnectionState.Error -> Color(0xFFB91C1C)
                                else -> Color.Black
                            }
                        )
                    }

                    if (printerState is PrinterConnectionState.Connected) {
                        Button(
                            onClick = { viewModel.bluetoothPrinterManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("قطع الاتصال", fontSize = 11.sp)
                        }
                    }
                }
            }

            // PAPER SIZE SELECTOR (58mm vs 80mm POS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "عرض ورق الطابعة الحرارية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPaperWidthDots == 384,
                            onClick = { selectedPaperWidthDots = 384 },
                            label = { Text("ورق صغير 58 مم (POS-58)") }
                        )
                        FilterChip(
                            selected = selectedPaperWidthDots == 576,
                            onClick = { selectedPaperWidthDots = 576 },
                            label = { Text("ورق كبير 80 مم (POS-80)") }
                        )
                    }
                }
            }

            // TEST PRINT BUTTON
            Button(
                onClick = {
                    val testBitmap = ReceiptBitmapHelper.createReceiptBitmap(
                        context = context,
                        storeConfig = config,
                        invoiceNumber = 9999,
                        dateString = "2026/10/04",
                        customerName = "تجربة طباعة الفاتورة",
                        paymentType = "نقداً",
                        dualRows = listOf(
                            com.example.ui.viewmodels.DualReceiptRow(
                                rightDescription = "صنف تجريبي 1",
                                rightQuantityStr = "1",
                                rightTotalAmountStr = "500",
                                leftDescription = "صنف تجريبي 2",
                                leftQuantityStr = "2",
                                leftTotalAmountStr = "1000"
                            )
                        )
                    )
                    viewModel.printInvoiceAsBitmap(testBitmap)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = printerState is PrinterConnectionState.Connected
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طباعة فاتورة تجريبية كصورة", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أجهزة البلوتوث المقترنة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                IconButton(onClick = { checkAndRequestPermissions() }) {
                    Icon(Icons.Outlined.BluetoothSearching, contentDescription = "تحديث القائمة")
                }
            }

            if (pairedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم يتم العثور على أجهزة بلوتوث مقترنة.\nيرجى اقتران الطابعة الحرارية أولاً من إعدادات البلوتوث بالجهاز.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pairedDevices) { device ->
                        val isCurrentConnected = (printerState as? PrinterConnectionState.Connected)?.deviceAddress == device.address
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.bluetoothPrinterManager.connectToDevice(device)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        tint = if (isCurrentConnected) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = device.name ?: "طابعة غير معروفة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (isCurrentConnected) {
                                    Badge(containerColor = Color(0xFF16A34A)) {
                                        Text("متصل الآن", color = Color.White, modifier = Modifier.padding(4.dp))
                                    }
                                } else {
                                    OutlinedButton(onClick = {
                                        coroutineScope.launch {
                                            viewModel.bluetoothPrinterManager.connectToDevice(device)
                                        }
                                    }) {
                                        Text("اتصال")
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
