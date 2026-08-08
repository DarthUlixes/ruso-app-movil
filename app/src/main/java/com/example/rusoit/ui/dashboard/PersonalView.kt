@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.rusoit.data.model.User
import com.example.rusoit.ui.components.PersonnelCard
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.util.Locale

private enum class PersonalTypeFilter(val label: String) {
    ALL("Todos"),
    EMPLOYEE("Empleados"),
    VOLUNTEER("Voluntarios")
}

@Composable
fun PersonalView(viewModel: MonitoringViewModel) {
    val personnelResource by viewModel.personnel.collectAsState()
    val shiftsResource by viewModel.workShifts.collectAsState()
    var typeFilter by remember { mutableStateOf(PersonalTypeFilter.ALL) }
    var guardiaFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPersonnel()
        viewModel.loadWorkShifts()
    }

    val shifts = (shiftsResource as? Resource.Success)?.data
        ?.mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotEmpty() } }
        ?.distinctBy { it.uppercase(Locale.getDefault()) }
        .orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// PERSONAL",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Personal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Filtra por tipo y guardia",
            style = MaterialTheme.typography.bodyMedium,
            color = HudColors.TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "TIPO",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.TextMuted,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PersonalTypeFilter.entries.forEach { filter ->
                FilterChip(
                    label = filter.label.uppercase(Locale.getDefault()),
                    selected = typeFilter == filter,
                    accent = when (filter) {
                        PersonalTypeFilter.EMPLOYEE -> HudColors.Green
                        PersonalTypeFilter.VOLUNTEER -> HudColors.Amber
                        else -> HudColors.AccentPrimary
                    },
                    onClick = { typeFilter = filter }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "GUARDIA",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.TextMuted,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                label = "TODAS",
                selected = guardiaFilter == null,
                accent = HudColors.AccentPrimary,
                onClick = { guardiaFilter = null }
            )
            shifts.forEach { name ->
                FilterChip(
                    label = name.uppercase(Locale.getDefault()),
                    selected = guardiaFilter.equals(name, ignoreCase = true),
                    accent = HudColors.Amber,
                    onClick = { guardiaFilter = name }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            personnelResource is Resource.Loading || personnelResource == null -> {
                LoadingSpinner("Actualizando lista...")
            }
            personnelResource is Resource.Error -> {
                ErrorMessage(
                    personnelResource?.message ?: "Error",
                    onRetry = { viewModel.loadPersonnel() }
                )
            }
            else -> {
                val data = personnelResource?.data.orEmpty()
                    .filter { !it.type_user.equals("sistemas", ignoreCase = true) }
                val filtered = remember(data, typeFilter, guardiaFilter) {
                    data.filter { person ->
                        matchesType(person, typeFilter) && matchesGuardia(person, guardiaFilter)
                    }
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Sin personal para este filtro",
                            color = HudColors.TextMuted
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { person ->
                            PersonnelCard(person = person) { }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else HudColors.BgCard,
            focusedContainerColor = HudColors.BgCardHover
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else HudColors.TextMuted,
            fontWeight = FontWeight.Black
        )
    }
}

private fun matchesType(person: User, filter: PersonalTypeFilter): Boolean = when (filter) {
    PersonalTypeFilter.ALL -> true
    PersonalTypeFilter.EMPLOYEE -> person.isEmployeeType()
    PersonalTypeFilter.VOLUNTEER -> person.isVolunteerType()
}

private fun matchesGuardia(person: User, guardia: String?): Boolean {
    if (guardia.isNullOrBlank()) return true
    return person.guardiaName().equals(guardia, ignoreCase = true)
}
