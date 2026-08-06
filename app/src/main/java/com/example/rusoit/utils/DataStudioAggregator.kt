package com.example.rusoit.utils

import com.example.rusoit.data.model.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DataStudioAggregator {

    private val localeEs = Locale("es", "MX")
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Semana lunes–domingo. weekOffset 0 = semana actual. */
    fun weekRange(weekOffset: Int): Pair<String, String> {
        val cal = Calendar.getInstance()
        // Lunes = primer día de la semana operativa
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Dom ... 7=Sab
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> dayOfWeek - Calendar.MONDAY
        }
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        clearTime(cal)
        val monday = cal.time
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val sunday = cal.time
        return isoFmt.format(monday) to isoFmt.format(sunday)
    }

    fun periodLabel(dateFrom: String, dateTo: String): String {
        val from = formatSpanishDate(dateFrom)
        val to = formatSpanishDate(dateTo)
        if (from.isEmpty() && to.isEmpty()) return ""
        if (from.isNotEmpty() && to.isNotEmpty() && dateFrom == dateTo) return from
        if (from.isNotEmpty() && to.isNotEmpty()) return "Del $from al $to"
        return from.ifEmpty { to }
    }

    fun periodShort(dateFrom: String, dateTo: String): String {
        val from = formatSpanishDate(dateFrom)
        val to = formatSpanishDate(dateTo)
        if (from.isNotEmpty() && to.isNotEmpty() && dateFrom == dateTo) return from
        if (from.isNotEmpty() && to.isNotEmpty()) return "$from al $to"
        return from.ifEmpty { to }.ifEmpty { "periodo seleccionado" }
    }

    fun weekTitle(weekOffset: Int): String = when (weekOffset) {
        0 -> "Semana actual"
        -1 -> "Semana anterior"
        1 -> "Semana siguiente"
        else -> if (weekOffset < 0) "Hace ${-weekOffset} semanas" else "En $weekOffset semanas"
    }

    fun formatSpanishDate(iso: String): String {
        return try {
            val date = isoFmt.parse(iso) ?: return ""
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, localeEs) ?: ""
            "$day de $monthName de ${cal.get(Calendar.YEAR)}"
        } catch (_: Exception) {
            ""
        }
    }

    fun build(
        servicesByType: List<TypeServiceCount>?,
        servicesByStatus: List<StatusCount>?,
        servicesByCologne: List<CologneCount>?,
        typeServices: List<TypeService>?,
        colognes: List<Cologne>?,
        vehicles: List<Vehicle>?,
        bases: List<BaseStation>?,
        shifts: List<WorkShift>?,
        dateFrom: String,
        dateTo: String
    ): DataStudioData {
        val typeNameOf = buildTypeNameLookup(typeServices)
        val cologneNameOf = buildCologneNameLookup(colognes)

        val incidentsByType = (servicesByType ?: emptyList())
            .map { row ->
                LabeledCount(
                    label = typeNameOf(row.typeId()).uppercase(Locale.getDefault()),
                    value = row.resolvedCount()
                )
            }
            .filter { it.value > 0 }
            .sortedByDescending { it.value }

        val totalIncidents = incidentsByType.sumOf { it.value }

        val incidentsByStatus = (servicesByStatus ?: emptyList())
            .map { row ->
                LabeledCount(
                    label = (row.status ?: row.name ?: "Sin estatus").uppercase(Locale.getDefault()),
                    value = row.resolvedCount()
                )
            }
            .filter { it.value > 0 }
            .sortedByDescending { it.value }

        val incidentsByCologne = (servicesByCologne ?: emptyList())
            .map { row ->
                LabeledCount(
                    label = cologneNameOf(row.cologneId()).uppercase(Locale.getDefault()),
                    value = row.resolvedCount()
                )
            }
            .filter { it.value > 0 }
            .sortedByDescending { it.value }

        val vehicleList = vehicles ?: emptyList()
        val operative = vehicleList.count { normalizeStatus(it.status) in OPERATIVE }
        val workshop = vehicleList.count { normalizeStatus(it.status) in WORKSHOP }
        val totalVehicles = vehicleList.size
        val inoperative = (totalVehicles - operative - workshop).coerceAtLeast(0)

        val baseList = bases ?: emptyList()
        val basesActiveCounted = baseList.count { normalizeStatus(it.status) in ACTIVE_BASE }
        val basesActive = if (basesActiveCounted > 0) basesActiveCounted else baseList.size

        val shiftList = shifts ?: emptyList()

        return DataStudioData(
            kpis = DataStudioKpis(
                totalIncidents = totalIncidents,
                fleetOperativePct = if (totalVehicles > 0) {
                    Math.round((operative.toFloat() / totalVehicles) * 100f)
                } else {
                    0
                },
                fleetOperative = operative,
                fleetTotal = totalVehicles,
                basesActive = basesActive,
                basesTotal = baseList.size,
                shiftsTotal = shiftList.size
            ),
            fleet = FleetBreakdown(
                operative = operative,
                workshop = workshop,
                inoperative = inoperative,
                total = totalVehicles
            ),
            incidentsByType = incidentsByType,
            incidentsByStatus = incidentsByStatus,
            incidentsByCologne = incidentsByCologne,
            available = DataStudioAvailability(
                services = servicesByType != null,
                status = servicesByStatus != null,
                fleet = vehicles != null,
                bases = bases != null,
                shifts = shifts != null,
                colonias = servicesByCologne != null
            ),
            dateFrom = dateFrom,
            dateTo = dateTo
        )
    }

    private val OPERATIVE = setOf("operativa", "activo", "activa")
    private val WORKSHOP = setOf("taller", "en taller", "mantenimiento")
    private val ACTIVE_BASE = setOf("activa", "activo", "operativa")

    private fun clearTime(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun normalizeStatus(status: String?): String =
        status?.lowercase(Locale.getDefault())?.trim().orEmpty()

    private fun buildTypeNameLookup(typeServices: List<TypeService>?): (Int?) -> String {
        val map = (typeServices ?: emptyList())
            .filter { it.id != null }
            .associate { it.id!! to (it.name ?: "Tipo ${it.id}") }
        return { id ->
            if (id == null) "Sin tipo"
            else map[id] ?: "Sin tipo"
        }
    }

    private fun buildCologneNameLookup(colognes: List<Cologne>?): (Int?) -> String {
        val map = (colognes ?: emptyList())
            .filter { it.id != null }
            .associate { it.id!! to (it.name ?: "Colonia ${it.id}") }
        return { id ->
            if (id == null) "Sin colonia"
            else map[id] ?: "Colonia $id"
        }
    }
}
