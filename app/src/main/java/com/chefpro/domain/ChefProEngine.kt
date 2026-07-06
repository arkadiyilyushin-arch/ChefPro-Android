package com.chefpro.domain

import com.chefpro.model.ChefProState
import com.chefpro.model.Dish
import com.chefpro.model.DishType
import com.chefpro.model.InventoryItem
import com.chefpro.model.ModelHelpers.discount
import com.chefpro.model.ModelHelpers.isLowStock
import com.chefpro.model.ModelHelpers.minSpent
import com.chefpro.model.ModelHelpers.tierFor
import com.chefpro.model.LoyaltyTier
import com.chefpro.model.Production
import com.chefpro.model.RecipeIngredient
import com.chefpro.model.Shift
import com.chefpro.model.StockMovement
import com.chefpro.model.StockMovementType
import com.chefpro.model.WriteOff
import com.chefpro.model.newId
import java.util.Calendar

enum class MenuEngineeringCategory {
    STAR,
    HORSE,
    PUZZLE,
    DOG,
}

data class AbcAnalysisItem(
    val item: InventoryItem,
    val value: Double,
    val valuePercent: Double,
    val cumulativePercent: Double,
    val category: String,
)

data class MenuEngineeringItem(
    val dish: Dish,
    val portions: Int,
    val margin: Double,
    val foodCostPercent: Double,
    val category: MenuEngineeringCategory,
)

data class BreakevenInput(
    val rent: Double = 0.0,
    val salaries: Double = 0.0,
    val utilities: Double = 0.0,
    val otherFixed: Double = 0.0,
    val averageCheck: Double = 0.0,
)

data class BreakevenResult(
    val fixedCosts: Double,
    val averageFoodCostPercent: Double,
    val variableCostRate: Double,
    val contributionMarginRate: Double,
    val breakevenRevenueMonthly: Double,
    val breakevenRevenueDaily: Double,
    val breakevenCoversMonthly: Double,
    val breakevenCoversDaily: Double,
    val currentMonthRevenue: Double,
    val safetyMarginPercent: Double,
    val isAboveBreakeven: Boolean,
)

data class ProductionResult(
    val success: Boolean,
    val inventoryItems: List<InventoryItem>,
    val writeOffs: List<WriteOff>,
    val productions: List<Production>,
    val stockMovements: List<StockMovement>,
)

object ChefProEngine {

    private val standardUnits = setOf("г", "кг", "мл", "л", "шт")

    fun convert(quantity: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return quantity

        if (fromUnit == "г" && toUnit == "кг") return quantity / 1000
        if (fromUnit == "кг" && toUnit == "г") return quantity * 1000
        if (fromUnit == "мл" && toUnit == "л") return quantity / 1000
        if (fromUnit == "л" && toUnit == "мл") return quantity * 1000

        if (standardUnits.contains(fromUnit) && standardUnits.contains(toUnit)) {
            return 0.0
        }

        return quantity
    }

    fun calculateDishCost(
        dish: Dish,
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): Double {
        return dish.ingredients.sumOf { ingredient ->
            ingredientCost(ingredient, dishes, inventory)
        }
    }

    private fun ingredientCost(
        ingredient: RecipeIngredient,
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): Double {
        val semifinished = dishes.firstOrNull {
            it.dishType == DishType.SEMIFINISHED &&
                it.name.equals(ingredient.productName, ignoreCase = true)
        }
        if (semifinished != null) {
            val sfCost = calculateDishCost(semifinished, dishes, inventory)
            val weightInIngredientUnit = convert(
                quantity = semifinished.portionWeight,
                fromUnit = semifinished.portionWeightUnit.ifEmpty { "г" },
                toUnit = ingredient.unit,
            )
            val weight = when {
                weightInIngredientUnit > 0 -> weightInIngredientUnit
                semifinished.portionWeight > 0 -> semifinished.portionWeight
                else -> 1.0
            }
            val rawQty = if (ingredient.yieldFactor > 0) {
                ingredient.quantity / ingredient.yieldFactor
            } else {
                ingredient.quantity
            }
            return (rawQty / weight) * sfCost
        }

        val item = findInventoryItem(ingredient.productName, inventory) ?: return 0.0
        val rawQty = if (ingredient.yieldFactor > 0) {
            ingredient.quantity / ingredient.yieldFactor
        } else {
            ingredient.quantity
        }
        val convertedQuantity = convert(rawQty, ingredient.unit, item.unit)
        return convertedQuantity * item.pricePerUnit
    }

