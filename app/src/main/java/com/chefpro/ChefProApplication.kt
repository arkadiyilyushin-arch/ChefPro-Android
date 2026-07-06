package com.chefpro

import android.app.Application
import com.chefpro.data.ChefProRepository
import com.chefpro.data.PhotoStorage
import com.chefpro.firebase.FirebaseSyncService
import com.chefpro.notifications.NotificationHelper

class ChefProApplication : Application() {

    lateinit var repository: ChefProRepository
        private set

    lateinit var syncService: FirebaseSyncService
        private set

    lateinit var photoStorage: PhotoStorage
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ChefProRepository(this)
        syncService = FirebaseSyncService(this)
        photoStorage = PhotoStorage(this)
        notificationHelper = NotificationHelper(this)
    }

    companion object {
        fun get(application: Application): ChefProApplication =
            application as ChefProApplication
    }
}
