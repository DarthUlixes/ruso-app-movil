@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.rusoit.data.model.Vehicle
import com.example.rusoit.data.model.VehicleTypeInventory
import com.example.rusoit.ui.components.FocusTrappedModal
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.util.Locale

private enum class VehicleStatusFilter(val label: String) {
    ALL("Todos"),
    OPERATIVE("Operativas"),
    WORKSHOP("Taller"),
    INOPERATIVE("In-Operativas")
}

/**
 * Control de Vehículos — flujo web:
 * GET /type-to-vehicles → GET /vehicle (filtrado por id_type) → ficha.
 */
@Composable
fun VehiclesControlView(
    viewModel: MonitoringViewModel,
    enabled: Boolean,
    onVehicleClick: (Vehicle) -> Unit
) {
    var selectedType by remember { mutableStateOf<VehicleTypeInventory?>(null) }

    if (selectedType == null) {
        VehicleTypesScreen(
            viewModel = viewModel,
            onTypeClick = { selectedType = it }
        )
    } else {
        VehiclesByTypeScreen(
            viewModel = viewModel,
            inventory = selectedType!!,
            enabled = enabled,
            onBack = {
                viewModel.clearVehiclesByType()
                selectedType = null
            },
            onVehicleClick = onVehicleClick
        )
    }
}

