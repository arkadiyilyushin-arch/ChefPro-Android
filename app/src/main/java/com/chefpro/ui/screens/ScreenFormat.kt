package com.chefpro.ui.screens

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ruLocale = Locale("ru", "RU")
private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", ruLocale)
private val dateFormat = SimpleDateFormat("d MMM yyyy", ruLocale)
private val timeFormat = SimpleDateFormat("HH:mm", ruLocale)

fun formatMoney(value: Double, currency: String = "₽"): String {
    return if (value % 1.0 == 0.0) "${value.toLong()} $currency" else String.format(ruLocale, "%.2f %s", value, currency)
}

fun formatPercent(value: Double): String = String.format(ruLocale, "%.1f%%", value)

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

fun parsePositiveDouble(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    return normalized.toDoubleOrNull()?.takeIf { it >= 0 }
}

fun parsePositiveInt(text: String): Int? {
    val normalized = text.trim()
    if (normalized.isEmpty()) return null
    return normalized.toIntOrNull()?.takeIf { it >= 0 }
}

fun isSameDay(millis: Long, reference: Long = System.currentTimeMillis()): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance().apply { timeInMillis = reference }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun startOfMonth(millis: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun enumLabelRu(name: String): String = when (name) {
    "RENT" -> "Аренда"
    "SALARY" -> "Зарплата"
    "UTILITIES" -> "Коммунальные"
    "MARKETING" -> "Маркетинг"
    "EQUIPMENT" -> "Оборудование"
    "OTHER" -> "Прочее"
    "ONCE" -> "Разовый"
    "MONTHLY" -> "Ежемесячно"
    "WEEKLY" -> "Еженедельно"
    "YEARLY" -> "Ежегодно"
    "CONFIRMED" -> "Подтверждено"
    "ARRIVED" -> "Пришли"
    "CANCELLED" -> "Отменено"
    "NO_SHOW" -> "Не пришли"
    "NEW" -> "Новые"
    "COOKING" -> "Готовится"
    "READY" -> "Готово"
    "OPENING" -> "Открытие"
    "CLOSING" -> "Закрытие"
    "SYSTEM" -> "Системная"
    "LIGHT" -> "Светлая"
    "DARK" -> "Тёмная"
    "RUSSIAN" -> "Русский"
    "ENGLISH" -> "English"
    "IIKO" -> "iiko"
    "POSTER" -> "Poster"
    "RKEEPER" -> "r_keeper"
    "TILLYPAD" -> "Tillypad"
    "MANUAL" -> "Ручной ввод"
    else -> name
}
