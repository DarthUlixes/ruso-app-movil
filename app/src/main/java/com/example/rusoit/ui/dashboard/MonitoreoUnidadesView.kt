@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.rusoit.data.model.UnitOnService
import com.example.rusoit.data.model.User
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private enum class MonitorFilter(val label: String) {
    ALL("Todas"),
    ON_SERVICE("En curso"),
    FREE("Libres")
}

/**
 * Monitoreo de unidades.
 * Ficha completa enfocable (como Partes): OK abre/cierra VER MÁS.
 * Sin botón anidado — en TV el Card padre se comía el foco.
 */
@Composable
fun MonitoreoUnidadesView(viewModel: MonitoringViewModel) {
    val resource by viewModel.unitsOnService.collectAsState()
    val personnelResource by viewModel.personnel.collectAsState()
    val colognesResource by viewModel.colognes.collectAsState()
    var filter by remember { mutableStateOf(MonitorFilter.ALL) }
    var focusedUnitKey by remember { mutableStateOf<String?>(null) }
    var expandedKeys by remember { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadPersonnel()
        viewModel.loadColognes()
        viewModel.loadUnitsOnService(silent = false)
        while (true) {
            delay(15_000)
            viewModel.loadUnitsOnService(silent = true)
        }
    }

    val userById = remember(personnelResource) {
        (personnelResource as? Resource.Success)?.data
            ?.associateBy { it.id }
            .orEmpty()
    }
    val cologneById = remember(colognesResource) {
        (colognesResource as? Resource.Success)?.data
            ?.filter { it.id != null }
            ?.associate { it.id!! to (it.name ?: "Colonia ${it.id}") }
            .orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// MONITOREO · UNIDADES Y SERVICIOS",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Monitoreo de unidades",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Selecciona la ficha y pulsa OK para VER MÁS / VER MENOS",
            style = MaterialTheme.typography.bodyMedium,
            color = HudColors.TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        val units = (resource as? Resource.Success)?.data.orEmpty()
        val onServiceCount = units.count { it.isOnService() }
        val free = units.size - onServiceCount

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonitorFilter.entries.forEach { f ->
                val count = when (f) {
                    MonitorFilter.ALL -> units.size
                    MonitorFilter.ON_SERVICE -> onServiceCount
                    MonitorFilter.FREE -> free
                }
                Surface(
                    onClick = { filter = f },
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (filter == f) HudColors.AccentGlow else HudColors.BgCard,
                        focusedContainerColor = HudColors.BgCardHover
                    )
                ) {
                    Text(
                        "${f.label.uppercase(Locale.getDefault())} ($count)",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = when {
                            filter == f && f == MonitorFilter.ON_SERVICE -> Color(0xFFE8C86A)
                            filter == f && f == MonitorFilter.FREE -> HudColors.Green
                            filter == f -> HudColors.Amber
                            else -> HudColors.TextMuted
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                onClick = { viewModel.loadUnitsOnService(silent = true) },
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = HudColors.BgCard,
                    focusedContainerColor = HudColors.BgCardHover
                )
            ) {
                Text(
                    "ACTUALIZAR",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = HudColors.AccentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            (resource is Resource.Loading || resource == null) && units.isEmpty() -> {
                LoadingSpinner("Cargando monitoreo de unidades...")
            }
            resource is Resource.Error && units.isEmpty() -> {
                ErrorMessage(
                    resource?.message ?: "Error al cargar /work-force/on-service-by-date-and-workshift",
                    onRetry = { viewModel.loadUnitsOnService(silent = false) }
                )
            }
            else -> {
                val filtered = when (filter) {
                    MonitorFilter.ALL -> units
                    MonitorFilter.ON_SERVICE -> units.filter { it.isOnService() }
                    MonitorFilter.FREE -> units.filter { !it.isOnService() }
                }
                if (filtered.isEmpty()) {
                    PlaceholderView(
                        when (filter) {
                            MonitorFilter.ON_SERVICE -> "Ninguna unidad en servicio ahora"
                            MonitorFilter.FREE -> "No hay unidades libres en el turno"
                            MonitorFilter.ALL -> "Sin unidades en estado de fuerza hoy"
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = filtered, key = { unitKey(it) }) { unit ->
                            val key = unitKey(unit)
                            UnitMonitorCard(
                                unit = unit,
                                userById = userById,
                                cologneById = cologneById,
                                selected = focusedUnitKey == key,
                                expanded = key in expandedKeys,
                                onFocused = { focusedUnitKey = key },
                                onToggleMore = {
                                    expandedKeys = if (key in expandedKeys) {
                                        expandedKeys - key
                                    } else {
                                        expandedKeys + key
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun unitKey(unit: UnitOnService): String =
    "${unit.work_force?.id ?: 0}-${unit.number_unit ?: unit.vehicle?.id ?: 0}"

@Composable
private fun UnitMonitorCard(
    unit: UnitOnService,
    userById: Map<Int, User>,
    cologneById: Map<Int, String>,
    selected: Boolean,
    expanded: Boolean,
    onFocused: () -> Unit,
    onToggleMore: () -> Unit
) {
    val onService = unit.isOnService()
    val accent = if (onService) Color(0xFFE8C86A) else HudColors.Green
    val statusLabel = if (onService) "EN CURSO" else "LIBRE"
    val service = unit.service
    val vehicle = unit.vehicle

    val crewLabel = remember(unit.work_force?.personal, userById) {
        val personal = unit.work_force?.personal.orEmpty()
        if (personal.isEmpty()) {
            "Sin tripulación"
        } else {
            personal.joinToString(" · ") { member ->
                val name = member.id_user?.let { id ->
                    userById[id]?.let { u ->
                        listOfNotNull(u.first_name, u.last_name).joinToString(" ").ifBlank { null }
                    }
                } ?: "Usuario ${member.id_user ?: "?"}"
                val pos = member.position_on_work_force?.takeIf { it.isNotBlank() }
                if (pos != null) "$name ($pos)" else name
            }
        }
    }

    // Una sola superficie enfocable (patrón Partes de Atención)
    Card(
        onClick = {
            onFocused()
            onToggleMore()
        },
        scale = CardDefaults.scale(focusedScale = 1.02f, pressedScale = 1f),
        colors = CardDefaults.colors(
            containerColor = if (selected) accent.copy(alpha = 0.16f) else accent.copy(alpha = 0.10f),
            focusedContainerColor = accent.copy(alpha = 0.22f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(70.dp)
                    .align(Alignment.Top)
                    .padding(start = 8.dp, top = 10.dp)
                    .background(HudColors.BgPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val cover = vehicle?.cover_img
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp, end = 12.dp, bottom = 10.dp, start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        unit.hubName().uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            statusLabel,
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                MonitorInfoCell(
                    "Tripulantes (${unit.crewCount()})",
                    crewLabel,
                    Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                if (onService && service != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "SERVICIO · Folio ${service.displayFolio()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonitorInfoCell(
                            "Tipo",
                            service.type_service?.name?.takeIf { it.isNotBlank() }
                                ?: service.id_type_service?.let { "Tipo #$it" }
                                ?: "Sin dato",
                            Modifier.weight(1.2f)
                        )
                        MonitorInfoCell(
                            "Colonia",
                            service.id_cologne?.let { cologneById[it] ?: "Colonia #$it" }
                                ?: "Sin dato",
                            Modifier.weight(1.2f)
                        )
                        MonitorInfoCell(
                            "Fecha / hora",
                            listOf(service.displayDate(), service.displayTime())
                                .filter { it != "—" }
                                .joinToString(" · ")
                                .ifBlank { "Sin dato" },
                            Modifier.weight(1.3f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonitorInfoCell("Calle", service.displayOrDash(service.stret), Modifier.weight(1f))
                        MonitorInfoCell("Cruce", service.displayOrDash(service.crossing), Modifier.weight(1f))
                        MonitorInfoCell("Reportante", service.displayOrDash(service.reporter), Modifier.weight(1f))
                        MonitorInfoCell(
                            "Tel.",
                            service.displayOrDash(service.phone_reporter),
                            Modifier.weight(0.9f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Disponible · sin servicio activo",
                        color = HudColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DATOS DE UNIDAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudColors.Amber,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonitorInfoCell(
                            "Unidad",
                            unit.number_unit?.toString() ?: "—",
                            Modifier.weight(1f)
                        )
                        MonitorInfoCell(
                            "Marca / modelo",
                            listOfNotNull(vehicle?.brandName(), vehicle?.model)
                                .joinToString(" ")
                                .ifBlank { "—" },
                            Modifier.weight(1.5f)
                        )
                        MonitorInfoCell(
                            "Placas",
                            vehicle?.vehicle_license_plates ?: "—",
                            Modifier.weight(1.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Indicador (no es botón anidado): OK sobre la ficha hace el toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (expanded) HudColors.Amber.copy(alpha = 0.22f) else HudColors.BgCard,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (expanded) "VER MENOS" else "VER MÁS",
                        color = HudColors.Amber,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorInfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted, fontSize = 10.sp)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
