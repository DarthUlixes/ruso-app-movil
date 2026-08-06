package com.example.rusoit.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String,
    val email: String? = null,
    val employee_number: String? = null
) {
    companion object {
        fun fromIdentifier(raw: String): LoginRequest {
            val value = raw.trim()
            return if (value.contains("@")) {
                LoginRequest(identifier = value, email = value)
            } else {
                LoginRequest(identifier = value, employee_number = value)
            }
        }
    }
}

@Serializable
data class OtpResponse(
    val message: String? = null,
    val requires_otp: Boolean? = null,
    val challenge_id: String,
    val email_hint: String? = null,
    val expires_in: Int? = null
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
