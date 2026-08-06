package com.example.rusoit.data.api

import com.example.rusoit.data.dto.*
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth")
    suspend fun requestOtp(@Body request: LoginRequest): OtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): LogoutResponse
}

@kotlinx.serialization.Serializable
data class RefreshRequest(val refresh_token: String)

@kotlinx.serialization.Serializable
data class RefreshResponse(val access_token: String, val message: String? = null)

@kotlinx.serialization.Serializable
data class LogoutRequest(val refresh_token: String? = null)

@kotlinx.serialization.Serializable
data class LogoutResponse(val message: String? = null)