@Composable
private fun VehicleTypesScreen(
    viewModel: MonitoringViewModel,
    onTypeClick: (VehicleTypeInventory) -> Unit
) {
    val inventoryResource by viewModel.vehicleTypesInventory.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadVehicleTypesInventory() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// CONTROL DE VEHÍCULOS",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Tipos de unidades",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            inventoryResource is Resource.Loading || inventoryResource == null -> {
                LoadingSpinner("Cargando tipos de vehículos...")
            }
            inventoryResource is Resource.Error -> {
                ErrorMessage(
                    inventoryResource?.message ?: "Error al cargar tipos",
                    onRetry = { viewModel.loadVehicleTypesInventory() }
                )
            }
            else -> {
                val types = inventoryResource?.data.orEmpty()
                if (types.isEmpty()) {
                    PlaceholderView("Sin tipos de vehículo registrados")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(types, key = { it.type.id ?: it.type.type.hashCode() }) { item ->
                            VehicleTypeCard(item = item, onClick = { onTypeClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleTypeCard(item: VehicleTypeInventory, onClick: () -> Unit) {
    val type = item.type
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard),
        modifier = Modifier.fillMaxWidth().height(230.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                type.type ?: "Sin tipo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(HudColors.BgPrimary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!type.image.isNullOrBlank()) {
                    AsyncImage(
                        model = type.image,
                        contentDescription = type.type,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Sin imagen", color = HudColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Operativas: ${item.operativeCount}",
                color = HudColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                "Taller: ${item.workshopCount} · In-op: ${item.inoperativeCount}",
                color = HudColors.Amber,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                "Total: ${item.totalCount}",
                color = HudColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VehiclesByTypeScreen(
    viewModel: MonitoringViewModel,
    inventory: VehicleTypeInventory,
    enabled: Boolean,
    onBack: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit
) {
    val vehiclesResource by viewModel.vehiclesByType.collectAsState()
    val typeId = inventory.type.id
    var statusFilter by remember { mutableStateOf(VehicleStatusFilter.ALL) }

    LaunchedEffect(typeId) {
        if (typeId != null) viewModel.loadVehiclesByType(typeId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "// CONTROL DE VEHÍCULOS · ${inventory.type.type?.uppercase(Locale.getDefault()) ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    inventory.type.type ?: "Unidades",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                onClick = onBack,
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = HudColors.BgCard,
                    focusedContainerColor = HudColors.BgCardHover
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = HudColors.AccentPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VOLVER A TIPOS", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VehicleStatusFilter.entries.forEach { filter ->
                val selected = statusFilter == filter
                Surface(
                    onClick = { statusFilter = filter },
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) HudColors.AccentGlow else HudColors.BgCard,
                        focusedContainerColor = HudColors.BgCardHover
                    )
                ) {
                    Text(
                        filter.label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) HudColors.AccentPrimary else HudColors.TextMuted,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                typeId == null -> PlaceholderView("Tipo sin id válido")
                vehiclesResource is Resource.Loading || vehiclesResource == null -> {
                    if (inventory.vehicles.isNotEmpty()) {
                        VehiclesGrid(
                            vehicles = inventory.vehicles.filterByStatus(statusFilter),
                            enabled = enabled,
                            onVehicleClick = onVehicleClick
                        )
                    } else {
                        LoadingSpinner("Cargando unidades...")
                    }
                }
                vehiclesResource is Resource.Error -> {
                    if (inventory.vehicles.isNotEmpty()) {
                        VehiclesGrid(
                            vehicles = inventory.vehicles.filterByStatus(statusFilter),
                            enabled = enabled,
                            onVehicleClick = onVehicleClick
                        )
                    } else {
                        ErrorMessage(
                            vehiclesResource?.message ?: "Error al cargar unidades",
                            onRetry = { viewModel.loadVehiclesByType(typeId) }
                        )
                    }
                }
                else -> {
                    val all = vehiclesResource?.data.orEmpty().ifEmpty { inventory.vehicles }
                    val filtered = all.filterByStatus(statusFilter)
                    if (filtered.isEmpty()) {
                        PlaceholderView("Sin unidades con ese filtro")
                    } else {
                        VehiclesGrid(
                            vehicles = filtered,
                            enabled = enabled,
                            onVehicleClick = onVehicleClick
                        )
                    }
                }
            }
        }
    }
}

private fun List<Vehicle>.filterByStatus(filter: VehicleStatusFilter): List<Vehicle> {
    return when (filter) {
        VehicleStatusFilter.ALL -> this
        VehicleStatusFilter.OPERATIVE -> filter { it.isOperative() }
        VehicleStatusFilter.WORKSHOP -> filter { it.isWorkshop() }
        VehicleStatusFilter.INOPERATIVE -> filter { !it.isOperative() && !it.isWorkshop() }
    }
}

@Composable
private fun VehiclesGrid(
    vehicles: List<Vehicle>,
    enabled: Boolean,
    onVehicleClick: (Vehicle) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(vehicles, key = { it.id ?: it.number_unit ?: it.hashCode() }) { vehicle ->
            VehicleUnitCard(vehicle = vehicle) {
                if (enabled) onVehicleClick(vehicle)
            }
        }
    }
}

@Composable
private fun VehicleUnitCard(vehicle: Vehicle, onClick: () -> Unit) {
    val status = vehicle.status?.lowercase() ?: "desconocido"
    val statusColor = when {
        vehicle.isOperative() -> HudColors.Green
        vehicle.isWorkshop() -> HudColors.Amber
        else -> Color.Red
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard),
        modifier = Modifier.fillMaxWidth().height(210.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                vehicle.displayName(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(HudColors.BgPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!vehicle.cover_img.isNullOrBlank()) {
                    AsyncImage(
                        model = vehicle.cover_img,
                        contentDescription = vehicle.displayName(),
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Sin imagen", color = HudColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${vehicle.brandName() ?: "Marca"} · ${vehicle.model ?: "Modelo"}",
                color = HudColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                status.uppercase(Locale.getDefault()),
                color = statusColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun VehicleDetailOverlay(
    vehicle: Vehicle,
    viewModel: MonitoringViewModel,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val detailResource by viewModel.vehicleDetail.collectAsState()
    val numberUnit = vehicle.number_unit

    LaunchedEffect(numberUnit) {
        if (numberUnit != null) viewModel.loadVehicleDetail(numberUnit)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearVehicleDetail() }
    }

    val detail = (detailResource as? Resource.Success)?.data ?: vehicle
    val statusColor = when {
        detail.isOperative() -> HudColors.Green
        detail.isWorkshop() -> HudColors.Amber
        else -> Color.Red
    }

    FocusTrappedModal(
        onDismiss = onDismiss,
        scrimAlpha = 0.85f,
        initialFocusRequester = focusRequester
    ) {
        Card(
            onClick = {},
            modifier = Modifier
                .width(680.dp)
                .heightIn(max = 560.dp),
            colors = CardDefaults.colors(containerColor = HudColors.BgCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(36.dp)
                    .verticalScroll(scrollState)
                    .focusable()
            ) {
                Text(
                    "DETALLE TÉCNICO DE UNIDAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    detail.displayName(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (!detail.cover_img.isNullOrBlank()) {
                    AsyncImage(
                        model = detail.cover_img,
                        contentDescription = detail.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(HudColors.BgPrimary, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                DetailRow("MARCA / MODELO:", "${detail.brandName() ?: "N/A"} ${detail.model ?: ""}")
                DetailRow("PLACAS OPERATIVAS:", detail.vehicle_license_plates ?: "SIN PLACAS")
                DetailRow("KILOMETRAJE TOTAL:", "${detail.kilometers?.toInt() ?: 0} KM")
                if (detail.kilometers_to_service != null) {
                    DetailRow("KM A SERVICIO:", "${detail.kilometers_to_service.toInt()} KM")
                }
                DetailRow("CATEGORÍA:", detail.type ?: "UNIDAD DE RESPUESTA")

                Spacer(modifier = Modifier.height(20.dp))
                Text("ESTADO MECÁNICO:", style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
                Text(
                    detail.status?.uppercase(Locale.getDefault()) ?: "DESCONOCIDO",
                    color = statusColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).focusRequester(focusRequester),
                    colors = ButtonDefaults.colors(containerColor = HudColors.AccentPrimary)
                ) {
                    Text("CERRAR FICHA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
