package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.AppIcons
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.InvoiceViewModel

enum class NavigationScreen(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    NEW_INVOICE("جديدة", AppIcons.Receipt, AppIcons.Receipt),
    HISTORY("السجل", AppIcons.History, AppIcons.History),
    PRODUCTS("الأصناف", AppIcons.Category, AppIcons.Category),
    PRINTER("الطابعة", AppIcons.Print, AppIcons.Print),
    SETTINGS("المتجر", AppIcons.Store, AppIcons.Store)
}

class MainActivity : ComponentActivity() {

    private val viewModel: InvoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Ensure native Arabic RTL layout
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationScreen.NEW_INVOICE) }
    val uiMessage by viewModel.uiEventMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept hardware/system back button
    BackHandler {
        if (currentTab != NavigationScreen.NEW_INVOICE) {
            currentTab = NavigationScreen.NEW_INVOICE
        } else {
            showExitDialog = true
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationScreen.entries.forEach { screen ->
                    val isSelected = currentTab == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = screen },
                        icon = {
                            Icon(
                                if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationScreen.NEW_INVOICE -> NewInvoiceScreen(
                    viewModel = viewModel,
                    onNavigateToPrinterSetup = { currentTab = NavigationScreen.PRINTER }
                )
                NavigationScreen.HISTORY -> InvoiceHistoryScreen(
                    viewModel = viewModel,
                    onEditInvoice = { inv ->
                        viewModel.loadInvoiceForEditing(inv)
                        currentTab = NavigationScreen.NEW_INVOICE
                    },
                    onNavigateToPrinterSetup = { currentTab = NavigationScreen.PRINTER }
                )
                NavigationScreen.PRODUCTS -> ProductsCatalogScreen(viewModel = viewModel)
                NavigationScreen.PRINTER -> PrinterSetupScreen(viewModel = viewModel)
                NavigationScreen.SETTINGS -> StoreSettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("تأكيد الخروج من التطبيق", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في إغلاق التطبيق؟ سيتم حفظ جميع بياناتك بأمان.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، خروج", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
