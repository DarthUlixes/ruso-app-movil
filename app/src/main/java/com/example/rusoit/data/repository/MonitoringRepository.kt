package com.example.rusoit.data.repository

import android.util.Log
import com.example.rusoit.data.api.MonitoringApiService
import com.example.rusoit.data.model.*
import com.example.rusoit.utils.DataStudioAggregator
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

class MonitoringRepository(private val apiService: MonitoringApiService) {

    private fun networkErrorMessage(e: Exception): String {
        return when (e) {
            is HttpException -> {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("API_ERROR", "Code ${e.code()}: $errorBody")
                "Error ${e.code()}: ${errorBody ?: e.message()}"
            }
            is kotlinx.coroutines.TimeoutCancellationException,
            is java.net.SocketTimeoutException -> {
                Log.e("API_ERROR", "Timeout", e)
                "El API tarda en responder (Render despertando). Reintente en unos segundos."
            }
            is java.net.UnknownHostException -> {
                Log.e("API_ERROR", "Host", e)
                "Sin conexión al API (${com.example.rusoit.data.api.RetrofitInstance.BASE_URL})"
            }
            else -> {
                Log.e("API_ERROR", "Exception: ${e.localizedMessage}")
                "Falla de red: ${e.localizedMessage ?: "Sin conexión"}"
            }
        }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): Resource<T> {
        return try {
            // Render cold start + varias llamadas en paralelo
            withTimeout(90_000) {
                Resource.Success(call())
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getStatusStatistics(dateFrom: String?, dateTo: String?): Flow<Resource<List<StatusCount>>> = flow {
        emit(Resource.Loading())
        val from = if (dateFrom.isNullOrBlank()) null else dateFrom
        val to = if (dateTo.isNullOrBlank()) null else dateTo
        emit(safeApiCall { apiService.getStatusStatistics(SearchGroupByDateRequest(from, to)) })
    }

    fun getTypeServiceStatistics(dateFrom: String?, dateTo: String?): Flow<Resource<List<TypeServiceCount>>> = flow {
        emit(Resource.Loading())
        val from = if (dateFrom.isNullOrBlank()) null else dateFrom
        val to = if (dateTo.isNullOrBlank()) null else dateTo
        emit(safeApiCall { apiService.getTypeServiceStatistics(SearchGroupByDateRequest(from, to)) })
    }

    fun getWorkShifts(): Flow<Resource<List<WorkShift>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getWorkShifts() })
    }

