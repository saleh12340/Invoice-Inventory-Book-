package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupRestoreResult
import com.example.data.local.StoreConfigEntity
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodels.InvoiceViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val configState by viewModel.storeConfig.collectAsState()
    val config = configState ?: StoreConfigEntity()

    var storeNameArabic by remember(config) { mutableStateOf(config.storeNameArabic) }
    var storeNameEnglish by remember(config) { mutableStateOf(config.storeNameEnglish) }
    var storeSubtitle by remember(config) { mutableStateOf(config.storeSubtitle) }
    var logoText by remember(config) { mutableStateOf(config.logoText) }
    var address by remember(config) { mutableStateOf(config.address) }
    var phone1 by remember(config) { mutableStateOf(config.phone1) }
    var phone2 by remember(config) { mutableStateOf(config.phone2) }
    var currencySymbol by remember(config) { mutableStateOf(config.currencySymbol) }
    var nextInvoiceNumberStr by remember(config) { mutableStateOf(config.nextInvoiceNumber.toString()) }
    var defaultDisclaimerNote by remember(config) { mutableStateOf(config.defaultDisclaimerNote) }

    // Dialog & file picker states for Backup / Restore
    var backupJsonToExport by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreJsonData by remember { mutableStateOf<String?>(null) }
    var isOperatingBackup by remember { mutableStateOf(false) }

    // File saver launcher for JSON backup
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && backupJsonToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(backupJsonToExport!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح في ملف", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ أثناء حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // File picker launcher for Restore
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                }
                if (!jsonString.isNullOrBlank()) {
                    restoreJsonData = jsonString
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "الملف المحدد فارغ!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل في قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات المتجر والبيانات", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val updated = StoreConfigEntity(
                        id = 1,
                        storeNameArabic = storeNameArabic,
                        storeNameEnglish = storeNameEnglish,
                        storeSubtitle = storeSubtitle,
                        logoText = logoText,
                        address = address,
                        phone1 = phone1,
                        phone2 = phone2,
                        currencySymbol = currencySymbol,
                        nextInvoiceNumber = nextInvoiceNumberStr.toIntOrNull() ?: 1001,
                        defaultDisclaimerNote = defaultDisclaimerNote
                    )
                    viewModel.updateStoreConfig(updated)
                },
                icon = { Icon(AppIcons.Save, contentDescription = null) },
                text = { Text("حفظ الإعدادات", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. BACKUP & RESTORE CARD (النسخ الاحتياطي والاستعادة)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي واستعادة البيانات",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "احفظ نسخة من جميع فواتيرك وأصنافك وإعداداتك لتجنب فقدان البيانات أو لنقلها لهاتف آخر بكل سهولة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share / Export JSON
                        Button(
                            onClick = {
                                isOperatingBackup = true
                                viewModel.createBackupJson { json ->
                                    isOperatingBackup = false
                                    backupJsonToExport = json

                                    // Open system share sheet
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - بقالة العزي")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة النسخة الاحتياطية (واتساب / درايف / ملف)")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isOperatingBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة نسخة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Save as file in storage
                        OutlinedButton(
                            onClick = {
                                isOperatingBackup = true
                                viewModel.createBackupJson { json ->
                                    isOperatingBackup = false
                                    backupJsonToExport = json
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    exportFileLauncher.launch("Fatooraty_Backup_$dateStr.json")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isOperatingBackup
                        ) {
                            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ كملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Restore button
                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E3A8A))
                    ) {
                        Icon(AppIcons.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة نسخة احتياطية من ملف (JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. STORE NAME & LOGO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "اسم المحل والتاجر",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = storeNameArabic,
                        onValueChange = { storeNameArabic = it },
                        label = { Text("اسم المحل / التاجر بالعربي") },
                        placeholder = { Text("مثال: بقالة العزي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(AppIcons.Store, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = storeNameEnglish,
                        onValueChange = { storeNameEnglish = it },
                        label = { Text("اسم المحل بالإنجليزي (اختياري)") },
                        placeholder = { Text("مثال: Al-Ezzi Grocery") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = storeSubtitle,
                        onValueChange = { storeSubtitle = it },
                        label = { Text("النشاط / الوصف الإضافي") },
                        placeholder = { Text("مثال: مواد غذائية واستهلاكية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 3. CONTACT & ADDRESS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "بيانات الاتصال والعنوان",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = phone1,
                        onValueChange = { phone1 = it },
                        label = { Text("رقم الهاتف / الجوال الأساسي") },
                        placeholder = { Text("مثال: 772437314") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = phone2,
                        onValueChange = { phone2 = it },
                        label = { Text("رقم هاتف إضافي (اختياري)") },
                        placeholder = { Text("مثال: 770000000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("عنوان المحل") },
                        placeholder = { Text("صنعاء - شارع ...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 4. INVOICE OPTIONS & CURRENCY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "خيارات الفاتورة والعملة",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("رمز العملة") },
                            placeholder = { Text("ر.ي / ر.س / $") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = nextInvoiceNumberStr,
                            onValueChange = { nextInvoiceNumberStr = it },
                            label = { Text("رقم التسلسل التالي") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = defaultDisclaimerNote,
                        onValueChange = { defaultDisclaimerNote = it },
                        label = { Text("ملاحظة وتعهد أسفل الفاتورة") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && restoreJsonData != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                restoreJsonData = null
            },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("تنبيه: سيتم استعادة الفواتير وقائمة الأصناف وإعدادات المحل من النسخة المحددة ودمجها في قاعدة البيانات.\n\nهل تريد المتابعة وتأكيد الاستعادة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = restoreJsonData!!
                        showRestoreConfirmDialog = false
                        restoreJsonData = null
                        viewModel.restoreFromBackupJson(json) { result ->
                            when (result) {
                                is BackupRestoreResult.Success -> {
                                    Toast.makeText(context, "تمت الاستعادة بنجاح: ${result.invoicesCount} فاتورة و ${result.productsCount} صنف!", Toast.LENGTH_LONG).show()
                                }
                                is BackupRestoreResult.Error -> {
                                    Toast.makeText(context, "خطأ: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("نعم، استعادة الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        restoreJsonData = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
