package com.example.rusoit.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.rusoit.viewmodel.LoginViewModel
import com.example.rusoit.viewmodel.ViewModelFactory
import com.example.rusoit.utils.Resource

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModelFactory: ViewModelFactory,
    onLoginSuccess: () -> Unit
) {
    val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
    val otpState by loginViewModel.otpState.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()

    var identifier by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpStep by remember { mutableStateOf(false) }
    var challengeId by remember { mutableStateOf("") }

    LaunchedEffect(otpState) {
        if (otpState is Resource.Success) {
            challengeId = otpState?.data?.challenge_id ?: ""
            isOtpStep = true
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success) {
            onLoginSuccess()
            loginViewModel.resetState()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "RUSOIT",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (!isOtpStep) "GESTIÓN OPERATIVA DE BOMBEROS" else "VERIFICACIÓN DE ACCESO",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.width(400.dp)) {
                if (!isOtpStep) {
                    // Paso 1: Ingreso de Correo o Número
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Correo o número de empleado", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = otpState !is Resource.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (otpState is Resource.Error) {
                        Text(otpState?.message ?: "Error", color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    Button(
                        onClick = { loginViewModel.requestOtp(identifier) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = otpState !is Resource.Loading && identifier.isNotEmpty(),
                        scale = ButtonDefaults.scale(focusedScale = 1.05f)
                    ) {
                        if (otpState is Resource.Loading) Text("ENVIANDO...") 
                        else Text("ENVIAR CÓDIGO", modifier = Modifier.padding(8.dp))
                    }
                } else {
                    // Paso 2: Ingreso de Código OTP
                    Text(
                        text = "Se ha enviado un código a: ${otpState?.data?.email_hint}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("Código de 6 dígitos", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = loginState !is Resource.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (loginState is Resource.Error) {
                        Text(loginState?.message ?: "Error", color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    Button(
                        onClick = { loginViewModel.verifyOtp(challengeId, otpCode) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = loginState !is Resource.Loading && otpCode.length == 6,
                        scale = ButtonDefaults.scale(focusedScale = 1.05f)
                    ) {
                        if (loginState is Resource.Loading) Text("VERIFICANDO...") 
                        else Text("INICIAR SESIÓN", modifier = Modifier.padding(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isOtpStep = false; loginViewModel.resetState() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Text("VOLVER", color = Color.Gray)
                    }
                }
            }
        }
    }
}
