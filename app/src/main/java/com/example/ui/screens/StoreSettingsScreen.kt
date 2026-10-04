package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreConfigEntity
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodels.InvoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsScreen(viewModel: InvoiceViewModel) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بيانات المحل والتاجر (ترويسة الفاتورة)", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        placeholder = { Text("مثال: محلات فايز مثنى وإخوانه") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(AppIcons.Store, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = storeNameEnglish,
                        onValueChange = { storeNameEnglish = it },
                        label = { Text("اسم المحل بالإنجليزي (اختياري)") },
                        placeholder = { Text("مثال: Fayez Muthanna & Brothers") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = storeSubtitle,
                            onValueChange = { storeSubtitle = it },
                            label = { Text("النشاط / التوكيلات") },
                            placeholder = { Text("مثال: التجارة العامة والتوكيلات") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = logoText,
                            onValueChange = { logoText = it },
                            label = { Text("رمز الشعار") },
                            placeholder = { Text("مثال: FZ") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }
                }
            }

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
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان التفصيلي للمحل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone1,
                            onValueChange = { phone1 = it },
                            label = { Text("هاتف 1 / ثابت") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone2,
                            onValueChange = { phone2 = it },
                            label = { Text("هاتف 2 / سيار") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