    fun foodCostPercent(
        dish: Dish,
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): Double {
        if (dish.salePrice <= 0) return 0.0
        val cost = calculateDishCost(dish, dishes, inventory)
        if (cost <= 0) return 0.0
        return cost / dish.salePrice * 100
    }

    fun hasFoodCostPercent(
        dish: Dish,
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): Boolean {
        return dish.salePrice > 0 && calculateDishCost(dish, dishes, inventory) > 0
    }

    fun unmatchedIngredients(
        dish: Dish,
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): List<String> {
        return dish.ingredients.mapNotNull { ingredient ->
            val isSemifinished = dishes.any {
                it.dishType == DishType.SEMIFINISHED &&
                    it.name.equals(ingredient.productName, ignoreCase = true)
            }
            if (isSemifinished) return@mapNotNull null

            val found = inventory.any { item ->
                item.name.equals(ingredient.productName, ignoreCase = true) ||
                    item.name.contains(ingredient.productName, ignoreCase = true) ||
                    ingredient.productName.contains(item.name, ignoreCase = true)
            }
            if (found) null else ingredient.productName
        }
    }

    fun canProduce(
        dish: Dish,
        portions: Int,
        inventory: List<InventoryItem>,
    ): Boolean {
        if (portions < 1) return false
        for (ingredient in dish.ingredients) {
            val item = inventory.firstOrNull {
                it.name.equals(ingredient.productName, ignoreCase = true)
            } ?: return false
            val needed = convert(
                quantity = ingredient.quantity * portions,
                fromUnit = ingredient.unit,
                toUnit = item.unit,
            )
            if (item.quantity < needed) return false
        }
        return true
    }

