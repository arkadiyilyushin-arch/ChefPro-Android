package com.chefpro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chefpro.ChefProApplication
import com.chefpro.data.ChefProRepository
import com.chefpro.data.DemoData
import com.chefpro.domain.ChefProEngine
import com.chefpro.domain.AbcAnalysisItem
import com.chefpro.domain.BreakevenInput
import com.chefpro.domain.BreakevenResult
import com.chefpro.domain.MenuEngineeringCategory
import com.chefpro.domain.MenuEngineeringItem
import com.chefpro.model.AppColorScheme
import com.chefpro.model.AppLanguage
import com.chefpro.firebase.FirebaseSyncService
import com.chefpro.model.ChecklistItem
import com.chefpro.model.ChecklistType
import com.chefpro.model.ChefProState
import com.chefpro.model.Delivery
import com.chefpro.model.Dish
import com.chefpro.model.DishType
import com.chefpro.model.Employee
import com.chefpro.model.ExtraPurchaseItem
import com.chefpro.model.InventoryAuditRecord
import com.chefpro.model.InventoryItem
import com.chefpro.model.KitchenOrder
import com.chefpro.model.LoyaltyCard
import com.chefpro.model.LoyaltyTransaction
import com.chefpro.model.MenuCollection
import com.chefpro.model.ModelHelpers.next
import com.chefpro.model.OperatingExpense
import com.chefpro.model.POSSaleRecord
import com.chefpro.model.PlanItem
import com.chefpro.model.PricePoint
import com.chefpro.model.RecipeVersion
import com.chefpro.model.Sale
import com.chefpro.model.Shift
import com.chefpro.model.StockMovement
import com.chefpro.model.StockMovementType
import com.chefpro.model.Supplier
import com.chefpro.model.TableReservation
import com.chefpro.model.TemperatureLog
import com.chefpro.model.UserProfile
import com.chefpro.model.WorkShift
import com.chefpro.model.WriteOff
import com.chefpro.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class UndoableAction(
    val description: String,
    val restore: () -> Unit,
)

class ChefProViewModel(application: Application) : AndroidViewModel(application) {

    private val app = ChefProApplication.get(application)
    private val repository: ChefProRepository = app.repository
    private val syncService: FirebaseSyncService = app.syncService
    private val notificationHelper: NotificationHelper = app.notificationHelper

    private val _state = MutableStateFlow(ChefProState())
    val state: StateFlow<ChefProState> = _state.asStateFlow()

    val isOffline: StateFlow<Boolean> = syncService.isOffline
    val pendingSyncCount: StateFlow<Int> = syncService.pendingSyncCount
    val lastSyncDate: StateFlow<Long?> = syncService.lastSyncDate
    val isSyncing: StateFlow<Boolean> = syncService.isSyncing
    val syncError: StateFlow<String?> = syncService.syncError

    private val _undoAction = MutableStateFlow<UndoableAction?>(null)
    val undoAction: StateFlow<UndoableAction?> = _undoAction.asStateFlow()

    private var uploadJob: Job? = null
    private var isSyncingFromCloud = false

