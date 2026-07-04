package com.colma.colmaApp.features.sesiones.SessionList.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SessionList.domain.SubSessionItem
import com.colma.colmaApp.features.sesiones.SessionList.viewmodel.SessionListEvent
import com.colma.colmaApp.features.sesiones.SessionList.viewmodel.SessionListUiState
import com.colma.colmaApp.features.multimedia.ui.scrollbar
import com.colma.colmaApp.ui.theme.ColmaAppTheme

// SessionListUI activity has been migrated to MainActivity.kt as a NavHost route.
// The Composable functions and helper classes are retained here for routing.

@Composable
fun SessionListScreen(
    uiState: SessionListUiState,
    onEvent: (SessionListEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.background))
                .scrollbar(lazyListState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            val hasHta = uiState.treatmentTypes.contains(TreatmentType.HTA)
            val hasDiabetes = uiState.treatmentTypes.contains(TreatmentType.DIABETES)
            val hasDbtHta = uiState.treatmentTypes.contains(TreatmentType.DBT_HTA)

            if (hasHta) {
                item {
                    StaggeredItemWrapper(index = 0) {
                        TreatmentHeader(title = "Hipertensión Arterial")
                    }
                }
                itemsIndexed(uiState.htaSessions, key = { _, it -> "hta-${it.id}" }) { index, session ->
                    StaggeredItemWrapper(index = index + 1) {
                        SessionItemView(
                            session = session,
                            subSessions = uiState.subSessions[session.id] ?: emptyList(),
                            expanded = uiState.expandedSessionId == session.id,
                            onEvent = onEvent
                        )
                    }
                }
            }

            if (hasHta && hasDiabetes) {
                item {
                    val dividerIndex = uiState.htaSessions.size + 1
                    StaggeredItemWrapper(index = dividerIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            if (hasDiabetes) {
                val startIndex = if (hasHta) uiState.htaSessions.size + 2 else 0
                item {
                    StaggeredItemWrapper(index = startIndex) {
                        TreatmentHeader(title = "Diabetes")
                    }
                }
                itemsIndexed(uiState.diabetesSessions, key = { _, it -> "diabetes-${it.id}" }) { index, session ->
                    StaggeredItemWrapper(index = startIndex + index + 1) {
                        SessionItemView(
                            session = session,
                            subSessions = uiState.subSessions[session.id] ?: emptyList(),
                            expanded = uiState.expandedSessionId == session.id,
                            onEvent = onEvent
                        )
                    }
                }
            }

            if (hasDbtHta) {
                item {
                    StaggeredItemWrapper(index = 0) {
                        TreatmentHeader(title = "Diabetes e Hipertensión Arterial")
                    }
                }
                itemsIndexed(uiState.diabetesSessions, key = { _, it -> "dbthta-${it.id}" }) { index, session ->
                    StaggeredItemWrapper(index = index + 1) {
                        SessionItemView(
                            session = session,
                            subSessions = uiState.subSessions[session.id] ?: emptyList(),
                            expanded = uiState.expandedSessionId == session.id,
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaggeredItemWrapper(
    index: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L) // slightly faster (40ms) stagger for snappier loading
        isVisible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f, // 30px offset
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "translationY"
    )
    Box(
        modifier = Modifier.graphicsLayer(
            alpha = alpha,
            translationY = translationY
        )
    ) {
        content()
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
    subSessions: List<SubSessionItem>,
    expanded: Boolean,
    onEvent: (SessionListEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 11.dp)
            .padding(horizontal = 11.dp)
    ) {
        SessionCard(
            session = session,
            selected = expanded,
            onClick = { onEvent(SessionListEvent.ExpandSession(session.id)) }
        )

        AnimatedVisibility(
            visible = expanded,
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
                items = subSessions,
                onSubClick = { sub ->
                    onEvent(SessionListEvent.SubSessionClick(session.id, sub.id, sub.title, session.treatmentType))
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
                    .sizeIn(maxWidth = 72.dp, maxHeight = 72.dp)
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
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .heightIn(min = 48.dp),
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

        if (sub.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completada",
                tint = Color(0xFF4CAF50)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = colorResource(R.color.principal)
            )
        }
    }
}
