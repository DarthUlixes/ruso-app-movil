package com.example.rusoit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rusoit.data.dto.LoginResponse
import com.example.rusoit.data.dto.OtpResponse
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.data.repository.AuthRepository
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _otpState = MutableStateFlow<Resource<OtpResponse>?>(null)
    val otpState = _otpState.asStateFlow()

    private val _loginState = MutableStateFlow<Resource<LoginResponse>?>(null)
    val loginState = _loginState.asStateFlow()

    fun requestOtp(identifier: String) {
        repository.requestOtp(identifier).onEach { result ->
            _otpState.value = result
        }.launchIn(viewModelScope)
    }

    fun verifyOtp(challengeId: String, code: String) {
        repository.verifyOtp(challengeId, code).onEach { result ->
            _loginState.value = result
            if (result is Resource.Success) {
                viewModelScope.launch {
                    result.data?.access_token?.let { sessionManager.saveToken(it) }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun resetState() {
        _otpState.value = null
        _loginState.value = null
    }
}
