package com.chefpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chefpro.model.ModelHelpers.isCritical
import com.chefpro.model.ReservationStatus
import com.chefpro.ui.components.MoreRow
import com.chefpro.ui.components.MoreSectionBlock
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel

enum class MoreDestination {
    ANALYTICS, MENU_ENGINEERING, PROFIT_LOSS, FOOD_COST_TREND, ABC_ANALYSIS, BREAKEVEN, SUPPLIER_ANALYTICS,
    SALES, OPERATING_EXPENSES, PURCHASE_BUDGET, PLAN_VS_FACT, MARKUP_CALC, PROFITABILITY, PRICE_CALC,
    REPORTS, PDF_EXPORT, CSV_EXPORT, WRITE_OFF_REPORT, PURCHASE_FORECAST, FC_BY_PERIOD, TOP_DISH_COST,
    EXPIRY, STOCK_MOVEMENTS, AUDIT, AUTO_ORDER, TEMPERATURE, BARCODE,
    RECIPE_TEMPLATES, DISH_GALLERY, PRODUCTION_PLAN, DIGITAL_MENU, MENU_COLLECTIONS, UNIT_CONVERTER,
    WORK_SCHEDULE, EMPLOYEE_ACTIVITY, CHECKLISTS,
    FLOOR_PLAN, RESERVATIONS, LOYALTY, POS,
    RESTAURANTS, SUPPLIERS, EMPLOYEES, SYNC, BACKUP, SETTINGS, PROFILE,
    SHIFT, KITCHEN_BOARD, STOP_GO, WAITER, WRITE_OFFS, PURCHASES, KITCHEN_MODE, SEARCH,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: ChefProViewModel,
    onNavigate: (MoreDestination) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val purchaseList by viewModel.purchaseList.collectAsState()
    val todayReservations = viewModel.todayReservations.value.count { it.status == ReservationStatus.CONFIRMED }
    val activeKitchen = state.kitchenOrders.count { it.status != com.chefpro.model.OrderStatus.READY }
    val checklistDone = state.checklists.count { it.isCompleted }
    val photosCount = state.dishes.count { it.photoFilename != null }
    val criticalTemp = state.temperatureLogs.any { it.isCritical() }

