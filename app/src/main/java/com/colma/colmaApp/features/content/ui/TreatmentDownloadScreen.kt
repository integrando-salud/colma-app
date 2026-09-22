package com.colma.colmaApp.features.content.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.colma.colmaApp.R
import com.colma.colmaApp.features.content.data.TreatmentDownloadRepositoryImpl
import com.colma.colmaApp.features.content.domain.TreatmentDownloadRepository
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TreatmentDownloadUiState(
    val statusText: String = "Iniciando...",
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    val error: String? = null,
    val showRetryButtons: Boolean = false
)

class TreatmentDownloadViewModel(
    private val repository: TreatmentDownloadRepository,
    private val context: Context,
    private val force: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreatmentDownloadUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startDownloadFlow()
    }

    fun startDownloadFlow() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    statusText = "Verificando conexión...",
                    progress = 0f,
                    error = null,
                    showRetryButtons = false,
                    isFinished = false
                )
            }

            val token = SessionManager.getAuthToken()
            val userId = SessionManager.getUserId()
            if (token == null || userId == null) {
                _uiState.update { 
                    it.copy(
                        statusText = "Error de autenticación.",
                        error = "Usuario no autenticado",
                        showRetryButtons = true
                    )
                }
                return@launch
            }

            val hasLocalData = repository.isTreatmentDownloaded(userId)
            if (hasLocalData && !force) {
                // Already have local data, skip directly to main screen
                _uiState.update { it.copy(isFinished = true, progress = 1.0f) }
                return@launch
            }

            val hasInternet = NetworkUtils.isInternetAvailable(context)
            if (!hasInternet) {
                _uiState.update { 
                    it.copy(
                        statusText = "No hay conexión a internet para descargar el tratamiento.",
                        error = "Sin conexión a internet",
                        showRetryButtons = true
                    )
                }
                return@launch
            }

            val result = repository.downloadAndSaveTreatment(token, userId) { progress, status ->
                _uiState.update { 
                    it.copy(
                        progress = progress.coerceIn(0f, 1f),
                        statusText = status
                    )
                }
            }

            result.onSuccess {
                _uiState.update { it.copy(isFinished = true, progress = 1.0f) }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        statusText = "Ocurrió un error al descargar el tratamiento.",
                        error = error.message ?: "Error desconocido",
                        showRetryButtons = true
                    )
                }
            }
        }
    }
}

@Composable
fun TreatmentDownloadScreen(
    navController: NavController,
    context: Context,
    force: Boolean = false,
    modifier: Modifier = Modifier
) {
    val repository = remember { TreatmentDownloadRepositoryImpl(context) }
    val viewModel = remember(force) { TreatmentDownloadViewModel(repository, context, force) }
    val uiState by viewModel.uiState.collectAsState()

    val principalColor = colorResource(id = R.color.principal)
    val principalBoton = colorResource(id = R.color.principal_boton)

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            navController.navigate("menu_main") {
                popUpTo("treatment_download") { inclusive = true }
            }
        }
    }

    // Animation for pulsing logo/icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        principalColor.copy(alpha = 0.15f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.error != null) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.colma_logo),
                        contentDescription = "Descargando",
                        tint = principalColor,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (uiState.error != null) "Error de Descarga" else "Descargando",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.statusText,
                        fontSize = 15.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.error == null) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = principalColor,
                        trackColor = principalColor.copy(alpha = 0.2f),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${(uiState.progress * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = principalColor
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.showRetryButtons) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.startDownloadFlow() },
                        colors = ButtonDefaults.buttonColors(containerColor = principalBoton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Reintentar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { 
                            navController.navigate("menu_main") {
                                popUpTo("treatment_download") { inclusive = true }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Continuar Offline", color = principalColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
