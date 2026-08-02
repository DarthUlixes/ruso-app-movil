package com.example.rusoit.ui.dashboard

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.example.rusoit.ui.components.PersonnelCard
import com.example.rusoit.ui.components.VehicleCard
import com.example.rusoit.viewmodel.MonitoringViewModel
import com.example.rusoit.viewmodel.ViewModelFactory
import com.example.rusoit.utils.Resource
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.data.api.RetrofitInstance
import com.example.rusoit.data.model.User
import com.example.rusoit.data.model.Tool
import com.example.rusoit.data.model.Folio

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DashboardScreen(
    factory: ViewModelFactory,
    onLogout: () -> Unit
) {
    val monitoringViewModel: MonitoringViewModel = viewModel(factory = factory)
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedVehicle by remember { mutableStateOf<com.example.rusoit.data.model.Vehicle?>(null) }
    
    val isOverlayOpen = selectedVehicle != null

    Box(modifier = Modifier.fillMaxSize().background(HudColors.BgPrimary)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar HUD con los 5 módulos + Extras
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(HudColors.BgNav)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SidebarIcon(Icons.Default.BarChart, selectedTab == 0, enabled = !isOverlayOpen) { selectedTab = 0 } // CAT
                SidebarIcon(Icons.AutoMirrored.Filled.List, selectedTab == 1, enabled = !isOverlayOpen) { selectedTab = 1 } // SERVICIOS
                SidebarIcon(Icons.Default.CalendarMonth, selectedTab == 2, enabled = !isOverlayOpen) { selectedTab = 2 } // TURNOS
                SidebarIcon(Icons.Default.Shield, selectedTab == 3, enabled = !isOverlayOpen) { selectedTab = 3 } // SCI
                SidebarIcon(Icons.Default.Build, selectedTab == 4, enabled = !isOverlayOpen) { selectedTab = 4 } // INVENTARIO
                
                Spacer(modifier = Modifier.weight(1f))
                
                SidebarIcon(Icons.Default.FireTruck, selectedTab == 5, enabled = !isOverlayOpen) { selectedTab = 5 } // UNIDADES
                SidebarIcon(Icons.Default.Person, selectedTab == 6, enabled = !isOverlayOpen) { selectedTab = 6 } // PERSONAL
                
                SidebarIcon(Icons.AutoMirrored.Filled.ExitToApp, false, enabled = !isOverlayOpen) {
                    monitoringViewModel.logout(onLogout)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                HeaderSection(enabled = !isOverlayOpen)
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MainContent",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> StatisticsView(monitoringViewModel, onSessionExpired = { monitoringViewModel.logout(onLogout) })
                        1 -> ServicesView(monitoringViewModel)
                        2 -> CalendarEventsView(monitoringViewModel)
                        3 -> SCIView(monitoringViewModel)
                        4 -> InventoryView(monitoringViewModel)
                        5 -> VehiclesView(monitoringViewModel, enabled = !isOverlayOpen) { selectedVehicle = it }
                        6 -> PersonnelView(monitoringViewModel)
                        else -> PlaceholderView("Módulo en desarrollo")
                    }
                }
            }
        }
        
        if (isOverlayOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).zIndex(10f)) {
                if (selectedVehicle != null) VehicleDetailOverlay(selectedVehicle!!) { selectedVehicle = null }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatisticsView(viewModel: MonitoringViewModel, onSessionExpired: () -> Unit) {
    val statusStats by viewModel.statusStats.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadStatusStats()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// ESTADÍSTICAS OPERATIVAS (CAT)", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Análisis Consolidado de Servicios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            statusStats is Resource.Loading || statusStats == null -> {
                LoadingSpinner("Consultando base de datos...")
            }
            statusStats is Resource.Error -> {
                ErrorMessage(
                    message = statusStats?.message ?: "Error desconocido", 
                    onRetry = { viewModel.loadStatusStats() },
                    onAction = { if (statusStats?.message?.contains("401") == true) onSessionExpired() }
                )
            }
            else -> {
                val data = statusStats?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("Sin servicios registrados en la base de datos")
                } else {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Card(onClick = {}, colors = CardDefaults.colors(containerColor = HudColors.BgCard), modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("ESTADO DE SERVICIOS", style = MaterialTheme.typography.titleLarge, color = HudColors.AccentPrimary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(data) { stat ->
                                        CategoryStatItem(stat.status ?: stat.name ?: "Otros", stat._count?.status ?: stat._count?.id ?: 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServicesView(viewModel: MonitoringViewModel) {
    val servicesResource by viewModel.services.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadServices() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// REGISTRO DE SERVICIOS (FOLIOS)", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Historial Operativo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            servicesResource is Resource.Loading || servicesResource == null -> { LoadingSpinner("Obteniendo servicios...") }
            servicesResource is Resource.Error -> { ErrorMessage(servicesResource?.message ?: "Error", onRetry = { viewModel.loadServices() }) }
            else -> {
                val data = servicesResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("No hay servicios registrados")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(data) { folio ->
                            Card(onClick = {}, colors = CardDefaults.colors(containerColor = HudColors.BgCard)) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("FOLIO: ${folio.folio ?: "N/A"}", fontWeight = FontWeight.Bold)
                                        Text("Ub: ${folio.stret ?: "Sin calle"} x ${folio.crossing ?: "S/C"}", color = HudColors.TextMuted, fontSize = 12.sp)
                                    }
                                    Box(modifier = Modifier.background(HudColors.AccentGlow, RoundedCornerShape(4.dp)).padding(8.dp)) {
                                        Text(folio.status?.uppercase() ?: "PENDIENTE", color = HudColors.AccentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CalendarEventsView(viewModel: MonitoringViewModel) {
    val shiftsResource by viewModel.workShifts.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkShifts() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// TURNOS DE GUARDIA", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Personal Programado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            shiftsResource is Resource.Loading || shiftsResource == null -> { LoadingSpinner("Obteniendo turnos...") }
            shiftsResource is Resource.Error -> { ErrorMessage(shiftsResource?.message ?: "Error", onRetry = { viewModel.loadWorkShifts() }) }
            else -> {
                val data = shiftsResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("No hay turnos activos")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(data) { shift ->
                            Card(onClick = {}, colors = CardDefaults.colors(containerColor = HudColors.BgCard)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("GUARDIA ${shift.name ?: ""}", style = MaterialTheme.typography.titleMedium, color = HudColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Tipo: ${shift.workday ?: "N/A"}", color = HudColors.Amber, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Op: ${shift.operative_personal ?: 0}", color = HudColors.Green, fontSize = 10.sp)
                                        Text("Inop: ${shift.in_operative_personal ?: 0}", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SCIView(viewModel: MonitoringViewModel) {
    val sciResource by viewModel.sciReports.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadSCIReports() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// SISTEMA DE COMANDO DE INCIDENTES", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Incidentes Críticos Activos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            sciResource is Resource.Loading || sciResource == null -> { LoadingSpinner("Buscando SCIs activos...") }
            sciResource is Resource.Error -> { ErrorMessage(sciResource?.message ?: "Error", onRetry = { viewModel.loadSCIReports() }) }
            else -> {
                val data = sciResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("Sin incidentes SCI en curso")
                } else {
                    LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(data) { report ->
                            Card(onClick = {}, colors = CardDefaults.colors(containerColor = HudColors.BgCard), shape = CardDefaults.shape(RoundedCornerShape(14.dp)), modifier = Modifier.height(160.dp)) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                    Box(modifier = Modifier.background(HudColors.AccentPrimary, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("SCI ACTIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(report.name ?: "Incidente", style = MaterialTheme.typography.titleLarge)
                                    Text(report.ubication ?: "Ubicación pendiente", style = MaterialTheme.typography.bodySmall, color = HudColors.TextMuted)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Status: ${report.status ?: "Activo"}", color = HudColors.Amber, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InventoryView(viewModel: MonitoringViewModel) {
    val toolsResource by viewModel.tools.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadTools() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// INVENTARIO DE EQUIPO Y HERRAMIENTAS", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Estado del Almacén", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            toolsResource is Resource.Loading || toolsResource == null -> { LoadingSpinner("Cargando inventario...") }
            toolsResource is Resource.Error -> { ErrorMessage(toolsResource?.message ?: "Error", onRetry = { viewModel.loadTools() }) }
            else -> {
                val data = toolsResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("No hay herramientas registradas")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(data) { tool ->
                            Card(onClick = {}, colors = CardDefaults.colors(containerColor = HudColors.BgCard)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(tool.name ?: "Herramienta", fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(tool.status ?: "OK", color = HudColors.Green, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PersonnelView(viewModel: MonitoringViewModel) {
    val personnelResource by viewModel.personnel.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPersonnel() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// PERSONAL", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Oficiales y Bomberos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            personnelResource is Resource.Loading || personnelResource == null -> { LoadingSpinner("Cargando personal...") }
            personnelResource is Resource.Error -> { ErrorMessage(personnelResource?.message ?: "Error", onRetry = { viewModel.loadPersonnel() }) }
            else -> {
                val data = personnelResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("Sin personal registrado")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(data) { person -> PersonnelCard(person = person) { } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarIcon(icon: ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.2f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else Color.Transparent,
            focusedContainerColor = HudColors.BgCardHover
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Icon(icon, null, modifier = Modifier.padding(12.dp), tint = if (selected) HudColors.AccentPrimary else HudColors.TextMuted)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeaderSection(enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("ESTACIÓN CENTRAL • RUSOIT", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Monitor Operativo de Comandancia", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
        Surface(onClick = { }, enabled = enabled, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)), colors = ClickableSurfaceDefaults.colors(containerColor = HudColors.BgCard)) {
            Text("GUARDIA ACTIVA: C", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = HudColors.Green, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VehiclesView(viewModel: MonitoringViewModel, enabled: Boolean, onVehicleClick: (com.example.rusoit.data.model.Vehicle) -> Unit) {
    val vehiclesResource by viewModel.vehicles.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadVehicles() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("// UNIDADES", style = MaterialTheme.typography.labelSmall, color = HudColors.AccentSecondary, letterSpacing = 2.sp)
        Text("Flota Operativa", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            vehiclesResource is Resource.Loading || vehiclesResource == null -> { LoadingSpinner("Cargando flota...") }
            vehiclesResource is Resource.Error -> { ErrorMessage(vehiclesResource?.message ?: "Error", onRetry = { viewModel.loadVehicles() }) }
            else -> {
                val data = vehiclesResource?.data ?: emptyList()
                if (data.isEmpty()) {
                    PlaceholderView("No hay vehículos registrados")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(data) { vehicle -> VehicleCard(vehicle = vehicle) { if (enabled) onVehicleClick(vehicle) } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VehicleDetailOverlay(vehicle: com.example.rusoit.data.model.Vehicle, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(onClick = {}, modifier = Modifier.width(600.dp), colors = CardDefaults.colors(containerColor = HudColors.BgCard)) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text("DETALLE DE UNIDAD: U-${vehicle.number_unit ?: "S/N"}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("MODELO: ${vehicle.model ?: "Genérico"} | PLACAS: ${vehicle.vehicle_license_plates ?: "S/P"}", color = HudColors.TextSecondary)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End).focusRequester(focusRequester)) { Text("CERRAR") }
            }
        }
    }
}

@Composable
fun LoadingSpinner(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = HudColors.AccentPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = HudColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit, onAction: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(500.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            val isAuthError = message.contains("401")
            Text(
                text = if (isAuthError) "SESIÓN EXPIRADA" else "ERROR", 
                color = Color.Red, 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = message, 
                color = HudColors.TextMuted, 
                style = MaterialTheme.typography.bodyMedium, 
                modifier = Modifier.padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = if (isAuthError) onAction else onRetry) {
                Text(if (isAuthError) "IR AL LOGIN" else "REINTENTAR")
            }
        }
    }
}

@Composable
fun PlaceholderView(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = HudColors.TextMuted, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
fun CategoryStatItem(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = HudColors.TextSecondary)
            Text(value.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = HudColors.Amber)
        }
        HorizontalDivider(color = HudColors.BorderSubtle, thickness = 0.5.dp)
    }
}
