@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.rusoit.data.model.DataStudioData
import com.example.rusoit.data.model.LabeledCount
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.DataStudioAggregator
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel

private val ChartPalette = listOf(
    Color(0xFFDC2626),
    Color(0xFFF59E0B),
    Color(0xFF22C55E),
    Color(0xFF3B82F6),
    Color(0xFF9467BD),
    Color(0xFFF97316),
    Color(0xFF17BECF),
    Color(0xFFE377C2)
)

@Composable
fun StatisticsView(viewModel: MonitoringViewModel, onSessionExpired: () -> Unit) {
    val dataStudio by viewModel.dataStudio.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()

    var pendingOffset by remember { mutableIntStateOf(weekOffset) }

    LaunchedEffect(Unit) {
        pendingOffset = weekOffset
        viewModel.loadDataStudio(weekOffset)
    }

    val (previewFrom, previewTo) = remember(pendingOffset) {
        DataStudioAggregator.weekRange(pendingOffset)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header compacto + filtro semanal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "// ROR-IT · RESUMEN OPERATIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    "Resumen Operativo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    DataStudioAggregator.periodLabel(previewFrom, previewTo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudColors.Amber,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeekFilterChip(
                    label = "◀",
                    selected = false,
                    onClick = {
                        val next = pendingOffset - 1
                        pendingOffset = next
                        viewModel.loadDataStudio(next)
                    }
                )
                WeekFilterChip(
                    label = DataStudioAggregator.weekTitle(pendingOffset),
                    selected = true,
                    onClick = {
                        pendingOffset = 0
                        viewModel.loadDataStudio(0)
                    }
                )
                WeekFilterChip(
                    label = "▶",
                    selected = false,
                    onClick = {
                        val next = pendingOffset + 1
                        pendingOffset = next
                        viewModel.loadDataStudio(next)
                    }
                )
            }
        }

        when {
            dataStudio is Resource.Loading || dataStudio == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HudColors.AccentPrimary, strokeWidth = 5.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cargando datos operativos...",
                            color = HudColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            dataStudio is Resource.Error -> {
                val message = dataStudio?.message ?: "Error de conexión"
                ErrorMessage(
                    message = message,
                    onRetry = { viewModel.loadDataStudio(pendingOffset) },
                    onAction = {
                        if (message.contains("401")) onSessionExpired()
                    }
                )
            }

            else -> {
                val data = dataStudio?.data ?: DataStudioData()
                val periodShort = DataStudioAggregator.periodShort(data.dateFrom, data.dateTo)

                // KPIs en fila compacta
                Row(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OperativeKpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Salidas en el periodo",
                        value = data.kpis.totalIncidents.toString(),
                        sub = "incidentes registrados",
                        color = HudColors.AccentPrimary
                    )
                    OperativeKpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Flota operativa",
                        value = "${data.kpis.fleetOperativePct}%",
                        sub = "${data.kpis.fleetOperative} de ${data.kpis.fleetTotal} unidades",
                        color = HudColors.Green
                    )
                    OperativeKpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Bases activas",
                        value = data.kpis.basesActive.toString(),
                        sub = "de ${data.kpis.basesTotal} en catálogo",
                        color = HudColors.Blue
                    )
                    OperativeKpiCard(
                        modifier = Modifier.weight(1f),
                        label = "Turnos configurados",
                        value = data.kpis.shiftsTotal.toString(),
                        sub = "guardias operativas",
                        color = HudColors.Amber
                    )
                }

                Text(
                    "// CENSO DE INCIDENTES · APIs DEL MÓDULO WEB",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    "Censo de Incidentes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 1) Por tipo — POST /folio/group-by-date
                ChartCard(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    title = "Incidentes por tipo",
                    subtitle = "Periodo consultado: $periodShort"
                ) {
                    when {
                        data.incidentsByType.isNotEmpty() -> TypeBarChart(data.incidentsByType)
                        data.available.services.not() -> EmptyInline("No fue posible cargar los incidentes por tipo.")
                        else -> EmptyInline("No hay servicios registrados en el periodo seleccionado.")
                    }
                }

                // 2) Por estatus + flota
                Row(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ChartCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Estados de servicios",
                        subtitle = periodShort
                    ) {
                        when {
                            data.incidentsByStatus.isNotEmpty() -> {
                                DonutChart(items = data.incidentsByStatus, colors = ChartPalette)
                            }
                            data.available.status.not() -> EmptyInline("No fue posible cargar los estados.")
                            else -> EmptyInline("Sin servicios en el periodo.")
                        }
                    }

                    ChartCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Estado de la flota",
                        subtitle = "${data.fleet.total} unidades"
                    ) {
                        if (data.fleet.total > 0) {
                            DonutChart(
                                items = listOf(
                                    LabeledCount("OPERATIVAS", data.fleet.operative),
                                    LabeledCount("EN TALLER", data.fleet.workshop),
                                    LabeledCount("INOPERATIVAS", data.fleet.inoperative)
                                ),
                                colors = listOf(HudColors.Green, HudColors.Amber, HudColors.AccentPrimary)
                            )
                        } else {
                            EmptyInline("Sin unidades registradas")
                        }
                    }
                }

                // 3) Por colonia — gráfica de pastel
                ChartCard(
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    title = "Incidentes por colonia",
                    subtitle = "Censo · todas las colonias · $periodShort"
                ) {
                    when {
                        data.incidentsByCologne.isNotEmpty() -> {
                            PieChart(items = data.incidentsByCologne, colors = ChartPalette)
                        }
                        data.available.colonias.not() -> {
                            EmptyInline("No fue posible cargar el censo por colonia.")
                        }
                        else -> EmptyInline("Sin incidentes por colonia en el periodo.")
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
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
            fontSize = 12.sp
        )
    }
}

