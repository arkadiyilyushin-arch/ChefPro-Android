package com.chefpro.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

fun newId(): String = java.util.UUID.randomUUID().toString()

val ALL_ALLERGENS = listOf(
    "Глютен",
    "Лактоза",
    "Яйца",
    "Орехи",
    "Рыба",
    "Морепродукты",
    "Соя",
    "Сельдерей",
    "Горчица",
    "Кунжут",
    "Сульфиты",
)

// MARK: - Recipe & dishes

@Serializable
data class RecipeIngredient(
    val id: String = newId(),
    val productName: String,
    val quantity: Double,
    val unit: String,
    val yieldFactor: Double = 1.0,
)

@Serializable
enum class DishType {
    @SerialName("Блюдо") DISH,
    @SerialName("Полуфабрикат") SEMIFINISHED,
}

@Serializable
enum class DishMenuStatus {
    @SerialName("Активное") ACTIVE,
    @SerialName("Сезонное") SEASONAL,
    @SerialName("Снято") REMOVED,
}

@Serializable
data class CookingStep(
    val id: String = newId(),
    val stepNumber: Int,
    val instruction: String,
    val photoFilename: String? = null,
    val durationMinutes: Int = 0,
    val tip: String = "",
)

@Serializable
data class Dish(
    val id: String = newId(),
    val name: String,
    val category: String,
    val salePrice: Double,
    val ingredients: List<RecipeIngredient>,
    val allergens: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val cookTime: Int = 0,
    val menuStatus: DishMenuStatus = DishMenuStatus.ACTIVE,
    val photoFilename: String? = null,
    val steps: List<CookingStep> = emptyList(),
    val dishType: DishType = DishType.DISH,
    val portionWeight: Double = 0.0,
    val portionWeightUnit: String = "г",
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,
    val isStopListed: Boolean = false,
    val isGoListed: Boolean = false,
)

@Serializable
data class Sale(
    val id: String = newId(),
    val dishName: String,
    val portions: Int,
    val date: Long,
    val employee: String,
)

@Serializable
data class PlanItem(
    val id: String = newId(),
    val dishID: String,
    val dishName: String,
    val portions: Int,
)

@Serializable
data class PricePoint(
    val id: String = newId(),
    val date: Long,
    val price: Double,
)

// MARK: - Inventory

@Serializable
data class InventoryItem(
    val id: String = newId(),
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val minQuantity: Double,
    val pricePerUnit: Double,
    val barcode: String = "",
    val priceHistory: List<PricePoint> = emptyList(),
    val expiryDate: Long? = null,
    val orderUnit: String = "",
    val orderUnitRatio: Double = 1.0,
    val sourceDishID: String? = null,
)

@Serializable
data class Delivery(
    val id: String = newId(),
    val supplier: String,
    val productName: String,
    val category: String = "",
    val quantity: Double,
    val unit: String,
    val price: Double,
    val date: Long,
    val acceptedBy: String,
    val notes: String = "",
)