    fun produceDish(
        state: ChefProState,
        dish: Dish,
        portions: Int,
        employeeName: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProductionResult {
        if (portions < 1 || !canProduce(dish, portions, state.inventoryItems)) {
            return ProductionResult(
                success = false,
                inventoryItems = state.inventoryItems,
                writeOffs = state.writeOffs,
                productions = state.productions,
                stockMovements = state.stockMovements,
            )
        }

        val snapshotCost = calculateDishCost(dish, state.dishes, state.inventoryItems) * portions
        val mutableInventory = state.inventoryItems.toMutableList()
        val newWriteOffs = state.writeOffs.toMutableList()
        val newMovements = state.stockMovements.toMutableList()

        for (ingredient in dish.ingredients) {
            val index = mutableInventory.indexOfFirst {
                it.name.equals(ingredient.productName, ignoreCase = true)
            }
            if (index < 0) continue

            val item = mutableInventory[index]
            val needed = convert(
                quantity = ingredient.quantity * portions,
                fromUnit = ingredient.unit,
                toUnit = item.unit,
            )
            mutableInventory[index] = item.copy(quantity = item.quantity - needed)

            newMovements.add(
                StockMovement(
                    id = newId(),
                    itemName = item.name,
                    itemID = item.id,
                    type = StockMovementType.PRODUCTION,
                    quantity = needed,
                    unit = item.unit,
                    date = nowMillis,
                    note = "Производство: ${dish.name} x$portions",
                ),
            )

            newWriteOffs.add(
                WriteOff(
                    id = newId(),
                    productName = item.name,
                    quantity = needed,
                    unit = item.unit,
                    reason = "Автосписание: ${dish.name} x$portions",
                    employee = employeeName,
                    date = nowMillis,
                ),
            )
        }

        if (dish.dishType == DishType.SEMIFINISHED && dish.portionWeight > 0) {
            val produced = dish.portionWeight * portions
            val sfIndex = mutableInventory.indexOfFirst { it.sourceDishID == dish.id }
            if (sfIndex >= 0) {
                val item = mutableInventory[sfIndex]
                mutableInventory[sfIndex] = item.copy(quantity = item.quantity + produced)
            }
        }

        val production = Production(
            id = newId(),
            dishName = dish.name,
            portions = portions,
            totalCost = snapshotCost,
            date = nowMillis,
            employee = employeeName,
        )

        return ProductionResult(
            success = true,
            inventoryItems = mutableInventory,
            writeOffs = newWriteOffs,
            productions = state.productions + production,
            stockMovements = newMovements,
        )
    }

    fun purchaseList(inventory: List<InventoryItem>): List<InventoryItem> {
        return inventory
            .filter { it.isLowStock() }
            .sortedBy { it.name.lowercase() }
    }

    fun lowStockItems(inventory: List<InventoryItem>): List<InventoryItem> {
        return inventory.filter { it.isLowStock() }
    }

    fun expiringItems(
        inventory: List<InventoryItem>,
        warningDays: Int = 3,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<InventoryItem> {
        return inventory
            .filter { isExpired(it, nowMillis) || isExpiringSoon(it, warningDays, nowMillis) }
            .sortedBy { it.expiryDate ?: Long.MAX_VALUE }
    }

    fun currentMonthRevenue(shiftHistory: List<Shift>, nowMillis: Long = System.currentTimeMillis()): Double {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return shiftHistory
            .filter { isSameMonth(it.openedAt, calendar) }
            .sumOf { it.revenue }
    }

    fun currentMonthAvgFoodCost(
        shiftHistory: List<Shift>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Double {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val shifts = shiftHistory.filter {
            isSameMonth(it.openedAt, calendar) && it.foodCostForShift > 0
        }
        if (shifts.isEmpty()) return 0.0
        return shifts.sumOf { it.foodCostForShift } / shifts.size
    }

    fun averageFoodCostPercent(
        dishes: List<Dish>,
        inventory: List<InventoryItem>,
    ): Double {
        val activeDishes = dishes.filter { it.salePrice > 0 }
        if (activeDishes.isEmpty()) return 30.0
        val total = activeDishes.sumOf { foodCostPercent(it, dishes, inventory) }
        return total / activeDishes.size
    }

    fun breakeven(
        state: ChefProState,
        input: BreakevenInput,
        nowMillis: Long = System.currentTimeMillis(),
    ): BreakevenResult {
        val fixedCosts = input.rent + input.salaries + input.utilities + input.otherFixed
        val avgFoodCost = averageFoodCostPercent(state.dishes, state.inventoryItems)
        val variableCostRate = avgFoodCost / 100.0
        val contributionMarginRate = 1.0 - variableCostRate
        val breakevenRevenue = if (contributionMarginRate > 0) {
            fixedCosts / contributionMarginRate
        } else {
            0.0
        }
        val breakevenPerDay = breakevenRevenue / 30.0
        val breakevenCovers = if (input.averageCheck > 0) {
            breakevenRevenue / input.averageCheck
        } else {
            0.0
        }
        val monthRevenue = currentMonthRevenue(state.shiftHistory, nowMillis)
        val safetyMargin = if (breakevenRevenue > 0) {
            (monthRevenue - breakevenRevenue) / breakevenRevenue * 100
        } else {
            0.0
        }

        return BreakevenResult(
            fixedCosts = fixedCosts,
            averageFoodCostPercent = avgFoodCost,
            variableCostRate = variableCostRate,
            contributionMarginRate = contributionMarginRate,
            breakevenRevenueMonthly = breakevenRevenue,
            breakevenRevenueDaily = breakevenPerDay,
            breakevenCoversMonthly = breakevenCovers,
            breakevenCoversDaily = breakevenCovers / 30.0,
            currentMonthRevenue = monthRevenue,
            safetyMarginPercent = safetyMargin,
            isAboveBreakeven = monthRevenue >= breakevenRevenue,
        )
    }

    fun abcAnalysis(inventory: List<InventoryItem>): List<AbcAnalysisItem> {
        val sorted = inventory.sortedByDescending { it.quantity * it.pricePerUnit }
        val total = sorted.sumOf { it.quantity * it.pricePerUnit }
        if (total <= 0) {
            return sorted.map { item ->
                AbcAnalysisItem(
                    item = item,
                    value = 0.0,
                    valuePercent = 0.0,
                    cumulativePercent = 0.0,
                    category = "C",
                )
            }
        }

        var cumulative = 0.0
        return sorted.map { item ->
            val value = item.quantity * item.pricePerUnit
            val percent = value / total * 100
            val previousCumulative = cumulative
            cumulative += percent
            val category = when {
                previousCumulative < 80 -> "A"
                previousCumulative < 95 -> "B"
                else -> "C"
            }
            AbcAnalysisItem(
                item = item,
                value = value,
                valuePercent = percent,
                cumulativePercent = cumulative,
                category = category,
            )
        }
    }

    fun menuEngineering(
        dishes: List<Dish>,
        productions: List<Production>,
        inventory: List<InventoryItem>,
    ): List<MenuEngineeringItem> {
        if (dishes.isEmpty()) return emptyList()

        val metrics = dishes.map { dish ->
            val portions = productions
                .filter { it.dishName == dish.name }
                .sumOf { it.portions }
            val cost = calculateDishCost(dish, dishes, inventory)
            val margin = if (dish.salePrice > 0) {
                (dish.salePrice - cost) / dish.salePrice
            } else {
                0.0
            }
            val fc = foodCostPercent(dish, dishes, inventory)
            MenuEngineeringItem(
                dish = dish,
                portions = portions,
                margin = margin,
                foodCostPercent = fc,
                category = MenuEngineeringCategory.DOG,
            )
        }

        val sortedPortions = metrics.map { it.portions }.sorted()
        val sortedMargins = metrics.map { it.margin }.sorted()
        val medianPopularity = sortedPortions[sortedPortions.size / 2]
        val medianMargin = sortedMargins[sortedMargins.size / 2]

        return metrics.map { metric ->
            val highPopularity = metric.portions >= medianPopularity
            val highProfit = metric.margin >= medianMargin
            val category = when {
                highPopularity && highProfit -> MenuEngineeringCategory.STAR
                highPopularity -> MenuEngineeringCategory.HORSE
                highProfit -> MenuEngineeringCategory.PUZZLE
                else -> MenuEngineeringCategory.DOG
            }
            metric.copy(category = category)
        }
    }

    fun loyaltyTier(totalSpent: Double) = tierFor(totalSpent)

    fun loyaltyDiscountPercent(totalSpent: Double): Int {
        return tierFor(totalSpent).discount()
    }

    fun loyaltyPointsForPurchase(amount: Double): Int {
        return (amount / 100).toInt()
    }

    fun loyaltyRedeemAmount(points: Int): Double {
        return points.toDouble()
    }

    fun pointsToNextTier(totalSpent: Double, currentTier: LoyaltyTier = tierFor(totalSpent)): Int {
        val tiers = LoyaltyTier.entries
        val index = tiers.indexOf(currentTier)
        if (index < 0 || index + 1 >= tiers.size) return 0
        val next = tiers[index + 1]
        return ((next.minSpent() - totalSpent) / 10).toInt().coerceAtLeast(0)
    }

    fun purchaseList(state: ChefProState): List<InventoryItem> = purchaseList(state.inventoryItems)

    fun lowStockItems(state: ChefProState): List<InventoryItem> = lowStockItems(state.inventoryItems)

    fun expiringItems(state: ChefProState, nowMillis: Long = System.currentTimeMillis()): List<InventoryItem> {
        return expiringItems(state.inventoryItems, state.expiryWarningDays, nowMillis)
    }

    private fun findInventoryItem(name: String, inventory: List<InventoryItem>): InventoryItem? {
        return inventory.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: inventory.firstOrNull {
                it.name.contains(name, ignoreCase = true) ||
                    name.contains(it.name, ignoreCase = true)
            }
    }

    private fun isExpired(item: InventoryItem, nowMillis: Long): Boolean {
        val expiry = item.expiryDate ?: return false
        return expiry < nowMillis
    }

    private fun isExpiringSoon(item: InventoryItem, warningDays: Int, nowMillis: Long): Boolean {
        val expiry = item.expiryDate ?: return false
        val warningLimit = nowMillis + warningDays.toLong() * 24 * 60 * 60 * 1000
        return expiry > nowMillis && expiry <= warningLimit
    }

    private fun isSameMonth(timestamp: Long, reference: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == reference.get(Calendar.MONTH)
    }
}
