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

    @GET("work-shift")
    suspend fun getWorkShifts(): List<WorkShift>

    @GET("sci")
    suspend fun getSCIReports(): List<SCIInformation>

    @GET("vehicle")
    suspend fun getVehicles(): List<Vehicle>

    @GET("users")
    suspend fun getPersonnel(): List<User>

    @GET("tools")
    suspend fun getTools(): List<Tool>

    @GET("folio")
    suspend fun getServices(): List<Folio>
}