@Serializable
data class ExtraPurchaseItem(
    val id: String = newId(),
    val name: String,
    val quantity: Double,
    val unit: String,
    val note: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class WriteOff(
    val id: String = newId(),
    val productName: String,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val employee: String,
    val date: Long,
)

@Serializable
enum class StockMovementType {
    @SerialName("Приход") DELIVERY,
    @SerialName("Списание") WRITE_OFF,
    @SerialName("Производство") PRODUCTION,
    @SerialName("Инвентаризация") AUDIT,
    @SerialName("Корректировка") ADJUSTMENT,
}

@Serializable
data class StockMovement(
    val id: String = newId(),
    val itemName: String,
    val itemID: String? = null,
    val type: StockMovementType,
    val quantity: Double,
    val unit: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
)

@Serializable
data class Production(
    val id: String = newId(),
    val dishName: String,
    val portions: Int,
    val totalCost: Double,
    val date: Long,
    val employee: String,
    val actualPortionWeight: Double = 0.0,
)

// MARK: - People & settings

@Serializable
data class UserProfile(
    val name: String,
    val position: String,
    val phone: String,
    val permissions: List<String>,
)

@Serializable
data class Employee(
    val id: String = newId(),
    val name: String,
    val position: String,
    val phone: String,
    val pin: String,
    val permissions: List<String>,
)

@Serializable
enum class AppColorScheme {
    @SerialName("Системная") SYSTEM,
    @SerialName("Светлая") LIGHT,
    @SerialName("Тёмная") DARK,
}

@Serializable
data class Supplier(
    val id: String = newId(),
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
)

@Serializable
data class Shift(
    val id: String = newId(),
    val openedAt: Long,
    val closedAt: Long? = null,
    val openedBy: String,
    val productionsCount: Int = 0,
    val writeOffsCount: Int = 0,
    val deliveriesCount: Int = 0,
    val totalProductionCost: Double = 0.0,
    val totalDeliveryCost: Double = 0.0,
    val revenue: Double = 0.0,
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0,
    val guestsCount: Int = 0,
    val foodCostForShift: Double = 0.0,
)

@Serializable
enum class ChecklistType {
    @SerialName("Открытие") OPENING,
    @SerialName("Закрытие") CLOSING,
}

@Serializable
data class ChecklistItem(
    val id: String = newId(),
    val text: String,
    val type: ChecklistType,
    val isDefault: Boolean = true,
    val isCompleted: Boolean = false,
    val completedBy: String = "",
    val completedAt: Long? = null,
)

@Serializable
data class MenuCollection(
    val id: String = newId(),
    val name: String,
    val emoji: String = "🍽️",
    val dishIDs: List<String> = emptyList(),
)

@Serializable
data class WorkShift(
    val id: String = newId(),
    val employeeID: String,
    val employeeName: String,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val notes: String = "",
)

@Serializable
enum class OrderStatus {
    @SerialName("Новые") NEW,
    @SerialName("Готовится") COOKING,
    @SerialName("Готово") READY,
}

@Serializable
data class KitchenOrder(
    val id: String = newId(),
    val dishName: String,
    val portions: Int,
    val tableNumber: String = "",
    val note: String = "",
    val course: Int = 1,
    val status: OrderStatus = OrderStatus.NEW,
    val createdAt: Long = System.currentTimeMillis(),
    val cookingStartedAt: Long? = null,
    val readyAt: Long? = null,
)

@Serializable
data class TemperatureLog(
    val id: String = newId(),
    val location: String,
    val temperature: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    val recordedBy: String = "",
    val notes: String = "",
)

@Serializable
enum class AppLanguage {
    @SerialName("Русский") RUSSIAN,
    @SerialName("English") ENGLISH,
}

@Serializable
data class AppBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val restaurantName: String,
    val dishes: List<Dish>,
    val inventoryItems: List<InventoryItem>,
    val deliveries: List<Delivery>,
    val writeOffs: List<WriteOff>,
    val productions: List<Production>,
    val employees: List<Employee>,
    val suppliers: List<Supplier>,
    val sales: List<Sale>,
    val checklists: List<ChecklistItem>,
    val menuCollections: List<MenuCollection>,
    val workSchedule: List<WorkShift>,
    val temperatureLogs: List<TemperatureLog>,
    val shiftHistory: List<Shift>,
    val stockMovements: List<StockMovement> = emptyList(),
)

@Serializable
data class RecipeVersion(
    val id: String = newId(),
    val dishID: String,
    val dishName: String,
    val savedAt: Long = System.currentTimeMillis(),
    val savedBy: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<CookingStep>,
    val salePrice: Double,
    val cookTime: Int,
    val notes: String = "",
)

// MARK: - Operating expenses

@Serializable
enum class OperatingExpenseCategory {
    @SerialName("Аренда") RENT,
    @SerialName("Зарплата") SALARY,
    @SerialName("Коммунальные") UTILITIES,
    @SerialName("Маркетинг") MARKETING,
    @SerialName("Оборудование") EQUIPMENT,
    @SerialName("Прочее") OTHER,
}

