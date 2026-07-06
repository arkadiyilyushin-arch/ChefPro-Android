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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chefpro.domain.BreakevenInput
import com.chefpro.domain.ChefProEngine
import com.chefpro.domain.MenuEngineeringCategory
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val monthRevenue = viewModel.currentMonthRevenue.value
    val monthFc = viewModel.currentMonthAvgFoodCost.value
    val avgFc = ChefProEngine.averageFoodCostPercent(state.dishes, state.inventoryItems)

    Scaffold(topBar = { TopAppBar(title = { Text("Сводная аналитика") }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("Выручка", formatMoney(monthRevenue), "за месяц", Icons.Default.TrendingUp, Modifier.weight(1f))
                InfoCard("FC смен", formatPercent(monthFc), "средний", Icons.Default.ShowChart, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("FC меню", formatPercent(avgFc), "средний", Icons.Default.Analytics, Modifier.weight(1f))
                InfoCard("Блюд", "${state.dishes.size}", "в меню", Icons.Default.BarChart, Modifier.weight(1f))
            }
            BigCard {
                Text("Производство: ${state.productions.size} • Списания: ${state.writeOffs.size}", fontWeight = FontWeight.Bold)
                Text("Приёмки: ${formatMoney(viewModel.totalDeliverySum.value)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuEngineeringView(viewModel: ChefProViewModel) {
    val items = viewModel.menuEngineering()
    val grouped = items.groupBy { it.category }

    Scaffold(topBar = { TopAppBar(title = { Text("Menu Engineering") }) }) { padding ->
        if (items.isEmpty()) {
            EmptyStateView(Icons.Default.BarChart, "Нет данных", "Добавьте блюда и производство", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuEngineeringCategory.entries.forEach { category ->
                    val group = grouped[category].orEmpty()
                    if (group.isEmpty()) return@forEach
                    item {
                        SectionTitle("${viewModel.menuEngineeringCategoryLabel(category)} (${group.size})")
                        group.sortedByDescending { it.portions }.forEach { item ->
                            BigCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(item.dish.name, fontWeight = FontWeight.Bold)
                                        Text("${item.portions} порц.", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                        Text("FC ${formatPercent(item.foodCostPercent)}", color = ChefAccent)
                                        Text("Маржа ${formatPercent(item.margin * 100)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val revenue = viewModel.currentMonthRevenue.value + state.sales.sumOf { sale ->
        state.dishes.firstOrNull { it.name == sale.dishName }?.salePrice?.times(sale.portions) ?: 0.0
    }
    val cogs = state.productions.filter { it.date >= startOfMonth() }.sumOf { it.totalCost } +
        state.deliveries.filter { it.date >= startOfMonth() }.sumOf { it.price }
    val opex = state.operatingExpenses.sumOf { it.amount }
    val profit = revenue - cogs - opex

    Scaffold(topBar = { TopAppBar(title = { Text("P&L") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            plRow("Выручка", revenue, Color(0xFF2E7D32))
            plRow("Себестоимость (COGS)", -cogs, Color.Red)
            plRow("Операц. расходы", -opex, Color(0xFFFF9800))
            BigCard {
                Text("Прибыль", style = MaterialTheme.typography.labelMedium)
                Text(formatMoney(profit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (profit >= 0) Color(0xFF2E7D32) else Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCostTrendView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val shifts = state.shiftHistory.filter { it.foodCostForShift > 0 }.take(12)

    Scaffold(topBar = { TopAppBar(title = { Text("Динамика Food Cost") }) }) { padding ->
        if (shifts.isEmpty()) {
            EmptyStateView(Icons.Default.ShowChart, "Нет данных", "Закройте смены с выручкой", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shifts, key = { it.id }) { shift ->
                    BigCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(formatDate(shift.openedAt), fontWeight = FontWeight.Bold)
                                Text("Выручка: ${formatMoney(shift.revenue)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatPercent(shift.foodCostForShift), fontWeight = FontWeight.Bold, color = ChefAccent, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ABCAnalysisView(viewModel: ChefProViewModel) {
    val items = viewModel.abcAnalysis()

    Scaffold(topBar = { TopAppBar(title = { Text("ABC-анализ склада") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.item.id }) { abc ->
                val color = when (abc.category) {
                    "A" -> Color(0xFF2E7D32)
                    "B" -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.outline
                }
                BigCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(abc.item.name, fontWeight = FontWeight.Bold)
                            Text(abc.item.category, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("Кат. ${abc.category}", color = color, fontWeight = FontWeight.Bold)
                            Text(formatMoney(abc.value), style = MaterialTheme.typography.bodySmall)
                            Text("${formatPercent(abc.valuePercent)} • Σ ${formatPercent(abc.cumulativePercent)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakevenView(viewModel: ChefProViewModel) {
    var rent by remember { mutableStateOf("") }
    var salaries by remember { mutableStateOf("") }
    var utilities by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    var avgCheck by remember { mutableStateOf("") }
    val result = viewModel.breakeven(
        BreakevenInput(
            rent = parsePositiveDouble(rent) ?: 0.0,
            salaries = parsePositiveDouble(salaries) ?: 0.0,
            utilities = parsePositiveDouble(utilities) ?: 0.0,
            otherFixed = parsePositiveDouble(other) ?: 0.0,
            averageCheck = parsePositiveDouble(avgCheck) ?: 0.0,
        ),
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Точка безубыточности") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(rent, { rent = it }, label = { Text("Аренда") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(salaries, { salaries = it }, label = { Text("Зарплаты") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(utilities, { utilities = it }, label = { Text("Коммунальные") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(other, { other = it }, label = { Text("Прочие") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(avgCheck, { avgCheck = it }, label = { Text("Средний чек") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            BigCard {
                Text("Точка безубыточности", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                plRow("Постоянные расходы", result.fixedCosts)
                plRow("FC средний", result.averageFoodCostPercent)
                plRow("Выручка/мес (BE)", result.breakevenRevenueMonthly)
                plRow("Выручка/день (BE)", result.breakevenRevenueDaily)
                plRow("Гостей/мес (BE)", result.breakevenCoversMonthly)
                Text(
                    if (result.isAboveBreakeven) "✓ Выше точки безубыточности (+${formatPercent(result.safetyMarginPercent)})"
                    else "⚠ Ниже точки безубыточности",
                    color = if (result.isAboveBreakeven) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopDishCostView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val ranked = state.dishes
        .map { dish -> dish to viewModel.dishCost(dish) }
        .sortedByDescending { it.second }
        .take(10)

    Scaffold(topBar = { TopAppBar(title = { Text("Топ-10 затрат") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ranked.size) { index ->
                val (dish, cost) = ranked[index]
                BigCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("#${index + 1} ${dish.name}", fontWeight = FontWeight.Bold)
                            Text("FC ${formatPercent(viewModel.foodCostPercent(dish))}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatMoney(cost), fontWeight = FontWeight.Bold, color = ChefAccent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCostByPeriodView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val byMonth = state.shiftHistory
        .filter { it.foodCostForShift > 0 }
        .groupBy {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.openedAt }
            "${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
        }
        .mapValues { (_, shifts) -> shifts.map { it.foodCostForShift }.average() }
        .toList()
        .sortedByDescending { it.first }

    Scaffold(topBar = { TopAppBar(title = { Text("FC по периодам") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(byMonth.size) { index ->
                val (period, fc) = byMonth[index]
                BigCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(period, fontWeight = FontWeight.Bold)
                        Text(formatPercent(fc), fontWeight = FontWeight.Bold, color = ChefAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun plRow(label: String, value: Double, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(formatMoney(value), fontWeight = FontWeight.Bold, color = color)
    }
}
