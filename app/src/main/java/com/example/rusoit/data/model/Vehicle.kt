package com.example.rusoit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Int? = null,
    val number_unit: Int? = null,
    val id_type: Int? = null,
    val cover_img: String? = null,
    val status: String? = null,
    val model: String? = null,
    val card_brand: String? = null,
    val vehicle_license_plates: String? = null,
    val kilometers: Float? = null,
    val type: String? = null
)

@Serializable
data class VehicleType(
    val id: Int? = null,
    val type: String? = null,
    val image: String? = null
)
