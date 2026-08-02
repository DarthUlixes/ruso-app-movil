package com.example.rusoit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.rusoit.data.model.Vehicle

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit
) {
    val statusColor = when (vehicle.status) {
        "Operativa" -> Color(0xFF4CAF50)
        "En Taller" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val statusIcon = when (vehicle.status) {
        "Operativa" -> Icons.Default.CheckCircle
        "En Taller" -> Icons.Default.Build
        else -> Icons.Default.Warning
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(150.dp),
        scale = CardDefaults.scale(focusedScale = 1.1f)
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
                    text = "U-${vehicle.number_unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = vehicle.type ?: "Unidad Operativa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = vehicle.status?.uppercase() ?: "DESCONOCIDO",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
