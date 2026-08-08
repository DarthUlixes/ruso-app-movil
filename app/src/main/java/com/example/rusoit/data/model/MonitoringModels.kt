package com.example.rusoit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.Locale

@Serializable
data class StatusCount(
    val status: String? = null,
    val name: String? = null,
    @SerialName("_count")
    val countRaw: JsonElement? = null,
    val count: Int? = null,
    val total: Int? = null,
    val quantity: Int? = null
) {
    fun resolvedCount(): Int {
        val fromRaw = FlexibleCountSerializer.parseCount(countRaw)
        if (fromRaw > 0) return fromRaw
        return count ?: total ?: quantity ?: fromRaw
    }
}

@Serializable
data class TypeServiceCount(
    val id_type_service: Int? = null,
    val id_type: Int? = null,
    val id: Int? = null,
    @SerialName("_count")
    val countRaw: JsonElement? = null,
    val count: Int? = null,
    val total: Int? = null,
    val quantity: Int? = null
) {
    fun typeId(): Int? = id_type_service ?: id_type ?: id
    fun resolvedCount(): Int {
        val fromRaw = FlexibleCountSerializer.parseCount(countRaw)
        if (fromRaw > 0) return fromRaw
        return count ?: total ?: quantity ?: fromRaw
    }
}

@Serializable
data class TypeService(
    val id: Int? = null,
    val name: String? = null
)

@Serializable
data class BaseStation(
    val id: Int? = null,
    val name: String? = null,
    val status: String? = null
)

data class LabeledCount(
    val label: String,
    val value: Int
)

data class DataStudioKpis(
    val totalIncidents: Int = 0,
    val fleetOperativePct: Int = 0,
    val fleetOperative: Int = 0,
    val fleetTotal: Int = 0,
    val basesActive: Int = 0,
    val basesTotal: Int = 0,
    val shiftsTotal: Int = 0
)

data class FleetBreakdown(
    val operative: Int = 0,
    val workshop: Int = 0,
    val inoperative: Int = 0,
    val total: Int = 0
)

data class DataStudioAvailability(
    val services: Boolean = true,
    val status: Boolean = true,
    val fleet: Boolean = true,
    val bases: Boolean = true,
    val shifts: Boolean = true,
    val colonias: Boolean = true
)

data class DataStudioData(
    val kpis: DataStudioKpis = DataStudioKpis(),
    val fleet: FleetBreakdown = FleetBreakdown(),
    val incidentsByType: List<LabeledCount> = emptyList(),
    val incidentsByStatus: List<LabeledCount> = emptyList(),
    /** Censo web: POST /folio/cologne-group-by-date-status/{status} */
    val incidentsByCologne: List<LabeledCount> = emptyList(),
    val available: DataStudioAvailability = DataStudioAvailability(),
    val dateFrom: String = "",
    val dateTo: String = ""
)

@Serializable
data class CologneCount(
    val id_cologne: Int? = null,
    val id: Int? = null,
    @SerialName("_count")
    val countRaw: JsonElement? = null,
    val count: Int? = null,
    val total: Int? = null,
    val quantity: Int? = null
) {
    fun cologneId(): Int? = id_cologne ?: id
    fun resolvedCount(): Int {
        val fromRaw = FlexibleCountSerializer.parseCount(countRaw)
        if (fromRaw > 0) return fromRaw
        return count ?: total ?: quantity ?: fromRaw
    }
}

@Serializable
data class WorkShift(
    val id: Int? = null,
    val name: String? = null,
    val workday: String? = null,
    val time_start: String? = null,
    val working_time: String? = null,
    val operative_personal: Int? = null,
    val in_operative_personal: Int? = null
)

@Serializable
data class CalendarEvent(
    val id: Int? = null,
    val name: String? = null,
    val title: String? = null,
    val content: String? = null,
    val tipe_to_event: String? = null,
    val date_to_start: String? = null,
    val date_to_conclution: String? = null,
    val start: String? = null,
    val end: String? = null,
    val estatus: String? = null,
    val is_autorized: String? = null,
    val who_autorisated: String? = null,
    val notes: String? = null
) {
    fun resolvedStart(): String =
        (date_to_start ?: start)?.take(10).orEmpty()

    fun resolvedEnd(): String =
        (date_to_conclution ?: end)?.take(10)?.ifBlank { null } ?: resolvedStart()

    fun resolvedTitle(): String =
        name ?: title ?: "Evento"

    fun resolvedContent(): String =
        notes ?: content ?: tipe_to_event ?: "Sin detalle"
}

/** Evento unificado para agenda TV (calendario de Inicio). */
data class AgendaCalendarEvent(
    val id: String,
    val title: String,
    val content: String = "",
    val start: String,
    val end: String = start,
    val eventClass: String = "green-event",
    val tipe_to_event: String? = null,
    val estatus: String? = null,
    val who_autorisated: String? = null,
    val notes: String? = null,
    val ubication: String? = null,
    val source: String = "agenda"
)

