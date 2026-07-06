package com.chefpro.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class ParsedReceiptLine(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val price: Double?,
)

data class ParsedReceipt(
    val supplier: String?,
    val lines: List<ParsedReceiptLine>,
    val total: Double?,
    val rawText: String,
)

object ReceiptOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): ParsedReceipt {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return parseReceiptText(result.text)
    }

    fun parseReceiptText(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val supplier = lines.firstOrNull()
        val parsedLines = mutableListOf<ParsedReceiptLine>()
        var total: Double? = null

        lines.drop(1).forEach { line ->
            val lower = line.lowercase(Locale.getDefault())
            if (lower.contains("итого") || lower.contains("total")) {
                total = extractLastNumber(line)
                return@forEach
            }
            val numbers = NUMBER_REGEX.findAll(line).map { it.value.replace(',', '.').toDoubleOrNull() }.filterNotNull().toList()
            val price = numbers.lastOrNull()
            val qty = numbers.dropLast(1).lastOrNull()
            parsedLines += ParsedReceiptLine(
                name = line.substringBeforeLast(" ").trim().ifBlank { line },
                quantity = qty,
                unit = extractUnit(line),
                price = price,
            )
        }

        return ParsedReceipt(
            supplier = supplier,
            lines = parsedLines,
            total = total,
            rawText = text,
        )
    }

    private fun extractLastNumber(line: String): Double? =
        NUMBER_REGEX.findAll(line).map { it.value.replace(',', '.').toDoubleOrNull() }.lastOrNull()

    private fun extractUnit(line: String): String? = when {
        line.contains("кг", ignoreCase = true) -> "кг"
        line.contains("г", ignoreCase = true) && !line.contains("кг", ignoreCase = true) -> "г"
        line.contains("л", ignoreCase = true) -> "л"
        line.contains("шт", ignoreCase = true) -> "шт"
        else -> null
    }

    private val NUMBER_REGEX = Regex("""\d+[.,]?\d*""")
}
