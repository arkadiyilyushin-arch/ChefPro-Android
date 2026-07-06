package com.chefpro.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chefpro.ChefProApplication
import com.chefpro.domain.ChefProEngine
import com.chefpro.model.ModelHelpers.isLowStock

class DailyDigestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ChefProApplication ?: return Result.success()
        val state = app.repository.loadState()
        if (!state.notificationsEnabled || !state.dailyDigestEnabled) return Result.success()

        val parts = buildList {
            val lowStock = state.inventoryItems.count { it.isLowStock() }
            if (lowStock > 0) add("⚠ Заканчивается: $lowStock поз.")
            val expiring = ChefProEngine.expiringItems(state).size
            if (expiring > 0) add("📅 Срок годности: $expiring поз.")
            if (isEmpty()) add("✅ Всё в порядке")
        }

        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.CHANNEL_ALERTS,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Дайджест — ${state.restaurantName}")
            .setContentText(parts.joinToString(" · "))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        androidx.core.app.NotificationManagerCompat.from(applicationContext)
            .notify(NotificationHelper.NOTIFY_DAILY_DIGEST, notification)
        return Result.success()
    }
}

class HaccpReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ChefProApplication ?: return Result.success()
        val state = app.repository.loadState()
        if (!state.notificationsEnabled || !state.haccpRemindersEnabled) return Result.success()

        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.CHANNEL_ALERTS,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HACCP: Запишите температуру")
            .setContentText("Время зафиксировать температуру холодильников и морозильников")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        androidx.core.app.NotificationManagerCompat.from(applicationContext)
            .notify(NotificationHelper.NOTIFY_HACCP, notification)
        return Result.success()
    }
}