@Serializable
data class SCIInformation(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val ubication: String? = null,
    val status: String? = null,
    val date_to_start: String? = null,
    val time_to_start: String? = null,
    val id_disturbing_phenomen: Int? = null
) {
    fun isActive(): Boolean {
        val s = status?.trim()?.lowercase().orEmpty()
        return s == "active" || s == "activo" || s.contains("activ")
    }

    /** Etiqueta de estado para UI TV (siempre en español). */
    fun statusLabelEs(): String = when {
        isActive() -> "ACTIVO"
        status.isNullOrBlank() -> "CERRADO"
        else -> status.trim().uppercase(Locale.getDefault())
            .replace("ACTIVE", "ACTIVO")
            .replace("INACTIVE", "INACTIVO")
            .replace("CLOSED", "CERRADO")
            .replace("CLOSE", "CERRADO")
    }

    fun parseLatLng(): Pair<Double, Double>? {
        val raw = ubication?.trim().orEmpty()
        if (raw.isEmpty()) return null

        // Formatos: "lat,lng" | "lat, lng" | "lat;lng" | con texto alrededor
        val match = Regex("""(-?\d+(?:\.\d+)?)\s*[,;]\s*(-?\d+(?:\.\d+)?)""")
            .find(raw)
        val lat = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val lng = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            return lat to lng
        }

        // Si viniera invertido (lng,lat) y el primero está fuera de latitud
        if (lat != null && lng != null &&
            lng in -90.0..90.0 && lat in -180.0..180.0 && lat !in -90.0..90.0
        ) {
            return lng to lat
        }
        return null
    }
}

@Serializable
data class User(
    val id: Int,
    val first_name: String? = null,
    val second_name: String? = null,
    val last_name: String? = null,
    val second_last_name: String? = null,
    val status_now: String? = null,
    val type_user: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val profile_image_url: String? = null,
    val cover_image: String? = null,
    val employees: EmployeeInfo? = null
) {
    fun fullName(): String = listOf(first_name, second_name, last_name, second_last_name)
        .filter { !it.isNullOrBlank() }
        .joinToString(" ")
        .ifBlank { "Sin nombre" }

    fun imageUrl(): String? =
        profile_image_url?.takeIf { it.isNotBlank() }
            ?: cover_image?.takeIf { it.isNotBlank() }
            ?: employees?.profile_image_url?.takeIf { it.isNotBlank() }
            ?: employees?.cover_image?.takeIf { it.isNotBlank() }

    fun guardiaName(): String? =
        employees?.work_shift?.name?.trim()?.takeIf { it.isNotEmpty() }

    fun isEmployeeType(): Boolean =
        type_user.equals("empleado", ignoreCase = true) || employees != null

    fun isVolunteerType(): Boolean =
        type_user.equals("voluntario", ignoreCase = true)
}

@Serializable
data class EmployeeInfo(
    val id: Int? = null,
    val id_user: Int? = null,
    val id_work_shift: Int? = null,
    val employee_number: String? = null,
    val position: String? = null,
    val type_position: String? = null,
    val work_shift: WorkShift? = null,
    val profile_image_url: String? = null,
    val cover_image: String? = null
)

@Serializable
data class ToolType(
    val id: Int? = null,
    val name: String? = null,
    val cover_image: String? = null,
    val on_inventory_active: Int? = null,
    val on_inventory_in_active: Int? = null,
    val type: String? = null
)

/** Tipo con conteos reales calculados desde GET /tools (el API a veces manda 0 en on_inventory_*). */
data class ToolTypeInventory(
    val type: ToolType,
    val activeCount: Int,
    val inactiveCount: Int,
    val tools: List<Tool> = emptyList()
)

@Serializable
data class Tool(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val cover_image: String? = null,
    val id_type: Int? = null
) {
    fun isActive(): Boolean =
        status?.equals("active", ignoreCase = true) == true
}

@Serializable
data class Folio(
    val id: Int,
    val folio: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val stret: String? = null,
    val crossing: String? = null,
    val date_to_open: String? = null,
    val date_to_close: String? = null,
    val time_to_open: String? = null,
    val time_to_close: String? = null,
    val kilometers: Int? = null,
    val close_kilometers: Int? = null,
    val kilometers_traveled: Int? = null,
    val reporter: String? = null,
    val phone_reporter: String? = null,
    val close_type: String? = null,
    val general_and_conclusion: String? = null,
    val id_type_service: Int? = null,
    val id_cologne: Int? = null,
    val vehicle_id: Int? = null,
    val personal_on_a_service: List<ServiceCrew>? = null
)

@Serializable
data class Cologne(
    val id: Int? = null,
    val name: String? = null
)

@Serializable
data class ServiceCrew(
    val id: Int,
    val position_on_service: String? = null,
    val id_user: Int? = null,
    val users: User? = null
)

@Serializable
data class SearchGroupByDateRequest(
    val date_from: String? = null,
    val date_to: String? = null
)

/** Catálogos para consulta TV de Partes de Atención. */
data class PartesCatalog(
    val folios: List<Folio> = emptyList(),
    val typeServices: List<TypeService> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val colognes: List<Cologne> = emptyList(),
    val users: List<User> = emptyList()
)
