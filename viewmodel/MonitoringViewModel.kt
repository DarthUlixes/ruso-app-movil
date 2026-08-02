package com.example.rusoit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.data.model.*
import com.example.rusoit.data.repository.MonitoringRepository
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MonitoringViewModel(
    private val repository: MonitoringRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _statusStats = MutableStateFlow<Resource<List<StatusCount>>?>(null)
    val statusStats = _statusStats.asStateFlow()

    private val _typeServiceStats = MutableStateFlow<Resource<List<TypeServiceCount>>?>(null)
    val typeServiceStats = _typeServiceStats.asStateFlow()

    private val _workShifts = MutableStateFlow<Resource<List<WorkShift>>?>(null)
    val workShifts = _workShifts.asStateFlow()

    private val _sciReports = MutableStateFlow<Resource<List<SCIInformation>>?>(null)
    val sciReports = _sciReports.asStateFlow()

    private val _vehicles = MutableStateFlow<Resource<List<Vehicle>>?>(null)
    val vehicles = _vehicles.asStateFlow()

    private val _personnel = MutableStateFlow<Resource<List<WorkForce>>?>(null)
    val personnel = _personnel.asStateFlow()

    fun loadStatusStats(dateFrom: String? = null, dateTo: String? = null) {
        repository.getStatusStatistics(dateFrom, dateTo).onEach { 
            _statusStats.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadTypeServiceStats(dateFrom: String? = null, dateTo: String? = null) {
        repository.getTypeServiceStatistics(dateFrom, dateTo).onEach { 
            _typeServiceStats.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadWorkShifts() {
        repository.getWorkShifts().onEach { 
            _workShifts.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadSCIReports() {
        repository.getSCIReports().onEach { 
            _sciReports.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadVehicles() {
        repository.getVehicles().onEach { 
            _vehicles.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadPersonnel() {
        repository.getPersonnel().onEach {
            _personnel.value = it
        }.launchIn(viewModelScope)
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onComplete()
        }
    }
}
