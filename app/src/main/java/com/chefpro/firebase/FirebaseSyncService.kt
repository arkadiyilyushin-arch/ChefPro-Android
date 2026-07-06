package com.chefpro.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chefpro.model.ChefProState
import com.chefpro.model.Delivery
import com.chefpro.model.Dish
import com.chefpro.model.Employee
import com.chefpro.model.InventoryAuditRecord
import com.chefpro.model.InventoryItem
import com.chefpro.model.KitchenOrder
import com.chefpro.model.OperatingExpense
import com.chefpro.model.Production
import com.chefpro.model.Sale
import com.chefpro.model.Shift
import com.chefpro.model.Supplier
import com.chefpro.model.TableReservation
import com.chefpro.model.UserProfile
import com.chefpro.model.WriteOff
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.syncDataStore by preferencesDataStore(name = "chefpro_sync")

data class ChefProCloudData(
    val dishes: List<Dish> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val deliveries: List<Delivery> = emptyList(),
    val writeOffs: List<WriteOff> = emptyList(),
    val productions: List<Production> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val profile: UserProfile? = null,
    val reservations: List<TableReservation> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val kitchenOrders: List<KitchenOrder> = emptyList(),
    val closedKitchenOrders: List<KitchenOrder> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val operatingExpenses: List<OperatingExpense> = emptyList(),
    val auditRecords: List<InventoryAuditRecord> = emptyList(),
    val shiftHistory: List<Shift> = emptyList(),
    val currentShift: Shift? = null,
    val restaurantName: String? = null,
)

class FirebaseSyncService(context: Context) {

    private val appContext = context.applicationContext
    private val db: FirebaseFirestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val restaurantIdKey = stringPreferencesKey("chefpro_restaurant_id")
    private val deviceIdKey = stringPreferencesKey("chefpro_device_id")

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    fun setForceOffline(force: Boolean) {
        _isOffline.value = force
    }

