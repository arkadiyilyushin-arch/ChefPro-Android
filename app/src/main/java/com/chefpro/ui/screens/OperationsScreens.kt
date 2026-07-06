package com.chefpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.domain.ChefProEngine
import com.chefpro.model.Delivery
import com.chefpro.model.ExtraPurchaseItem
import com.chefpro.model.KitchenOrder
import com.chefpro.model.ModelHelpers.actionLabel
import com.chefpro.model.ModelHelpers.averageCheck
import com.chefpro.model.ModelHelpers.courseName
import com.chefpro.model.ModelHelpers.duration
import com.chefpro.model.ModelHelpers.isOpen
import com.chefpro.model.OrderStatus
import com.chefpro.model.PlanItem
import com.chefpro.model.WriteOff
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveriesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var supplier by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("кг") }
    var price by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Приёмка") }) },
        floatingActionButton = {
            PermissionGate(permission = "Приемка", hasPermission = viewModel::hasPermission) {
                Button(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Приёмка")
                }
            }
        },
    ) { padding ->
        if (state.deliveries.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.LocalShipping,
                message = "Приёмок пока нет",
                hint = "Добавьте первую поставку",
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SectionTitle("Всего: ${formatMoney(viewModel.totalDeliverySum.value)}", Modifier.padding(horizontal = 16.dp)) }
                items(state.deliveries.reversed(), key = { it.id }) { delivery ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Text(delivery.productName, fontWeight = FontWeight.Bold)
                            Text("${delivery.supplier} • ${delivery.acceptedBy}", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${delivery.quantity} ${delivery.unit}")
                                Text(formatMoney(delivery.price), color = ChefAccent, fontWeight = FontWeight.Bold)
                            }
                            Text(formatDateTime(delivery.date), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новая приёмка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("Поставщик") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(product, { product = it }, label = { Text("Продукт") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(category, { category = it }, label = { Text("Категория") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(quantity, { quantity = it }, label = { Text("Кол-во") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(unit, { unit = it }, label = { Text("Ед.") }, modifier = Modifier.width(80.dp))
                    }
                    OutlinedTextField(price, { price = it }, label = { Text("Сумма") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = parsePositiveDouble(quantity) ?: return@TextButton
                    val total = parsePositiveDouble(price) ?: return@TextButton
                    if (product.isBlank()) return@TextButton
                    viewModel.addDelivery(
                        Delivery(
                            supplier = supplier.ifBlank { "Без поставщика" },
                            productName = product.trim(),
                            category = category.trim(),
                            quantity = qty,
                            unit = unit.ifBlank { "шт" },
                            price = total,
                            date = System.currentTimeMillis(),
                            acceptedBy = state.profile.name,
                        ),
                    )
                    showDialog = false
                    supplier = ""; product = ""; category = ""; quantity = ""; price = ""
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteOffsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("кг") }
    var reason by remember { mutableStateOf("") }

    PermissionGate(permission = "Списания", hasPermission = viewModel::hasPermission) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Списания") }) },
            floatingActionButton = {
                Button(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Списать")
                }
            },
        ) { padding ->
            if (state.writeOffs.isEmpty()) {
                EmptyStateView(Icons.Default.Delete, "Списаний нет", "Добавьте первое списание", Modifier.padding(padding).fillMaxSize())
            } else {
                LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.writeOffs.reversed(), key = { it.id }) { wo ->
                        BigCard(Modifier.padding(horizontal = 16.dp)) {
                            Text(wo.productName, fontWeight = FontWeight.Bold)
                            Text("${wo.quantity} ${wo.unit} • ${wo.reason}", style = MaterialTheme.typography.bodySmall)
                            Text("${wo.employee} • ${formatDateTime(wo.date)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Списание") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(product, { product = it }, label = { Text("Продукт") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(quantity, { quantity = it }, label = { Text("Кол-во") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(unit, { unit = it }, label = { Text("Ед.") }, modifier = Modifier.width(80.dp))
                    }
                    OutlinedTextField(reason, { reason = it }, label = { Text("Причина") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = parsePositiveDouble(quantity) ?: return@TextButton
                    if (product.isBlank() || reason.isBlank()) return@TextButton
                    viewModel.addWriteOff(
                        WriteOff(
                            productName = product.trim(),
                            quantity = qty,
                            unit = unit.ifBlank { "шт" },
                            reason = reason.trim(),
                            employee = state.profile.name,
                            date = System.currentTimeMillis(),
                        ),
                    )
                    showDialog = false
                }) { Text("Списать") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showClose by remember { mutableStateOf(false) }
    var cash by remember { mutableStateOf("") }
    var card by remember { mutableStateOf("") }
    var guests by remember { mutableStateOf("") }
    val shift = state.currentShift

    Scaffold(topBar = { TopAppBar(title = { Text("Смена") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (shift != null && shift.isOpen()) {
                BigCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Смена открыта", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text("Открыл: ${shift.openedBy}")
                        Text("Начало: ${formatDateTime(shift.openedAt)}")
                        Text("Длительность: ${shift.duration()}", color = ChefAccent, fontWeight = FontWeight.Bold)
                    }
                }
                SectionTitle("За эту смену")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard("Производство", "${state.productions.count { it.date >= shift.openedAt }}", "операций", Icons.Default.Restaurant, Modifier.weight(1f))
                    InfoCard("Списания", "${state.writeOffs.count { it.date >= shift.openedAt }}", "операций", Icons.Default.Delete, Modifier.weight(1f))
                }
                BigActionButton("Закрыть смену", Icons.Default.StopCircle, containerColor = Color(0xFFC62828), onClick = { showClose = true })
            } else {
                BigCard {
                    Text("Смена не открыта", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Нажмите кнопку ниже чтобы начать")
                }
                BigActionButton("Открыть смену", Icons.Default.PlayCircle, onClick = { viewModel.openShift() })
            }
            if (state.shiftHistory.isNotEmpty()) {
                SectionTitle("История смен")
                state.shiftHistory.take(10).forEach { past ->
                    BigCard {
                        Column {
                            Text(formatDateTime(past.openedAt), fontWeight = FontWeight.Bold)
                            Text("Сотрудник: ${past.openedBy}", style = MaterialTheme.typography.bodySmall)
                            if (past.revenue > 0) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Выручка", style = MaterialTheme.typography.labelSmall)
                                        Text(formatMoney(past.revenue), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                    if (past.guestsCount > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Гостей", style = MaterialTheme.typography.labelSmall)
                                            Text("${past.guestsCount}", fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Ср. чек", style = MaterialTheme.typography.labelSmall)
                                            Text(formatMoney(past.averageCheck()), color = ChefAccent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (past.foodCostForShift > 0) {
                                    Text("Food Cost: ${formatPercent(past.foodCostForShift)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClose) {
        AlertDialog(
            onDismissRequest = { showClose = false },
            title = { Text("Закрытие смены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cash, { cash = it }, label = { Text("Наличные") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(card, { card = it }, label = { Text("Карта") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(guests, { guests = it }, label = { Text("Гостей") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.closeShiftWithRevenue(
                        cashRevenue = parsePositiveDouble(cash) ?: 0.0,
                        cardRevenue = parsePositiveDouble(card) ?: 0.0,
                        guestsCount = parsePositiveInt(guests) ?: 0,
                    )
                    showClose = false
                }) { Text("Закрыть") }
            },
            dismissButton = { TextButton(onClick = { showClose = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val purchaseList by viewModel.purchaseList.collectAsState()
    var showExtra by remember { mutableStateOf(false) }
    var extraName by remember { mutableStateOf("") }
    var extraQty by remember { mutableStateOf("") }
    var extraUnit by remember { mutableStateOf("шт") }
    var extraNote by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Закупки") }) },
        floatingActionButton = {
            Button(onClick = { showExtra = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Доп. позиция")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SectionTitle("К закупке (${purchaseList.size})", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (purchaseList.isEmpty()) {
                item { EmptyStateView(Icons.Default.ShoppingCart, "Всё в норме", "Низких остатков нет", Modifier.padding(16.dp)) }
            } else {
                items(purchaseList, key = { it.id }) { item ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(item.category, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${item.quantity} ${item.unit}", color = Color.Red, fontWeight = FontWeight.Bold)
                                Text("мин: ${item.minQuantity}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            if (state.extraPurchaseItems.isNotEmpty()) {
                item { SectionTitle("Дополнительно", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                items(state.extraPurchaseItems, key = { it.id }) { extra ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(extra.name, fontWeight = FontWeight.Bold)
                                Text("${extra.quantity} ${extra.unit}", style = MaterialTheme.typography.bodySmall)
                                if (extra.note.isNotBlank()) Text(extra.note, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.removeExtraPurchaseItem(extra) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExtra) {
        AlertDialog(
            onDismissRequest = { showExtra = false },
            title = { Text("Доп. позиция") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(extraName, { extraName = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(extraQty, { extraQty = it }, label = { Text("Кол-во") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(extraUnit, { extraUnit = it }, label = { Text("Ед.") }, modifier = Modifier.width(80.dp))
                    }
                    OutlinedTextField(extraNote, { extraNote = it }, label = { Text("Заметка") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = parsePositiveDouble(extraQty) ?: 1.0
                    if (extraName.isBlank()) return@TextButton
                    viewModel.addExtraPurchaseItem(ExtraPurchaseItem(name = extraName.trim(), quantity = qty, unit = extraUnit, note = extraNote.trim()))
                    showExtra = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showExtra = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionPlanView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedDishId by remember { mutableStateOf<String?>(null) }
    var portions by remember { mutableStateOf("1") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("План производства") }) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.currentProductionPlan.isNotEmpty()) {
                    Button(onClick = { viewModel.executeProductionPlan() }) { Text("Выполнить") }
                }
                Button(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = null) }
            }
        },
    ) { padding ->
        if (state.currentProductionPlan.isEmpty()) {
            EmptyStateView(Icons.Default.Restaurant, "План пуст", "Добавьте блюда в план", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.currentProductionPlan, key = { it.id }) { plan ->
                    val dish = state.dishes.firstOrNull { it.id == plan.dishID }
                    val canProduce = dish?.let { ChefProEngine.canProduce(it, plan.portions, state.inventoryItems) } ?: false
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.dishName, fontWeight = FontWeight.Bold)
                                Text("${plan.portions} порц.", style = MaterialTheme.typography.bodySmall)
                                Text(if (canProduce) "✓ Достаточно ингредиентов" else "⚠ Недостаточно", color = if (canProduce) Color(0xFF2E7D32) else Color.Red, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.removePlanItem(plan) }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Добавить в план") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dishes.forEach { dish ->
                        FilterChip(
                            selected = selectedDishId == dish.id,
                            onClick = { selectedDishId = dish.id },
                            label = { Text(dish.name) },
                        )
                    }
                    OutlinedTextField(portions, { portions = it }, label = { Text("Порций") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dish = state.dishes.firstOrNull { it.id == selectedDishId } ?: return@TextButton
                    val p = parsePositiveInt(portions) ?: 1
                    viewModel.addPlanItem(PlanItem(dishID = dish.id, dishName = dish.name, portions = p))
                    showAdd = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenBoardView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val columns = listOf(OrderStatus.NEW, OrderStatus.COOKING, OrderStatus.READY)

    Scaffold(topBar = { TopAppBar(title = { Text("Kitchen Board") }) }) { padding ->
        Row(Modifier.padding(padding).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { status ->
                val orders = state.kitchenOrders.filter { it.status == status }
                Column(Modifier.weight(1f).padding(4.dp)) {
                    Text(
                        when (status) {
                            OrderStatus.NEW -> "Новые"
                            OrderStatus.COOKING -> "Готовится"
                            OrderStatus.READY -> "Готово"
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(orders, key = { it.id }) { order ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(order.dishName, fontWeight = FontWeight.Bold)
                                    Text("${order.portions} порц. • ${order.courseName()}", style = MaterialTheme.typography.bodySmall)
                                    if (order.tableNumber.isNotBlank()) Text("Стол ${order.tableNumber}", style = MaterialTheme.typography.labelSmall)
                                    val action = order.status.actionLabel()
                                    if (action.isNotBlank()) {
                                        TextButton(onClick = { viewModel.advanceKitchenOrder(order) }) { Text(action) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenModeView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val active = state.kitchenOrders.filter { it.status != OrderStatus.READY }

    Scaffold(topBar = { TopAppBar(title = { Text("Kitchen Mode") }) }) { padding ->
        if (active.isEmpty()) {
            EmptyStateView(Icons.Default.Restaurant, "Нет заказов", "Все заказы выполнены", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(active, key = { it.id }) { order ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Text(order.dishName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Стол ${order.tableNumber.ifBlank { "—" }} • ${order.courseName()}")
                            Text("${order.portions} порций", color = ChefAccent, fontWeight = FontWeight.Bold)
                            if (order.note.isNotBlank()) Text("«${order.note}»", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            BigActionButton(order.status.actionLabel().ifBlank { "Готово" }, Icons.Default.Restaurant, onClick = { viewModel.advanceKitchenOrder(order) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterModeView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var table by remember { mutableStateOf("") }
    var selectedDishId by remember { mutableStateOf<String?>(null) }
    var portions by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Режим официанта") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(table, { table = it }, label = { Text("Стол") }, modifier = Modifier.fillMaxWidth())
            SectionTitle("Блюдо")
            state.dishes.filter { !it.isStopListed }.forEach { dish ->
                FilterChip(selected = selectedDishId == dish.id, onClick = { selectedDishId = dish.id }, label = { Text("${dish.name} — ${formatMoney(dish.salePrice)}") })
            }
            OutlinedTextField(portions, { portions = it }, label = { Text("Порций") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
            BigActionButton("Отправить на кухню", Icons.Default.TableBar) {
                val dish = state.dishes.firstOrNull { it.id == selectedDishId } ?: return@BigActionButton
                viewModel.addKitchenOrder(
                    KitchenOrder(
                        dishName = dish.name,
                        portions = parsePositiveInt(portions) ?: 1,
                        tableNumber = table.trim(),
                        note = note.trim(),
                    ),
                )
                note = ""
            }
            SectionTitle("Активные заказы")
            state.kitchenOrders.filter { it.tableNumber == table.trim() && table.isNotBlank() }.forEach { order ->
                BigCard {
                    Text("${order.dishName} × ${order.portions}")
                    Text(order.status.name.let { enumLabelRu(it) }, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
