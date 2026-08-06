package com.example.rusoit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rusoit.navigation.Screen
import com.example.rusoit.ui.dashboard.DashboardScreen
import com.example.rusoit.ui.login.LoginScreen
import com.example.rusoit.ui.theme.RusoitTheme
import com.example.rusoit.data.api.MonitoringApiService
import com.example.rusoit.data.api.AuthApiService
import com.example.rusoit.data.api.RetrofitInstance
import com.example.rusoit.data.repository.MonitoringRepository
import com.example.rusoit.data.repository.AuthRepository
import com.example.rusoit.data.local.SessionManager
import com.example.rusoit.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    
    private lateinit var sessionManager: SessionManager

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(applicationContext)
        
        val initialToken = runBlocking {
            sessionManager.authToken.first()
        }

        val retrofit = RetrofitInstance.getRetrofit(sessionManager)
        val authApiService = retrofit.create(AuthApiService::class.java)
        val monitoringApiService = retrofit.create(MonitoringApiService::class.java)
        
        val authRepository = AuthRepository(authApiService, sessionManager)
        val monitoringRepository = MonitoringRepository(monitoringApiService)
        
        val factory = ViewModelFactory(
            monitoringRepository = monitoringRepository,
            authRepository = authRepository,
            sessionManager = sessionManager
        )

        setContent {
            RusoitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val currentToken by sessionManager.authToken.collectAsState(initial = initialToken)
                    val isLoggedIn = !currentToken.isNullOrBlank()
                    
                    RusoitApp(factory, isLoggedIn)
                }
            }
        }
    }
}

@Composable
fun RusoitApp(factory: ViewModelFactory, isLoggedIn: Boolean) {
    val navController = rememberNavController()
    
    // Si el estado de login cambia a false, navegamos automáticamente al Login
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController, 
        startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModelFactory = factory,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                factory = factory,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
