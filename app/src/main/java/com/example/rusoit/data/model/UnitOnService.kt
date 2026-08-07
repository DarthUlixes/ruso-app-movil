package com.example.rusoit.data.model

import kotlinx.serialization.Serializable

/**
 * Respuesta de GET /work-force/on-service-by-date-and-workshift
 * Unidades del estado de fuerza del día operativo + servicio activo.
 */
@Serializable
data class UnitOnService(
    val number_unit: Int? = null,
    val label: String? = null,
    /** "en curso" | "libre" */
    val status: String? = null,
    val vehicle: UnitOnServiceVehicle? = null,
    val work_force: UnitOnServiceWorkForce? = null,
    val service: UnitOnServiceActive? = null
) {
    fun isOnService(): Boolean =
        status?.equals("en curso", ignoreCase = true) == true || service != null

    fun displayUnit(): String =
        number_unit?.let { "U-$it" } ?: "U-S/N"

    fun hubName(): String =
        work_force?.logistics_hubs?.name ?: "Sin base"

    fun serviceTypeName(): String =
        service?.type_service?.name ?: "—"

    fun crewCount(): Int =
        work_force?.personal?.size ?: 0
}

@Serializable
data class UnitOnServiceVehicle(
    val id: Int? = null,
    val number_unit: Int? = null,
    val status: String? = null,
    val cover_img: String? = null,
    val model: String? = null,
    val card_brand: String? = null,
    val car_brand: String? = null,
    val kilometers: Float? = null,
    val vehicle_license_plates: String? = null,
    val id_type: Int? = null
) {
    fun brandName(): String? = car_brand ?: card_brand
}

@Serializable
data class UnitOnServiceWorkForce(
    val id: Int? = null,
    val id_work_shift: Int? = null,
    val id_logistics_hubs: Int? = null,
    val date_work_shift: String? = null,
    val notes: String? = null,
    val logistics_hubs: UnitOnServiceHub? = null,
    val personal: List<UnitOnServicePersonal> = emptyList()
)

@Serializable
data class UnitOnServiceHub(
    val id: Int? = null,
    val name: String? = null,
    val type_hub: String? = null
)

@Serializable
data class UnitOnServicePersonal(
    val id: Int? = null,
    val id_work_force: Int? = null,
    val id_user: Int? = null,
    val position_on_work_force: String? = null
)

@Serializable
data class UnitOnServiceActive(
    val id: Int? = null,
    val folio: String? = null,
    val status: String? = null,
    val id_type_service: Int? = null,
    val type_service: TypeService? = null,
    val id_cologne: Int? = null,
    val stret: String? = null,
    val crossing: String? = null,
    val reporter: String? = null,
    val phone_reporter: String? = null,
    val date_to_open: String? = null,
    val time_to_open: String? = null
) {
    /** Folio textual o id numérico del servicio. */
    fun displayFolio(): String {
        val f = folio?.trim().orEmpty()
        if (f.isNotEmpty()) return f
        return id?.let { "#$it" } ?: "—"
    }

    fun displayDate(): String {
        val raw = date_to_open?.trim().orEmpty()
        if (raw.isEmpty()) return "—"
        // ISO: 2026-08-06T00:00:00.000Z → 2026-08-06
        return raw.take(10)
    }

    fun displayTime(): String {
        val raw = time_to_open?.trim().orEmpty()
        if (raw.isEmpty()) return "—"
        // ISO con T: ...T14:30:00... → 14:30:00
        val tIdx = raw.indexOf('T')
        if (tIdx >= 0 && tIdx + 1 < raw.length) {
            return raw.substring(tIdx + 1).take(8)
        }
        return raw.take(8)
    }

    fun displayOrDash(value: String?): String {
        val v = value?.trim().orEmpty()
        return if (v.isEmpty()) "Sin dato" else v
    }
}
