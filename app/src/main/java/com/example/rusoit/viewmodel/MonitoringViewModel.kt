package com.example.rusoit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.data.model.*
import com.example.rusoit.data.repository.AuthRepository
import com.example.rusoit.data.repository.MonitoringRepository
import com.example.rusoit.utils.DataStudioAggregator
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MonitoringViewModel(
    private val repository: MonitoringRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _statusStats = MutableStateFlow<Resource<List<StatusCount>>?>(null)
    val statusStats = _statusStats.asStateFlow()

    private val _typeServiceStats = MutableStateFlow<Resource<List<TypeServiceCount>>?>(null)
    val typeServiceStats = _typeServiceStats.asStateFlow()

    private val _workShifts = MutableStateFlow<Resource<List<WorkShift>>?>(null)
    val workShifts = _workShifts.asStateFlow()

    private val _dataStudio = MutableStateFlow<Resource<DataStudioData>?>(null)
    val dataStudio = _dataStudio.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset = _weekOffset.asStateFlow()

    private val _calendarEvents = MutableStateFlow<Resource<List<CalendarEvent>>?>(null)
    val calendarEvents = _calendarEvents.asStateFlow()

    private val _agendaEvents = MutableStateFlow<Resource<List<AgendaCalendarEvent>>?>(null)
    val agendaEvents = _agendaEvents.asStateFlow()

    private val _sciReports = MutableStateFlow<Resource<List<SCIInformation>>?>(null)
    val sciReports = _sciReports.asStateFlow()

    private val _sciAll = MutableStateFlow<Resource<List<SCIInformation>>?>(null)
    val sciAll = _sciAll.asStateFlow()

    private val _sciDetail = MutableStateFlow<Resource<SCIInformation>?>(null)
    val sciDetail = _sciDetail.asStateFlow()

    private val _vehicles = MutableStateFlow<Resource<List<Vehicle>>?>(null)
    val vehicles = _vehicles.asStateFlow()

    private val _vehicleTypesInventory = MutableStateFlow<Resource<List<VehicleTypeInventory>>?>(null)
    val vehicleTypesInventory = _vehicleTypesInventory.asStateFlow()

    private val _vehiclesByType = MutableStateFlow<Resource<List<Vehicle>>?>(null)
    val vehiclesByType = _vehiclesByType.asStateFlow()

    private val _vehicleDetail = MutableStateFlow<Resource<Vehicle>?>(null)
    val vehicleDetail = _vehicleDetail.asStateFlow()

    private val _personnel = MutableStateFlow<Resource<List<User>>?>(null)
    val personnel = _personnel.asStateFlow()

    private val _tools = MutableStateFlow<Resource<List<Tool>>?>(null)
    val tools = _tools.asStateFlow()

    private val _typeTools = MutableStateFlow<Resource<List<ToolType>>?>(null)
    val typeTools = _typeTools.asStateFlow()

    private val _toolsInventory = MutableStateFlow<Resource<List<ToolTypeInventory>>?>(null)
    val toolsInventory = _toolsInventory.asStateFlow()

    private val _toolsByType = MutableStateFlow<Resource<List<Tool>>?>(null)
    val toolsByType = _toolsByType.asStateFlow()

    private val _services = MutableStateFlow<Resource<List<Folio>>?>(null)
    val services = _services.asStateFlow()

    private val _partesCatalog = MutableStateFlow<Resource<PartesCatalog>?>(null)
    val partesCatalog = _partesCatalog.asStateFlow()

    private val _folioDetail = MutableStateFlow<Resource<Folio>?>(null)
    val folioDetail = _folioDetail.asStateFlow()

    private val _unitsOnService = MutableStateFlow<Resource<List<UnitOnService>>?>(null)
    val unitsOnService = _unitsOnService.asStateFlow()

    private val _colognes = MutableStateFlow<Resource<List<Cologne>>?>(null)
    val colognes = _colognes.asStateFlow()

    fun loadStatusStats(dateFrom: String? = null, dateTo: String? = null) {
        repository.getStatusStatistics(dateFrom, dateTo).onEach { 
            _statusStats.value = it 
        }.launchIn(viewModelScope)
    }

    fun setWeekOffset(offset: Int) {
        _weekOffset.value = offset
    }

    fun shiftWeek(delta: Int) {
        _weekOffset.value = _weekOffset.value + delta
    }

    fun loadDataStudio(weekOffset: Int = _weekOffset.value) {
        _weekOffset.value = weekOffset
        viewModelScope.launch {
            _dataStudio.value = Resource.Loading()
            val (from, to) = DataStudioAggregator.weekRange(weekOffset)
            _dataStudio.value = repository.getDataStudioData(from, to)
        }
    }

    fun loadWorkShifts() {
        repository.getWorkShifts().onEach { 
            _workShifts.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadUnitsOnService(silent: Boolean = false) {
        viewModelScope.launch {
            val hadData = _unitsOnService.value is Resource.Success
            if (!silent || !hadData) {
                _unitsOnService.value = Resource.Loading()
            }
            val result = repository.fetchUnitsOnService()
            // En refresh silencioso no borramos la lista si falla temporalmente
            if (result is Resource.Success || !hadData || !silent) {
                _unitsOnService.value = result
            }
        }
    }

    fun loadColognes() {
        repository.getColognes().onEach {
            _colognes.value = it
        }.launchIn(viewModelScope)
    }

    fun loadCalendarEvents() {
        repository.getCalendarEvents().onEach {
            _calendarEvents.value = it
        }.launchIn(viewModelScope)
    }

    fun loadAgendaEvents() {
        viewModelScope.launch {
            _agendaEvents.value = Resource.Loading()
            _agendaEvents.value = repository.getAgendaEvents()
        }
    }

    fun loadSCIReports() {
        repository.getSCIReports().onEach { 
            _sciReports.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadAllSCIReports() {
        repository.getAllSCIReports().onEach {
            _sciAll.value = it
        }.launchIn(viewModelScope)
    }

    fun loadSCIDetail(id: Int) {
        viewModelScope.launch {
            _sciDetail.value = Resource.Loading()
            _sciDetail.value = repository.fetchSCIById(id)
        }
    }

    fun clearSCIDetail() {
        _sciDetail.value = null
    }

    fun loadVehicles() {
        repository.getVehicles().onEach { 
            _vehicles.value = it 
        }.launchIn(viewModelScope)
    }

    fun loadVehicleTypesInventory() {
        viewModelScope.launch {
            _vehicleTypesInventory.value = Resource.Loading()
            _vehicleTypesInventory.value = repository.getVehicleTypesInventory()
        }
    }

    fun loadVehiclesByType(idType: Int) {
        val cached = _vehicleTypesInventory.value?.data
            ?.firstOrNull { it.type.id == idType }
            ?.vehicles
        if (!cached.isNullOrEmpty()) {
            _vehiclesByType.value = Resource.Success(cached)
        }
        repository.getVehiclesByType(idType).onEach {
            _vehiclesByType.value = it
        }.launchIn(viewModelScope)
    }

    fun clearVehiclesByType() {
        _vehiclesByType.value = null
    }

    fun loadVehicleDetail(numberUnit: Int) {
        viewModelScope.launch {
            _vehicleDetail.value = Resource.Loading()
            _vehicleDetail.value = repository.fetchVehicleByNumber(numberUnit)
        }
    }

    fun clearVehicleDetail() {
        _vehicleDetail.value = null
    }

    fun loadPersonnel() {
        repository.getPersonnel().onEach {
            _personnel.value = it
        }.launchIn(viewModelScope)
    }

    fun loadTools() {
        repository.getTools().onEach {
            _tools.value = it
        }.launchIn(viewModelScope)
    }

    fun loadTypeTools() {
        repository.getTypeTools().onEach {
            _typeTools.value = it
        }.launchIn(viewModelScope)
    }

    fun loadToolsInventory() {
        viewModelScope.launch {
            _toolsInventory.value = Resource.Loading()
            _toolsInventory.value = repository.getToolTypesInventory()
        }
    }

    fun loadToolsByType(idType: Int) {
        // Primero intentar cache del catálogo ya cargado
        val cached = _toolsInventory.value?.data
            ?.firstOrNull { it.type.id == idType }
            ?.tools
        if (!cached.isNullOrEmpty()) {
            _toolsByType.value = Resource.Success(cached)
        }
        repository.getToolsByType(idType).onEach {
            _toolsByType.value = it
        }.launchIn(viewModelScope)
    }

    fun clearToolsByType() {
        _toolsByType.value = null
    }

    fun loadServices() {
        repository.getServices().onEach {
            _services.value = it
        }.launchIn(viewModelScope)
    }

    fun loadPartesCatalog() {
        viewModelScope.launch {
            _partesCatalog.value = Resource.Loading()
            _partesCatalog.value = repository.getPartesCatalog()
        }
    }

    fun loadFolioDetail(id: Int) {
        viewModelScope.launch {
            _folioDetail.value = Resource.Loading()
            repository.getServiceById(id).collect { _folioDetail.value = it }
        }
    }

    fun clearFolioDetail() {
        _folioDetail.value = null
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            // AuthRepository destruye access + refresh siempre (aunque falle el API)
            runCatching { authRepository.logout() }
            sessionManager.destroyTokens()
            onComplete()
        }
    }
}
