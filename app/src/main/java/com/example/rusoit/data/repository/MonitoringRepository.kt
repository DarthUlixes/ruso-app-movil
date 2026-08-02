package com.example.rusoit.data.repository

import android.util.Log
import com.example.rusoit.data.api.MonitoringApiService
import com.example.rusoit.data.model.*
import com.example.rusoit.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

class MonitoringRepository(private val apiService: MonitoringApiService) {

    private suspend fun <T> safeApiCall(call: suspend () -> T): Resource<T> {
        return try {
            withTimeout(20000) {
                Resource.Success(call())
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("API_ERROR", "Code ${e.code()}: $errorBody")
            Resource.Error("Error ${e.code()}: ${errorBody ?: e.message()}")
        } catch (e: Exception) {
            Log.e("API_ERROR", "Exception: ${e.localizedMessage}")
            Resource.Error("Falla de red: ${e.localizedMessage ?: "Sin conexión"}")
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

    fun getSCIReports(): Flow<Resource<List<SCIInformation>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSCIReports() })
    }

    fun getVehicles(): Flow<Resource<List<Vehicle>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getVehicles() })
    }

    fun getPersonnel(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getPersonnel() })
    }

    fun getTools(): Flow<Resource<List<Tool>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTools() })
    }

    fun getServices(): Flow<Resource<List<Folio>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getServices() })
    }
}
