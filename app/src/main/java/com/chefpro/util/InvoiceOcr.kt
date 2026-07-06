package com.chefpro.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class ParsedInvoiceLine(
    val description: String,
    val quantity: Double?,
    val unitPrice: Double?,
    val lineTotal: Double?,
)

data class ParsedInvoice(
    val invoiceNumber: String?,
    val supplier: String?,
    val issueDate: String?,
    val lines: List<ParsedInvoiceLine>,
    val subtotal: Double?,
    val tax: Double?,
    val total: Double?,
    val rawText: String,
)

object InvoiceOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): ParsedInvoice {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return parseInvoiceText(result.text)
    }

    fun parseInvoiceText(text: String): ParsedInvoice {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var invoiceNumber: String? = null
        var supplier: String? = lines.firstOrNull()
        var issueDate: String? = null
        var subtotal: Double? = null
        var tax: Double? = null
        var total: Double? = null
        val items = mutableListOf<ParsedInvoiceLine>()

        lines.forEach { line ->
            val lower = line.lowercase(Locale.getDefault())
            when {
                lower.contains("счёт") || lower.contains("invoice") || lower.contains("№") -> {
                    invoiceNumber = line.substringAfterLast('№', line).trim().ifBlank { line }
                }
                lower.contains("дата") || DATE_REGEX.containsMatchIn(line) -> {
                    issueDate = DATE_REGEX.find(line)?.value ?: line.substringAfter(":").trim()
                }
                lower.contains("поставщик") || lower.contains("supplier") -> {
                    supplier = line.substringAfter(":").trim().ifBlank { supplier }
                }
                lower.contains("ндс") || lower.contains("vat") || lower.contains("tax") -> {
                    tax = extractLastNumber(line)
                }
                lower.contains("итого") || lower.contains("total") -> {
                    total = extractLastNumber(line)
                }
                lower.contains("сумма") || lower.contains("subtotal") -> {
                    subtotal = extractLastNumber(line)
                }
                else -> {
                    val numbers = extractNumbers(line)
                    if (numbers.size >= 2) {
                        items += ParsedInvoiceLine(
                            description = line.substringBefore(numbers.first().toString()).trim().ifBlank { line },
                            quantity = numbers.firstOrNull(),
                            unitPrice = numbers.getOrNull(1),
                            lineTotal = numbers.lastOrNull(),
                        )
                    }
                }
            }
        }

        return ParsedInvoice(
            invoiceNumber = invoiceNumber,
            supplier = supplier,
            issueDate = issueDate,
            lines = items,
            subtotal = subtotal,
            tax = tax,
            total = total,
            rawText = text,
        )
    }

    private fun extractNumbers(line: String): List<Double> =
        NUMBER_REGEX.findAll(line).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()

    private fun extractLastNumber(line: String): Double? = extractNumbers(line).lastOrNull()

    private val NUMBER_REGEX = Regex("""\d+[.,]?\d*""")
    private val DATE_REGEX = Regex("""\d{1,2}[./]\d{1,2}[./]\d{2,4}""")
}
