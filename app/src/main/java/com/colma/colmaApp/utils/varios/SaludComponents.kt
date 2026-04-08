package com.colma.colmaApp.utils.varios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple data class for history items */
data class HealthEntry(
    val id: Long? = null,
    val label: String,
    val value: String,
    val timestamp: Long,
    val rawValue1: Double? = null,
    val rawValue2: Double? = null,
    val isFasting: Boolean? = null
)

/** HistoryCard same as you had — keeps an onClear callback */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCard(
    history: List<HealthEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onEntryClick: ((HealthEntry) -> Unit)? = null,
    showTitle: Boolean = true,
    showLabel: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (showTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.hsl(0f, 0f, 0.10f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            if (history.isEmpty()) {
                Text(
                    text = "No hay registros aún",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(history) { entry ->
                        HistoryRow(
                            entry,
                            onClick = onEntryClick?.let { { it(entry) } },
                            showLabel = showLabel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRow(
    entry: HealthEntry,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showLabel: Boolean = true
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    // Visual mapping per type
    val signaturePink = Color(0xFFE84589)
    val pressureBlue = Color(0xFF2B8CFF)
    val (iconTint, bgAlpha, valueColor) = when (entry.label.lowercase(Locale.getDefault())) {
        "glucosa" -> Triple(painterResource(R.drawable.outline_glucose_24), 0.14f, signaturePink)
        "presion", "presión", "presion arterial" -> Triple(painterResource(R.drawable.outline_blood_pressure_24), 0.12f, pressureBlue)
        else -> Triple(rememberVectorPainter(Icons.Default.Face), 0.12f, Color.Black)
    }

    // Build display value:
    // 1) detect and keep a fasting-like suffix (e.g. "· Ayuno", "· Sin Ayuno", "Ayuno", "fasting")
    //    - capture optional separator (· • - |) preceding the token
    // 2) clean the main part removing unit tokens (mg/dl, mmHg, etc.)
    // 3) trim and reattach the suffix (preserving the separator)
    val displayValue = remember(entry.value) {
        val raw = (entry.value ?: "").trim()
        if (raw.isEmpty()) return@remember raw

        // regex to capture suffix (including optional preceding separator like "·")
        val fastingRegex = Regex(
            """(?i)(?:\s*[·•\-\|]\s*)?(ayuno|sin ayuno|sinayuno|ayunas?|fasting|no fasting)\b.*$"""
        )
        val fastingMatch = fastingRegex.find(raw)

        val mainPart: String
        val suffixPart: String
        if (fastingMatch != null) {
            // keep the suffix exactly as it appears (including separator)
            suffixPart = raw.substring(fastingMatch.range.first).trim()
            mainPart = raw.substring(0, fastingMatch.range.first).trim()
        } else {
            suffixPart = ""
            mainPart = raw
        }

        // remove common explicit unit tokens from the main part
        var main = mainPart.replace(Regex("""(?i)\b(mg\/dl|mmhg|mmol\/l|bpm|kg|cm|%|mmol\/L)\b"""), "").trim()

        // remove any trailing punctuation/words that are not part of numeric values.
        // keep digits, slash (for pressure), dot/comma, hyphen.
        main = main.replace(Regex("""\s*[^\d\/\.,-]+$"""), "").trim()

        // avoid dangling separators: if main becomes only a separator (like "/"), drop it
        if (main.matches(Regex("""^[\/\-,\.]+$"""))) {
            main = ""
        }

        // reattach suffix if present
        val combined = when {
            main.isNotBlank() && suffixPart.isNotBlank() -> "$main $suffixPart"
            main.isBlank() && suffixPart.isNotBlank() -> suffixPart
            else -> main
        }

        combined
    }

    @Composable
    fun CardContent() {
        val isDangerous = remember(entry) {
            var dangerous = false
            when (entry.label.lowercase(Locale.getDefault())) {
                "glucosa" -> {
                    entry.rawValue1?.let { v ->
                        val low = v <= 69.0
                        val high = if (entry.isFasting == true) v >= 131.0 else v >= 181.0
                        if (low || high) {
                            dangerous = true
                        }
                    }
                }
                "presion", "presión", "presion arterial" -> {
                    val s = entry.rawValue1
                    val d = entry.rawValue2
                    if (s != null && d != null) {
                        val highS = s > 140
                        val lowS = s < 90
                        val highD = d > 90
                        val lowD = d < 60
                        if (highS || lowS || highD || lowD) {
                            dangerous = true
                        }
                    }
                }
            }
            dangerous
        }
        val textColor = if (isDangerous) Color.Red else Color(0xFF008000)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(valueColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = iconTint, contentDescription = entry.label, tint = valueColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (showLabel) {
                    Text(
                        text = entry.label,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDangerous) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = displayValue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Text(
                    text = remember(entry.timestamp) { formatter.format(Date(entry.timestamp)) },
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            CardContent()
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            CardContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFF7F7F8) else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textColor = if (enabled) Color.Black else Color.Gray
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            val iconTint = if (enabled) Color.Gray else Color.LightGray
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Ingresar $label",
                tint = iconTint
            )
        }
    }
}

fun parseDateTimeToMillis(dateTimeStr: String?): Long? {
    if (dateTimeStr.isNullOrBlank()) return null

    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(dateTimeStr)?.time
    } catch (_: Exception) {
        null
    }
}

fun formatTimestamp(millis: Long?): String {
    if (millis == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
