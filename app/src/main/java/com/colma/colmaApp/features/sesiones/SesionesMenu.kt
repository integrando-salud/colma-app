package com.colma.colmaApp.features.sesiones

import androidx.core.content.ContextCompat
import com.colma.colmaApp.features.sesiones.TreatmentType
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.saveable.rememberSaveable


data class SessionItem(
    val id: Long,
    val title: String,
    val treatmentType: TreatmentType,
    val thumbnailRes: Int = R.drawable.missing_photo,
    val duration: String = "10 min"
)

data class SubSessionItem(
    val id: Long,
    val title: String,
    val duration: String = "5 min"
)

class SesionesMenu : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        val treatmentTypesString = intent?.getStringExtra("treatmentTypes") ?: "HTA"

        setContent {
            val treatmentTypes = remember(treatmentTypesString) {
                treatmentTypesString.split(',').mapNotNull {
                    try {
                        TreatmentType.valueOf(it.trim().uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }.ifEmpty { listOf(TreatmentType.HTA) }
            }

            ColmaAppTheme {
                val ctx = this
                SesionesMenuScreen(
                    treatmentTypes = treatmentTypes,
                    onSessionClick = { sessionId, title ->
                        // No-op for now
                    },
                    onSubSessionClick = { parentId, subId, title, treatmentType ->
                        val intent = Intent(ctx, MultimediaSelector::class.java).apply {
                            putExtra("sessionId", parentId)
                            putExtra("sessionTitle", "Semana $parentId")
                            putExtra("subSessionId", subId)
                            putExtra("subSessionTitle", title)
                            putExtra("treatmentTypes", treatmentType.name)
                        }
                        startActivity(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesionesMenuScreen(
    treatmentTypes: List<TreatmentType>,
    onSessionClick: (Long, String) -> Unit,
    onSubSessionClick: (parentSessionId: Long, subSessionId: Long, title: String, treatmentType: TreatmentType) -> Unit,
    onBack: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val topBarTitle = remember(treatmentTypes) {
        when {
            treatmentTypes.contains(TreatmentType.HTA) && treatmentTypes.contains(TreatmentType.DIABETES) -> "Hipertensión y Diabetes"
            treatmentTypes.contains(TreatmentType.HTA) -> "Hipertensión"
            treatmentTypes.contains(TreatmentType.DIABETES) -> "Diabetes"
            else -> "Sesiones"
        }
    }

    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding(), color = colorResource(R.color.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                title = { Text(topBarTitle, color = colorResource(R.color.white), fontWeight = FontWeight.Bold)},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor)
            )

            var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                val hasHta = treatmentTypes.contains(TreatmentType.HTA)
                val hasDiabetes = treatmentTypes.contains(TreatmentType.DIABETES)

                if (hasHta) {
                    item {
                        TreatmentHeader(title = "Hipertensión Arterial")
                    }
                    items(HTASession(), key = { "hta-${it.id}" }) { session ->
                        SessionItemView(
                            session = session,
                            expandedId = expandedId,
                            onExpand = { id -> expandedId = if (expandedId == id) null else id },
                            onSessionClick = onSessionClick,
                            onSubSessionClick = onSubSessionClick
                        )
                    }
                }

                if (hasHta && hasDiabetes) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )
                    }
                }

                if (hasDiabetes) {
                    item {
                        TreatmentHeader(title = "Diabetes")
                    }
                    items(DiabetesSession(), key = { "diabetes-${it.id}" }) { session ->
                        SessionItemView(
                            session = session,
                            expandedId = expandedId,
                            onExpand = { id -> expandedId = if (expandedId == id) null else id },
                            onSessionClick = onSessionClick,
                            onSubSessionClick = onSubSessionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TreatmentHeader(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.principal),
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp, start = 11.dp)
    )
}

@Composable
fun SessionItemView(
    session: SessionItem,
    expandedId: Long?,
    onExpand: (Long) -> Unit,
    onSessionClick: (Long, String) -> Unit,
    onSubSessionClick: (parentSessionId: Long, subSessionId: Long, title: String, treatmentType: TreatmentType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 11.dp)
            .padding(horizontal = 11.dp)
    ) {
        val selected = expandedId == session.id

        SessionCard(
            session = session,
            selected = selected,
            onClick = {
                onExpand(session.id)
                onSessionClick(session.id, session.title)
            }
        )

        AnimatedVisibility(
            visible = selected,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            SecondarySessionList(
                modifier = Modifier.fillMaxWidth(),
                parentSession = session,
                items = when (session.treatmentType) {
                    TreatmentType.HTA -> HTASubSessions(session.id)
                    TreatmentType.DIABETES -> DiabetesSubSessions(session.id)
                },
                onSubClick = { sub ->
                    onSubSessionClick(session.id, sub.id, sub.title, session.treatmentType)
                }
            )
        }
    }
}


@Composable
fun SessionCard(session: SessionItem, selected: Boolean = false, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(targetValue = if (selected) colorResource(R.color.principal) else colorResource(R.color.white), label = "")
    val titleColor by animateColorAsState(targetValue = if (selected) Color.White else Color.Black, label = "")
    val shape = if (selected) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp) else RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = session.thumbnailRes),
                contentDescription = "Miniatura ${session.title}",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(8.dp))

            }

            Spacer(modifier = Modifier.width(8.dp))

        }
    }
}

@Composable
fun SecondarySessionList(
    modifier: Modifier = Modifier,
    parentSession: SessionItem,
    items: List<SubSessionItem>,
    onSubClick: (SubSessionItem) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.white))
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(text = "Contenidos de ${parentSession.title}", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { sub ->
                    SubSessionRow(sub = sub, onClick = { onSubClick(sub) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SubSessionRow(sub: SubSessionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(colorResource(R.color.principal), shape = RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = sub.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = colorResource(R.color.principal))
    }
}


fun HTASession(): List<SessionItem> = (1..16).map {
    SessionItem(it.toLong(), "Semana $it hipertensión", TreatmentType.HTA, thumbnailRes = R.drawable.missing_photo)
}

fun HTASubSessions(parentId: Long): List<SubSessionItem> {
    return listOf(
        SubSessionItem(1, "Sesion 1", "05:00"),
        SubSessionItem(2, "Sesion 2", "07:00"),
        SubSessionItem(3, "Sesion 3", "03:00"),
    )
}

fun DiabetesSession(): List<SessionItem> = (1..16).map {
    SessionItem(it.toLong() + 16, "Semana $it Diabetes", TreatmentType.DIABETES, thumbnailRes = R.drawable.missing_photo)
}

fun DiabetesSubSessions(parentId: Long): List<SubSessionItem> {
    return listOf(
        SubSessionItem(1, "Sesion 1", "05:00"),
        SubSessionItem(2, "Sesion 2", "07:00"),
        SubSessionItem(3, "Sesion 3", "03:00"),
    )
}