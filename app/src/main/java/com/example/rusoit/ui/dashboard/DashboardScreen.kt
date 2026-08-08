@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.rusoit.R
import com.example.rusoit.viewmodel.MonitoringViewModel
import com.example.rusoit.viewmodel.ViewModelFactory
import com.example.rusoit.utils.Resource
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.data.model.*
import com.example.rusoit.utils.CurrentGuardResolver
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    factory: ViewModelFactory,
    onLogout: () -> Unit
) {
    val monitoringViewModel: MonitoringViewModel = viewModel(factory = factory)
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedSCI by remember { mutableStateOf<SCIInformation?>(null) }
    var selectedFolio by remember { mutableStateOf<Folio?>(null) }
    
    val isOverlayOpen = selectedVehicle != null || selectedSCI != null || selectedFolio != null

    // Monitoreo en tiempo real para SCI (Polleo cada 30 segundos como en despacho web)
    LaunchedEffect(Unit) {
        while(true) {
            monitoringViewModel.loadSCIReports()
            delay(30000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(HudColors.BgPrimary)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isOverlayOpen) {
                        Modifier.focusProperties { canFocus = false }
                    } else {
                        Modifier
                    }
                )
        ) {
            // Sidebar HUD — logout siempre anclado abajo
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(104.dp)
                    .background(HudColors.BgNav)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    SidebarIcon(Icons.Default.Sensors, "MONITOREO\nUNIDADES", selectedTab == 0, enabled = !isOverlayOpen) { selectedTab = 0 }
                    SidebarIcon(Icons.Default.BarChart, "RESUMEN\nOPERATIVO", selectedTab == 1, enabled = !isOverlayOpen) { selectedTab = 1 }
                    SidebarIcon(Icons.AutoMirrored.Filled.List, "PARTES DE\nATENCIÓN", selectedTab == 2, enabled = !isOverlayOpen) { selectedTab = 2 }
                    SidebarIcon(Icons.Default.CalendarMonth, "AGENDA", selectedTab == 3, enabled = !isOverlayOpen) { selectedTab = 3 }
                    SidebarIcon(Icons.Default.Shield, "SCI", selectedTab == 4, enabled = !isOverlayOpen) { selectedTab = 4 }
                    SidebarIcon(Icons.Default.FireTruck, "CONTROL DE\nVEHÍCULO", selectedTab == 5, enabled = !isOverlayOpen) { selectedTab = 5 }
                    SidebarIcon(Icons.Default.Person, "PERSONAL", selectedTab == 6, enabled = !isOverlayOpen) { selectedTab = 6 }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp),
                    color = HudColors.TextMuted.copy(alpha = 0.35f)
                )

                SidebarIcon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    "CERRAR\nSESIÓN",
                    selected = false,
                    enabled = !isOverlayOpen,
                    color = HudColors.AccentPrimary
                ) {
                    monitoringViewModel.logout(onLogout)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                HeaderSection(
                    showBrand = selectedTab != 0,
                    viewModel = monitoringViewModel
                )
                Spacer(modifier = Modifier.height(if (selectedTab != 0) 16.dp else 8.dp))
                
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MainContent",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> MonitoreoUnidadesView(monitoringViewModel)
                        1 -> StatisticsView(monitoringViewModel, onSessionExpired = { monitoringViewModel.logout(onLogout) })
                        2 -> ServicesView(monitoringViewModel) { selectedFolio = it }
                        3 -> AgendaView(monitoringViewModel)
                        4 -> SCIView(monitoringViewModel) { selectedSCI = it }
                        5 -> VehiclesControlView(monitoringViewModel, enabled = !isOverlayOpen) { selectedVehicle = it }
                        6 -> PersonalView(monitoringViewModel)
                        else -> PlaceholderView("Módulo no disponible")
                    }
                }
            }
        }
        
        // Modales: el foco queda atrapado hasta pulsar Cerrar
        if (selectedVehicle != null) {
            VehicleDetailOverlay(selectedVehicle!!, monitoringViewModel) { selectedVehicle = null }
        }
        
        if (selectedSCI != null) {
            SCIDetailOverlay(
                sci = selectedSCI!!,
                viewModel = monitoringViewModel
            ) { selectedSCI = null }
        }
        
        if (selectedFolio != null) {
            ServiceDetailOverlay(
                folio = selectedFolio!!,
                viewModel = monitoringViewModel
            ) { selectedFolio = null }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = HudColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SidebarIcon(icon: ImageVector, label: String, selected: Boolean, enabled: Boolean, color: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        enabled = enabled, 
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f), 
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else Color.Transparent, 
            focusedContainerColor = HudColors.BgCardHover
        ), 
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).width(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(20.dp), 
                tint = color ?: (if (selected) HudColors.AccentPrimary else HudColors.TextMuted)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = color ?: (if (selected) HudColors.TextPrimary else HudColors.TextMuted),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 8.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun HeaderSection(
    showBrand: Boolean = true,
    viewModel: MonitoringViewModel
) {
    val shiftsResource by viewModel.workShifts.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkShifts() }

    // Recalcular cada minuto por si cambia el turno
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tick = System.currentTimeMillis()
        }
    }

    val guardLabel = remember(shiftsResource, tick) {
        val shifts = (shiftsResource as? Resource.Success)?.data.orEmpty()
        CurrentGuardResolver.displayLabel(CurrentGuardResolver.resolve(shifts, tick))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (showBrand) {
            Text(
                "SISTEMA DE MONITOREO • ROR_IT",
                style = MaterialTheme.typography.titleMedium,
                color = HudColors.AccentPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        Surface(
            onClick = { },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = HudColors.BgCard)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_bomberos),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.size(8.dp).background(HudColors.Green, CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "GUARDIA ACTUAL",
                        color = HudColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        guardLabel,
                        color = HudColors.Green,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PlaceholderView(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
        Text(text.uppercase(), color = HudColors.TextMuted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) 
    }
}

@Composable
fun CategoryStatItem(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), style = MaterialTheme.typography.bodyMedium, color = HudColors.TextSecondary, fontWeight = FontWeight.Bold)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = HudColors.Amber)
        }
        HorizontalDivider(color = HudColors.BorderSubtle, thickness = 1.dp)
    }
}

@Composable
fun LoadingSpinner(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = HudColors.AccentPrimary, strokeWidth = 5.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text.uppercase(), color = HudColors.TextMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit, onAction: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(550.dp)) {
            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color.Red, modifier = Modifier.size(70.dp))
            Spacer(modifier = Modifier.height(20.dp))
            val isAuthError = message.contains("401") || message.lowercase().contains("expired")
            Text(text = if (isAuthError) "AUTORIZACIÓN RECHAZADA" else "FALLO DE SISTEMA", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text(text = if (isAuthError) "Su sesión ha expirado por seguridad. Reautentique." else message, color = HudColors.TextMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button(
                onClick = if (isAuthError) onAction else onRetry,
                colors = ButtonDefaults.colors(containerColor = if (isAuthError) HudColors.AccentPrimary else Color.Gray)
            ) { 
                Text(if (isAuthError) "REGRESAR AL LOGIN" else "REINTENTAR ACCESO", fontWeight = FontWeight.Black)
            }
        }
    }
}
