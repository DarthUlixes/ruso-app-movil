package com.example.rusoit.data.repository

import com.example.rusoit.data.api.AuthApiService
import com.example.rusoit.data.api.LogoutRequest
import com.example.rusoit.data.dto.*
import com.example.rusoit.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepository(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
) {

    suspend fun requestOtp(request: LoginRequest): OtpResponse {
        return apiService.requestOtp(request)
    }

    suspend fun verifyOtp(request: VerifyOtpRequest): LoginResponse {
        return apiService.verifyOtp(request)
    }

    /**
     * Cierra sesión en API (si responde) y siempre destruye access + refresh locales.
     */
    suspend fun logout() {
        val refresh = runCatching { sessionManager.refreshToken.first() }.getOrNull()
        withTimeoutOrNull(8_000) {
            runCatching {
                if (!refresh.isNullOrBlank()) {
                    apiService.logout(LogoutRequest(refresh_token = refresh))
                } else {
                    apiService.logout(LogoutRequest())
                }
            }
        }
        sessionManager.destroyTokens()
    }
}