@Composable
private fun OperativeKpiCard(
    modifier: Modifier,
    label: String,
    value: String,
    sub: String,
    color: Color
) {
    Card(
        onClick = {},
        modifier = modifier,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Black
            )
            Text(sub, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChartCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        onClick = {},
        modifier = modifier,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = HudColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun EmptyInline(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = HudColors.TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TypeBarChart(data: List<LabeledCount>) {
    val rows = data.filter { it.value > 0 }.ifEmpty { data }
    val max = rows.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.take(12).forEach { item ->
            val barHeight = (item.value.toFloat() / max * 180f).dp.coerceAtLeast(8.dp)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    item.value.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(barHeight)
                        .background(
                            HudColors.AccentPrimary.copy(alpha = 0.9f),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    item.label,
                    color = HudColors.TextMuted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PieChart(items: List<LabeledCount>, colors: List<Color>) {
    val positive = items.filter { it.value > 0 }.sortedByDescending { it.value }
    val top = positive.take(7)
    val otherSum = positive.drop(7).sumOf { it.value }
    val rows = if (otherSum > 0) {
        top + LabeledCount("OTRAS", otherSum)
    } else {
        top.ifEmpty { items }
    }
    val total = rows.sumOf { it.value }.toFloat()
    if (total <= 0f) {
        EmptyInline("Sin datos en el periodo")
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.size(170.dp)) {
                var startAngle = -90f
                rows.forEachIndexed { index, item ->
                    val sweep = (item.value / total) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                        startAngle += sweep
                    }
                }
            }
            // Centro legible sobre el pastel
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(HudColors.BgCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    total.toInt().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEachIndexed { index, item ->
                val pct = ((item.value / total) * 100f).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = HudColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${item.value} · $pct%",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(items: List<LabeledCount>, colors: List<Color>) {
    val rows = items.filter { it.value > 0 }.ifEmpty { items }
    val total = rows.sumOf { it.value }.toFloat()
    if (total <= 0f) {
        EmptyInline("Sin datos en el periodo")
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                rows.forEachIndexed { index, item ->
                    val sweep = (item.value / total) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
            }
            Text(
                total.toInt().toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = HudColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.value.toString(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
