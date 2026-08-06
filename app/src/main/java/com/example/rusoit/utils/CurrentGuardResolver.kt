package com.example.rusoit.utils

import com.example.rusoit.data.model.WorkShift
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object CurrentGuardResolver {

    /**
     * Resuelve la guardia activa según [WorkShift.time_start] + [WorkShift.working_time].
     * Prefiere turnos operativos si hay varios solapados.
     */
    fun resolve(shifts: List<WorkShift>, nowMillis: Long = System.currentTimeMillis()): WorkShift? {
        if (shifts.isEmpty()) return null

        val nowMinutes = minutesOfDay(nowMillis)
        val active = shifts.filter { it.isActiveAt(nowMinutes) }

        if (active.isNotEmpty()) {
            return active.firstOrNull {
                it.workday?.equals("operativo", ignoreCase = true) == true
            } ?: active.first()
        }

        // Fallback: el turno operativo con nombre, o el primero
        return shifts.firstOrNull {
            it.workday?.equals("operativo", ignoreCase = true) == true && !it.name.isNullOrBlank()
        } ?: shifts.firstOrNull { !it.name.isNullOrBlank() }
    }

    fun displayLabel(shift: WorkShift?): String {
        val name = shift?.name?.trim().orEmpty()
        return if (name.isNotEmpty()) name.uppercase(Locale.getDefault()) else "SIN GUARDIA"
    }

    private fun WorkShift.isActiveAt(nowMinutes: Int): Boolean {
        val start = parseTimeToMinutes(time_start) ?: return false
        val duration = parseTimeToMinutes(working_time) ?: return false
        if (duration <= 0) return false
        val end = (start + duration) % (24 * 60)
        return if (start < end) {
            nowMinutes in start until end
        } else {
            // Cruza medianoche
            nowMinutes >= start || nowMinutes < end
        }
    }

    private fun minutesOfDay(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /** Acepta "HH:mm:ss", "HH:mm" o minutos como número en string. */
    private fun parseTimeToMinutes(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val t = raw.trim()
        if (t.contains(':')) {
            val parts = t.split(':')
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        return t.toIntOrNull()?.let { minutes ->
            // Si viniera en segundos (poco probable)
            if (minutes > 24 * 60) TimeUnit.SECONDS.toMinutes(minutes.toLong()).toInt()
            else minutes
        }
    }
}