    Scaffold(topBar = { TopAppBar(title = { Text("Ещё") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(MoreDestination.PROFILE) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ChefAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.profile.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(state.profile.name, fontWeight = FontWeight.Bold)
                        Text(state.profile.position, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("БЫСТРЫЙ ДОСТУП", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickTile("Смена", if (state.currentShift != null) "●" else null, Color(0xFF2E7D32)) { onNavigate(MoreDestination.SHIFT) }
                quickTile("Kitchen Board", if (activeKitchen > 0) "$activeKitchen" else null, ChefAccent) { onNavigate(MoreDestination.KITCHEN_BOARD) }
                quickTile("Закупки", purchaseList.size.takeIf { it > 0 }?.toString(), Color(0xFF00838F)) { onNavigate(MoreDestination.PURCHASES) }
            }

            MoreSectionBlock("Аналитика", Color(0xFF1976D2)) {
                MoreRow("Сводная аналитика", Icons.Default.Analytics, Color(0xFF1976D2)) { onNavigate(MoreDestination.ANALYTICS) }
                MoreRow("Menu Engineering", Icons.Default.BarChart, Color(0xFF1976D2)) { onNavigate(MoreDestination.MENU_ENGINEERING) }
                MoreRow("P&L", Icons.Default.TrendingUp, Color(0xFF1976D2)) { onNavigate(MoreDestination.PROFIT_LOSS) }
                MoreRow("Динамика Food Cost", Icons.Default.Assessment, Color(0xFF1976D2)) { onNavigate(MoreDestination.FOOD_COST_TREND) }
                MoreRow("ABC-анализ склада", Icons.Default.Storage, Color(0xFF1976D2)) { onNavigate(MoreDestination.ABC_ANALYSIS) }
                MoreRow("Точка безубыточности", Icons.Default.TrendingUp, Color(0xFF1976D2)) { onNavigate(MoreDestination.BREAKEVEN) }
                MoreRow("Поставщики", Icons.Default.ShoppingCart, Color(0xFF1976D2)) { onNavigate(MoreDestination.SUPPLIER_ANALYTICS) }
            }

            MoreSectionBlock("Финансы", Color(0xFF2E7D32)) {
                MoreRow("Продажи", Icons.Default.ShoppingCart, Color(0xFF2E7D32), if (state.sales.isEmpty()) null else "${state.sales.size}") { onNavigate(MoreDestination.SALES) }
                MoreRow("Операц. расходы", Icons.Default.AttachMoney, Color(0xFF2E7D32), if (state.operatingExpenses.isEmpty()) null else "${state.operatingExpenses.size}") { onNavigate(MoreDestination.OPERATING_EXPENSES) }
                MoreRow("Бюджет закупок", Icons.Default.Assessment, Color(0xFF2E7D32)) { onNavigate(MoreDestination.PURCHASE_BUDGET) }
                MoreRow("План vs Факт", Icons.Default.BarChart, Color(0xFF2E7D32)) { onNavigate(MoreDestination.PLAN_VS_FACT) }
                MoreRow("Калькулятор наценки", Icons.Default.TrendingUp, Color(0xFF2E7D32)) { onNavigate(MoreDestination.MARKUP_CALC) }
                MoreRow("Рейтинг прибыльности", Icons.Default.EmojiEvents, Color(0xFF2E7D32)) { onNavigate(MoreDestination.PROFITABILITY) }
                MoreRow("Калькулятор цены", Icons.Default.Percent, Color(0xFF2E7D32)) { onNavigate(MoreDestination.PRICE_CALC) }
            }

            MoreSectionBlock("Отчёты", Color(0xFF3949AB)) {
                MoreRow("Отчёты", Icons.Default.Assessment, Color(0xFF3949AB)) { onNavigate(MoreDestination.REPORTS) }
                MoreRow("PDF-отчёты", Icons.Default.Assessment, Color(0xFF3949AB)) { onNavigate(MoreDestination.PDF_EXPORT) }
                MoreRow("CSV-экспорт", Icons.Default.TableChart, Color(0xFF3949AB)) { onNavigate(MoreDestination.CSV_EXPORT) }
                MoreRow("Списания", Icons.Default.Storage, Color(0xFF3949AB)) { onNavigate(MoreDestination.WRITE_OFF_REPORT) }
                MoreRow("Прогноз закупок", Icons.Default.ShoppingCart, Color(0xFF3949AB)) { onNavigate(MoreDestination.PURCHASE_FORECAST) }
                MoreRow("FC по периодам", Icons.Default.Schedule, Color(0xFF3949AB)) { onNavigate(MoreDestination.FC_BY_PERIOD) }
                MoreRow("Топ-10 затрат", Icons.Default.TrendingUp, Color(0xFF3949AB)) { onNavigate(MoreDestination.TOP_DISH_COST) }
            }

            MoreSectionBlock("Склад и закупки", ChefAccent) {
                MoreRow("Автозаказ", Icons.Default.ShoppingCart, ChefAccent, purchaseList.size.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.AUTO_ORDER) }
                MoreRow("Температурный журнал", Icons.Default.Thermostat, ChefAccent, if (criticalTemp) "!" else null) { onNavigate(MoreDestination.TEMPERATURE) }
            }

            MoreSectionBlock("Меню и производство", Color(0xFFE91E63)) {
                MoreRow("Шаблоны техкарт", Icons.Default.MenuBook, Color(0xFFE91E63)) { onNavigate(MoreDestination.RECIPE_TEMPLATES) }
                MoreRow("Галерея блюд", Icons.Default.MenuBook, Color(0xFFE91E63), photosCount.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.DISH_GALLERY) }
                MoreRow("План производства", Icons.Default.Schedule, Color(0xFFE91E63), state.currentProductionPlan.size.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.PRODUCTION_PLAN) }
                MoreRow("Цифровое меню", Icons.Default.MenuBook, Color(0xFFE91E63)) { onNavigate(MoreDestination.DIGITAL_MENU) }
                MoreRow("Сборники меню", Icons.Default.MenuBook, Color(0xFFE91E63)) { onNavigate(MoreDestination.MENU_COLLECTIONS) }
                MoreRow("Конвертер единиц", Icons.Default.GridView, Color(0xFFE91E63)) { onNavigate(MoreDestination.UNIT_CONVERTER) }
            }

            MoreSectionBlock("Персонал", Color(0xFF7B1FA2)) {
                MoreRow("График работы", Icons.Default.Schedule, Color(0xFF7B1FA2)) { onNavigate(MoreDestination.WORK_SCHEDULE) }
                MoreRow("Чеклист смены", Icons.Default.Assessment, Color(0xFF7B1FA2), if (state.checklists.isEmpty()) null else "$checklistDone/${state.checklists.size}") { onNavigate(MoreDestination.CHECKLISTS) }
                MoreRow("Сотрудники", Icons.Default.People, Color(0xFF7B1FA2)) { onNavigate(MoreDestination.EMPLOYEES) }
            }

            MoreSectionBlock("Гости и сервис", Color(0xFF00838F)) {
                MoreRow("План зала", Icons.Default.GridView, Color(0xFF00838F)) { onNavigate(MoreDestination.FLOOR_PLAN) }
                MoreRow("Бронирование", Icons.Default.CalendarMonth, Color(0xFF00838F), todayReservations.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.RESERVATIONS) }
                MoreRow("Программа лояльности", Icons.Default.Star, Color(0xFF00838F), state.loyaltyCards.size.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.LOYALTY) }
                MoreRow("Интеграция с кассой", Icons.Default.PointOfSale, Color(0xFF00838F), state.posRecords.size.takeIf { it > 0 }?.toString()) { onNavigate(MoreDestination.POS) }
            }

            MoreSectionBlock("Система", Color.Gray) {
                MoreRow("Рестораны", Icons.Default.Inventory, Color.Gray, state.restaurantName.takeIf { it.isNotBlank() }) { onNavigate(MoreDestination.RESTAURANTS) }
                MoreRow("Поставщики", Icons.Default.ShoppingCart, Color.Gray) { onNavigate(MoreDestination.SUPPLIERS) }
                MoreRow("Синхронизация", Icons.Default.Sync, Color.Gray, when {
                    isSyncing -> "…"
                    syncError != null -> "!"
                    else -> null
                }) { onNavigate(MoreDestination.SYNC) }
                MoreRow("Резервная копия", Icons.Default.Storage, Color.Gray) { onNavigate(MoreDestination.BACKUP) }
                MoreRow("Настройки", Icons.Default.Settings, Color.Gray) { onNavigate(MoreDestination.SETTINGS) }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RowScope.quickTile(label: String, badge: String?, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            if (badge != null) {
                Text(badge, color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}