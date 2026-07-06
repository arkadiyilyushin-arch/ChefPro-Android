package com.chefpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.chefpro.model.Supplier
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Supplier?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    fun openEdit(supplier: Supplier? = null) {
        editing = supplier
        name = supplier?.name ?: ""
        phone = supplier?.phone ?: ""
        email = supplier?.email ?: ""
        notes = supplier?.notes ?: ""
        showDialog = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Поставщики") }) },
        floatingActionButton = {
            Button(onClick = { openEdit() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Добавить")
            }
        },
    ) { padding ->
        if (state.suppliers.isEmpty()) {
            EmptyStateView(Icons.Default.LocalShipping, "Нет поставщиков", "Добавьте первого поставщика", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.suppliers, key = { it.id }) { supplier ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(supplier.name, fontWeight = FontWeight.Bold)
                                if (supplier.phone.isNotBlank()) Text(supplier.phone, style = MaterialTheme.typography.bodySmall)
                                if (supplier.email.isNotBlank()) Text(supplier.email, style = MaterialTheme.typography.bodySmall)
                                if (supplier.notes.isNotBlank()) Text(supplier.notes, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { openEdit(supplier) }) { Text("✎") }
                            IconButton(onClick = { viewModel.deleteSupplier(supplier) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Новый поставщик" else "Редактировать") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) return@TextButton
                    val supplier = Supplier(
                        id = editing?.id ?: com.chefpro.model.newId(),
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        notes = notes.trim(),
                    )
                    if (editing == null) viewModel.addSupplier(supplier) else viewModel.updateSupplier(supplier)
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAutoOrderView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val purchaseList by viewModel.purchaseList.collectAsState()
    var selectedSupplierId by remember { mutableStateOf<String?>(state.suppliers.firstOrNull()?.id) }

    Scaffold(topBar = { TopAppBar(title = { Text("Автозаказ") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Поставщик")
            state.suppliers.forEach { supplier ->
                FilterChip(
                    selected = selectedSupplierId == supplier.id,
                    onClick = { selectedSupplierId = supplier.id },
                    label = { Text(supplier.name) },
                )
            }
            val supplier = state.suppliers.firstOrNull { it.id == selectedSupplierId }
            if (supplier != null) {
                BigCard {
                    Text(supplier.name, fontWeight = FontWeight.Bold)
                    Text(supplier.phone, style = MaterialTheme.typography.bodySmall)
                }
            }
            SectionTitle("К закупке (${purchaseList.size})")
            if (purchaseList.isEmpty()) {
                EmptyStateView(Icons.Default.AutoMode, "Заказ не нужен", "Все остатки в норме")
            } else {
                purchaseList.forEach { item ->
                    val need = (item.minQuantity - item.quantity).coerceAtLeast(0.0) + item.minQuantity
                    BigCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("Остаток: ${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Заказать: ${String.format("%.1f", need)} ${item.unit}", color = ChefAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                state.extraPurchaseItems.forEach { extra ->
                    BigCard {
                        Text("${extra.name} — ${extra.quantity} ${extra.unit} (доп.)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAnalyticsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Аналитика поставщиков") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.suppliers.forEach { supplier ->
                val deliveries = state.deliveries.filter { it.supplier.equals(supplier.name, ignoreCase = true) }
                val total = deliveries.sumOf { it.price }
                val count = deliveries.size
                item(key = supplier.id) {
                    BigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(supplier.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoCard("Поставок", "$count", "операций", Icons.Default.LocalShipping, Modifier.weight(1f))
                                InfoCard("Сумма", formatMoney(total), "всего", Icons.Default.Analytics, Modifier.weight(1f))
                            }
                            if (deliveries.isNotEmpty()) {
                                Text("Последняя: ${formatDate(deliveries.maxOf { it.date })}", style = MaterialTheme.typography.labelSmall)
                                val topProduct = deliveries.groupBy { it.productName }.maxByOrNull { it.value.size }?.key
                                if (topProduct != null) Text("Частый товар: $topProduct", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
