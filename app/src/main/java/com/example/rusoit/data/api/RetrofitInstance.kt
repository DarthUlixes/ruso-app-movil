package com.example.rusoit.data.api

import android.util.Log
import com.example.rusoit.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    /** API producción (mismo host que VUE_APP_API_URL del web). */
    const val BASE_URL = "https://rusoit-api.onrender.com/"

    /** Render free tier puede tardar en despertar (cold start). */
    private const val TIMEOUT_SECONDS = 60L

    internal val json = Json {
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
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SECONDS + 15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                val token = runBlocking {
                    withTimeoutOrNull(2000) {
                        sessionManager.authToken.first()
                    }
                }
                token?.let {
                    requestBuilder.header("Authorization", "Bearer $it")
                }
                chain.proceed(requestBuilder.build())
            }
            .authenticator(TokenAuthenticator(sessionManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}

class TokenAuthenticator(private val sessionManager: SessionManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Evitar bucles infinitos de refresh
        if (responseCount(response) >= 2) {
            clearSession()
            return null
        }

        Log.d("AUTH", "Handling 401 Unauthorized. Attempting refresh...")

        val refreshToken = runBlocking { sessionManager.refreshToken.first() }
        if (refreshToken.isNullOrBlank()) {
            clearSession()
            return null
        }

        synchronized(this) {
            val currentToken = runBlocking { sessionManager.authToken.first() }

            if (response.request.header("Authorization") != "Bearer $currentToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newAccessToken = refreshAccessToken(refreshToken)
            if (newAccessToken != null) {
                runBlocking { sessionManager.updateAccessToken(newAccessToken) }
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                clearSession()
            }
        }

        return null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    private fun clearSession() {
        runBlocking { sessionManager.clearSession() }
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(RetrofitInstance.BASE_URL)
                .client(client)
                .addConverterFactory(
                    RetrofitInstance.json
                        .asConverterFactory("application/json".toMediaType())
                )
                .build()

            val api = retrofit.create(AuthApiService::class.java)
            val response = runBlocking { api.refresh(RefreshRequest(refreshToken)) }
            response.access_token
        } catch (e: Exception) {
            Log.e("AUTH", "Failed to refresh access token", e)
            null
        }
    }
}