    val currentEmployee: StateFlow<Employee?> = state.map { s ->
        s.currentEmployeeId?.let { id -> s.employees.firstOrNull { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoggedIn: StateFlow<Boolean> = state.map { it.currentEmployeeId != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lowStockItems = state.map { ChefProEngine.lowStockItems(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expiringItems = state.map { ChefProEngine.expiringItems(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val purchaseList = state.map { ChefProEngine.purchaseList(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalDeliverySum = state.map { s -> s.deliveries.sumOf { it.price } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val currentMonthRevenue = state.map { ChefProEngine.currentMonthRevenue(it.shiftHistory) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val currentMonthAvgFoodCost = state.map { ChefProEngine.currentMonthAvgFoodCost(it.shiftHistory) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val dishCategories = state.map { s -> s.dishes.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inventoryCategories = state.map { s -> s.inventoryItems.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayReservations = state.map { s ->
        val cal = Calendar.getInstance()
        s.reservations
            .filter {
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.date }
                itemCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }
            .sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            var loaded = repository.loadState()
            if (loaded.dishes.isEmpty() && loaded.inventoryItems.isEmpty()) {
                loaded = DemoData.populateDemoData(loaded)
            }
            if (loaded.checklists.isEmpty()) {
                loaded = loaded.copy(checklists = defaultChecklists())
            }
            loaded = syncAllSemifinished(loaded)
            _state.value = loaded
            repository.saveState(loaded)
            notificationHelper.scheduleAll(loaded)

            runCatching { syncService.registerAsMember() }
            runCatching { mergeCloudData(syncService.syncFromCloud().getOrNull()) }
            startRealtimeListeners()
        }
    }

    fun hasPermission(permission: String): Boolean =
        _state.value.profile.permissions.contains(permission)

    fun login(employee: Employee, pin: String): Boolean {
        val s = _state.value
        if (!s.employees.any { it.id == employee.id } || employee.pin != pin) return false
        mutate(
            description = "Вход: ${employee.name}",
            persist = true,
            sync = false,
        ) { current ->
            current.copy(
                currentEmployeeId = employee.id,
                isLoggedIn = true,
                profile = UserProfile(
                    name = employee.name,
                    position = employee.position,
                    phone = employee.phone,
                    permissions = employee.permissions,
                ),
            )
        }
        return true
    }

    fun logout() {
        mutate("Выход") { it.copy(currentEmployeeId = null, isLoggedIn = false) }
    }

    fun undoLastAction() {
        _undoAction.value?.restore()
        _undoAction.value = null
    }

    // MARK: - Auth / employees

    fun addEmployee(employee: Employee) = mutate("Добавлен сотрудник ${employee.name}") {
        it.copy(employees = it.employees + employee)
    }

    fun updateEmployee(employee: Employee) = mutate("Обновлён сотрудник ${employee.name}") { current ->
        val employees = current.employees.map { if (it.id == employee.id) employee else it }
        val profile = if (current.currentEmployeeId == employee.id) {
            UserProfile(employee.name, employee.position, employee.phone, employee.permissions)
        } else {
            current.profile
        }
        current.copy(employees = employees, profile = profile)
    }

    fun deleteEmployee(employee: Employee) = mutate("Удалён сотрудник ${employee.name}") { current ->
        val loggedOut = if (current.currentEmployeeId == employee.id) {
            current.copy(currentEmployeeId = null, isLoggedIn = false)
        } else {
            current
        }
        loggedOut.copy(employees = loggedOut.employees.filterNot { it.id == employee.id })
    }

    // MARK: - Dishes

    fun addDish(dish: Dish) = mutate("Добавлено блюдо ${dish.name}") { current ->
        var next = current.copy(dishes = current.dishes + dish)
        if (dish.dishType == DishType.SEMIFINISHED) {
            next = syncSemifinishedInventoryItem(next, dish)
        }
        next
    }

    fun updateDish(updatedDish: Dish) = mutate("Обновлено блюдо ${updatedDish.name}") { current ->
        val old = current.dishes.firstOrNull { it.id == updatedDish.id } ?: return@mutate current
        var next = current.copy(dishes = current.dishes.map { if (it.id == updatedDish.id) updatedDish else it })
        next = when {
            updatedDish.dishType == DishType.SEMIFINISHED -> syncSemifinishedInventoryItem(next, updatedDish)
            old.dishType == DishType.SEMIFINISHED -> next.copy(
                inventoryItems = next.inventoryItems.filterNot { it.sourceDishID == updatedDish.id },
            )
            else -> next
        }
        next
    }

    fun deleteDish(dish: Dish) = mutate("Удалено блюдо ${dish.name}") { current ->
        current.copy(
            dishes = current.dishes.filterNot { it.id == dish.id },
            inventoryItems = if (dish.dishType == DishType.SEMIFINISHED) {
                current.inventoryItems.filterNot { it.sourceDishID == dish.id }
            } else {
                current.inventoryItems
            },
        )
    }

    fun toggleFavorite(dish: Dish) = mutate(sync = false, persist = true) { current ->
        current.copy(
            dishes = current.dishes.map {
                if (it.id == dish.id) it.copy(isFavorite = !it.isFavorite) else it
            },
        )
    }

    fun saveRecipeVersion(dish: Dish, notes: String = "") = mutate(sync = false) { current ->
        val version = RecipeVersion(
            dishID = dish.id,
            dishName = dish.name,
            savedBy = current.profile.name,
            ingredients = dish.ingredients,
            steps = dish.steps,
            salePrice = dish.salePrice,
            cookTime = dish.cookTime,
            notes = notes,
        )
        val versions = (listOf(version) + current.recipeVersions).take(50)
        current.copy(recipeVersions = versions)
    }

    fun restoreRecipeVersion(version: RecipeVersion) = mutate("Восстановлена версия ${version.dishName}") { current ->
        current.copy(
            dishes = current.dishes.map { dish ->
                if (dish.id != version.dishID) dish
                else dish.copy(
                    ingredients = version.ingredients,
                    steps = version.steps,
                    salePrice = version.salePrice,
                    cookTime = version.cookTime,
                )
            },
        )
    }

    fun versionsForDish(dishId: String): List<RecipeVersion> =
        _state.value.recipeVersions.filter { it.dishID == dishId }

    // MARK: - Inventory

    fun updateInventoryItem(updatedItem: InventoryItem) =
        mutate("Обновлён склад: ${updatedItem.name}") { current ->
            current.copy(
                inventoryItems = current.inventoryItems.map {
                    if (it.id == updatedItem.id) updatedItem else it
                },
            )
        }

    fun deleteInventoryItem(item: InventoryItem) =
        mutate("Удалена позиция ${item.name}") { current ->
            current.copy(inventoryItems = current.inventoryItems.filterNot { it.id == item.id })
        }

    fun inventoryItemForBarcode(code: String): InventoryItem? {
        if (code.isBlank()) return null
        return _state.value.inventoryItems.firstOrNull { it.barcode == code }
    }

    // MARK: - Deliveries / write-offs / production

    fun addDelivery(delivery: Delivery) = mutate("Приёмка: ${delivery.productName}") { current ->
        val movement = StockMovement(
            itemName = delivery.productName,
            type = StockMovementType.DELIVERY,
            quantity = delivery.quantity,
            unit = delivery.unit,
            date = delivery.date,
            note = "От: ${delivery.supplier}",
        )
        val deliveryPricePerUnit = if (delivery.quantity > 0) delivery.price / delivery.quantity else 0.0
        val inventory = current.inventoryItems.toMutableList()
        val index = inventory.indexOfFirst {
            it.name.equals(delivery.productName, ignoreCase = true) && it.unit == delivery.unit
        }
        if (index >= 0) {
            val item = inventory[index]
            if (deliveryPricePerUnit > 0) {
                val currentValue = item.quantity * item.pricePerUnit
                val newValue = delivery.quantity * deliveryPricePerUnit
                val combinedQty = item.quantity + delivery.quantity
                val newPrice = if (combinedQty > 0) (currentValue + newValue) / combinedQty else deliveryPricePerUnit
                val history = if (deliveryPricePerUnit > 0 && item.pricePerUnit != deliveryPricePerUnit) {
                    item.priceHistory + PricePoint(date = delivery.date, price = deliveryPricePerUnit)
                } else {
                    item.priceHistory
                }
                inventory[index] = item.copy(
                    quantity = item.quantity + delivery.quantity,
                    pricePerUnit = newPrice,
                    priceHistory = history,
                )
            } else {
                inventory[index] = item.copy(quantity = item.quantity + delivery.quantity)
            }
        } else {
            inventory.add(
                InventoryItem(
                    name = delivery.productName,
                    category = delivery.category.ifBlank { "Без категории" },
                    quantity = delivery.quantity,
                    unit = delivery.unit,
                    minQuantity = 1.0,
                    pricePerUnit = deliveryPricePerUnit,
                    priceHistory = if (deliveryPricePerUnit > 0) {
                        listOf(PricePoint(date = delivery.date, price = deliveryPricePerUnit))
                    } else {
                        emptyList()
                    },
                ),
            )
        }
        current.copy(
            deliveries = current.deliveries + delivery,
            inventoryItems = inventory,
            stockMovements = current.stockMovements + movement,
        )
    }

    fun addWriteOff(writeOff: WriteOff) = mutate("Списание: ${writeOff.productName}") { current ->
        val movement = StockMovement(
            itemName = writeOff.productName,
            type = StockMovementType.WRITE_OFF,
            quantity = writeOff.quantity,
            unit = writeOff.unit,
            date = writeOff.date,
            note = writeOff.reason,
        )
        val inventory = current.inventoryItems.map { item ->
            if (item.name.equals(writeOff.productName, ignoreCase = true) && item.unit == writeOff.unit) {
                item.copy(quantity = item.quantity - writeOff.quantity)
            } else {
                item
            }
        }
        current.copy(
            writeOffs = current.writeOffs + writeOff,
            inventoryItems = inventory,
            stockMovements = current.stockMovements + movement,
        )
    }

    fun produceDish(dish: Dish, portions: Int): Boolean {
        val current = _state.value
        val result = ChefProEngine.produceDish(current, dish, portions, current.profile.name)
        if (!result.success) return false
        mutate("Производство: ${dish.name} ×$portions", trackUndo = false) {
            it.copy(
                inventoryItems = result.inventoryItems,
                writeOffs = result.writeOffs,
                productions = result.productions,
                stockMovements = result.stockMovements,
            )
        }
        notificationHelper.scheduleAll(_state.value)
        return true
    }

    fun canProduce(dish: Dish, portions: Int): Boolean =
        ChefProEngine.canProduce(dish, portions, _state.value.inventoryItems)

    fun calculateDishCost(dish: Dish): Double =
        ChefProEngine.calculateDishCost(dish, _state.value.dishes, _state.value.inventoryItems)

    fun foodCostPercent(dish: Dish): Double =
        ChefProEngine.foodCostPercent(dish, _state.value.dishes, _state.value.inventoryItems)

    fun hasFoodCostPercent(dish: Dish): Boolean =
        ChefProEngine.hasFoodCostPercent(dish, _state.value.dishes, _state.value.inventoryItems)

    fun unmatchedIngredients(dish: Dish): List<String> =
        ChefProEngine.unmatchedIngredients(dish, _state.value.dishes, _state.value.inventoryItems)

    // MARK: - Extra purchases / plan

    fun addExtraPurchaseItem(item: ExtraPurchaseItem) = mutate(sync = false) {
        it.copy(extraPurchaseItems = it.extraPurchaseItems + item)
    }

    fun removeExtraPurchaseItem(item: ExtraPurchaseItem) = mutate(sync = false) {
        it.copy(extraPurchaseItems = it.extraPurchaseItems.filterNot { e -> e.id == item.id })
    }

    fun clearExtraPurchaseItems() = mutate(sync = false) {
        it.copy(extraPurchaseItems = emptyList())
    }

    fun addPlanItem(item: PlanItem) = mutate(sync = false) {
        it.copy(currentProductionPlan = it.currentProductionPlan + item)
    }

    fun removePlanItem(item: PlanItem) = mutate(sync = false) {
        it.copy(currentProductionPlan = it.currentProductionPlan.filterNot { it.id == item.id })
    }

    fun clearProductionPlan() = mutate(sync = false) {
        it.copy(currentProductionPlan = emptyList())
    }

    fun executeProductionPlan(): Int {
        val current = _state.value
        var working = current
        var executed = 0
        for (planItem in current.currentProductionPlan) {
            val dish = working.dishes.firstOrNull { it.id == planItem.dishID } ?: continue
            val result = ChefProEngine.produceDish(working, dish, planItem.portions, working.profile.name)
            if (!result.success) continue
            working = working.copy(
                inventoryItems = result.inventoryItems,
                writeOffs = result.writeOffs,
                productions = result.productions,
                stockMovements = result.stockMovements,
                sales = working.sales + Sale(
                    dishName = dish.name,
                    portions = planItem.portions,
                    date = System.currentTimeMillis(),
                    employee = working.profile.name,
                ),
            )
            executed++
        }
        mutate("Выполнен план производства ($executed)", trackUndo = false) {
            working.copy(currentProductionPlan = emptyList())
        }
        return executed
    }

    // MARK: - Shift

    fun openShift() = mutate("Смена открыта") { current ->
        if (current.currentShift != null) return@mutate current
        current.copy(
            currentShift = Shift(
                openedAt = System.currentTimeMillis(),
                openedBy = current.profile.name,
            ),
        )
    }

    fun closeShift() = mutate("Смена закрыта") { current ->
        val shift = current.currentShift ?: return@mutate current
        finalizeShift(current, shift, cashRevenue = 0.0, cardRevenue = 0.0, guestsCount = 0)
    }

    fun closeShiftWithRevenue(cashRevenue: Double, cardRevenue: Double, guestsCount: Int) =
        mutate("Смена закрыта с выручкой") { current ->
            val shift = current.currentShift ?: return@mutate current
            finalizeShift(current, shift, cashRevenue, cardRevenue, guestsCount)
        }

    // MARK: - Kitchen orders

    fun addKitchenOrder(order: KitchenOrder) {
        mutate("Заказ: ${order.dishName}") { it.copy(kitchenOrders = it.kitchenOrders + order) }
        if (_state.value.notificationsEnabled) {
            notificationHelper.notifyKitchenOrder(order.dishName, order.portions, order.tableNumber)
        }
        viewModelScope.launch { runCatching { syncService.uploadKitchenOrder(order) } }
    }

    fun advanceOrderStatus(order: KitchenOrder) = mutate("Статус заказа: ${order.dishName}") { current ->
        val idx = current.kitchenOrders.indexOfFirst { it.id == order.id }
        if (idx < 0) return@mutate current
        val existing = current.kitchenOrders[idx]
        val nextStatus = existing.status.next() ?: return@mutate current
        val now = System.currentTimeMillis()
        val updated = when (nextStatus) {
            com.chefpro.model.OrderStatus.COOKING -> existing.copy(status = nextStatus, cookingStartedAt = now)
            com.chefpro.model.OrderStatus.READY -> existing.copy(status = nextStatus, readyAt = now)
            else -> existing.copy(status = nextStatus)
        }
        val orders = current.kitchenOrders.toMutableList()
        orders[idx] = updated
        viewModelScope.launch { runCatching { syncService.uploadKitchenOrder(updated) } }
        current.copy(kitchenOrders = orders)
    }

    fun deleteKitchenOrder(order: KitchenOrder) = mutate("Удалён заказ") {
        it.copy(kitchenOrders = it.kitchenOrders.filterNot { o -> o.id == order.id })
    }

    fun archiveKitchenOrder(order: KitchenOrder) = mutate("Архив заказа") { current ->
        val closed = listOf(order) + current.closedKitchenOrders
        viewModelScope.launch {
            runCatching {
                syncService.deleteKitchenOrder(order.id)
                syncService.uploadClosedKitchenOrder(order)
            }
        }
        current.copy(
            kitchenOrders = current.kitchenOrders.filterNot { it.id == order.id },
            closedKitchenOrders = closed.take(100),
        )
    }

    fun archiveTableOrders(tableNumber: String) = mutate("Архив стола $tableNumber") { current ->
        val tableOrders = current.kitchenOrders.filter { it.tableNumber == tableNumber }
        val closed = tableOrders.sortedBy { it.createdAt } + current.closedKitchenOrders
        current.copy(
            kitchenOrders = current.kitchenOrders.filterNot { it.tableNumber == tableNumber },
            closedKitchenOrders = closed.take(100),
        )
    }

    // MARK: - Suppliers

    fun addSupplier(supplier: Supplier) = mutate("Поставщик ${supplier.name}") {
        it.copy(suppliers = it.suppliers + supplier)
    }

    fun updateSupplier(supplier: Supplier) = mutate("Обновлён поставщик") { current ->
        current.copy(suppliers = current.suppliers.map { if (it.id == supplier.id) supplier else it })
    }

    fun deleteSupplier(supplier: Supplier) = mutate("Удалён поставщик") {
        it.copy(suppliers = it.suppliers.filterNot { s -> s.id == supplier.id })
    }

    // MARK: - Sales

    fun addSale(sale: Sale) = mutate(sync = false) { it.copy(sales = it.sales + sale) }
    fun deleteSale(sale: Sale) = mutate(sync = false) { it.copy(sales = it.sales.filterNot { it.id == sale.id }) }

    // MARK: - Reservations

    fun addReservation(reservation: TableReservation) = mutate("Бронь ${reservation.guestName}") { current ->
        if (current.notificationsEnabled) {
            notificationHelper.scheduleReservationReminder(reservation)
        }
        current.copy(reservations = current.reservations + reservation)
    }

    fun updateReservation(reservation: TableReservation) = mutate("Обновлена бронь") { current ->
        current.copy(
            reservations = current.reservations.map { if (it.id == reservation.id) reservation else it },
        )
    }

    fun deleteReservation(reservation: TableReservation) = mutate("Удалена бронь") { current ->
        notificationHelper.cancelReservationReminder(reservation.id)
        current.copy(reservations = current.reservations.filterNot { it.id == reservation.id })
    }

    // MARK: - Loyalty

    fun addLoyaltyCard(card: LoyaltyCard) = mutate(sync = false) {
        it.copy(loyaltyCards = it.loyaltyCards + card)
    }

    fun updateLoyaltyCard(card: LoyaltyCard) = mutate(sync = false) { current ->
        current.copy(loyaltyCards = current.loyaltyCards.map { if (it.id == card.id) card else it })
    }

    fun deleteLoyaltyCard(card: LoyaltyCard) = mutate(sync = false) {
        it.copy(loyaltyCards = it.loyaltyCards.filterNot { c -> c.id == card.id })
    }

    fun addPurchaseToLoyalty(cardId: String, amount: Double, description: String = "") = mutate(sync = false) { current ->
        val idx = current.loyaltyCards.indexOfFirst { it.id == cardId }
        if (idx < 0) return@mutate current
        val card = current.loyaltyCards[idx]
        val points = ChefProEngine.loyaltyPointsForPurchase(amount)
        val tx = LoyaltyTransaction(amount = amount, points = points, description = description)
        val updated = card.copy(
            transactions = listOf(tx) + card.transactions,
            totalSpent = card.totalSpent + amount,
            points = card.points + points,
            visitsCount = card.visitsCount + 1,
        )
        current.copy(
            loyaltyCards = current.loyaltyCards.toMutableList().also { it[idx] = updated },
        )
    }

    fun redeemLoyaltyPoints(cardId: String, points: Int) = mutate(sync = false) { current ->
        val idx = current.loyaltyCards.indexOfFirst { it.id == cardId }
        if (idx < 0) return@mutate current
        val card = current.loyaltyCards[idx]
        if (card.points < points) return@mutate current
        val amount = ChefProEngine.loyaltyRedeemAmount(points)
        val tx = LoyaltyTransaction(amount = -amount, points = -points, description = "Списание баллов")
        val updated = card.copy(
            transactions = listOf(tx) + card.transactions,
            points = card.points - points,
        )
        current.copy(
            loyaltyCards = current.loyaltyCards.toMutableList().also { it[idx] = updated },
        )
    }

    fun loyaltyCardForPhone(phone: String): LoyaltyCard? =
        _state.value.loyaltyCards.firstOrNull { it.phone == phone }

    // MARK: - POS

    fun addPOSRecord(record: POSSaleRecord) = mutate(sync = false) {
        it.copy(posRecords = it.posRecords + record)
    }

    fun deletePOSRecord(record: POSSaleRecord) = mutate(sync = false) {
        it.copy(posRecords = it.posRecords.filterNot { r -> r.id == record.id })
    }

    fun importPOSRecords(records: List<POSSaleRecord>) = mutate("Импорт POS") { current ->
        val sales = records.map { record ->
            Sale(
                dishName = record.dishName,
                portions = record.quantity,
                date = record.date,
                employee = "POS Import",
            )
        }
        current.copy(
            posRecords = current.posRecords + records,
            sales = current.sales + sales,
        )
    }

    // MARK: - Checklists / schedule / temperature

    fun addChecklist(item: ChecklistItem) = mutate(sync = false) {
        it.copy(checklists = it.checklists + item)
    }

    fun updateChecklist(item: ChecklistItem) = mutate(sync = false) { current ->
        current.copy(checklists = current.checklists.map { if (it.id == item.id) item else it })
    }

    fun deleteChecklist(item: ChecklistItem) = mutate(sync = false) {
        it.copy(checklists = it.checklists.filterNot { c -> c.id == item.id })
    }

    fun resetChecklists(type: ChecklistType) = mutate(sync = false) { current ->
        current.copy(
            checklists = current.checklists.map { item ->
                if (item.type != type) item
                else item.copy(isCompleted = false, completedBy = "", completedAt = null)
            },
        )
    }

    fun completeChecklist(item: ChecklistItem, employee: String) = mutate(sync = false) { current ->
        current.copy(
            checklists = current.checklists.map {
                if (it.id != item.id) it
                else it.copy(isCompleted = true, completedBy = employee, completedAt = System.currentTimeMillis())
            },
        )
    }

    fun addCollection(collection: MenuCollection) = mutate(sync = false) {
        it.copy(menuCollections = it.menuCollections + collection)
    }

    fun updateCollection(collection: MenuCollection) = mutate(sync = false) { current ->
        current.copy(menuCollections = current.menuCollections.map { if (it.id == collection.id) collection else it })
    }

    fun deleteCollection(collection: MenuCollection) = mutate(sync = false) {
        it.copy(menuCollections = it.menuCollections.filterNot { c -> c.id == collection.id })
    }

    fun addWorkShift(shift: WorkShift) = mutate(sync = false) {
        it.copy(workSchedule = it.workSchedule + shift)
    }

    fun updateWorkShift(shift: WorkShift) = mutate(sync = false) { current ->
        current.copy(workSchedule = current.workSchedule.map { if (it.id == shift.id) shift else it })
    }

    fun deleteWorkShift(shift: WorkShift) = mutate(sync = false) {
        it.copy(workSchedule = it.workSchedule.filterNot { s -> s.id == shift.id })
    }

    fun addTemperatureLog(log: TemperatureLog) = mutate(sync = false) {
        it.copy(temperatureLogs = listOf(log) + it.temperatureLogs)
    }

    fun deleteTemperatureLog(log: TemperatureLog) = mutate(sync = false) {
        it.copy(temperatureLogs = it.temperatureLogs.filterNot { t -> t.id == log.id })
    }

    fun latestTemperatureLog(location: String): TemperatureLog? =
        _state.value.temperatureLogs.firstOrNull { it.location == location }

    // MARK: - Operating expenses / audit

    fun addOperatingExpense(expense: OperatingExpense) = mutate("Расход: ${expense.name}") {
        it.copy(operatingExpenses = it.operatingExpenses + expense)
    }

    fun updateOperatingExpense(expense: OperatingExpense) = mutate("Обновлён расход") { current ->
        current.copy(
            operatingExpenses = current.operatingExpenses.map { if (it.id == expense.id) expense else it },
        )
    }

    fun deleteOperatingExpense(expense: OperatingExpense) = mutate("Удалён расход") {
        it.copy(operatingExpenses = it.operatingExpenses.filterNot { e -> e.id == expense.id })
    }

    fun addAuditRecord(record: InventoryAuditRecord) = mutate("Инвентаризация") { current ->
        current.copy(auditRecords = listOf(record) + current.auditRecords)
    }

    fun deleteAuditRecord(record: InventoryAuditRecord) = mutate("Удалена инвентаризация") {
        it.copy(auditRecords = it.auditRecords.filterNot { r -> r.id == record.id })
    }

    // MARK: - Settings

    fun updateSettings(transform: (ChefProState) -> ChefProState) = mutate(sync = false, trackUndo = false) {
        val next = transform(it)
        notificationHelper.scheduleAll(next)
        next
    }

    fun setHasSeenOnboarding(seen: Boolean) = updateSettings { it.copy(hasSeenOnboarding = seen) }

    fun setNotificationsEnabled(enabled: Boolean) = updateSettings { it.copy(notificationsEnabled = enabled) }

    fun resetDemoData() {
        val reset = DemoData.resetDemoData().copy(
            checklists = defaultChecklists(),
            currentEmployeeId = null,
            isLoggedIn = false,
        )
        _state.value = reset
        repository.saveState(reset)
        scheduleUpload()
    }

    fun importBackup(jsonString: String) {
        val imported = repository.importBackup(jsonString)
        _state.value = imported
        notificationHelper.scheduleAll(imported)
        scheduleUpload()
    }


    // MARK: - Screen helpers

    fun menuEngineering(): List<MenuEngineeringItem> {
        val s = _state.value
        return ChefProEngine.menuEngineering(s.dishes, s.productions, s.inventoryItems)
    }

    fun menuEngineeringCategoryLabel(category: MenuEngineeringCategory): String = when (category) {
        MenuEngineeringCategory.STAR -> "Звёзды"
        MenuEngineeringCategory.HORSE -> "Лошадки"
        MenuEngineeringCategory.PUZZLE -> "Загадки"
        MenuEngineeringCategory.DOG -> "Собаки"
    }

    fun abcAnalysis(): List<AbcAnalysisItem> =
        ChefProEngine.abcAnalysis(_state.value.inventoryItems)

    fun breakeven(input: BreakevenInput): BreakevenResult =
        ChefProEngine.breakeven(_state.value, input)

    fun dishCost(dish: Dish): Double = calculateDishCost(dish)

    fun advanceKitchenOrder(order: KitchenOrder) = advanceOrderStatus(order)

    fun exportBackupJson(): String = exportBackup()

    fun importBackupJson(json: String) = importBackup(json)

    fun updateProfile(profile: UserProfile) = updateSettings { it.copy(profile = profile) }

    fun setAppColorScheme(scheme: AppColorScheme) = updateSettings { it.copy(appColorScheme = scheme) }

    fun setAppLanguage(language: AppLanguage) = updateSettings { it.copy(appLanguage = language) }

    fun setDailyDigestEnabled(enabled: Boolean) = updateSettings { it.copy(dailyDigestEnabled = enabled) }

    fun setHaccpRemindersEnabled(enabled: Boolean) = updateSettings { it.copy(haccpRemindersEnabled = enabled) }

    fun setFoodCostThreshold(value: Double) = updateSettings { it.copy(foodCostThreshold = value) }

    fun setExpiryWarningDays(days: Int) = updateSettings { it.copy(expiryWarningDays = days) }

    fun setPurchaseBudget(budget: Double) = updateSettings { it.copy(purchaseBudget = budget) }

    fun setMonthlyRevenuePlan(plan: Double) = updateSettings { it.copy(monthlyRevenuePlan = plan) }

    fun setMonthlyFoodCostTarget(target: Double) = updateSettings { it.copy(monthlyFoodCostTarget = target) }

    fun setRestaurantName(name: String) = updateSettings { it.copy(restaurantName = name) }

    fun setOfflineMode(enabled: Boolean) {
        syncService.setForceOffline(enabled)
    }

    fun clearPendingSync() {
        syncService.clearPendingSync()
    }

    fun exportBackup(): String = repository.exportBackup()

    // MARK: - Sync

    fun syncToCloud() {
        viewModelScope.launch {
            runCatching { syncService.uploadAll(_state.value) }
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            runCatching { mergeCloudData(syncService.syncFromCloud().getOrNull()) }
        }
    }

    fun connectToDevice(restaurantId: String) {
        viewModelScope.launch {
            runCatching {
                syncService.connectToDevice(restaurantId)
                mergeCloudData(syncService.syncFromCloud().getOrNull())
                startRealtimeListeners()
            }
        }
    }

    fun syncOnForeground() = syncFromCloud()

    // MARK: - Internal helpers

    private fun mutate(
        description: String = "",
        persist: Boolean = true,
        sync: Boolean = true,
        trackUndo: Boolean = true,
        block: (ChefProState) -> ChefProState,
    ) {
        val previous = _state.value
        val next = block(previous)
        if (next == previous) return
        _state.value = next
        if (trackUndo && description.isNotBlank()) {
            _undoAction.value = UndoableAction(description) { _state.value = previous }
        }
        if (persist) repository.saveState(next)
        notificationHelper.scheduleAll(next)
        if (sync && !isSyncingFromCloud) scheduleUpload()
    }

    private fun scheduleUpload() {
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            delay(2_000)
            if (isSyncingFromCloud || syncService.isOffline.value) {
                return@launch
            }
            runCatching { syncService.uploadAll(_state.value) }
        }
    }

    private fun mergeCloudData(cloud: com.chefpro.firebase.ChefProCloudData?) {
        if (cloud == null) return
        isSyncingFromCloud = true
        val current = _state.value
        val merged = current.copy(
            dishes = cloud.dishes.ifEmpty { current.dishes },
            inventoryItems = cloud.inventoryItems.ifEmpty { current.inventoryItems },
            deliveries = cloud.deliveries.ifEmpty { current.deliveries },
            writeOffs = cloud.writeOffs.ifEmpty { current.writeOffs },
            productions = cloud.productions.ifEmpty { current.productions },
            employees = cloud.employees.ifEmpty { current.employees },
            reservations = cloud.reservations.ifEmpty { current.reservations },
            suppliers = cloud.suppliers.ifEmpty { current.suppliers },
            kitchenOrders = cloud.kitchenOrders.ifEmpty { current.kitchenOrders },
            closedKitchenOrders = cloud.closedKitchenOrders.ifEmpty { current.closedKitchenOrders },
            sales = cloud.sales.ifEmpty { current.sales },
            operatingExpenses = cloud.operatingExpenses.ifEmpty { current.operatingExpenses },
            auditRecords = cloud.auditRecords.ifEmpty { current.auditRecords },
            shiftHistory = cloud.shiftHistory.ifEmpty { current.shiftHistory },
            profile = cloud.profile ?: current.profile,
            currentShift = cloud.currentShift ?: current.currentShift,
            restaurantName = cloud.restaurantName?.takeIf { it.isNotBlank() } ?: current.restaurantName,
        )
        _state.value = merged
        repository.saveState(merged)
        isSyncingFromCloud = false
    }

    private fun startRealtimeListeners() {
        syncService.startCollectionListeners(
            onDishes = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(dishes = items) } },
            onInventory = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(inventoryItems = items) } },
            onEmployees = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(employees = items) } },
            onKitchenOrders = { items -> patchFromCloud { it.copy(kitchenOrders = items) } },
            onClosedKitchenOrders = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(closedKitchenOrders = items) } },
            onReservations = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(reservations = items) } },
            onSales = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(sales = items) } },
            onOperatingExpenses = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(operatingExpenses = items) } },
            onSuppliers = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(suppliers = items) } },
            onDeliveries = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(deliveries = items) } },
            onWriteOffs = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(writeOffs = items) } },
            onProductions = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(productions = items) } },
            onShiftHistory = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(shiftHistory = items) } },
            onAuditRecords = { items -> if (items.isNotEmpty()) patchFromCloud { it.copy(auditRecords = items) } },
            onRootDoc = { shift, name ->
                patchFromCloud { current ->
                    current.copy(
                        currentShift = shift ?: current.currentShift,
                        restaurantName = name?.takeIf { it.isNotBlank() } ?: current.restaurantName,
                    )
                }
            },
        )
    }

    private fun patchFromCloud(transform: (ChefProState) -> ChefProState) {
        if (syncService.isSyncing.value) return
        isSyncingFromCloud = true
        val next = transform(_state.value)
        _state.value = next
        repository.saveState(next)
        isSyncingFromCloud = false
    }

    private fun finalizeShift(
        current: ChefProState,
        shift: Shift,
        cashRevenue: Double,
        cardRevenue: Double,
        guestsCount: Int,
    ): ChefProState {
        val since = shift.openedAt
        val closedAt = System.currentTimeMillis()
        val productionsInShift = current.productions.filter { it.date >= since }
        val writeOffsInShift = current.writeOffs.filter { it.date >= since }
        val deliveriesInShift = current.deliveries.filter { it.date >= since }
        val totalProductionCost = productionsInShift.sumOf { it.totalCost }
        val totalDeliveryCost = deliveriesInShift.sumOf { it.price }
        val revenue = cashRevenue + cardRevenue
        val foodCost = if (revenue > 0) totalProductionCost / revenue * 100 else 0.0
        val closed = shift.copy(
            closedAt = closedAt,
            productionsCount = productionsInShift.size,
            writeOffsCount = writeOffsInShift.size,
            deliveriesCount = deliveriesInShift.size,
            totalProductionCost = totalProductionCost,
            totalDeliveryCost = totalDeliveryCost,
            cashRevenue = cashRevenue,
            cardRevenue = cardRevenue,
            revenue = revenue,
            guestsCount = guestsCount,
            foodCostForShift = foodCost,
        )
        return current.copy(
            currentShift = null,
            shiftHistory = listOf(closed) + current.shiftHistory,
        )
    }

    private fun syncSemifinishedInventoryItem(state: ChefProState, dish: Dish): ChefProState {
        val unit = dish.portionWeightUnit.ifBlank { "г" }
        val costPerUnit = if (dish.portionWeight > 0) {
            ChefProEngine.calculateDishCost(dish, state.dishes, state.inventoryItems) / dish.portionWeight
        } else {
            ChefProEngine.calculateDishCost(dish, state.dishes, state.inventoryItems)
        }
        val index = state.inventoryItems.indexOfFirst { it.sourceDishID == dish.id }
        val inventory = state.inventoryItems.toMutableList()
        if (index >= 0) {
            inventory[index] = inventory[index].copy(name = dish.name, unit = unit, pricePerUnit = costPerUnit)
        } else {
            inventory.add(
                InventoryItem(
                    name = dish.name,
                    category = "Полуфабрикаты",
                    quantity = 0.0,
                    unit = unit,
                    minQuantity = 0.0,
                    pricePerUnit = costPerUnit,
                    sourceDishID = dish.id,
                ),
            )
        }
        return state.copy(inventoryItems = inventory)
    }

    private fun syncAllSemifinished(state: ChefProState): ChefProState {
        var working = state
        state.dishes.filter { it.dishType == DishType.SEMIFINISHED }.forEach { dish ->
            if (working.inventoryItems.none { it.sourceDishID == dish.id }) {
                working = syncSemifinishedInventoryItem(working, dish)
            }
        }
        return working
    }

    private fun defaultChecklists(): List<ChecklistItem> {
        val opening = listOf(
            "Проверить температуру холодильников",
            "Принять доставку и проверить товар",
            "Подготовить рабочие станции",
            "Проверить наличие всех ингредиентов",
            "Включить оборудование и проверить работу",
            "Провести брифинг команды",
        )
        val closing = listOf(
            "Списать остатки по итогам смены",
            "Убрать и продезинфицировать рабочие места",
            "Проверить и отключить оборудование",
            "Закрыть холодильники и морозильные камеры",
            "Провести инвентаризацию на конец дня",
            "Оформить отчет о смене",
        )
        return opening.map { ChecklistItem(text = it, type = ChecklistType.OPENING) } +
            closing.map { ChecklistItem(text = it, type = ChecklistType.CLOSING) }
    }
}
