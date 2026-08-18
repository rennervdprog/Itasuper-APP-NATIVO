package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Um pré-pedido só é aceito na janela configurada antes da próxima abertura do mesmo dia.
 * A decisão usa America/Sao_Paulo para não depender do fuso configurado no aparelho.
 */
fun Store.preorderReleaseAtMillis(nowMillis: Long = System.currentTimeMillis()): Long? {
    if (isOpen || forceClosed || !preorderEnabled || preorderMinutesBefore <= 0) return null

    val zone = TimeZone.getTimeZone("America/Sao_Paulo")
    val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
    val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1 // Calendar: domingo=1; banco/web: domingo=0.
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val today = openingHours.firstOrNull { it.dayOfWeek == currentDay && !it.isClosedAllDay } ?: return null
    val timeParts = today.openTime.split(":")
    val openingHour = timeParts.getOrNull(0)?.toIntOrNull() ?: return null
    val openingMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: return null
    val openingMinutes = openingHour * 60 + openingMinute

    if (nowMinutes >= openingMinutes || nowMinutes < openingMinutes - preorderMinutesBefore) return null

    return Calendar.getInstance(zone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, openingHour)
        set(Calendar.MINUTE, openingMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