@Serializable
enum class ExpenseRecurrence {
    @SerialName("Разовый") ONCE,
    @SerialName("Ежемесячно") MONTHLY,
    @SerialName("Еженедельно") WEEKLY,
    @SerialName("Ежегодно") YEARLY,
}

@Serializable
data class OperatingExpense(
    val id: String = newId(),
    val name: String,
    val amount: Double,
    val category: OperatingExpenseCategory,
    val recurrence: ExpenseRecurrence = ExpenseRecurrence.MONTHLY,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
)

// MARK: - Inventory audit

@Serializable
data class AuditLineRecord(
    val id: String = newId(),
    val itemName: String,
    val unit: String,
    val category: String,
    val systemQty: Double,
    val actualQty: Double,
)

@Serializable
data class InventoryAuditRecord(
    val id: String = newId(),
    val date: Long = System.currentTimeMillis(),
    val auditor: String = "",
    val lines: List<AuditLineRecord> = emptyList(),
)

// MARK: - Reservations

@Serializable
enum class ReservationStatus {
    @SerialName("Подтверждено") CONFIRMED,
    @SerialName("Пришли") ARRIVED,
    @SerialName("Отменено") CANCELLED,
    @SerialName("Не пришли") NO_SHOW,
}

@Serializable
data class TableReservation(
    val id: String = newId(),
    val guestName: String,
    val guestPhone: String = "",
    val tableNumber: String,
    val persons: Int,
    val date: Long,
    val duration: Int = 120,
    val notes: String = "",
    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val createdBy: String = "",
)

// MARK: - Loyalty

@Serializable
enum class LoyaltyTier {
    @SerialName("Бронза") BRONZE,
    @SerialName("Серебро") SILVER,
    @SerialName("Золото") GOLD,
    @SerialName("Платина") PLATINUM,
}

@Serializable
data class LoyaltyTransaction(
    val id: String = newId(),
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val points: Int,
    val description: String = "",
)

@Serializable
data class LoyaltyCard(
    val id: String = newId(),
    val cardNumber: String,
    val guestName: String,
    val phone: String = "",
    val email: String = "",
    val points: Int = 0,
    val totalSpent: Double = 0.0,
    val visitsCount: Int = 0,
    val registeredAt: Long = System.currentTimeMillis(),
    val transactions: List<LoyaltyTransaction> = emptyList(),
)

// MARK: - POS integration

@Serializable
enum class POSSystem {
    @SerialName("iiko") IIKO,
    @SerialName("Poster") POSTER,
    @SerialName("r_keeper") RKEEPER,
    @SerialName("Tillypad") TILLYPAD,
    @SerialName("Ручной ввод") MANUAL,
}

@Serializable
data class POSSaleRecord(
    val id: String = newId(),
    val date: Long,
    val dishName: String,
    val quantity: Int,
    val amount: Double,
    val posSystem: POSSystem,
    val importedAt: Long = System.currentTimeMillis(),
)

// MARK: - Computed property helpers

object ModelHelpers {

    val kitchenOrderCourseNames = mapOf(
        1 to "1 курс",
        2 to "2 курс",
        3 to "3 курс",
        4 to "4 курс",
        5 to "5 курс",
    )

    fun InventoryItem.isLowStock(): Boolean = quantity <= minQuantity

    fun InventoryItem.isExpired(now: Long = System.currentTimeMillis()): Boolean {
        val expiry = expiryDate ?: return false
        return expiry < now
    }

    fun InventoryItem.isExpiringSoon(now: Long = System.currentTimeMillis()): Boolean {
        val expiry = expiryDate ?: return false
        val inThreeDays = now + TimeUnit.DAYS.toMillis(3)
        return expiry > now && expiry <= inThreeDays
    }

    fun InventoryItem.effectiveOrderUnit(): String =
        orderUnit.ifEmpty { unit }

    fun Shift.averageCheck(): Double =
        if (guestsCount > 0) revenue / guestsCount else 0.0

