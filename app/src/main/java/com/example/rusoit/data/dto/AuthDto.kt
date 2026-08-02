package com.example.rusoit.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String
)

@Serializable
data class OtpResponse(
    val message: String,
    val requires_otp: Boolean,
    val challenge_id: String,
    val email_hint: String,
    val expires_in: Int
)

@Serializable
data class VerifyOtpRequest(
    val challenge_id: String,
    val code: String
)

@Serializable
data class LoginResponse(
    val message: String? = null,
    val access_token: String,
    val refresh_token: String? = null
)

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String? = null,
    val type_user: String? = null
)
