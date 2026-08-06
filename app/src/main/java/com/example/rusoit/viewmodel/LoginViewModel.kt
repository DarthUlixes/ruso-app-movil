package com.example.rusoit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.rusoit.data.dto.LoginRequest
import com.example.rusoit.data.dto.VerifyOtpRequest
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.data.repository.AuthRepository
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _otpState = MutableStateFlow<Resource<String>?>(null)
    val otpState = _otpState.asStateFlow()

    private val _loginState = MutableStateFlow<Resource<String>?>(null)
    val loginState = _loginState.asStateFlow()

    private val _otpChallengeId = MutableStateFlow<String?>(null)
    val otpChallengeId = _otpChallengeId.asStateFlow()

    fun requestOtp(identifier: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading()
            // Limpiamos sesión previa para evitar entrar con tokens viejos
            sessionManager.clearSession()
            try {
                val response = repository.requestOtp(LoginRequest.fromIdentifier(identifier))
                _otpChallengeId.value = response.challenge_id
                _otpState.value = Resource.Success(response.message ?: "Código enviado")
            } catch (e: retrofit2.HttpException) {
                val detail = e.response()?.errorBody()?.string().orEmpty()
                Log.e("LOGIN", "OTP HTTP ${e.code()}: $detail")
                _otpState.value = Resource.Error(
                    when (e.code()) {
                        401, 403, 404 -> "Usuario no autorizado o no encontrado."
                        429 -> "Demasiados intentos. Espere un momento."
                        else -> "No se pudo enviar el código. Verifique sus datos e intente de nuevo."
                    }
                )
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("LOGIN", "OTP timeout", e)
                _otpState.value = Resource.Error(
                    "El servidor tarda en responder (despertando). Intente de nuevo en unos segundos."
                )
            } catch (e: java.net.UnknownHostException) {
                Log.e("LOGIN", "OTP host", e)
                _otpState.value = Resource.Error("Sin conexión a internet o DNS.")
            } catch (e: Exception) {
                Log.e("LOGIN", "OTP error", e)
                _otpState.value = Resource.Error(
                    "No se pudo conectar con el API. Intente de nuevo."
                )
            }
        }
    }

    fun verifyOtp(code: String) {
        val challengeId = _otpChallengeId.value ?: return
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            try {
                val response = repository.verifyOtp(VerifyOtpRequest(challengeId, code.trim()))
                if (response.access_token.isBlank()) {
                    _loginState.value = Resource.Error("Respuesta sin token de acceso.")
                    return@launch
                }
                sessionManager.saveTokens(response.access_token, response.refresh_token)
                _loginState.value = Resource.Success("Acceso concedido")
            } catch (e: retrofit2.HttpException) {
                Log.e("LOGIN", "Verify HTTP ${e.code()}", e)
                _loginState.value = Resource.Error("Código de verificación incorrecto o expirado.")
            } catch (e: java.net.SocketTimeoutException) {
                _loginState.value = Resource.Error(
                    "El servidor tarda en responder. Intente de nuevo."
                )
            } catch (e: Exception) {
                Log.e("LOGIN", "Verify error", e)
                _loginState.value = Resource.Error("No se pudo verificar el código. Intente de nuevo.")
            }
        }
    }

    fun resetState() {
        _otpState.value = null
        _loginState.value = null
        _otpChallengeId.value = null
    }
}
