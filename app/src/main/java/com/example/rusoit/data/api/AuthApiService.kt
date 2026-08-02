package com.example.rusoit.data.api

import com.example.rusoit.data.dto.*
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth")
    suspend fun requestOtp(@Body request: LoginRequest): OtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): LoginResponse
}
