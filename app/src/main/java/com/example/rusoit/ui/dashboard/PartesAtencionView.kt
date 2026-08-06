@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.rusoit.data.model.*
import com.example.rusoit.ui.components.FocusTrappedModal
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.utils.ServiceSummaryBuilder
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.util.Locale

private enum class ParteDetailTab { GENERALES, TRIPULACION, RESUMEN }

/**
 * Consulta TV de Partes de Atención (espejo web /sumarys + /sumary-service/:id).
 */
@Composable
fun ServicesView(viewModel: MonitoringViewModel, onFolioClick: (Folio) -> Unit) {
    val catalogResource by viewModel.partesCatalog.collectAsState()
    var folioQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) } // null = todos (excepto en curso)
    var typeFilter by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPartesCatalog() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// PARTES DE ATENCIÓN",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Consulta de Partes de Atención",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))

        when {
            catalogResource is Resource.Loading || catalogResource == null -> {
                LoadingSpinner("Cargando partes de atención...")
            }
            catalogResource is Resource.Error -> {
                ErrorMessage(
                    catalogResource?.message ?: "Error al cargar partes",
                    onRetry = { viewModel.loadPartesCatalog() }
                )
            }
            else -> {
                val catalog = catalogResource?.data ?: PartesCatalog()
                val lookups = remember(catalog) { PartesLookups.from(catalog) }

                val filtered = remember(catalog.folios, folioQuery, statusFilter, typeFilter) {
                    catalog.folios
                        .asSequence()
                        .filter { it.status?.lowercase(Locale.getDefault()) != "en curso" }
                        .filter { folio ->
                            val q = folioQuery.trim()
                            if (q.isEmpty()) true
                            else {
                                val idStr = folio.id.toString()
                                val folioStr = folio.folio.orEmpty()
                                if (q.startsWith("0")) idStr == q.trimStart('0').ifEmpty { "0" } || idStr == q
                                else idStr.startsWith(q) || folioStr.contains(q, ignoreCase = true)
                            }
                        }
                        .filter { folio ->
                            statusFilter == null ||
                                folio.status?.equals(statusFilter, ignoreCase = true) == true
                        }
                        .filter { folio ->
                            typeFilter == null || folio.id_type_service == typeFilter
                        }
                        .toList()
                }

                // Filtros TV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = folioQuery,
                        onValueChange = { folioQuery = it },
                        modifier = Modifier.width(220.dp),
                        singleLine = true,
                        label = { Text("Buscar folio", color = HudColors.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HudColors.AccentPrimary,
                            unfocusedBorderColor = HudColors.BorderSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = HudColors.AccentPrimary
                        )
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChipTv("Todos", statusFilter == null) { statusFilter = null }
                        }
                        item {
                            FilterChipTv("Cerrado", statusFilter == "cerrado") { statusFilter = "cerrado" }
                        }
                        item {
                            FilterChipTv("Falsa alarma", statusFilter == "falsa alarma") {
                                statusFilter = "falsa alarma"
                            }
                        }
                        item {
                            FilterChipTv("Cancelado", statusFilter == "cancelado por usuario") {
                                statusFilter = "cancelado por usuario"
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (catalog.typeServices.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChipTv("Tipo: todos", typeFilter == null) { typeFilter = null }
                        }
                        items(catalog.typeServices.take(12)) { type ->
                            val id = type.id ?: return@items
                            FilterChipTv(
                                label = (type.name ?: "Tipo $id").uppercase(Locale.getDefault()),
                                selected = typeFilter == id
                            ) { typeFilter = if (typeFilter == id) null else id }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    "${filtered.size} partes",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    PlaceholderView("Sin partes con esos filtros")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { folio ->
                            ParteListCard(
                                folio = folio,
                                lookups = lookups,
                                onClick = { onFolioClick(folio) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipTv(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else HudColors.BgCard,
            focusedContainerColor = HudColors.BgCardHover
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (selected) HudColors.AccentPrimary else HudColors.TextSecondary,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ParteListCard(folio: Folio, lookups: PartesLookups, onClick: () -> Unit) {
    val status = folio.status?.lowercase(Locale.getDefault()).orEmpty()
    val statusColor = when (status) {
        "cerrado" -> HudColors.Green
        "falsa alarma" -> HudColors.Amber
        "cancelado por usuario" -> HudColors.AccentPrimary
        else -> HudColors.Blue
    }
    val closeType = when (status) {
        "falsa alarma", "cancelado por usuario", "" -> "Sin Efecto"
        else -> folio.close_type ?: "—"
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FOLIO: ${folio.id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = HudColors.AccentPrimary
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        folio.status?.uppercase(Locale.getDefault()) ?: "S/E",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                InfoCell("Tipo de servicio", lookups.typeName(folio.id_type_service), Modifier.weight(1f))
                InfoCell("Unidad", lookups.unitNumber(folio.vehicle_id), Modifier.weight(1f))
                InfoCell("Colonia", lookups.cologneName(folio.id_cologne), Modifier.weight(1f))
                InfoCell("Fecha", folio.date_to_open?.take(10) ?: "—", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                InfoCell("Operador", lookups.operatorName(folio), Modifier.weight(1f))
                InfoCell("Hora", formatTime(folio.time_to_open), Modifier.weight(1f))
                InfoCell("Tipo de cierre", closeType, Modifier.weight(1f))
                InfoCell("Calle", folio.stret ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ServiceDetailOverlay(
    folio: Folio,
    viewModel: MonitoringViewModel,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val detailResource by viewModel.folioDetail.collectAsState()
    val catalogResource by viewModel.partesCatalog.collectAsState()
    var tab by remember { mutableStateOf(ParteDetailTab.GENERALES) }

    LaunchedEffect(folio.id) {
        viewModel.loadFolioDetail(folio.id)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearFolioDetail() }
    }

    val catalog = catalogResource?.data ?: PartesCatalog()
    val lookups = remember(catalog) { PartesLookups.from(catalog) }
    val current = when (detailResource) {
        is Resource.Success -> detailResource?.data ?: folio
        else -> folio
    }

    FocusTrappedModal(
        scrimAlpha = 0.9f,
        initialFocusRequester = focusRequester
    ) {
        Card(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.colors(containerColor = HudColors.BgCard)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "// PARTES DE ATENCIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudColors.AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Folio: ${current.id}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(containerColor = HudColors.AccentPrimary)
                    ) {
                        Text("CERRAR", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailTabChip("Generales", tab == ParteDetailTab.GENERALES) {
                        tab = ParteDetailTab.GENERALES
                    }
                    DetailTabChip("Tripulación", tab == ParteDetailTab.TRIPULACION) {
                        tab = ParteDetailTab.TRIPULACION
                    }
                    DetailTabChip("Resumen", tab == ParteDetailTab.RESUMEN) {
                        tab = ParteDetailTab.RESUMEN
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (detailResource is Resource.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HudColors.AccentPrimary)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(22.dp)
                    ) {
                        when (tab) {
                            ParteDetailTab.GENERALES -> GeneralesTab(current, lookups)
                            ParteDetailTab.TRIPULACION -> TripulacionTab(current, lookups)
                            ParteDetailTab.RESUMEN -> ResumenTab(current)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else Color.Transparent,
            focusedContainerColor = HudColors.BgCardHover
        )
    ) {
        Text(
            label.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) HudColors.AccentPrimary else HudColors.TextMuted,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun GeneralesTab(folio: Folio, lookups: PartesLookups) {
    val status = folio.status?.lowercase(Locale.getDefault()).orEmpty()
    val closeType = when (status) {
        "falsa alarma", "cancelado por usuario", "" -> "Sin Efecto"
        else -> folio.close_type ?: "—"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailField("Tipo de cierre", closeType, Modifier.weight(1f))
            DetailField("Reportante", folio.reporter ?: "—", Modifier.weight(1f))
            DetailField("Celular", folio.phone_reporter ?: "—", Modifier.weight(1f))
            DetailField("Estatus", folio.status?.uppercase(Locale.getDefault()) ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailField("Colonia", lookups.cologneName(folio.id_cologne), Modifier.weight(1f))
            DetailField("Calle", folio.stret ?: "—", Modifier.weight(1f))
            DetailField("Cruce", folio.crossing ?: "—", Modifier.weight(1f))
            DetailField("Tipo servicio", lookups.typeName(folio.id_type_service), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailField(
                "Fecha de apertura",
                "${folio.date_to_open?.take(10) ?: "—"} · ${formatTime(folio.time_to_open)}",
                Modifier.weight(1f)
            )
            DetailField(
                "Fecha de cierre",
                "${folio.date_to_close?.take(10) ?: "—"} · ${formatTime(folio.time_to_close)}",
                Modifier.weight(1f)
            )
        }
        DetailField("Generales y conclusión", folio.general_and_conclusion ?: "—", Modifier.fillMaxWidth())
    }
}

@Composable
private fun TripulacionTab(folio: Folio, lookups: PartesLookups) {
    val crew = folio.personal_on_a_service.orEmpty()
    val operator = lookups.operatorName(folio)
    val assistants = crew
        .filter {
            val pos = it.position_on_service?.lowercase(Locale.getDefault()).orEmpty()
            !pos.contains("chofer") && !pos.contains("conductor")
        }
        .map { lookups.crewMemberName(it) }
        .filter { it != "—" }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailField("Operador", operator, Modifier.weight(1f))
            DetailField("Unidad", lookups.unitNumber(folio.vehicle_id), Modifier.weight(1f))
        }
        DetailField(
            "Tripulación",
            if (assistants.isEmpty()) "Sin tripulación registrada" else assistants.joinToString("\n"),
            Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DetailField("Km inicio", folio.kilometers?.toString() ?: "—", Modifier.weight(1f))
            DetailField("Km fin", folio.close_kilometers?.toString() ?: "—", Modifier.weight(1f))
            DetailField("Km recorrido", folio.kilometers_traveled?.toString() ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumenTab(folio: Folio) {
    val text = when {
        !folio.summary.isNullOrBlank() -> folio.summary
        else -> ServiceSummaryBuilder.build(folio)
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            lineHeight = 30.sp
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return when {
        raw.length >= 19 -> raw.substring(11, 19)
        raw.length >= 8 && raw.contains(":") -> raw.takeLast(8)
        else -> raw
    }
}

private class PartesLookups(
    private val types: Map<Int, String>,
    private val units: Map<Int, String>,
    private val colognes: Map<Int, String>,
    private val users: Map<Int, String>
) {
    fun typeName(id: Int?): String =
        id?.let { types[it] }?.uppercase(Locale.getDefault()) ?: "Sin tipo"

    fun unitNumber(vehicleId: Int?): String =
        vehicleId?.let { units[it] }?.let { "U-$it" } ?: "Sin unidad"

    fun cologneName(id: Int?): String =
        id?.let { colognes[it] } ?: "Sin colonia"

    fun crewMemberName(crew: ServiceCrew): String {
        val nested = listOfNotNull(crew.users?.first_name, crew.users?.last_name)
            .joinToString(" ")
            .trim()
        if (nested.isNotEmpty()) return nested
        val fromMap = crew.id_user?.let { users[it] }
        return fromMap?.ifBlank { null } ?: "—"
    }

    fun operatorName(folio: Folio): String {
        val crew = folio.personal_on_a_service.orEmpty()
        val chofer = crew.find {
            val pos = it.position_on_service?.lowercase(Locale.getDefault()).orEmpty()
            pos.contains("chofer") || pos.contains("conductor")
        }
        return chofer?.let { crewMemberName(it) } ?: "Sin operador"
    }

    companion object {
        fun from(catalog: PartesCatalog): PartesLookups {
            val types = catalog.typeServices
                .mapNotNull { t -> t.id?.let { it to (t.name ?: "Tipo $it") } }
                .toMap()
            val units = catalog.vehicles
                .mapNotNull { v ->
                    val id = v.id ?: return@mapNotNull null
                    id to (v.number_unit?.toString() ?: id.toString())
                }
                .toMap()
            val colognes = catalog.colognes
                .mapNotNull { c -> c.id?.let { it to (c.name ?: "Colonia $it") } }
                .toMap()
            val users = catalog.users.associate { u ->
                u.id to listOfNotNull(u.first_name, u.last_name).joinToString(" ").trim()
            }
            return PartesLookups(types, units, colognes, users)
        }
    }
}
