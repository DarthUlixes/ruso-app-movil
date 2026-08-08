package com.example.rusoit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.rusoit.data.model.User
import com.example.rusoit.ui.theme.HudColors
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PersonnelCard(
    person: User,
    onClick: () -> Unit
) {
    val fullName = person.fullName()
    val role = person.type_user?.uppercase(Locale.getDefault()) ?: "PERSONAL"
    val guardia = person.guardiaName()?.uppercase(Locale.getDefault())
    val position = person.employees?.position?.takeIf { it.isNotBlank() }
    val status = person.status_now?.lowercase(Locale.getDefault()) ?: "activo"
    val isActive = status == "activo" || status == "activa"
    val accent = if (isActive) HudColors.Green else HudColors.TextMuted
    val imageUrl = person.imageUrl()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        colors = CardDefaults.colors(
            containerColor = accent.copy(alpha = 0.10f),
            focusedContainerColor = accent.copy(alpha = 0.20f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
                    .width(88.dp)
                    .fillMaxHeight()
                    .padding(10.dp)
                    .background(HudColors.BgPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, end = 14.dp, bottom = 12.dp, start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(role, position).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = HudColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (guardia != null) {
                    Text(
                        "Guardia $guardia",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = HudColors.Amber
                    )
                }
            }
        }
    }
}
