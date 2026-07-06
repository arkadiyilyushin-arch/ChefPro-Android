package com.chefpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.chefpro.model.DishMenuStatus
import com.chefpro.model.LoyaltyCard
import com.chefpro.model.ModelHelpers.discount
import com.chefpro.model.ModelHelpers.tier
import com.chefpro.model.POSSaleRecord
import com.chefpro.model.POSSystem
import com.chefpro.model.ReservationStatus
import com.chefpro.model.TableReservation
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableReservationView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var guestName by remember { mutableStateOf("") }
    var guestPhone by remember { mutableStateOf("") }
    var table by remember { mutableStateOf("") }
    var persons by remember { mutableStateOf("2") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Бронирование") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Бронь")
            }
        },
    ) { padding ->
        val today = viewModel.todayReservations.value
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionTitle("Сегодня (${today.size})", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            if (state.reservations.isEmpty()) {
                item { EmptyStateView(Icons.Default.CalendarMonth, "Броней нет", "Добавьте первую бронь", Modifier.padding(16.dp)) }
            } else {
                items(state.reservations.sortedBy { it.date }, key = { it.id }) { reservation ->
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Text(reservation.guestName, fontWeight = FontWeight.Bold)
                            Text("Стол ${reservation.tableNumber} • ${reservation.persons} чел.", style = MaterialTheme.typography.bodySmall)
                            Text("${formatDateTime(reservation.date)} • ${enumLabelRu(reservation.status.name)}", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReservationStatus.entries.forEach { status ->
                                    FilterChip(
                                        selected = reservation.status == status,
                                        onClick = { viewModel.updateReservation(reservation.copy(status = status)) },
                                        label = { Text(enumLabelRu(status.name), style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
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
            title = { Text("Новая бронь") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(guestName, { guestName = it }, label = { Text("Имя гостя") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(guestPhone, { guestPhone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(table, { table = it }, label = { Text("Стол") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(persons, { persons = it }, label = { Text("Гостей") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (guestName.isBlank() || table.isBlank()) return@TextButton
                    viewModel.addReservation(
                        TableReservation(
                            guestName = guestName.trim(),
                            guestPhone = guestPhone.trim(),
                            tableNumber = table.trim(),
                            persons = parsePositiveInt(persons) ?: 2,
                            date = System.currentTimeMillis() + 2 * 60 * 60 * 1000L,
                            createdBy = state.profile.name,
                        ),
                    )
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var guestName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var purchaseAmount by remember { mutableStateOf("") }
    var selectedCardId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Программа лояльности") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Карта")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loyaltyCards.isEmpty()) {
                item { EmptyStateView(Icons.Default.Star, "Карт нет", "Создайте первую карту", Modifier.padding(16.dp)) }
            } else {
                items(state.loyaltyCards, key = { it.id }) { card ->
                    val tier = card.tier()
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Text(card.guestName, fontWeight = FontWeight.Bold)
                            Text("№ ${card.cardNumber} • ${enumLabelRu(tier.name)} (${tier.discount()}%)", style = MaterialTheme.typography.bodySmall)
                            Text("${card.points} баллов • ${formatMoney(card.totalSpent)}", color = ChefAccent, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { selectedCardId = card.id; purchaseAmount = "" }) { Text("Начислить покупку") }
                            if (selectedCardId == card.id) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(purchaseAmount, { purchaseAmount = it }, label = { Text("Сумма") }, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        val amount = parsePositiveDouble(purchaseAmount) ?: return@TextButton
                                        viewModel.addPurchaseToLoyalty(card.id, amount, "Покупка")
                                        selectedCardId = null
                                    }) { Text("OK") }
                                }
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
            title = { Text("Новая карта") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(guestName, { guestName = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cardNumber, { cardNumber = it }, label = { Text("Номер карты") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (guestName.isBlank()) return@TextButton
                    viewModel.addLoyaltyCard(
                        LoyaltyCard(
                            guestName = guestName.trim(),
                            phone = phone.trim(),
                            cardNumber = cardNumber.ifBlank { System.currentTimeMillis().toString().takeLast(8) },
                        ),
                    )
                    showDialog = false
                }) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSIntegrationView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showImport by remember { mutableStateOf(false) }
    var dishName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("") }
    var posSystem by remember { mutableStateOf(POSSystem.MANUAL) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Интеграция с кассой") }) },
        floatingActionButton = {
            Button(onClick = { showImport = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Импорт")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Всего записей: ${state.posRecords.size}", fontWeight = FontWeight.Bold)
            POSSystem.entries.forEach { sys ->
                FilterChip(selected = posSystem == sys, onClick = { posSystem = sys }, label = { Text(enumLabelRu(sys.name)) })
            }
            if (state.posRecords.isEmpty()) {
                EmptyStateView(Icons.Default.PointOfSale, "Нет данных POS", "Импортируйте продажи вручную")
            } else {
                state.posRecords.takeLast(20).reversed().forEach { record ->
                    BigCard {
                        Text(record.dishName, fontWeight = FontWeight.Bold)
                        Text("${record.quantity} × ${formatMoney(record.amount)} • ${enumLabelRu(record.posSystem.name)}", style = MaterialTheme.typography.bodySmall)
                        Text(formatDateTime(record.date), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Импорт продажи") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dishes.forEach { dish ->
                        FilterChip(selected = dishName == dish.name, onClick = { dishName = dish.name; amount = dish.salePrice.toString() }, label = { Text(dish.name) })
                    }
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Кол-во") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dishName.isBlank()) return@TextButton
                    viewModel.addPOSRecord(
                        POSSaleRecord(
                            date = System.currentTimeMillis(),
                            dishName = dishName,
                            quantity = parsePositiveInt(quantity) ?: 1,
                            amount = parsePositiveDouble(amount) ?: 0.0,
                            posSystem = posSystem,
                        ),
                    )
                    showImport = false
                }) { Text("Импорт") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalMenuView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val activeDishes = state.dishes.filter { it.menuStatus == DishMenuStatus.ACTIVE && !it.isStopListed }
    val byCategory = activeDishes.groupBy { it.category }

    Scaffold(topBar = { TopAppBar(title = { Text("Цифровое меню") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            byCategory.forEach { (category, dishes) ->
                item {
                    SectionTitle(category)
                    dishes.forEach { dish ->
                        BigCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(dish.name, fontWeight = FontWeight.Bold)
                                    if (dish.cookTime > 0) Text("${dish.cookTime} мин", style = MaterialTheme.typography.labelSmall)
                                    if (dish.allergens.isNotEmpty()) Text("⚠ ${dish.allergens.joinToString()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
                                    if (dish.isGoListed) Text("🔥 Хит", color = ChefAccent, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatMoney(dish.salePrice), fontWeight = FontWeight.Bold, color = ChefAccent, style = MaterialTheme.typography.titleMedium)
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
fun FloorPlanView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val tables = (1..12).map { it.toString() }
    val occupied = state.kitchenOrders.map { it.tableNumber }.filter { it.isNotBlank() }.toSet()
    val reserved = state.reservations.filter { it.status == ReservationStatus.CONFIRMED }.map { it.tableNumber }.toSet()

    Scaffold(topBar = { TopAppBar(title = { Text("План зала") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                legendDot("Свободен", Color(0xFF2E7D32))
                legendDot("Занят", Color.Red)
                legendDot("Бронь", Color(0xFFFF9800))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tables.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { table ->
                            val color = when {
                                table in occupied -> Color.Red
                                table in reserved -> Color(0xFFFF9800)
                                else -> Color(0xFF2E7D32)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .border(2.dp, color, MaterialTheme.shapes.medium)
                                    .background(color.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                                    .clickable { },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.GridView, contentDescription = null, tint = color)
                                    Text(table, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
            SectionTitle("Активные заказы")
            state.kitchenOrders.filter { it.tableNumber.isNotBlank() }.forEach { order ->
                Text("Стол ${order.tableNumber}: ${order.dishName} × ${order.portions}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun legendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, MaterialTheme.shapes.small))
        Text(" $label", style = MaterialTheme.typography.labelSmall)
    }
}
