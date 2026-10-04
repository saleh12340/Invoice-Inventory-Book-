package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.InvoiceViewModel

enum class NavigationScreen(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    NEW_INVOICE("جديدة", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    HISTORY("السجل", Icons.Filled.History, Icons.Outlined.History),
    PRODUCTS("الأصناف", Icons.Filled.Category, Icons.Outlined.Category),
    PRINTER("الطابعة", Icons.Filled.Print, Icons.Outlined.Print),
    SETTINGS("المتجر", Icons.Filled.Store, Icons.Outlined.Store)
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
    var currentTab by remember { mutableStateOf(NavigationScreen.NEW_INVOICE) }
    val uiMessage by viewModel.uiEventMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
}
