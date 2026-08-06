@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.rusoit.data.model.Tool
import com.example.rusoit.data.model.ToolTypeInventory
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.util.Locale

/**
 * Herramientas TV — tipos y conteos reales desde API.
 * Detalle por tipo con scroll (Óptimos / Mantenimiento).
 */
@Composable
fun InventoryView(viewModel: MonitoringViewModel, onToolClick: (Tool) -> Unit) {
    var selectedInventory by remember { mutableStateOf<ToolTypeInventory?>(null) }

    if (selectedInventory == null) {
        ToolTypesScreen(
            viewModel = viewModel,
            onTypeClick = { selectedInventory = it }
        )
    } else {
        ToolsByTypeScreen(
            viewModel = viewModel,
            inventory = selectedInventory!!,
            onBack = {
                viewModel.clearToolsByType()
                selectedInventory = null
            },
            onToolClick = onToolClick
        )
    }
}

@Composable
private fun ToolTypesScreen(
    viewModel: MonitoringViewModel,
    onTypeClick: (ToolTypeInventory) -> Unit
) {
    val inventoryResource by viewModel.toolsInventory.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadToolsInventory() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// HERRAMIENTAS",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Tipos de herramienta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            inventoryResource is Resource.Loading || inventoryResource == null -> {
                LoadingSpinner("Cargando inventario de herramientas...")
            }
            inventoryResource is Resource.Error -> {
                ErrorMessage(
                    inventoryResource?.message ?: "Error al cargar herramientas",
                    onRetry = { viewModel.loadToolsInventory() }
                )
            }
            else -> {
                val types = inventoryResource?.data.orEmpty()
                if (types.isEmpty()) {
                    PlaceholderView("Sin tipos de herramienta")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(types, key = { it.type.id ?: it.type.name.hashCode() }) { item ->
                            ToolTypeCard(item = item, onClick = { onTypeClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTypeCard(item: ToolTypeInventory, onClick: () -> Unit) {
    val type = item.type
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                type.name ?: "Sin nombre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(HudColors.BgPrimary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!type.cover_image.isNullOrBlank()) {
                    AsyncImage(
                        model = type.cover_image,
                        contentDescription = type.name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        "Sin imagen",
                        color = HudColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Óptimas: ${item.activeCount}",
                color = HudColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                "Mal estado o revisión: ${item.inactiveCount}",
                color = HudColors.Amber,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                "Total: ${item.activeCount + item.inactiveCount}",
                color = HudColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ToolsByTypeScreen(
    viewModel: MonitoringViewModel,
    inventory: ToolTypeInventory,
    onBack: () -> Unit,
    onToolClick: (Tool) -> Unit
) {
    val toolsResource by viewModel.toolsByType.collectAsState()
    val typeId = inventory.type.id
    val toolType = inventory.type

    LaunchedEffect(typeId) {
        if (typeId != null) viewModel.loadToolsByType(typeId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "// HERRAMIENTAS · ${toolType.name?.uppercase(Locale.getDefault()) ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HudColors.AccentSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    toolType.name ?: "Herramientas",
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

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                typeId == null -> PlaceholderView("Tipo sin id válido")
                toolsResource is Resource.Loading || toolsResource == null -> {
                    // Mostrar cache inmediato mientras refresca
                    if (inventory.tools.isNotEmpty()) {
                        ToolsSplitPanels(
                            tools = inventory.tools,
                            onToolClick = onToolClick
                        )
                    } else {
                        LoadingSpinner("Cargando herramientas...")
                    }
                }
                toolsResource is Resource.Error -> {
                    if (inventory.tools.isNotEmpty()) {
                        ToolsSplitPanels(tools = inventory.tools, onToolClick = onToolClick)
                    } else {
                        ErrorMessage(
                            toolsResource?.message ?: "Error al cargar herramientas del tipo",
                            onRetry = { viewModel.loadToolsByType(typeId) }
                        )
                    }
                }
                else -> {
                    val all = toolsResource?.data.orEmpty().ifEmpty { inventory.tools }
                    ToolsSplitPanels(tools = all, onToolClick = onToolClick)
                }
            }
        }
    }
}

@Composable
private fun ToolsSplitPanels(tools: List<Tool>, onToolClick: (Tool) -> Unit) {
    val active = tools.filter { it.isActive() }
    val maintenance = tools.filter { !it.isActive() }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ToolsStatusPanel(
            modifier = Modifier.weight(1f),
            title = "Óptimos",
            accent = HudColors.Green,
            tools = active,
            onToolClick = onToolClick
        )
        ToolsStatusPanel(
            modifier = Modifier.weight(1f),
            title = "Mantenimiento",
            accent = HudColors.Amber,
            tools = maintenance,
            onToolClick = onToolClick
        )
    }
}

@Composable
private fun ToolsStatusPanel(
    modifier: Modifier,
    title: String,
    accent: Color,
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit
) {
    // Box (no Surface clickable): el Surface con onClick={} se comía el D-pad
    // y no dejaba enfocar ni scrollear las herramientas.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(HudColors.BgCard, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accent)
            Text("${tools.size} equipos", style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
            Spacer(modifier = Modifier.height(12.dp))
            if (tools.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin registros", color = HudColors.TextMuted, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(tools, key = { it.id ?: it.name.hashCode() }) { tool ->
                        ToolRowCard(tool = tool, accent = accent, onClick = { onToolClick(tool) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRowCard(tool: Tool, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = HudColors.BgPrimary,
            focusedContainerColor = HudColors.BgCardHover
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(HudColors.BgCard, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!tool.cover_image.isNullOrBlank()) {
                        AsyncImage(
                            model = tool.cover_image,
                            contentDescription = tool.name,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            "—",
                            color = HudColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tool.name ?: "Herramienta",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tool.status?.uppercase(Locale.getDefault()) ?: "S/E",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            if (!tool.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    tool.description,
                    color = HudColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