    fun clearPendingSync() {
        _pendingSyncCount.value = 0
    }

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _lastSyncDate = MutableStateFlow<Long?>(null)
    val lastSyncDate: StateFlow<Long?> = _lastSyncDate.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private var cachedRestaurantId: String? = null
    private var cachedDeviceId: String? = null
    private val collectionListeners = mutableListOf<ListenerRegistration>()
    private var rootListener: ListenerRegistration? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        scope.launch {
            ensureIdsLoadedBlocking()
        }
        registerNetworkCallback()
    }

    @Volatile
    private var idsLoaded = false

    val deviceID: String
        get() {
            ensureIdsLoadedBlocking()
            return cachedDeviceId!!
        }

    val restaurantID: String
        get() {
            ensureIdsLoadedBlocking()
            return cachedRestaurantId!!
        }

    val syncCode: String
        get() = restaurantID.replace("-", "").take(8).uppercase()

    private fun ensureIdsLoadedBlocking() {
        if (idsLoaded) return
        synchronized(this) {
            if (idsLoaded) return
            kotlinx.coroutines.runBlocking {
                cachedRestaurantId = readOrCreateId(restaurantIdKey)
                cachedDeviceId = readOrCreateId(deviceIdKey)
            }
            idsLoaded = true
        }
    }

    suspend fun ensureIdsLoaded() {
        ensureIdsLoadedBlocking()
    }

    suspend fun setRestaurantID(id: String) {
        appContext.syncDataStore.edit { prefs ->
            prefs[restaurantIdKey] = id
        }
        cachedRestaurantId = id
        stopCollectionListeners()
    }

    suspend fun connectToDevice(restaurantId: String): Result<Unit> = runCatching {
        setRestaurantID(restaurantId)
        registerAsMember()
        syncFromCloud()
        Unit
    }

    suspend fun signInAnonymouslyIfNeeded() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }

    suspend fun registerAsMember() {
        ensureIdsLoaded()
        signInAnonymouslyIfNeeded()
        val uid = Firebase.auth.currentUser?.uid ?: return
        db.collection("restaurants")
            .document(restaurantID)
            .collection("members")
            .document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "deviceID" to deviceID,
                ),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun uploadAll(state: ChefProState) {
        if (_isOffline.value) {
            _pendingSyncCount.value = _pendingSyncCount.value + 1
            return
        }
        ensureIdsLoaded()
        _isSyncing.value = true
        _syncError.value = null
        try {
            signInAnonymouslyIfNeeded()
            val root = db.collection("restaurants").document(restaurantID)

            uploadCollection(state.dishes, root.collection("dishes"))
            uploadCollection(state.inventoryItems, root.collection("inventory"))
            uploadCollection(state.deliveries, root.collection("deliveries"))
            uploadCollection(state.writeOffs, root.collection("writeOffs"))
            uploadCollection(state.productions, root.collection("productions"))
            uploadCollection(state.employees, root.collection("employees"))
            uploadCollection(state.reservations, root.collection("reservations"))
            uploadCollection(state.suppliers, root.collection("suppliers"))
            uploadCollection(state.kitchenOrders, root.collection("kitchenOrders"))
            uploadCollection(state.closedKitchenOrders, root.collection("closedKitchenOrders"))
            uploadCollection(state.sales, root.collection("sales"))
            uploadCollection(state.operatingExpenses, root.collection("operatingExpenses"))
            uploadCollection(state.auditRecords, root.collection("auditRecords"))
            uploadCollection(state.shiftHistory, root.collection("shiftHistory"))

            root.collection("profile")
                .document("current")
                .set(FirestoreCodec.encode(state.profile))
                .await()

            val rootData = mutableMapOf<String, Any?>(
                "restaurantName" to state.restaurantName,
                "restaurantID" to restaurantID,
                "lastUpdatedByDevice" to deviceID,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (state.currentShift != null) {
                rootData["currentShift"] = FirestoreCodec.encode(state.currentShift)
            } else {
                rootData["currentShift"] = FieldValue.delete()
            }

            root.set(rootData, SetOptions.merge()).await()
            _lastSyncDate.value = System.currentTimeMillis()
            _pendingSyncCount.value = 0
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage ?: e.toString()
            throw e
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncFromCloud(): Result<ChefProCloudData> = runCatching {
        if (_isSyncing.value) return@runCatching ChefProCloudData()
        ensureIdsLoaded()
        _isSyncing.value = true
        _syncError.value = null
        try {
            signInAnonymouslyIfNeeded()
            val root = db.collection("restaurants").document(restaurantID)

            val dishes = downloadCollection<Dish>(root.collection("dishes"))
            val inventoryItems = downloadCollection<InventoryItem>(root.collection("inventory"))
            val deliveries = downloadCollection<Delivery>(root.collection("deliveries"))
            val writeOffs = downloadCollection<WriteOff>(root.collection("writeOffs"))
            val productions = downloadCollection<Production>(root.collection("productions"))
            val employees = downloadCollection<Employee>(root.collection("employees"))
            val reservations = downloadCollection<TableReservation>(root.collection("reservations"))
            val suppliers = downloadCollection<Supplier>(root.collection("suppliers"))
            val kitchenOrders = downloadCollection<KitchenOrder>(root.collection("kitchenOrders"))
            val closedKitchenOrders = downloadCollection<KitchenOrder>(root.collection("closedKitchenOrders"))
            val sales = downloadCollection<Sale>(root.collection("sales"))
            val operatingExpenses = downloadCollection<OperatingExpense>(root.collection("operatingExpenses"))
            val auditRecords = downloadCollection<InventoryAuditRecord>(root.collection("auditRecords"))
            val shiftHistory = downloadCollection<Shift>(root.collection("shiftHistory"))

            val profileSnap = root.collection("profile").document("current").get().await()
            val profile = profileSnap.data?.let { FirestoreCodec.decode<UserProfile>(it) }

            val rootSnap = root.get().await()
            val rootData = rootSnap.data
            val restaurantName = rootData?.get("restaurantName") as? String
            val currentShift = (rootData?.get("currentShift") as? Map<*, *>)?.let {
                @Suppress("UNCHECKED_CAST")
                FirestoreCodec.decode<Shift>(it as Map<String, Any?>)
            }

            _lastSyncDate.value = System.currentTimeMillis()
            ChefProCloudData(
                dishes = dishes,
                inventoryItems = inventoryItems,
                deliveries = deliveries,
                writeOffs = writeOffs,
                productions = productions,
                employees = employees,
                profile = profile,
                reservations = reservations,
                suppliers = suppliers,
                kitchenOrders = kitchenOrders,
                closedKitchenOrders = closedKitchenOrders,
                sales = sales,
                operatingExpenses = operatingExpenses,
                auditRecords = auditRecords,
                shiftHistory = shiftHistory,
                currentShift = currentShift,
                restaurantName = restaurantName,
            )
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage ?: e.toString()
            throw e
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun uploadKitchenOrder(order: KitchenOrder) {
        ensureIdsLoaded()
        signInAnonymouslyIfNeeded()
        db.collection("restaurants")
            .document(restaurantID)
            .collection("kitchenOrders")
            .document(order.id)
            .set(FirestoreCodec.encode(order))
            .await()
    }

    suspend fun deleteKitchenOrder(id: String) {
        ensureIdsLoaded()
        signInAnonymouslyIfNeeded()
        db.collection("restaurants")
            .document(restaurantID)
            .collection("kitchenOrders")
            .document(id)
            .delete()
            .await()
    }

    suspend fun uploadClosedKitchenOrder(order: KitchenOrder) {
        ensureIdsLoaded()
        signInAnonymouslyIfNeeded()
        db.collection("restaurants")
            .document(restaurantID)
            .collection("closedKitchenOrders")
            .document(order.id)
            .set(FirestoreCodec.encode(order))
            .await()
    }

    fun startCollectionListeners(
        onDishes: (List<Dish>) -> Unit,
        onInventory: (List<InventoryItem>) -> Unit,
        onEmployees: (List<Employee>) -> Unit,
        onKitchenOrders: (List<KitchenOrder>) -> Unit,
        onClosedKitchenOrders: (List<KitchenOrder>) -> Unit,
        onReservations: (List<TableReservation>) -> Unit,
        onSales: (List<Sale>) -> Unit,
        onOperatingExpenses: (List<OperatingExpense>) -> Unit,
        onSuppliers: (List<Supplier>) -> Unit,
        onDeliveries: (List<Delivery>) -> Unit,
        onWriteOffs: (List<WriteOff>) -> Unit,
        onProductions: (List<Production>) -> Unit,
        onShiftHistory: (List<Shift>) -> Unit,
        onAuditRecords: (List<InventoryAuditRecord>) -> Unit,
        onRootDoc: (Shift?, String?) -> Unit,
    ) {
        scope.launch {
            ensureIdsLoaded()
            stopCollectionListeners()

            val root = db.collection("restaurants").document(restaurantID)
            collectionListeners += addListener("dishes", root, onDishes)
            collectionListeners += addListener("inventory", root, onInventory)
            collectionListeners += addListener("employees", root, onEmployees)
            collectionListeners += addListener("kitchenOrders", root, onKitchenOrders)
            collectionListeners += addListener("closedKitchenOrders", root, onClosedKitchenOrders)
            collectionListeners += addListener("reservations", root, onReservations)
            collectionListeners += addListener("sales", root, onSales)
            collectionListeners += addListener("operatingExpenses", root, onOperatingExpenses)
            collectionListeners += addListener("suppliers", root, onSuppliers)
            collectionListeners += addListener("deliveries", root, onDeliveries)
            collectionListeners += addListener("writeOffs", root, onWriteOffs)
            collectionListeners += addListener("productions", root, onProductions)
            collectionListeners += addListener("shiftHistory", root, onShiftHistory)
            collectionListeners += addListener("auditRecords", root, onAuditRecords)

            rootListener = root.addSnapshotListener { snapshot, _ ->
                val data = snapshot?.data ?: return@addSnapshotListener
                if (snapshot.metadata.isFromCache) return@addSnapshotListener
                val updatedBy = data["lastUpdatedByDevice"] as? String ?: return@addSnapshotListener
                if (updatedBy == deviceID) return@addSnapshotListener
                val name = data["restaurantName"] as? String
                val shift = (data["currentShift"] as? Map<*, *>)?.let {
                    @Suppress("UNCHECKED_CAST")
                    FirestoreCodec.decode<Shift>(it as Map<String, Any?>)
                }
                onRootDoc(shift, name)
            }
        }
    }

    fun stopCollectionListeners() {
        collectionListeners.forEach { it.remove() }
        collectionListeners.clear()
        rootListener?.remove()
        rootListener = null
    }

    private inline fun <reified T> addListener(
        collectionName: String,
        root: com.google.firebase.firestore.DocumentReference,
        crossinline onUpdate: (List<T>) -> Unit,
    ): ListenerRegistration {
        return root.collection(collectionName).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || snapshot.metadata.isFromCache) return@addSnapshotListener
            val items = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { FirestoreCodec.decode<T>(it) }
            }
            onUpdate(items)
        }
    }

    private suspend inline fun <reified T> uploadCollection(
        items: List<T>,
        collection: com.google.firebase.firestore.CollectionReference,
    ) {
        if (items.isEmpty()) return
        val batch = db.batch()
        items.forEach { item ->
            val id = FirestoreCodec.documentId(item)
            val doc = collection.document(id)
            batch.set(doc, @Suppress("UNCHECKED_CAST") (FirestoreCodec.encode(item).mapValues { it.value as Any } as Map<String, Any>), SetOptions.merge())
        }
        batch.commit().await()
    }

    private suspend inline fun <reified T> downloadCollection(
        collection: com.google.firebase.firestore.CollectionReference,
    ): List<T> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { FirestoreCodec.decode<T>(it) }
        }
    }

    private suspend fun readOrCreateId(key: androidx.datastore.preferences.core.Preferences.Key<String>): String {
        val prefs = appContext.syncDataStore.data.first()
        return prefs[key] ?: UUID.randomUUID().toString().also { newId ->
            appContext.syncDataStore.edit { it[key] = newId }
        }
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOffline.value = false
                }

                override fun onLost(network: Network) {
                    _isOffline.value = true
                }
            },
        )
        _isOffline.value = !isNetworkAvailable(connectivityManager)
    }

    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

