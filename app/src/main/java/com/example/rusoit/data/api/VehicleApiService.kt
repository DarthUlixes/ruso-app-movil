package com.example.rusoit.data.api

import com.example.rusoit.data.model.Vehicle
import retrofit2.http.*

interface VehicleApiService {
    @GET("vehicles")
    suspend fun getVehicles(): List<Vehicle>

    @GET("vehicles/{id}")
    suspend fun getVehicleById(@Path("id") id: Int): Vehicle

    @POST("vehicles")
    suspend fun createVehicle(@Body vehicle: Vehicle): Vehicle

    @PUT("vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: Int, @Body vehicle: Vehicle): Vehicle

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int)
}
