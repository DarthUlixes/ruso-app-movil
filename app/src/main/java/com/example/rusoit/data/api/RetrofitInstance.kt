package com.example.rusoit.data.api

import android.util.Log
import com.example.rusoit.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    // CAMBIA ESTA IP si usas un dispositivo real (ej: "192.168.1.XX")
    const val BASE_URL = "http://10.0.2.2:3000/" 

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun getRetrofit(sessionManager: SessionManager): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                
                val token = runBlocking {
                    val t = withTimeoutOrNull(3000) {
                        sessionManager.authToken.first()
                    }
                    t
                }
                
                if (token != null) {
                    Log.d("AUTH_DEBUG", "Sending Token: Bearer ${token.take(10)}...")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    Log.e("AUTH_DEBUG", "No token found in SessionManager")
                }
                
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