private object FirestoreCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    inline fun <reified T> encode(value: T): Map<String, Any?> {
        val jsonString = json.encodeToString(value)
        return jsonStringToMap(jsonString)
    }

    inline fun <reified T> decode(map: Map<String, Any?>): T? {
        return runCatching {
            val normalized = normalizeFirestoreMap(map)
            val jsonString = JSONObject(normalized).toString()
            json.decodeFromString<T>(jsonString)
        }.getOrNull()
    }

    inline fun <reified T> documentId(item: T): String {
        val map = encode(item)
        return map["id"] as? String ?: UUID.randomUUID().toString()
    }

    private fun normalizeFirestoreMap(map: Map<String, Any?>): Map<String, Any?> {
        return map.mapValues { (_, value) -> normalizeValue(value) }
    }

    private fun normalizeValue(value: Any?): Any? = when (value) {
        null -> null
        is com.google.firebase.Timestamp -> value.toDate().time
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            normalizeFirestoreMap(value as Map<String, Any?>)
        }
        is List<*> -> value.map { normalizeValue(it) }
        else -> value
    }

    private fun jsonStringToMap(jsonString: String): Map<String, Any?> {
        return jsonObjectToMap(JSONObject(jsonString))
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        obj.keys().forEach { key ->
            map[key] = jsonValueToNative(obj.get(key))
        }
        return map
    }

    private fun jsonValueToNative(value: Any?): Any? = when (value) {
        JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> buildList {
            for (i in 0 until value.length()) {
                add(jsonValueToNative(value.get(i)))
            }
        }
        else -> value
    }
}
