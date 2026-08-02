package com.example.rusoit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StatusCount(
    val status: String? = null,
    val name: String? = null,
    val _count: CountDetail? = null
)

@Serializable
data class CountDetail(
    val id: Int? = null,
    val status: Int? = null,
    val _all: Int? = null
)

@Serializable
data class TypeServiceCount(
    val id_type_service: Int? = null,
    val _count: CountDetail? = null
)

@Serializable
data class WorkShift(
    val id: Int,
    val name: String? = null,
    val workday: String? = null,
    val operative_personal: Int? = null,
    val in_operative_personal: Int? = null
)

@Serializable
data class SCIInformation(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val ubication: String? = null,
    val status: String? = null,
    val date_to_start: String? = null
)

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
    val phone: String? = null
)

@Serializable
data class Tool(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val cover_image: String? = null,
    val id_type: Int? = null
)

@Serializable
data class Folio(
    val id: Int,
    val folio: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val stret: String? = null,
    val crossing: String? = null,
    val date_to_open: String? = null
)

@Serializable
data class SearchGroupByDateRequest(
    val date_from: String? = null,
    val date_to: String? = null
)
