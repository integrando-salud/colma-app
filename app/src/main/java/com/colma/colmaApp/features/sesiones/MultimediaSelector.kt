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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.colma.colmaApp.features.multimediaScreens.ActionActivity
import com.colma.colmaApp.features.multimediaScreens.DescriptionActivity
import com.colma.colmaApp.features.multimediaScreens.FinishActivity
import com.colma.colmaApp.features.multimediaScreens.MetasActivity

/**
 * MultimediaSelector Activity: shows three stacked cards: Descripción, Video, Audio.
 * It reads sessionId/sessionTitle/subSessionId/subSessionTitle from the incoming Intent extras
 * and passes them along when launching the target Activity.
 *
 * Expects that ActionActivity and FinishActivity exist in your project (video/audio screens).
 * A light DescriptionActivity is included below (you can replace it with your own).
 */
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

    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(text = sessionTitle, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        val secondLine = when {
                            !subSessionTitle.isNullOrBlank() -> subSessionTitle
                            subSessionId != null && sessionId >= 0L -> friendlySubLabel(subSessionId, sessionId)
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

            // content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Description card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            // open DescriptionActivity (defined below)
                            val i = Intent(context, DescriptionActivity::class.java).apply {
                                putExtra("sessionId", sessionId)
                                putExtra("sessionTitle", sessionTitle)
                                putExtra("subSessionId", subSessionId)
                                putExtra("subSessionTitle", subSessionTitle)
                            }
                            context.startActivity(i)
                        },
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
                        Icon(
                            painter = painterResource(R.drawable.outline_unknown_document_24),
                            contentDescription = "Descripción",
                            tint = signatureColor,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Descripción", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Ver información y objetivos de la sesión", fontSize = 13.sp, color = Color.Gray)
                        }

                    }
                }

                // Video card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            // start ActionActivity for video (reuses your existing ActionActivity)
                            val i = Intent(context, ActionActivity::class.java).apply {
                                putExtra("sessionId", sessionId)
                                putExtra("sessionTitle", sessionTitle)
                                putExtra("subSessionId", subSessionId)
                                putExtra("subSessionTitle", subSessionTitle)
                            }
                            context.startActivity(i)
                        },
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
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = signatureColor,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Video", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Ver el vídeo explicativo de la sesión", fontSize = 13.sp, color = Color.Gray)
                        }


                    }
                }

                // Audio card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            // start FinishActivity for audio (reuses your existing FinishActivity)
                            val i = Intent(context, FinishActivity::class.java).apply {
                                putExtra("sessionId", sessionId)
                                putExtra("sessionTitle", sessionTitle)
                                putExtra("subSessionId", subSessionId)
                                putExtra("subSessionTitle", subSessionTitle)
                            }
                            context.startActivity(i)
                        },
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
                        Icon(
                            painter = painterResource(R.drawable.outline_headphones_24),
                            contentDescription = "Audio",
                            tint = signatureColor,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Audio", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Escuchar el audio explicativo de la sesión", fontSize = 13.sp, color = Color.Gray)
                        }

                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            // start ActionActivity for video (reuses your existing ActionActivity)
                            val i = Intent(context, MetasActivity::class.java).apply {
                                putExtra("sessionId", sessionId)
                                putExtra("sessionTitle", sessionTitle)
                                putExtra("subSessionId", subSessionId)
                                putExtra("subSessionTitle", subSessionTitle)
                            }
                            context.startActivity(i)
                        },
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
                        Icon(
                            painter = painterResource(R.drawable.outline_trophy_24),
                            contentDescription = "Metas",
                            tint = signatureColor,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Metas", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Configura tus metas para esta sesion", fontSize = 13.sp, color = Color.Gray)
                        }


                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // push cards up if you want
            }
        }
    }
}