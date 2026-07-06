package com.chefpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.chefpro.model.Dish
import com.chefpro.model.OperatingExpense
import com.chefpro.model.OperatingExpenseCategory
import com.chefpro.model.Sale
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var dishName by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("1") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Продажи") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Продажа")
            }
        },
    ) { padding ->
        val totalRevenue = state.sales.sumOf { sale ->
            val dish = state.dishes.firstOrNull { it.name == sale.dishName }
            (dish?.salePrice ?: 0.0) * sale.portions
        }
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                InfoCard("Выручка", formatMoney(totalRevenue), "${state.sales.size} продаж", Icons.Default.AttachMoney, Modifier.padding(16.dp))
            }
            if (state.sales.isEmpty()) {
                item { EmptyStateView(Icons.Default.ShoppingCart, "Продаж нет", "Добавьте первую продажу") }
            } else {
                items(state.sales.reversed(), key = { it.id }) { sale ->
                    val dish = state.dishes.firstOrNull { it.name == sale.dishName }
                    val amount = (dish?.salePrice ?: 0.0) * sale.portions
                    BigCard(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(sale.dishName, fontWeight = FontWeight.Bold)
                                Text("${sale.portions} порц. • ${sale.employee}", style = MaterialTheme.typography.bodySmall)
                                Text(formatDateTime(sale.date), style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatMoney(amount), color = ChefAccent, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.deleteSale(sale) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
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
            title = { Text("Новая продажа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dishes.forEach { dish ->
                        FilterChip(selected = dishName == dish.name, onClick = { dishName = dish.name }, label = { Text(dish.name) })
                    }
                    OutlinedTextField(portions, { portions = it }, label = { Text("Порций") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dishName.isBlank()) return@TextButton
                    viewModel.addSale(
                        Sale(
                            dishName = dishName,
                            portions = parsePositiveInt(portions) ?: 1,
                            date = System.currentTimeMillis(),
                            employee = state.profile.name,
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
fun OperatingExpensesView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(OperatingExpenseCategory.OTHER) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Операционные расходы") }) },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) { Icon(Icons.Default.Add, contentDescription = null); Text("Расход") }
        },
    ) { padding ->
        val total = state.operatingExpenses.sumOf { it.amount }
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { InfoCard("Всего", formatMoney(total), "${state.operatingExpenses.size} статей", Icons.Default.AttachMoney, Modifier.padding(16.dp)) }
            items(state.operatingExpenses.reversed(), key = { it.id }) { expense ->
                BigCard(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.name, fontWeight = FontWeight.Bold)
                            Text(enumLabelRu(expense.category.name), style = MaterialTheme.typography.bodySmall)
                            Text(formatDate(expense.date), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(formatMoney(expense.amount), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteOperatingExpense(expense) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новый расход") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OperatingExpenseCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(enumLabelRu(cat.name)) })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = parsePositiveDouble(amount) ?: return@TextButton
                    if (name.isBlank()) return@TextButton
                    viewModel.addOperatingExpense(OperatingExpense(name = name.trim(), amount = amt, category = category))
                    showDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseBudgetView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var budgetText by remember(state.purchaseBudget) { mutableStateOf(if (state.purchaseBudget > 0) state.purchaseBudget.toString() else "") }
    val spent = state.deliveries.filter { it.date >= startOfMonth() }.sumOf { it.price }
    val budget = state.purchaseBudget
    val remaining = budget - spent
    val percent = if (budget > 0) spent / budget * 100 else 0.0

    Scaffold(topBar = { TopAppBar(title = { Text("Бюджет закупок") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(budgetText, { budgetText = it }, label = { Text("Бюджет на месяц") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.setPurchaseBudget(parsePositiveDouble(budgetText) ?: 0.0) }) { Text("Сохранить бюджет") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("Потрачено", formatMoney(spent), "за месяц", Icons.Default.ShoppingCart, Modifier.weight(1f))
                InfoCard("Остаток", formatMoney(remaining.coerceAtLeast(0.0)), if (remaining < 0) "перерасход!" else "доступно", Icons.Default.TrendingUp, Modifier.weight(1f))
            }
            BigCard {
                Text("Использовано: ${formatPercent(percent)}", fontWeight = FontWeight.Bold, color = if (percent > 100) Color.Red else ChefAccent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanVsFactView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var revenuePlanText by remember(state.monthlyRevenuePlan) { mutableStateOf(if (state.monthlyRevenuePlan > 0) state.monthlyRevenuePlan.toLong().toString() else "") }
    var fcTarget by remember { mutableDoubleStateOf(state.monthlyFoodCostTarget) }
    val factRevenue = viewModel.currentMonthRevenue.value
    val factFc = viewModel.currentMonthAvgFoodCost.value
    val revenuePlan = state.monthlyRevenuePlan
    val revenuePercent = if (revenuePlan > 0) factRevenue / revenuePlan * 100 else 0.0

    Scaffold(topBar = { TopAppBar(title = { Text("План vs Факт") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(revenuePlanText, { revenuePlanText = it }, label = { Text("План выручки/мес") }, modifier = Modifier.fillMaxWidth())
            SectionTitle("Целевой Food Cost: ${fcTarget.toInt()}%")
            Slider(value = fcTarget.toFloat(), onValueChange = { fcTarget = it.toDouble() }, valueRange = 15f..50f, steps = 6)
            Button(onClick = {
                viewModel.setMonthlyRevenuePlan(parsePositiveDouble(revenuePlanText) ?: 0.0)
                viewModel.setMonthlyFoodCostTarget(fcTarget)
            }) { Text("Сохранить план") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("Выручка", formatMoney(factRevenue), "факт ${formatPercent(revenuePercent)}", Icons.Default.TrendingUp, Modifier.weight(1f))
                InfoCard("FC", formatPercent(factFc), "цель ${formatPercent(fcTarget)}", Icons.Default.Percent, Modifier.weight(1f))
            }
            BigCard {
                Column {
                    Text("План выручки: ${formatMoney(revenuePlan)}", fontWeight = FontWeight.Bold)
                    Text("Факт: ${formatMoney(factRevenue)}", color = if (factRevenue >= revenuePlan) Color(0xFF2E7D32) else Color.Red)
                    Spacer(Modifier.height(8.dp))
                    Text("FC факт: ${formatPercent(factFc)} / цель ${formatPercent(fcTarget)}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkupCalculatorView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var costInput by remember { mutableStateOf("") }
    var targetFc by remember { mutableDoubleStateOf(30.0) }
    val cost = parsePositiveDouble(costInput) ?: 0.0
    val recommended = if (targetFc > 0) cost / (targetFc / 100) else 0.0
    val markup = if (cost > 0) (recommended - cost) / cost * 100 else 0.0
    val margin = recommended - cost
    val fcColor = when {
        targetFc > state.foodCostThreshold -> Color.Red
        targetFc > state.foodCostThreshold * 0.85 -> Color(0xFFFF9800)
        else -> ChefAccent
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Калькулятор наценки") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BigCard {
                OutlinedTextField(costInput, { costInput = it }, label = { Text("Себестоимость") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
            BigCard {
                Text("Целевой Food Cost: ${targetFc.toInt()}%", fontWeight = FontWeight.Bold, color = fcColor)
                Slider(value = targetFc.toFloat(), onValueChange = { targetFc = it.toDouble() }, valueRange = 15f..50f, steps = 6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 30, 35, 40).forEach { v ->
                        FilterChip(selected = targetFc.toInt() == v, onClick = { targetFc = v.toDouble() }, label = { Text("$v%") })
                    }
                }
            }
            if (cost > 0) {
                BigCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Рекомендуемая цена", style = MaterialTheme.typography.labelMedium)
                        Text(formatMoney(recommended), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = ChefAccent)
                        Spacer(Modifier.height(12.dp))
                        financeRow("Себестоимость", formatMoney(cost))
                        financeRow("Наценка", formatPercent(markup))
                        financeRow("Маржа", formatMoney(margin))
                        financeRow("Food Cost", formatPercent(targetFc), fcColor)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceCalculatorView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var targetFc by remember { mutableDoubleStateOf(30.0) }

    Scaffold(topBar = { TopAppBar(title = { Text("Калькулятор цены") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.dishes.forEach { dish ->
                FilterChip(selected = selectedDish?.id == dish.id, onClick = { selectedDish = dish }, label = { Text(dish.name) })
            }
            val dish = selectedDish
            if (dish != null) {
                val cost = viewModel.dishCost(dish)
                val currentFc = viewModel.foodCostPercent(dish)
                val recommended = if (targetFc > 0) cost / (targetFc / 100) else 0.0
                BigCard {
                    Text(dish.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    financeRow("Себестоимость", formatMoney(cost))
                    financeRow("Текущая цена", formatMoney(dish.salePrice))
                    financeRow("Текущий FC", formatPercent(currentFc))
                }
                Text("Целевой FC: ${targetFc.toInt()}%")
                Slider(value = targetFc.toFloat(), onValueChange = { targetFc = it.toDouble() }, valueRange = 15f..50f, steps = 6)
                BigCard {
                    Text("Рекомендуемая цена: ${formatMoney(recommended)}", fontWeight = FontWeight.Bold, color = ChefAccent, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityRankingView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val ranked = state.dishes
        .filter { it.salePrice > 0 }
        .map { dish ->
            val cost = viewModel.dishCost(dish)
            val margin = dish.salePrice - cost
            val marginPercent = if (dish.salePrice > 0) margin / dish.salePrice * 100 else 0.0
            Triple(dish, margin, marginPercent)
        }
        .sortedByDescending { it.second }

    Scaffold(topBar = { TopAppBar(title = { Text("Рейтинг прибыльности") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ranked.size) { index ->
                val (dish, margin, marginPercent) = ranked[index]
                BigCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${index + 1}", fontWeight = FontWeight.Bold, color = ChefAccent, modifier = Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dish.name, fontWeight = FontWeight.Bold)
                            Text(dish.category, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(margin), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text(formatPercent(marginPercent), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun financeRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}
