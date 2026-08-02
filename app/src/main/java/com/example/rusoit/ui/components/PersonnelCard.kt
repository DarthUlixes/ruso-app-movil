package com.example.rusoit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.rusoit.data.model.User

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PersonnelCard(
    person: User,
    onClick: () -> Unit
) {
    val firstName = person.first_name ?: "Desconocido"
    val secondName = person.second_name ?: ""
    val lastName = person.last_name ?: ""
    val secondLastName = person.second_last_name ?: ""
    
    val fullName = listOf(firstName, secondName, lastName, secondLastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")

    val role = person.type_user?.uppercase() ?: "BOMBERO"
    
    val status = person.status_now?.lowercase() ?: "activo"
    val isActive = status == "activo" || status == "activa"

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(100.dp),
        scale = CardDefaults.scale(focusedScale = 1.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