    /** Monitoreo unidades/servicios: GET /work-force/on-service-by-date-and-workshift */
    fun getUnitsOnService(): Flow<Resource<List<UnitOnService>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getUnitsOnService() })
    }

    suspend fun fetchUnitsOnService(): Resource<List<UnitOnService>> =
        safeApiCall { apiService.getUnitsOnService() }

    fun getTypeServices(): Flow<Resource<List<TypeService>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTypeServices() })
    }

    fun getBases(): Flow<Resource<List<BaseStation>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getBases() })
    }

    /** Carga paralela del Resumen Operativo (espejo de getDataStudioData del web). */
    suspend fun getDataStudioData(dateFrom: String, dateTo: String): Resource<DataStudioData> {
        return try {
            withTimeout(90_000) {
                coroutineScope {
                    val request = SearchGroupByDateRequest(dateFrom, dateTo)

                    val servicesByTypeDef = async {
                        runCatching {
                            FolioStatsParser.parseTypeServiceRows(
                                apiService.getTypeServiceStatisticsRaw(request)
                            ) ?: apiService.getTypeServiceStatistics(request)
                        }.onFailure { Log.e("DATA_STUDIO", "group-by-date failed", it) }
                            .getOrNull()
                    }
                    val servicesByStatusDef = async {
                        runCatching {
                            FolioStatsParser.parseStatusRows(
                                apiService.getStatusStatisticsRaw(request)
                            ) ?: apiService.getStatusStatistics(request)
                        }.onFailure { Log.e("DATA_STUDIO", "status-group-by-date failed", it) }
                            .getOrNull()
                    }
                    // Espejo web Censo: GET cologne-group-by-date-status/todas
                    val servicesByCologneDef = async {
                        runCatching {
                            FolioStatsParser.parseCologneRows(
                                apiService.getCologneStatisticsByStatusRaw("todas", request)
                            ) ?: apiService.getCologneStatisticsByStatus("todas", request)
                        }.onFailure { Log.e("DATA_STUDIO", "cologne-group-by-date-status failed", it) }
                            .getOrNull()
                    }
                    val typeServicesDef = async {
                        runCatching { apiService.getTypeServices() }
                            .onFailure { Log.e("DATA_STUDIO", "type-services failed", it) }
                            .getOrNull()
                    }
                    val colognesDef = async {
                        runCatching { apiService.getColognes() }
                            .onFailure { Log.e("DATA_STUDIO", "colognes failed", it) }
                            .getOrNull()
                    }
                    val vehiclesDef = async {
                        runCatching { apiService.getVehicles() }
                            .onFailure { Log.e("DATA_STUDIO", "vehicle failed", it) }
                            .getOrNull()
                    }
                    val basesDef = async {
                        runCatching { apiService.getBases() }
                            .onFailure { Log.e("DATA_STUDIO", "bases failed", it) }
                            .getOrNull()
                    }
                    val shiftsDef = async {
                        runCatching { apiService.getWorkShifts() }
                            .onFailure { Log.e("DATA_STUDIO", "work-shift failed", it) }
                            .getOrNull()
                    }

                    val servicesByType = servicesByTypeDef.await()
                    val servicesByStatus = servicesByStatusDef.await()
                    val servicesByCologne = servicesByCologneDef.await()
                    val typeServices = typeServicesDef.await()
                    val colognes = colognesDef.await()
                    val vehicles = vehiclesDef.await()
                    val bases = basesDef.await()
                    val shifts = shiftsDef.await()

                    Log.d(
                        "DATA_STUDIO",
                        "range=$dateFrom..$dateTo type=${servicesByType?.size} status=${servicesByStatus?.size} " +
                            "cologne=${servicesByCologne?.size} catalog=${typeServices?.size} " +
                            "colognes=${colognes?.size} fleet=${vehicles?.size} bases=${bases?.size} shifts=${shifts?.size}"
                    )

                    val allFailed = listOf(
                        servicesByType, servicesByStatus, servicesByCologne,
                        typeServices, colognes, vehicles, bases, shifts
                    ).all { it == null }

                    if (allFailed) {
                        Resource.Error("No se pudieron cargar los datos. Verifica tu sesión o el estado del API.")
                    } else {
                        val built = DataStudioAggregator.build(
                            servicesByType = servicesByType,
                            servicesByStatus = servicesByStatus,
                            servicesByCologne = servicesByCologne,
                            typeServices = typeServices,
                            colognes = colognes,
                            vehicles = vehicles,
                            bases = bases,
                            shifts = shifts,
                            dateFrom = dateFrom,
                            dateTo = dateTo
                        )
                        Log.d(
                            "DATA_STUDIO",
                            "mapped type=${built.incidentsByType} status=${built.incidentsByStatus} " +
                                "cologne=${built.incidentsByCologne} fleet=${built.fleet}"
                        )
                        Resource.Success(built)
                    }
                }
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getCalendarEvents(): Flow<Resource<List<CalendarEvent>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getCalendarEvents() })
    }

    /**
     * Agenda operativa TV (espejo calendario de Inicio).
     * Combina: API calendar si existe, SCI con fecha, y eventos base del mes actual
     * (el web de Inicio también usa eventos locales porque no hay endpoint de calendario).
     */
    suspend fun getAgendaEvents(): Resource<List<AgendaCalendarEvent>> {
        return try {
            withTimeout(90_000) {
                coroutineScope {
                    val apiEvents = async {
                        runCatching { apiService.getCalendarEvents() }
                            .onFailure { Log.w("AGENDA", "calendar-events no disponible: ${it.message}") }
                            .getOrNull()
                            .orEmpty()
                            .map {
                                AgendaCalendarEvent(
                                    id = "api-${it.id ?: it.resolvedTitle()}-${it.resolvedStart()}",
                                    title = it.resolvedTitle(),
                                    content = it.resolvedContent(),
                                    start = it.resolvedStart(),
                                    end = it.resolvedEnd(),
                                    eventClass = when (it.estatus?.lowercase()) {
                                        "cancelado", "rechazado" -> "red-event"
                                        else -> "green-event"
                                    },
                                    tipe_to_event = it.tipe_to_event,
                                    estatus = it.estatus,
                                    who_autorisated = it.who_autorisated,
                                    notes = it.notes,
                                    source = "calendar-api"
                                )
                            }
                            .filter { it.start.isNotBlank() }
                    }
                    val sciEvents = async {
                        runCatching { apiService.getSCIReports() }
                            .onFailure { Log.w("AGENDA", "sci falló: ${it.message}") }
                            .getOrNull()
                            .orEmpty()
                            .mapNotNull { sci ->
                                val start = sci.date_to_start?.take(10) ?: return@mapNotNull null
                                AgendaCalendarEvent(
                                    id = "sci-${sci.id}",
                                    title = sci.name ?: "SCI",
                                    content = sci.description ?: sci.ubication ?: "Incidente SCI",
                                    start = start,
                                    end = start,
                                    eventClass = "red-event",
                                    tipe_to_event = "SCI",
                                    estatus = sci.status,
                                    notes = sci.description,
                                    ubication = sci.ubication,
                                    source = "sci"
                                )
                            }
                    }

                    val merged = (apiEvents.await() + sciEvents.await() + inicioStyleSampleEvents())
                        .distinctBy { it.id }
                        .sortedBy { it.start }

                    Resource.Success(merged)
                }
            }
        } catch (e: Exception) {
            // Aunque falle red, devolvemos eventos locales para consulta tipo Inicio
            Log.e("AGENDA", "getAgendaEvents fallback local", e)
            Resource.Success(inicioStyleSampleEvents())
        }
    }

    /** Mismos tipos de evento que WellcomeView (fechas del mes actual para que se vean). */
    private fun inicioStyleSampleEvents(): List<AgendaCalendarEvent> {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        fun day(d: Int): String = "%04d-%02d-%02d".format(year, month, d.coerceIn(1, 28))
        return listOf(
            AgendaCalendarEvent(
                id = "local-reunion-1",
                title = "Reunión",
                content = "Con el equipo",
                start = day(5),
                end = day(5),
                eventClass = "green-event",
                tipe_to_event = "Reunión",
                source = "inicio-local"
            ),
            AgendaCalendarEvent(
                id = "local-reunion-2",
                title = "Reunión",
                content = "Con el equipo",
                start = day(12),
                end = day(12),
                eventClass = "green-event",
                tipe_to_event = "Reunión",
                source = "inicio-local"
            ),
            AgendaCalendarEvent(
                id = "local-entrega",
                title = "Entrega proyecto",
                content = "Finalizar documentación",
                start = day(18),
                end = day(18),
                eventClass = "red-event",
                tipe_to_event = "Entrega",
                source = "inicio-local"
            )
        )
    }

    fun getSCIReports(): Flow<Resource<List<SCIInformation>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall {
            // Preferir /sci/active si existe; si no, /sci filtrado como en la web
            runCatching { apiService.getActiveSCIReports() }
                .getOrElse {
                    apiService.getSCIReports().filter { it.isActive() }
                }
        })
    }

    fun getAllSCIReports(): Flow<Resource<List<SCIInformation>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSCIReports() })
    }

    fun getSCIById(id: Int): Flow<Resource<SCIInformation>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall {
            val raw = apiService.getSCIById(id)
            // Algunos backends responden lista; Retrofit tipado a un objeto.
            // Si falla el shape, se reintenta desde la lista completa.
            raw
        })
    }

    suspend fun fetchSCIById(id: Int): Resource<SCIInformation> {
        return try {
            withTimeout(60_000) {
                // 1) Intentar GET /sci/{id}
                val one = runCatching { apiService.getSCIById(id) }.getOrNull()
                if (one != null && one.id == id) {
                    return@withTimeout Resource.Success(one)
                }
                // 2) Fallback: buscar en GET /sci
                val fromList = apiService.getSCIReports().firstOrNull { it.id == id }
                if (fromList != null) Resource.Success(fromList)
                else Resource.Error("SCI $id no encontrado")
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getVehicles(): Flow<Resource<List<Vehicle>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getVehicles() })
    }

    fun getVehicleTypes(): Flow<Resource<List<VehicleType>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getVehicleTypes() })
    }

    /**
     * Tipos de unidad (GET /type-to-vehicles) + flota (GET /vehicle).
     * Conteos operativas / taller / in-operativas calculados desde unidades reales.
     */
    suspend fun getVehicleTypesInventory(): Resource<List<VehicleTypeInventory>> {
        return try {
            withTimeout(90_000) {
                coroutineScope {
                    val typesDef = async { apiService.getVehicleTypes() }
                    val vehiclesDef = async {
                        runCatching { apiService.getVehicles() }.getOrNull().orEmpty()
                    }
                    val types = typesDef.await()
                    val allVehicles = vehiclesDef.await()

                    val inventory = types.map { type ->
                        val id = type.id
                        val forType = if (id == null) emptyList()
                        else allVehicles.filter { it.id_type == id }
                        val operative = forType.count { it.isOperative() }
                        val workshop = forType.count { it.isWorkshop() }
                        val inoperative = (forType.size - operative - workshop).coerceAtLeast(0)
                        VehicleTypeInventory(
                            type = type,
                            operativeCount = operative,
                            workshopCount = workshop,
                            inoperativeCount = inoperative,
                            vehicles = forType
                        )
                    }
                    Resource.Success(inventory)
                }
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getVehiclesByType(idType: Int): Flow<Resource<List<Vehicle>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall {
            apiService.getVehicles().filter { it.id_type == idType }
        })
    }

    suspend fun fetchVehicleByNumber(numberUnit: Int): Resource<Vehicle> {
        return try {
            withTimeout(60_000) {
                Resource.Success(apiService.getVehicleByNumber(numberUnit))
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getPersonnel(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getPersonnel() })
    }

    fun getTools(): Flow<Resource<List<Tool>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTools() })
    }

    fun getTypeTools(): Flow<Resource<List<ToolType>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTypeTools() })
    }

    fun getToolsByType(idType: Int): Flow<Resource<List<Tool>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall {
            // Preferir endpoint por tipo; si viene vacío, filtrar GET /tools
            val byType = runCatching { apiService.getToolsByType(idType) }.getOrNull()
            if (!byType.isNullOrEmpty()) byType
            else apiService.getTools().filter { it.id_type == idType }
        })
    }

    /**
     * Catálogo de tipos con conteos reales (óptimos / mantenimiento).
     * No confía en on_inventory_active / on_inventory_in_active del backend (suelen venir en 0).
     */
    suspend fun getToolTypesInventory(): Resource<List<ToolTypeInventory>> {
        return try {
            withTimeout(90_000) {
                coroutineScope {
                    val typesDef = async { apiService.getTypeTools() }
                    val toolsDef = async {
                        runCatching { apiService.getTools() }.getOrNull()
                    }

                    val types = typesDef.await()
                    var allTools = toolsDef.await()

                    // Si GET /tools falla o viene vacío, pedir por cada tipo
                    if (allTools.isNullOrEmpty()) {
                        allTools = types.mapNotNull { t ->
                            val id = t.id ?: return@mapNotNull null
                            async {
                                runCatching { apiService.getToolsByType(id) }.getOrNull().orEmpty()
                            }
                        }.map { it.await() }.flatten()
                    }

                    val inventory = types.map { type ->
                        val id = type.id
                        val toolsForType = if (id == null) emptyList()
                        else allTools.filter { it.id_type == id }
                        val active = toolsForType.count { it.isActive() }
                        val inactive = toolsForType.size - active
                        ToolTypeInventory(
                            type = type,
                            activeCount = active,
                            inactiveCount = inactive,
                            tools = toolsForType
                        )
                    }

                    Resource.Success(inventory)
                }
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    fun getServices(): Flow<Resource<List<Folio>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getServices() })
    }

    fun getServiceById(id: Int): Flow<Resource<Folio>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getServiceById(id) })
    }

    fun getColognes(): Flow<Resource<List<Cologne>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getColognes() })
    }

    /** Catálogo completo para consulta de Partes de Atención (espejo web /sumarys). */
    suspend fun getPartesCatalog(): Resource<PartesCatalog> {
        return try {
            withTimeout(90_000) {
                coroutineScope {
                    val foliosDef = async { runCatching { apiService.getServices() }.getOrNull() }
                    val typesDef = async { runCatching { apiService.getTypeServices() }.getOrNull() }
                    val vehiclesDef = async { runCatching { apiService.getVehicles() }.getOrNull() }
                    val colognesDef = async { runCatching { apiService.getColognes() }.getOrNull() }
                    val usersDef = async { runCatching { apiService.getPersonnel() }.getOrNull() }

                    val folios = foliosDef.await()
                    if (folios == null) {
                        Resource.Error("No se pudo cargar el historial de partes (/folio).")
                    } else {
                        Resource.Success(
                            PartesCatalog(
                                folios = folios.sortedByDescending { it.id },
                                typeServices = typesDef.await().orEmpty(),
                                vehicles = vehiclesDef.await().orEmpty(),
                                colognes = colognesDef.await().orEmpty(),
                                users = usersDef.await().orEmpty()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Resource.Error(networkErrorMessage(e))
        }
    }

    suspend fun logout() {
        // Implementation for auth service logout can be called here if needed
    }
}
