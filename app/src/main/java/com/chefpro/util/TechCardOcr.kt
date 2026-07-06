package com.chefpro.util

import android.graphics.Bitmap
import com.chefpro.model.CookingStep
import com.chefpro.model.RecipeIngredient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class ParsedTechCard(
    val name: String?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<CookingStep>,
    val rawText: String,
)

object TechCardOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): ParsedTechCard {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return parseTechCardText(result.text)
    }

    fun parseTechCardText(text: String): ParsedTechCard {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val name = lines.firstOrNull()?.takeIf { !it.startsWith("-") && !STEP_PREFIX.containsMatchIn(it) }

        val ingredients = mutableListOf<RecipeIngredient>()
        val steps = mutableListOf<CookingStep>()
        var stepNumber = 1

        lines.forEach { line ->
            when {
                INGREDIENT_LINE.matches(line) -> {
                    val match = INGREDIENT_LINE.find(line) ?: return@forEach
                    ingredients += RecipeIngredient(
                        productName = match.groupValues[1].trim(),
                        quantity = match.groupValues[2].replace(',', '.').toDoubleOrNull() ?: 0.0,
                        unit = match.groupValues[3].trim(),
                    )
                }
                line.startsWith("-") -> {
                    ingredients += RecipeIngredient(
                        productName = line.removePrefix("-").trim(),
                        quantity = 0.0,
                        unit = "г",
                    )
                }
                STEP_PREFIX.containsMatchIn(line) || line.firstOrNull()?.isDigit() == true -> {
                    val instruction = line.replaceFirst(STEP_PREFIX, "").trim()
                    if (instruction.isNotBlank()) {
                        steps += CookingStep(stepNumber = stepNumber++, instruction = instruction)
                    }
                }
            }
        }

        return ParsedTechCard(
            name = name,
            ingredients = ingredients,
            steps = steps,
            rawText = text,
        )
    }

    private val INGREDIENT_LINE = Regex("""^(.+?)\s+(\d+[.,]?\d*)\s*([a-zA-Zа-яА-ЯёЁ]+)$""")
    private val STEP_PREFIX = Regex("""^\d+[\.)]\s*""")
}
