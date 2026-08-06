@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.rusoit.data.model.AgendaCalendarEvent
import com.example.rusoit.ui.components.FocusTrappedModal
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Agenda operativa TV — espejo del calendario de Inicio (WellcomeView):
 * mes navegable + listas Hoy / día seleccionado + detalle de consulta.
 */
@Composable
fun AgendaView(viewModel: MonitoringViewModel) {
    val eventsResource by viewModel.agendaEvents.collectAsState()
    var visibleMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            clearTime()
        })
    }
    var selectedDate by remember { mutableStateOf(todayIso()) }
    var selectedEvent by remember { mutableStateOf<AgendaCalendarEvent?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAgendaEvents() }

    val localeEs = remember { Locale("es", "MX") }
    val monthLabel = remember(visibleMonth) {
        SimpleDateFormat("MMMM yyyy", localeEs).format(visibleMonth.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// CALENDARIO",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Agenda operativa",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))

        when {
            eventsResource is Resource.Loading || eventsResource == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HudColors.AccentPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando agenda...", color = HudColors.TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                // Si hubo error de red, getAgendaEvents ya devolvió locales; aún así mostramos
                val events = eventsResource?.data.orEmpty()
                val today = todayIso()
                val eventsToday = remember(events, today) { events.filter { it.start == today } }
                val eventsSelected = remember(events, selectedDate) {
                    events.filter { it.start == selectedDate }
                }
                val daysWithEvents = remember(events, visibleMonth) {
                    events.map { it.start }.toSet()
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Calendario mensual
                    Card(
                        onClick = {},
                        modifier = Modifier.weight(1.15f).fillMaxHeight(),
                        colors = CardDefaults.colors(containerColor = HudColors.BgCard)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MonthNavChip("◀") {
                                    visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                        add(Calendar.MONTH, -1)
                                    }
                                }
                                Text(
                                    monthLabel,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                MonthNavChip("▶") {
                                    visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                        add(Calendar.MONTH, 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                                    Text(
                                        d,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        color = HudColors.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val cells = remember(visibleMonth) { monthCells(visibleMonth) }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(7),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(cells) { cell ->
                                    if (cell == null) {
                                        Box(modifier = Modifier.aspectRatio(1f))
                                    } else {
                                        val iso = cell
                                        val dayNum = iso.takeLast(2).toIntOrNull() ?: 0
                                        val isSelected = iso == selectedDate
                                        val isToday = iso == today
                                        val hasEvents = iso in daysWithEvents
                                        Surface(
                                            onClick = { selectedDate = iso },
                                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = when {
                                                    isSelected -> HudColors.AccentPrimary
                                                    isToday -> HudColors.AccentGlow
                                                    else -> Color.Transparent
                                                },
                                                focusedContainerColor = HudColors.BgCardHover
                                            ),
                                            modifier = Modifier.aspectRatio(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        dayNum.toString(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else HudColors.TextPrimary
                                                    )
                                                    if (hasEvents) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .background(
                                                                    if (isSelected) Color.White else HudColors.Amber,
                                                                    CircleShape
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Listas Hoy + día seleccionado (como Inicio)
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DayEventPanel(
                            modifier = Modifier.weight(1f),
                            title = "Hoy",
                            subtitle = formatSpanishDate(today),
                            events = eventsToday,
                            onEventClick = { selectedEvent = it }
                        )
                        DayEventPanel(
                            modifier = Modifier.weight(1f),
                            title = formatSpanishDate(selectedDate),
                            subtitle = if (selectedDate == today) "Día seleccionado (hoy)" else "Día seleccionado",
                            events = eventsSelected,
                            onEventClick = { selectedEvent = it }
                        )
                    }
                }
            }
        }
    }

    selectedEvent?.let { event ->
        AgendaEventDetailOverlay(event = event, onDismiss = { selectedEvent = null })
    }
}

@Composable
private fun MonthNavChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = HudColors.BgPrimary,
            focusedContainerColor = HudColors.BgCardHover
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun DayEventPanel(
    modifier: Modifier,
    title: String,
    subtitle: String,
    events: List<AgendaCalendarEvent>,
    onEventClick: (AgendaCalendarEvent) -> Unit
) {
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.colors(containerColor = HudColors.BgCard)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
            Spacer(modifier = Modifier.height(10.dp))
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin eventos", color = HudColors.TextMuted, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(events, key = { it.id }) { event ->
                        Surface(
                            onClick = { onEventClick(event) },
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = HudColors.BgPrimary,
                                focusedContainerColor = HudColors.BgCardHover
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (event.eventClass == "red-event") HudColors.AccentPrimary
                                            else HudColors.Green,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        event.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        event.content,
                                        color = HudColors.TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaEventDetailOverlay(event: AgendaCalendarEvent, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    FocusTrappedModal(
        scrimAlpha = 0.88f,
        initialFocusRequester = focusRequester
    ) {
        Card(
            onClick = {},
            modifier = Modifier.fillMaxWidth(0.7f).wrapContentHeight(),
            colors = CardDefaults.colors(containerColor = HudColors.BgCard)
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    "// CONSULTA DE EVENTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                DetailLine("Fecha", formatSpanishDate(event.start))
                if (event.end != event.start) DetailLine("Hasta", formatSpanishDate(event.end))
                DetailLine("Detalle", event.content)
                event.tipe_to_event?.let { DetailLine("Tipo", it) }
                event.estatus?.let { DetailLine("Estatus", it.uppercase(Locale.getDefault())) }
                event.ubication?.let { DetailLine("Ubicación", it) }
                event.who_autorisated?.let { DetailLine("Autoriza", it) }
                event.notes?.takeIf { it != event.content }?.let { DetailLine("Notas", it) }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).focusRequester(focusRequester),
                    colors = ButtonDefaults.colors(containerColor = HudColors.AccentPrimary)
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

private fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun todayIso(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH)
    )
}

private fun formatSpanishDate(iso: String): String {
    return try {
        val parts = iso.split("-")
        if (parts.size != 3) return iso
        val cal = Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(cal.time)
    } catch (_: Exception) {
        iso
    }
}

/** Celdas lun–dom del mes; null = hueco previo. */
private fun monthCells(monthStart: Calendar): List<String?> {
    val cal = monthStart.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.clearTime()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Dom ... 7=Sab
    val offset = when (firstDow) {
        Calendar.SUNDAY -> 6
        else -> firstDow - Calendar.MONDAY
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = MutableList<String?>(offset) { null }
    for (d in 1..daysInMonth) {
        cells += "%04d-%02d-%02d".format(year, month, d)
    }
    while (cells.size % 7 != 0) cells += null
    return cells
}
