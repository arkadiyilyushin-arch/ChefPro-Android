package com.chefpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.model.InventoryItem
import com.chefpro.model.ModelHelpers.isExpired
import com.chefpro.model.ModelHelpers.isExpiringSoon
import com.chefpro.model.ModelHelpers.isLowStock
import com.chefpro.model.newId
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.StatusBadge
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.theme.ErrorRed
import com.chefpro.ui.theme.SuccessGreen
import com.chefpro.ui.theme.WarningAmber
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val INVENTORY_PERMISSION = "Склад"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: ChefProViewModel,
    onItemClick: (String) -> Unit,
    onAddItem: () -> Unit,
    onScanBarcode: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }

    PermissionGate(permission = INVENTORY_PERMISSION, viewModel = viewModel) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.inventory) },
                    actions = {
                        IconButton(onClick = onScanBarcode) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddItem) {
                    Icon(Icons.Default.Add, contentDescription = strings.addProduct)
                }
            },
        ) { padding ->
            if (state.inventoryItems.isEmpty()) {
                EmptyStateView(
                    message = strings.noInventory,
                    icon = Icons.Default.Inventory2,
                    actionLabel = strings.addProduct,
                    onAction = onAddItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.inventoryItems.sortedBy { it.name }, key = { it.id }) { item ->
                        InventoryListItem(
                            item = item,
                            currencyFormat = currencyFormat,
                            strings = strings,
                            onClick = { onItemClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryListItem(
    item: InventoryItem,
    currencyFormat: NumberFormat,
    strings: com.chefpro.ui.localization.AppStrings,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${item.quantity} ${item.unit} · ${currencyFormat.format(item.pricePerUnit)}/${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (item.barcode.isNotBlank()) {
                    Text(
                        text = "${strings.barcode}: ${item.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.isLowStock()) {
                    StatusBadge(text = strings.lowStock, color = WarningAmber)
                }
                when {
                    item.isExpired() -> StatusBadge(text = strings.expired, color = ErrorRed)
                    item.isExpiringSoon() -> StatusBadge(text = strings.expiringSoon, color = WarningAmber)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEditScreen(
    viewModel: ChefProViewModel,
    itemId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onScanBarcode: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val existing = itemId?.let { id -> state.inventoryItems.find { it.id == id } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "") }
    var quantity by remember(existing) { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var unit by remember(existing) { mutableStateOf(existing?.unit ?: "кг") }
    var minQuantity by remember(existing) { mutableStateOf(existing?.minQuantity?.toString() ?: "") }
    var pricePerUnit by remember(existing) { mutableStateOf(existing?.pricePerUnit?.toString() ?: "") }
    var barcode by remember(existing) { mutableStateOf(existing?.barcode ?: "") }
    var expiryText by remember(existing) {
        mutableStateOf(
            existing?.expiryDate?.let {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
            } ?: "",
        )
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) strings.edit else strings.addProduct) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showBarcodeDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                    if (existing != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.delete)
                        }
                    }
                    IconButton(onClick = {
                        val expiryDate = parseExpiryDate(expiryText)
                        val item = InventoryItem(
                            id = existing?.id ?: newId(),
                            name = name.trim(),
                            category = category.trim(),
                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                            unit = unit.trim(),
                            minQuantity = minQuantity.toDoubleOrNull() ?: 0.0,
                            pricePerUnit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                            barcode = barcode.trim(),
                            expiryDate = expiryDate,
                            priceHistory = existing?.priceHistory ?: emptyList(),
                        )
                        if (existing != null) {
                            viewModel.updateInventoryItem(item)
                        } else {
                            viewModel.updateSettings { it.copy(inventoryItems = it.inventoryItems + item) }
                        }
                        onSaved()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = strings.save)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.name) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(strings.category) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(strings.quantity) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(strings.unit) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minQuantity,
                    onValueChange = { minQuantity = it },
                    label = { Text(strings.minQuantity) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { pricePerUnit = it },
                    label = { Text(strings.pricePerUnit) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(strings.barcode) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showBarcodeDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                },
            )
            OutlinedTextField(
                value = expiryText,
                onValueChange = { expiryText = it },
                label = { Text("${strings.expiryDate} (dd.MM.yyyy)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("31.12.2026") },
            )

            existing?.let { item ->
                Spacer(modifier = Modifier.height(8.dp))
                val stockColor = when {
                    item.isExpired() -> ErrorRed
                    item.isExpiringSoon() -> WarningAmber
                    item.isLowStock() -> WarningAmber
                    else -> SuccessGreen
                }
                val statusText = when {
                    item.isExpired() -> strings.expired
                    item.isExpiringSoon() -> strings.expiringSoon
                    item.isLowStock() -> strings.lowStock
                    else -> "OK"
                }
                StatusBadge(text = statusText, color = stockColor)
            }
        }
    }

    if (showDeleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.delete) },
            text = { Text(existing.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteInventoryItem(existing)
                    showDeleteDialog = false
                    onBack()
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(strings.cancel) }
            },
        )
    }

    if (showBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showBarcodeDialog = false },
            title = { Text(strings.scanBarcode) },
            text = {
                Column {
                    Text(
                        text = "Enter barcode manually or use camera scanner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scannedBarcode,
                        onValueChange = { scannedBarcode = it },
                        label = { Text(strings.barcode) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    barcode = scannedBarcode.trim()
                    existing?.let { item ->
                        viewModel.updateInventoryItem(item.copy(barcode = scannedBarcode.trim()))
                    }
                    onScanBarcode(scannedBarcode.trim())
                    showBarcodeDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeDialog = false }) { Text(strings.cancel) }
            },
        )
    }
}

private fun parseExpiryDate(text: String): Long? {
    if (text.isBlank()) return null
    return runCatching {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(text.trim())?.time
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    viewModel: ChefProViewModel,
    itemId: String?,
    onBarcodeScanned: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    var manualBarcode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.scanBarcode) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Camera barcode scanning integrates with ML Kit. Enter barcode manually below for now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = manualBarcode,
                onValueChange = { manualBarcode = it },
                label = { Text(strings.barcode) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    val code = manualBarcode.trim()
                    if (code.isNotEmpty()) {
                        itemId?.let { id ->
                            state.inventoryItems.find { it.id == id }?.let { item ->
                                viewModel.updateInventoryItem(item.copy(barcode = code))
                            }
                        }
                        onBarcodeScanned(code)
                    }
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(strings.save)
            }
        }
    }
}
