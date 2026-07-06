package com.chefpro.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.chefpro.model.ModelHelpers.isLowStock
import com.chefpro.ui.components.BigActionButton
import com.chefpro.ui.components.BigCard
import com.chefpro.ui.components.EmptyStateView
import com.chefpro.ui.components.InfoCard
import com.chefpro.ui.components.PermissionGate
import com.chefpro.ui.components.SectionTitle
import com.chefpro.ui.theme.ChefAccent
import com.chefpro.ui.viewmodel.ChefProViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val productionCost = state.productions.sumOf { it.totalCost }

    PermissionGate(permission = "Отчеты", hasPermission = viewModel::hasPermission) {
        Scaffold(topBar = { TopAppBar(title = { Text("Отчёты") }) }) { padding ->
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoCard("Приёмка", formatMoney(viewModel.totalDeliverySum.value), "расходы", Icons.Default.ShoppingCart, Modifier.weight(1f))
                        InfoCard("Производство", formatMoney(productionCost), "себест.", Icons.Default.Assessment, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoCard("Списания", "${state.writeOffs.size}", "операций", Icons.Default.Delete, Modifier.weight(1f))
                        InfoCard("Низкий остаток", "${viewModel.lowStockItems.value.size}", "позиций", Icons.Default.TrendingDown, Modifier.weight(1f))
                    }
                }
                item { SectionTitle("Последнее производство") }
                if (state.productions.isEmpty()) {
                    item { BigCard { Text("Производства пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                } else {
                    items(state.productions.takeLast(10).reversed(), key = { it.id }) { prod ->
                        BigCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(prod.dishName, fontWeight = FontWeight.Bold)
                                    Text("${prod.portions} порц. • ${prod.employee}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatMoney(prod.totalCost), fontWeight = FontWeight.Bold, color = ChefAccent)
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
fun WriteOffReportView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val grouped = state.writeOffs.groupBy { it.reason }.mapValues { (_, list) -> list.sumOf { it.quantity } }

    Scaffold(topBar = { TopAppBar(title = { Text("Отчёт по списаниям") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard("Всего", "${state.writeOffs.size}", "операций", Icons.Default.Delete)
            grouped.forEach { (reason, qty) ->
                BigCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reason, fontWeight = FontWeight.Bold)
                        Text(String.format("%.1f ед.", qty), color = ChefAccent)
                    }
                }
            }
            SectionTitle("Последние")
            state.writeOffs.takeLast(15).reversed().forEach { wo ->
                BigCard {
                    Text("${wo.productName} — ${wo.quantity} ${wo.unit}")
                    Text("${wo.employee} • ${formatDateTime(wo.date)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseForecastView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val purchaseList by viewModel.purchaseList.collectAsState()
    val forecastCost = purchaseList.sumOf { item ->
        val need = (item.minQuantity - item.quantity).coerceAtLeast(0.0) + item.minQuantity
        need * item.pricePerUnit
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Прогноз закупок") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard("Прогноз", formatMoney(forecastCost), "${purchaseList.size} позиций", Icons.Default.ShoppingCart)
            if (purchaseList.isEmpty()) {
                EmptyStateView(Icons.Default.ShoppingCart, "Закупки не нужны", "Остатки в норме")
            } else {
                purchaseList.forEach { item ->
                    val need = (item.minQuantity - item.quantity).coerceAtLeast(0.0) + item.minQuantity
                    val cost = need * item.pricePerUnit
                    BigCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("Заказать ${String.format("%.1f", need)} ${item.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatMoney(cost), fontWeight = FontWeight.Bold, color = ChefAccent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("PDF-отчёты") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Экспорт сводного отчёта в текстовый файл (PDF через системное приложение)", style = MaterialTheme.typography.bodySmall)
            BigActionButton("Экспорт сводки", Icons.Default.PictureAsPdf) {
                val content = buildString {
                    appendLine("ChefPro — Сводный отчёт")
                    appendLine("Ресторан: ${state.restaurantName}")
                    appendLine("Дата: ${formatDateTime(System.currentTimeMillis())}")
                    appendLine()
                    appendLine("Выручка за месяц: ${formatMoney(viewModel.currentMonthRevenue.value)}")
                    appendLine("Приёмки: ${formatMoney(viewModel.totalDeliverySum.value)}")
                    appendLine("Производство: ${formatMoney(state.productions.sumOf { it.totalCost })}")
                    appendLine("Списания: ${state.writeOffs.size}")
                    appendLine("Низкий остаток: ${viewModel.lowStockItems.value.size}")
                    appendLine()
                    appendLine("--- Блюда ---")
                    state.dishes.forEach { dish ->
                        appendLine("${dish.name}: FC ${formatPercent(viewModel.foodCostPercent(dish))}")
                    }
                }
                val file = File(context.cacheDir, "chefpro_report_${System.currentTimeMillis()}.txt")
                file.writeText(content)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "ChefPro Report")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Экспорт PDF/отчёта"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvExportView(viewModel: ChefProViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("CSV-экспорт") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigActionButton("Экспорт склада (CSV)", Icons.Default.TableChart) {
                shareCsv(context, "inventory.csv", buildInventoryCsv(state))
            }
            BigActionButton("Экспорт списаний (CSV)", Icons.Default.TableChart) {
                shareCsv(context, "writeoffs.csv", buildWriteOffsCsv(state))
            }
            BigActionButton("Экспорт продаж (CSV)", Icons.Default.Download) {
                shareCsv(context, "sales.csv", buildSalesCsv(state, viewModel))
            }
            BigActionButton("Полный бэкап (JSON)", Icons.Default.Download) {
                val json = viewModel.exportBackupJson()
                val file = File(context.cacheDir, "chefpro_backup.json")
                file.writeText(json)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Экспорт JSON"))
            }
        }
    }
}

private fun buildInventoryCsv(state: com.chefpro.model.ChefProState): String {
    val header = "name,category,quantity,unit,minQuantity,pricePerUnit"
    val rows = state.inventoryItems.map { "${it.name},${it.category},${it.quantity},${it.unit},${it.minQuantity},${it.pricePerUnit}" }
    return (listOf(header) + rows).joinToString("\n")
}

private fun buildWriteOffsCsv(state: com.chefpro.model.ChefProState): String {
    val header = "product,quantity,unit,reason,employee,date"
    val rows = state.writeOffs.map { "${it.productName},${it.quantity},${it.unit},\"${it.reason}\",${it.employee},${formatDateTime(it.date)}" }
    return (listOf(header) + rows).joinToString("\n")
}

private fun buildSalesCsv(state: com.chefpro.model.ChefProState, viewModel: ChefProViewModel): String {
    val header = "dish,portions,amount,employee,date"
    val rows = state.sales.map { sale ->
        val dish = state.dishes.firstOrNull { it.name == sale.dishName }
        val amount = (dish?.salePrice ?: 0.0) * sale.portions
        "${sale.dishName},${sale.portions},$amount,${sale.employee},${formatDateTime(sale.date)}"
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun shareCsv(context: android.content.Context, filename: String, content: String) {
    val file = File(context.cacheDir, filename)
    file.writeText(content)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Экспорт CSV"))
}
