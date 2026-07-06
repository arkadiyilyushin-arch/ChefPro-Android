package com.chefpro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chefpro.model.ChefProState
import com.chefpro.model.ModelHelpers.isLowStock
import com.chefpro.model.TableReservation
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    private val appContext = context.applicationContext

    init {
        createChannels()
    }

    fun scheduleAll(state: ChefProState) {
        if (!state.notificationsEnabled) {
            cancelAllScheduled()
            return
        }
        scheduleLowStockNotifications(state)
        scheduleExpiryNotifications(state)
        scheduleDailyDigest(state)
        scheduleHaccpReminders(state)
    }

    fun scheduleReservationReminder(reservation: TableReservation) {
        val fireAt = reservation.date - TimeUnit.HOURS.toMillis(2)
        if (fireAt <= System.currentTimeMillis()) return
        showNotification(
            id = reservation.id.hashCode(),
            title = "Бронь через 2 часа",
            body = "${reservation.guestName} · Стол ${reservation.tableNumber} · ${reservation.persons} гост.",
            tag = "reservation-${reservation.id}",
        )
    }

    fun cancelReservationReminder(reservationId: String) {
        NotificationManagerCompat.from(appContext).cancel("reservation-$reservationId", reservationId.hashCode())
    }

    fun notifyKitchenOrder(dishName: String, portions: Int, tableNumber: String) {
        val tableInfo = if (tableNumber.isBlank()) "" else " — стол $tableNumber"
        showNotification(
            id = (dishName + tableNumber + System.currentTimeMillis()).hashCode(),
            title = "Новый заказ",
            body = "$dishName × $portions$tableInfo",
            tag = "kitchen-order",
        )
    }

    private fun scheduleLowStockNotifications(state: ChefProState) {
        state.inventoryItems
            .filter { it.isLowStock() }
            .forEach { item ->
                showNotification(
                    id = item.id.hashCode(),
                    title = "Нужно заказать: ${item.name}",
                    body = "Осталось ${formatQty(item.quantity)} ${item.unit}, минимум ${formatQty(item.minQuantity)} ${item.unit}",
                    tag = "lowstock-${item.id}",
                )
            }
    }

    private fun scheduleExpiryNotifications(state: ChefProState) {
        val now = System.currentTimeMillis()
        val warningLimit = now + TimeUnit.DAYS.toMillis(state.expiryWarningDays.toLong())
        state.inventoryItems.forEach { item ->
            val expiry = item.expiryDate ?: return@forEach
            if (expiry <= now || expiry > warningLimit) return@forEach
            val days = TimeUnit.MILLISECONDS.toDays(expiry - now).toInt().coerceAtLeast(0)
            val body = if (days == 0) "Истекает сегодня!" else "Истекает через $days дн."
            showNotification(
                id = ("expiry-${item.id}").hashCode(),
                title = "Срок годности: ${item.name}",
                body = body,
                tag = "expiry-${item.id}",
            )
        }
    }

    private fun scheduleDailyDigest(state: ChefProState) {
        val workManager = WorkManager.getInstance(appContext)
        workManager.cancelUniqueWork(WORK_DAILY_DIGEST)
        if (!state.dailyDigestEnabled) return

        val initialDelay = millisUntilHour(8, 0)
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(WORK_DAILY_DIGEST)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_DIGEST,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun scheduleHaccpReminders(state: ChefProState) {
        val workManager = WorkManager.getInstance(appContext)
        workManager.cancelUniqueWork(WORK_HACCP)
        if (!state.haccpRemindersEnabled) return

        val intervalHours = state.haccpIntervalHours.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<HaccpReminderWorker>(
            intervalHours.toLong(),
            TimeUnit.HOURS,
        )
            .addTag(WORK_HACCP)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_HACCP,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancelAllScheduled() {
        WorkManager.getInstance(appContext).apply {
            cancelUniqueWork(WORK_DAILY_DIGEST)
            cancelUniqueWork(WORK_HACCP)
        }
        NotificationManagerCompat.from(appContext).cancelAll()
    }

    private fun showNotification(id: Int, title: String, body: String, tag: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(appContext).notify(tag, id, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ALERTS,
            "ChefPro уведомления",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Низкий остаток, срок годности, HACCP, брони"
        }
        manager.createNotificationChannel(channel)
    }

    private fun millisUntilHour(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

    companion object {
        const val CHANNEL_ALERTS = "chefpro_alerts"
        const val WORK_DAILY_DIGEST = "chefpro_daily_digest"
        const val WORK_HACCP = "chefpro_haccp"
        const val NOTIFY_DAILY_DIGEST = 1001
        const val NOTIFY_HACCP = 1002
    }
}
