package com.chefpro.data

import android.content.Context
import com.chefpro.model.AppBackup
import com.chefpro.model.ChefProState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ChefProRepository(context: Context) {

    private val appContext = context.applicationContext
    private val stateFile = File(appContext.filesDir, STATE_FILE_NAME)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun loadState(): ChefProState {
        if (!stateFile.exists()) {
            return ChefProState()
        }
        return runCatching {
            json.decodeFromString<ChefProState>(stateFile.readText())
        }.getOrElse { ChefProState() }
    }

    fun saveState(state: ChefProState) {
        stateFile.parentFile?.mkdirs()
        stateFile.writeText(json.encodeToString(state))
    }

    fun exportBackup(): String {
        val state = loadState()
        val backup = state.toAppBackup()
        return json.encodeToString(backup)
    }

    fun importBackup(jsonString: String): ChefProState {
        val backup = json.decodeFromString<AppBackup>(jsonString)
        val merged = backup.toChefProState(loadState())
        saveState(merged)
        return merged
    }

    private fun ChefProState.toAppBackup(): AppBackup {
        return AppBackup(
            restaurantName = restaurantName,
            dishes = dishes,
            inventoryItems = inventoryItems,
            deliveries = deliveries,
            writeOffs = writeOffs,
            productions = productions,
            employees = employees,
            suppliers = suppliers,
            sales = sales,
            checklists = checklists,
            menuCollections = menuCollections,
            workSchedule = workSchedule,
            temperatureLogs = temperatureLogs,
            shiftHistory = shiftHistory,
            stockMovements = stockMovements,
        )
    }

    private fun AppBackup.toChefProState(existing: ChefProState): ChefProState {
        return existing.copy(
            restaurantName = restaurantName,
            dishes = dishes,
            inventoryItems = inventoryItems,
            deliveries = deliveries,
            writeOffs = writeOffs,
            productions = productions,
            employees = employees,
            suppliers = suppliers,
            sales = sales,
            checklists = checklists,
            menuCollections = menuCollections,
            workSchedule = workSchedule,
            temperatureLogs = temperatureLogs,
            shiftHistory = shiftHistory,
            stockMovements = stockMovements,
        )
    }

    companion object {
        private const val STATE_FILE_NAME = "chefpro_state.json"
    }
}
