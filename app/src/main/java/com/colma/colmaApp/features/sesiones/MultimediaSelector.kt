package com.colma.colmaApp.features.sesiones

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import com.colma.colmaApp.features.multimediaScreens.ActionActivity
import com.colma.colmaApp.features.multimediaScreens.DescriptionActivity
import com.colma.colmaApp.features.multimediaScreens.FinishActivity
import com.colma.colmaApp.features.multimediaScreens.HTAQuiz
import com.colma.colmaApp.features.multimediaScreens.MetasActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.contentColorFor

// New enum and data class for multimedia options
enum class MultimediaContentType {
    DESCRIPTION,
    VIDEO,
    AUDIO,
    METAS,
    QUIZ
}

data class MultimediaOption(
    val type: MultimediaContentType,
    val title: String,
    val description: String,
    val iconResId: Int? = null, // For drawable resources (e.g., R.drawable.xxx)
    val iconImageVector: ImageVector? = null, // For ImageVector icons (e.g., Icons.Default.PlayArrow)
    val contentAssetId: Int? = null // For raw resource IDs (e.g., R.raw.xxx)
)

/**
 * Helper function to define multimedia options based on the subSessionId.
 * Note: For video assets, `R.raw.exercise` is used as a placeholder.
 * For distinct videos, different raw video assets would need to be added
 * and referenced here. `ActionActivity` must be updated to consume
 * the `contentAssetId` extra to play the correct media.
 */
fun getMultimediaOptionsForSubSession(subSessionId: Long): List<MultimediaOption> {
    val descriptionOption = MultimediaOption(
        type = MultimediaContentType.DESCRIPTION,
        title = "Descripción",
        description = "Ver información y objetivos de la sesión",
        iconResId = R.drawable.outline_unknown_document_24
    )

    return when (subSessionId) {
        1L -> listOf(
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video Introducción",
                description = "Ver el vídeo de introducción a la sesión",
                iconImageVector = Icons.Default.PlayArrow,
                contentAssetId = R.raw.hta_sesion_5_1 // Video 1 asset
            )
        )
        2L -> listOf(
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video Explicativo",
                description = "Ver el vídeo explicativo de la sesión",
                iconImageVector = Icons.Default.PlayArrow,
                contentAssetId = R.raw.hta_sesion_5_2 // Video 2 asset. TODO: Replace with a different video asset if available.
            ),
            MultimediaOption(
                type = MultimediaContentType.QUIZ,
                title = "Quiz Módulo 5",
                description = "Responde las preguntas del módulo 5",
                iconResId = R.drawable.outline_trophy_24
            )
        )
        3L -> listOf(
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video Principal",
                description = "Primer video de la sesión",
                iconImageVector = Icons.Default.PlayArrow,
                contentAssetId = R.raw.hta_sesion_5_3 // Video A asset
            ),
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video Complementario",
                description = "Segundo video de la sesión",
                iconImageVector = Icons.Default.PlayArrow,
                contentAssetId = R.raw.hta_sesion_5_3_experiencia // Video B asset. TODO: Replace with a different video asset if available.
            ),
            MultimediaOption(
                type = MultimediaContentType.METAS,
                title = "Metas",
                description = "Configura tus metas para esta sesión",
                iconResId = R.drawable.outline_trophy_24
            )
        )
        // Default options if subSessionId does not match any specific case
        else -> listOf(
            descriptionOption,
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video Genérico",
                description = "Ver el vídeo genérico de la sesión",
                iconImageVector = Icons.Default.PlayArrow,
                contentAssetId = R.raw.exercise
            ),
            MultimediaOption(
                type = MultimediaContentType.AUDIO,
                title = "Audio Genérico",
                description = "Escuchar el audio genérico de la sesión",
                iconResId = R.drawable.outline_headphones_24,
                contentAssetId = R.raw.audio_explicativo
            ),
            MultimediaOption(
                type = MultimediaContentType.METAS,
                title = "Metas",
                description = "Configura tus metas para esta sesión",
                iconResId = R.drawable.outline_trophy_24
            )
        )
    }
}


class MultimediaSelector : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        // read incoming extras (if any)
        val sessionId = intent?.getLongExtra("sessionId", -1L) ?: -1L
        val sessionTitle = intent?.getStringExtra("sessionTitle") ?: "Sesión"
        val subSessionId = intent?.getLongExtra("subSessionId", -1L) ?: -1L
        val subSessionTitle = intent?.getStringExtra("subSessionTitle") ?: ""

        setContent {
            ColmaAppTheme {
                MultimediaSelectorScreen(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    subSessionId = subSessionId,
                    subSessionTitle = subSessionTitle,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaSelectorScreen(
    sessionId: Long,
    sessionTitle: String,
    subSessionId: Long,
    subSessionTitle: String,
    onBack: () -> Unit = {}
) {
    val signatureColor = colorResource(R.color.principal)
    val context = LocalContext.current
    val sTitle = remember(sessionTitle) { sessionTitle }


    fun friendlySubLabel(subId: Long, parentId: Long): String {
        return try {
            val maybeIndex = subId - parentId * 10
            if (maybeIndex in 1..999) {
                "Semana $maybeIndex"
            } else {
                "Semana $subId"
            }
        } catch (e: Exception) {
            "Semana $subId"
        }
    }

    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding(), color = colorResource(R.color.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(text = sessionTitle, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        val secondLine = when {
                            !subSessionTitle.isNullOrBlank() -> subSessionTitle
                            subSessionId != -1L && sessionId >= 0L -> friendlySubLabel(subSessionId, sessionId)
                            else -> null
                        }
                        if (secondLine != null) {
                            Text(text = secondLine, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )

            // Dynamic content based on subSessionId
            val multimediaOptions = getMultimediaOptionsForSubSession(subSessionId)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                multimediaOptions.forEach { option ->
                    MultimediaOptionCard(option = option, signatureColor = signatureColor) {
                        val intent = when (option.type) {
                            MultimediaContentType.DESCRIPTION -> Intent(context, DescriptionActivity::class.java)
                            MultimediaContentType.VIDEO -> Intent(context, ActionActivity::class.java)
                            MultimediaContentType.AUDIO -> Intent(context, FinishActivity::class.java)
                            MultimediaContentType.METAS -> Intent(context, MetasActivity::class.java)
                            MultimediaContentType.QUIZ -> Intent(context, HTAQuiz::class.java)
                        }
                        intent.apply {
                            putExtra("sessionId", sessionId)
                            putExtra("sessionTitle", sessionTitle)
                            putExtra("subSessionId", subSessionId)
                            putExtra("subSessionTitle", subSessionTitle)
                            // Pass the specific asset ID if available
                            if (option.contentAssetId != null) {
                                putExtra("contentAssetId", option.contentAssetId)
                            }
                        }
                        context.startActivity(intent)
                    }
                }
                Spacer(modifier = Modifier.weight(1f)) // push cards up if you want
            }
        }
    }
}

/**
 * Reusable Composable to display a multimedia option as a Card.
 */
@Composable
fun MultimediaOptionCard(
    option: MultimediaOption,
    signatureColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.iconImageVector != null) {
                Icon(
                    imageVector = option.iconImageVector,
                    contentDescription = option.title,
                    tint = signatureColor,
                    modifier = Modifier.size(36.dp)
                )
            } else if (option.iconResId != null) {
                Icon(
                    painter = painterResource(id = option.iconResId),
                    contentDescription = option.title,
                    tint = signatureColor,
                    modifier = Modifier.size(36.dp)
                )
            }


            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(option.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(option.description, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}