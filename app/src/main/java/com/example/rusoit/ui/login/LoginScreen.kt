@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.rusoit.R
import com.example.rusoit.viewmodel.LoginViewModel
import com.example.rusoit.viewmodel.ViewModelFactory
import com.example.rusoit.utils.Resource
import com.example.rusoit.ui.theme.HudColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModelFactory: ViewModelFactory,
    onLoginSuccess: () -> Unit
) {
    val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
    val otpState by loginViewModel.otpState.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()
    val challengeId by loginViewModel.otpChallengeId.collectAsState()

    var identifier by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    val isOtpStep = challengeId != null

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success) {
            onLoginSuccess()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp).background(HudColors.BgPrimary),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_bomberos),
                contentDescription = "Bomberos Tlajomulco",
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "RUSO IT MONITOR TV",
                style = MaterialTheme.typography.displayMedium,
                color = HudColors.AccentPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (!isOtpStep) "CENTRO DE MONITOREO • COMANDANCIA" else "VERIFICACIÓN DE ACCESO",
                style = MaterialTheme.typography.headlineSmall,
                color = HudColors.AccentSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.width(400.dp)) {
                if (!isOtpStep) {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Correo o número de empleado", color = HudColors.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = otpState !is Resource.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudColors.AccentPrimary,
                            unfocusedBorderColor = HudColors.BorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (otpState is Resource.Error) {
                        Text(otpState?.message ?: "Error", color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    val sending = otpState is Resource.Loading
                    LoginActionButton(
                        label = if (sending) "ENVIANDO..." else "ENVIAR CÓDIGO",
                        enabled = !sending && identifier.isNotEmpty(),
                        busy = sending,
                        onClick = { loginViewModel.requestOtp(identifier) }
                    )
                } else {
                    Text(
                        text = "Ingrese el código de seguridad enviado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("Código de 6 dígitos", color = HudColors.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = loginState !is Resource.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudColors.AccentPrimary,
                            unfocusedBorderColor = HudColors.BorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (loginState is Resource.Error) {
                        Text(loginState?.message ?: "Error", color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    val verifying = loginState is Resource.Loading
                    LoginActionButton(
                        label = if (verifying) "VERIFICANDO..." else "VERIFICAR",
                        enabled = !verifying && otpCode.length == 6,
                        busy = verifying,
                        onClick = { loginViewModel.verifyOtp(otpCode) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val backInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(10.dp))
                            .focusable(interactionSource = backInteraction)
                            .clickable(
                                interactionSource = backInteraction,
                                indication = null,
                                enabled = !verifying
                            ) { loginViewModel.resetState() }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            text = "VOLVER",
                            style = TextStyle(
                                color = HudColors.TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginActionButton(
    label: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val canClick = enabled && !busy

    val background = when {
        busy -> HudColors.AccentPrimary
        canClick && focused -> HudColors.AccentSecondary
        canClick -> HudColors.AccentPrimary
        else -> HudColors.AccentPrimary.copy(alpha = 0.45f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .focusable(enabled = canClick || busy, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = canClick
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // BasicText: evita el bug de TV Material3 Button/Surface que deja el label vacío
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        )
    }
}
