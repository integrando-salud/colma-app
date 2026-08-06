package com.colma.colmaApp.features.instructions.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R

data class InfoItem(
    val title: String,
    val url: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color
)

@Composable
fun AdditionalInfoScreen() {
    val context = LocalContext.current

    val items = remember {
        listOf(
            InfoItem(
                title = "Información adicional sobre la diabetes",
                url = "https://proyectocolma.com/rep-dbt/",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFD81B60),
                iconBgColor = Color(0xFFFCE4EC)
            ),
            InfoItem(
                title = "Información sobre la hipertensión",
                url = "https://proyectocolma.com/rep-hta/",
                icon = Icons.Default.Warning,
                iconColor = Color(0xFFF4511E),
                iconBgColor = Color(0xFFFFF3E0)
            ),
            InfoItem(
                title = "Información sobre salud mental",
                url = "https://proyectocolma.com/rep-salud-mental/",
                icon = Icons.Default.Face,
                iconColor = Color(0xFF8E24AA),
                iconBgColor = Color(0xFFF3E5F5)
            ),
            InfoItem(
                title = "Alimentación saludable",
                url = "https://proyectocolma.com/rep-alimentacion/",
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF2E7D32),
                iconBgColor = Color(0xFFE8F5E9)
            ),
            InfoItem(
                title = "Alimentación para personas con diabetes",
                url = "https://proyectocolma.com/rep-alimentacion-dbt/",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFF0288D1),
                iconBgColor = Color(0xFFE1F5FE)
            ),
            InfoItem(
                title = "Alimentación para personas con hipertensión",
                url = "https://proyectocolma.com/rep-alimentacion-hta/",
                icon = Icons.Default.Warning,
                iconColor = Color(0xFF558B2F),
                iconBgColor = Color(0xFFF1F8E9)
            ),
            InfoItem(
                title = "Actividad física",
                url = "https://proyectocolma.com/rep-actividad-fisica/",
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFF00838F),
                iconBgColor = Color(0xFFE0F7FA)
            ),
            InfoItem(
                title = "Dormir mejor",
                url = "https://proyectocolma.com/rep-sueno/",
                icon = Icons.Default.Star,
                iconColor = Color(0xFF3F51B5),
                iconBgColor = Color(0xFFE8EAF6)
            ),
            InfoItem(
                title = "Manejo del estrés",
                url = "https://proyectocolma.com/rep-estres/",
                icon = Icons.Default.Face,
                iconColor = Color(0xFF00796B),
                iconBgColor = Color(0xFFE0F2F1)
            ),
            InfoItem(
                title = "Consumo de tabaco",
                url = "https://proyectocolma.com/rep-tabaco/",
                icon = Icons.Default.Info,
                iconColor = Color(0xFF6D4C41),
                iconBgColor = Color(0xFFEFEBE9)
            )
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(R.color.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Recursos educativos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.principal),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Selecciona cualquiera de los temas a continuación para acceder a información y recomendaciones de salud en tu navegador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored box with icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(item.iconBgColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text column
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Chevron arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ver más",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
