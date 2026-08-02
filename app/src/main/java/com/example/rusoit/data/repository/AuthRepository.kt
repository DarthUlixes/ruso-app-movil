package com.example.rusoit.data.repository

import com.example.rusoit.data.api.AuthApiService
import com.example.rusoit.data.dto.*
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class AuthRepository(private val apiService: AuthApiService) {

    fun requestOtp(identifier: String): Flow<Resource<OtpResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.requestOtp(LoginRequest(identifier))
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al solicitar código"))
        }
    }

    fun verifyOtp(challengeId: String, code: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.verifyOtp(VerifyOtpRequest(challengeId, code))
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Código incorrecto o expirado"))
        }
    }
}
