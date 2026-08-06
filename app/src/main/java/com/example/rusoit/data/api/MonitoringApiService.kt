package com.example.rusoit.data.api

import com.example.rusoit.data.model.*
import retrofit2.http.*

interface MonitoringApiService {
    
    @POST("folio/status-group-by-date")
    suspend fun getStatusStatistics(
        @Body request: SearchGroupByDateRequest
    ): List<StatusCount>

    @POST("folio/group-by-date")
    suspend fun getTypeServiceStatistics(
        @Body request: SearchGroupByDateRequest
    ): List<TypeServiceCount>

    /** Respuesta cruda por si el backend envuelve el array. */
    @POST("folio/status-group-by-date")
    suspend fun getStatusStatisticsRaw(
        @Body request: SearchGroupByDateRequest
    ): kotlinx.serialization.json.JsonElement

    @POST("folio/group-by-date")
    suspend fun getTypeServiceStatisticsRaw(
        @Body request: SearchGroupByDateRequest
    ): kotlinx.serialization.json.JsonElement

    /** Censo web: incidentes agrupados por colonia (status = "todas" u otro). */
    @POST("folio/cologne-group-by-date-status/{status}")
    suspend fun getCologneStatisticsByStatus(
        @Path("status") status: String,
        @Body request: SearchGroupByDateRequest
    ): List<CologneCount>

    @POST("folio/cologne-group-by-date-status/{status}")
    suspend fun getCologneStatisticsByStatusRaw(
        @Path("status") status: String,
        @Body request: SearchGroupByDateRequest
    ): kotlinx.serialization.json.JsonElement

    @GET("work-shift")
    suspend fun getWorkShifts(): List<WorkShift>

    @GET("type-services/")
    suspend fun getTypeServices(): List<TypeService>

    @GET("bases")
    suspend fun getBases(): List<BaseStation>

    @GET("calendar-events") // Ajustado para tabla calendar_events
    suspend fun getCalendarEvents(): List<CalendarEvent>

    @GET("sci")
    suspend fun getSCIReports(): List<SCIInformation>

    @GET("sci/{id}")
    suspend fun getSCIById(@Path("id") id: Int): SCIInformation

    @GET("sci/active")
    suspend fun getActiveSCIReports(): List<SCIInformation>

    @GET("vehicle")
    suspend fun getVehicles(): List<Vehicle>

    @GET("vehicle/{numberUnit}")
    suspend fun getVehicleByNumber(@Path("numberUnit") numberUnit: Int): Vehicle

    @GET("type-to-vehicles")
    suspend fun getVehicleTypes(): List<VehicleType>

    @GET("users")
    suspend fun getPersonnel(): List<User>

    @GET("tools")
    suspend fun getTools(): List<Tool>

    @GET("type-tools")
    suspend fun getTypeTools(): List<ToolType>

    @GET("tools/type/{idType}")
    suspend fun getToolsByType(@Path("idType") idType: Int): List<Tool>

    @GET("colognes")
    suspend fun getColognes(): List<Cologne>

    @GET("folio")
    suspend fun getServices(): List<Folio>

    @GET("folio/{id}")
    suspend fun getServiceById(@Path("id") id: Int): Folio
}