    fun Shift.isOpen(): Boolean = closedAt == null

    fun Shift.duration(now: Long = System.currentTimeMillis()): String {
        val end = closedAt ?: now
        val secs = ((end - openedAt) / 1000).toInt().coerceAtLeast(0)
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return if (h > 0) "${h}ч ${m}м" else "${m}м"
    }

    fun WorkShift.duration(): String {
        val secs = ((endTime - startTime) / 1000).toInt().coerceAtLeast(0)
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return if (h > 0) "${h}ч ${m}м" else "${m}м"
    }

    fun OrderStatus.next(): OrderStatus? = when (this) {
        OrderStatus.NEW -> OrderStatus.COOKING
        OrderStatus.COOKING -> OrderStatus.READY
        OrderStatus.READY -> null
    }

    fun OrderStatus.actionLabel(): String = when (this) {
        OrderStatus.NEW -> "В готовку"
        OrderStatus.COOKING -> "Готово!"
        OrderStatus.READY -> ""
    }

    fun KitchenOrder.courseName(): String =
        kitchenOrderCourseNames[course] ?: "$course курс"

    fun StockMovementType.sign(): String = when (this) {
        StockMovementType.DELIVERY -> "+"
        StockMovementType.WRITE_OFF, StockMovementType.PRODUCTION -> "−"
        StockMovementType.AUDIT, StockMovementType.ADJUSTMENT -> "±"
    }

    fun TemperatureLog.isOk(): Boolean =
        temperature >= -25 && temperature <= 8

    fun TemperatureLog.isCritical(): Boolean =
        temperature > 8 || temperature < -25

    fun TemperatureLog.statusLabel(): String = when {
        isCritical() -> "⚠ Нарушение"
        isOk() -> "✓ Норма"
        else -> "В норме"
    }

    fun AuditLineRecord.difference(): Double = actualQty - systemQty

    fun InventoryAuditRecord.totalItems(): Int = lines.size

    fun InventoryAuditRecord.filledItems(): Int = lines.size

    fun InventoryAuditRecord.discrepancies(): Int =
        lines.count { kotlin.math.abs(it.difference()) > 0.001 }

    fun InventoryAuditRecord.totalShortage(): Double =
        lines.sumOf { minOf(it.difference(), 0.0) }

    fun InventoryAuditRecord.totalSurplus(): Double =
        lines.sumOf { maxOf(it.difference(), 0.0) }

    fun TableReservation.endDate(): Long =
        date + TimeUnit.MINUTES.toMillis(duration.toLong())

    fun LoyaltyTier.minSpent(): Double = when (this) {
        LoyaltyTier.BRONZE -> 0.0
        LoyaltyTier.SILVER -> 10_000.0
        LoyaltyTier.GOLD -> 30_000.0
        LoyaltyTier.PLATINUM -> 100_000.0
    }

    fun LoyaltyTier.discount(): Int = when (this) {
        LoyaltyTier.BRONZE -> 3
        LoyaltyTier.SILVER -> 5
        LoyaltyTier.GOLD -> 7
        LoyaltyTier.PLATINUM -> 10
    }

    fun tierFor(totalSpent: Double): LoyaltyTier = when {
        totalSpent >= LoyaltyTier.PLATINUM.minSpent() -> LoyaltyTier.PLATINUM
        totalSpent >= LoyaltyTier.GOLD.minSpent() -> LoyaltyTier.GOLD
        totalSpent >= LoyaltyTier.SILVER.minSpent() -> LoyaltyTier.SILVER
        else -> LoyaltyTier.BRONZE
    }

    fun LoyaltyCard.tier(): LoyaltyTier = tierFor(totalSpent)

    fun LoyaltyCard.pointsToNextTier(): Int {
        val tiers = LoyaltyTier.entries
        val current = tier()
        val idx = tiers.indexOf(current)
        if (idx < 0 || idx + 1 >= tiers.size) return 0
        val next = tiers[idx + 1]
        return ((next.minSpent() - totalSpent) / 10).toInt()
    }
}
