package com.example.rusoit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.rusoit.data.model.Vehicle
import com.example.rusoit.ui.theme.HudColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit
) {
    val status = vehicle.status?.lowercase() ?: "desconocido"
    val isOperational = status == "operativa" || status == "operativo"
    val isInWorkshop = status == "taller" || status == "en taller"

    val statusColor = when {
        isOperational -> HudColors.Green
        isInWorkshop -> HudColors.Amber
        else -> Color.Red
    }

    val statusIcon = when {
        isOperational -> Icons.Default.Check
        isInWorkshop -> Icons.Default.Build
        else -> Icons.Default.Warning
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(140.dp),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        colors = CardDefaults.colors(containerColor = HudColors.BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "U-${vehicle.number_unit ?: "S/N"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HudColors.TextPrimary
                )
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = "${vehicle.brandName() ?: "Marca"} - ${vehicle.model ?: "Modelo"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HudColors.TextMuted,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "KM: ${vehicle.kilometers?.toInt() ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HudColors.TextSecondary
                    )
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
