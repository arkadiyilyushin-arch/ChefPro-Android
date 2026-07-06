package com.chefpro.util

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.chefpro.domain.ChefProEngine
import com.chefpro.model.ChefProState
import com.chefpro.model.Dish
import com.chefpro.ui.screens.formatDateTime
import com.chefpro.ui.screens.formatMoney
import com.chefpro.ui.screens.formatPercent
import java.io.File
import java.io.FileOutputStream

object PdfExport {

    fun generateSummaryReport(
        state: ChefProState,
        currentMonthRevenue: Double,
        totalDeliverySum: Double,
        lowStockCount: Int,
        foodCostForDish: (Dish) -> Double,
    ): String = buildString {
        appendLine("ChefPro — Сводный отчёт")
        appendLine("Ресторан: ${state.restaurantName}")
        appendLine("Дата: ${formatDateTime(System.currentTimeMillis())}")
        appendLine()
        appendLine("Выручка за месяц: ${formatMoney(currentMonthRevenue)}")
        appendLine("Приёмки: ${formatMoney(totalDeliverySum)}")
        appendLine("Производство: ${formatMoney(state.productions.sumOf { it.totalCost })}")
        appendLine("Списания: ${state.writeOffs.size}")
        appendLine("Низкий остаток: $lowStockCount")
        appendLine()
        appendLine("--- Блюда ---")
        state.dishes.forEach { dish ->
            appendLine("${dish.name}: FC ${formatPercent(foodCostForDish(dish))}")
        }
    }

    fun generateInventoryReport(state: ChefProState): String = buildString {
        appendLine("ChefPro — Склад")
        appendLine("Ресторан: ${state.restaurantName}")
        appendLine("Дата: ${formatDateTime(System.currentTimeMillis())}")
        appendLine()
        state.inventoryItems.sortedBy { it.name }.forEach { item ->
            appendLine(
                "${item.name} | ${item.category} | ${item.quantity} ${item.unit} | " +
                    "мин ${item.minQuantity} | ${formatMoney(item.pricePerUnit)}/ед.",
            )
        }
    }

    fun writeTextReport(context: Context, filename: String, content: String): File {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        return file
    }

    fun writePdfReport(context: Context, filename: String, lines: List<String>): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        var y = 40f
        lines.forEach { line ->
            if (y > 800f) return@forEach
            canvas.drawText(line.take(90), 40f, y, paint)
            y += 16f
        }
        document.finishPage(page)
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareSummaryReport(
        context: Context,
        state: ChefProState,
        currentMonthRevenue: Double,
        totalDeliverySum: Double,
    ) {
        val content = generateSummaryReport(
            state = state,
            currentMonthRevenue = currentMonthRevenue,
            totalDeliverySum = totalDeliverySum,
            lowStockCount = ChefProEngine.lowStockItems(state).size,
            foodCostForDish = { dish ->
                ChefProEngine.foodCostPercent(dish, state.dishes, state.inventoryItems)
            },
        )
        val file = writePdfReport(
            context = context,
            filename = "chefpro_report_${System.currentTimeMillis()}.pdf",
            lines = content.lines(),
        )
        shareFile(context, file, "application/pdf", "Экспорт PDF")
    }
}
