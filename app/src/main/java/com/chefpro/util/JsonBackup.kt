package com.chefpro.util

import com.chefpro.data.ChefProRepository
import com.chefpro.model.AppBackup
import com.chefpro.model.ChefProState
import kotlinx.serialization.json.Json

object JsonBackup {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(repository: ChefProRepository): String = repository.exportBackup()

    fun import(repository: ChefProRepository, jsonString: String): ChefProState =
        repository.importBackup(jsonString)

    fun validate(jsonString: String): Boolean =
        runCatching { json.decodeFromString<AppBackup>(jsonString) }.isSuccess
}
