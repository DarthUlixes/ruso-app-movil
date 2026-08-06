package com.example.rusoit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Int? = null,
    val number_unit: Int? = null,
    val id_type: Int? = null,
    val cover_img: String? = null,
    val front_img: String? = null,
    val back_img: String? = null,
    val left_img: String? = null,
    val up_img: String? = null,
    val status: String? = null,
    val model: String? = null,
    /** API real suele mandar car_brand; se mantiene card_brand por compat. */
    val car_brand: String? = null,
    val card_brand: String? = null,
    val vehicle_license_plates: String? = null,
    val kilometers: Float? = null,
    val kilometers_to_service: Float? = null,
    val type: String? = null
) {
    fun brandName(): String? = car_brand ?: card_brand

    fun displayName(): String = "U-${number_unit ?: "S/N"}"

    fun isOperative(): Boolean {
        val s = status?.lowercase()?.trim().orEmpty()
        return s == "operativa" || s == "operativo"
    }

    fun isWorkshop(): Boolean {
        val s = status?.lowercase()?.trim().orEmpty()
        return s == "taller" || s == "en taller"
    }
}

@Serializable
data class VehicleType(
    val id: Int? = null,
    val type: String? = null,
    val image: String? = null,
    val operative: Int? = null,
    @SerialName("in_operative")
    val in_operative: Int? = null
)

/** Tipo con unidades y conteos calculados desde GET /vehicle. */
data class VehicleTypeInventory(
    val type: VehicleType,
    val operativeCount: Int,
    val workshopCount: Int,
    val inoperativeCount: Int,
    val vehicles: List<Vehicle> = emptyList()
) {
    val totalCount: Int get() = vehicles.size
}
