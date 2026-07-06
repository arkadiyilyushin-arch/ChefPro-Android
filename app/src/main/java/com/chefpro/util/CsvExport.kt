package com.chefpro.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.chefpro.model.ChefProState
import com.chefpro.model.Dish
import com.chefpro.ui.screens.formatDateTime
import java.io.File

object CsvExport {

    fun inventoryCsv(state: ChefProState): String {
        val header = "name,category,quantity,unit,minQuantity,pricePerUnit,barcode,expiryDate"
        val rows = state.inventoryItems.map { item ->
            listOf(
                item.name.csvEscape(),
                item.category.csvEscape(),
                item.quantity.toString(),
                item.unit.csvEscape(),
                item.minQuantity.toString(),
                item.pricePerUnit.toString(),
                item.barcode.csvEscape(),
                item.expiryDate?.let { formatDateTime(it) }.orEmpty().csvEscape(),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun writeOffsCsv(state: ChefProState): String {
        val header = "product,quantity,unit,reason,employee,date"
        val rows = state.writeOffs.map { wo ->
            listOf(
                wo.productName.csvEscape(),
                wo.quantity.toString(),
                wo.unit.csvEscape(),
                wo.reason.csvEscape(),
                wo.employee.csvEscape(),
                formatDateTime(wo.date).csvEscape(),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun salesCsv(state: ChefProState): String {
        val header = "dish,portions,amount,employee,date"
        val rows = state.sales.map { sale ->
            val dish = state.dishes.firstOrNull { it.name == sale.dishName }
            val amount = (dish?.salePrice ?: 0.0) * sale.portions
            listOf(
                sale.dishName.csvEscape(),
                sale.portions.toString(),
                amount.toString(),
                sale.employee.csvEscape(),
                formatDateTime(sale.date).csvEscape(),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun dishesCsv(state: ChefProState, foodCostForDish: (Dish) -> Double): String {
        val header = "name,category,salePrice,foodCostPercent,cookTime,dishType"
        val rows = state.dishes.map { dish ->
            listOf(
                dish.name.csvEscape(),
                dish.category.csvEscape(),
                dish.salePrice.toString(),
                foodCostForDish(dish).toString(),
                dish.cookTime.toString(),
                dish.dishType.name.csvEscape(),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun writeCsvFile(context: Context, filename: String, content: String): File {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        return file
    }

    fun shareCsv(context: Context, filename: String, content: String, chooserTitle: String = "Экспорт CSV") {
        val file = writeCsvFile(context, filename, content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun String.csvEscape(): String =
        if (any { it == ',' || it == '"' || it == '\n' }) "\"${replace("\"", "\"\"")}\"" else this
}
