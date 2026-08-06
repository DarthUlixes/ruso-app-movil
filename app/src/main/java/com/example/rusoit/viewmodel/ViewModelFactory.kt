package com.example.rusoit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.data.repository.AuthRepository
import com.example.rusoit.data.repository.MonitoringRepository

class ViewModelFactory(
    private val monitoringRepository: MonitoringRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val sessionManager: SessionManager? = null
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MonitoringViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                MonitoringViewModel(monitoringRepository!!, authRepository!!, sessionManager!!) as T
            }
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                LoginViewModel(authRepository!!, sessionManager!!) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
