package com.codeco.tabacaleros.features.home.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codeco.tabacaleros.ui.theme.TabacalerosTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codeco.tabacaleros.R
import com.codeco.tabacaleros.features.DNI.ui.dniUI
import com.codeco.tabacaleros.features.censo.ui.CensoUI
import com.codeco.tabacaleros.features.home.ui.subViews.DNI
import com.codeco.tabacaleros.features.home.ui.subViews.ProducerCard
import androidx.compose.foundation.layout.systemBarsPadding
import com.codeco.tabacaleros.features.home.ui.CameraView.CameraScreen
import com.codeco.tabacaleros.features.home.ui.CameraView.EditPhotoScreen
import com.codeco.tabacaleros.features.home.viewmodel.HomeEvent
import com.codeco.tabacaleros.features.home.viewmodel.HomeUiState
import com.codeco.tabacaleros.features.home.viewmodel.HomeViewModel
import com.codeco.tabacaleros.features.home.viewmodel.HomeViewModelFactory
import com.codeco.tabacaleros.features.home.viewmodel.UploadState
import com.codeco.tabacaleros.features.utils.configureSystemBars
import java.io.File

class HomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()
        setContent {
            TabacalerosTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit
) {
    val pageCount = 2
    val pagerState = rememberPagerState { pageCount }
    val context = LocalContext.current

    val isComplete = uiState.dniUri != null && uiState.producerUri != null

    LaunchedEffect(uiState.uploadState) {
        when (uiState.uploadState) {
            is UploadState.Success -> {
                Toast.makeText(context, "Imágenes subidas con éxito", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, CensoUI::class.java)
                context.startActivity(intent)
                onEvent(HomeEvent.ResetUploadState) // Reset state after navigation
            }
            is UploadState.Error -> {
                val message = (uiState.uploadState as UploadState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onEvent(HomeEvent.ResetUploadState) // Reset state after showing error
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .systemBarsPadding(),
            containerColor = colorResource(R.color.background),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Registro Tabacalero", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, dniUI::class.java))
                                (context as Activity?)?.finish()
                            }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Abrir menú")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.principal),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Image(
                    painter = painterResource(id = R.drawable.logo_ministerio_tabaco),
                    contentDescription = "Footer",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 5.dp)
                        .height(80.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> DNI(
                                modifier = Modifier.fillMaxSize(),
                                dniUri = uiState.dniUri,
                                onTakeDniPicture = { onEvent(HomeEvent.TakeDniPictureClicked) },
                                onEditDniPicture = { onEvent(HomeEvent.EditDniPictureClicked) }
                            )
                            1 -> ProducerCard(
                                modifier = Modifier.fillMaxSize(),
                                producerUri = uiState.producerUri,
                                onTakeProducerPicture = { onEvent(HomeEvent.TakeProducerPictureClicked) },
                                onEditProducerPicture = { onEvent(HomeEvent.EditProducerPictureClicked) }
                            )
                        }
                    }

                    PagerDotsIndicator(
                        currentPage = pagerState.currentPage,
                        pageCount = pageCount,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Button(
                        onClick = {
                            onEvent(HomeEvent.OnContinueClicked)
                        },
                        enabled = isComplete && uiState.uploadState != UploadState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = colorResource(R.color.accent),
                                shape = RoundedCornerShape(8.dp)
                            ),
                    ) {
                        Text("Continuar")
                    }
                }
                if (uiState.uploadState == UploadState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        if (uiState.showDniCamera) {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                onImageCaptured = { file -> onEvent(HomeEvent.OnDniPictureTaken(file)) }
            )
        }

        if (uiState.showProducerCamera) {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                onImageCaptured = { file -> onEvent(HomeEvent.OnProducerPictureTaken(file)) }
            )
        }

        uiState.editingDniPhoto?.path?.let {
            EditPhotoScreen(
                photoFile = File(it),
                onApply = { file -> onEvent(HomeEvent.OnDniPictureEdited(file)) },
                onCancel = { onEvent(HomeEvent.DismissEditDni) }
            )
        }

        uiState.editingProducerPhoto?.path?.let {
            EditPhotoScreen(
                photoFile = File(it),
                onApply = { file -> onEvent(HomeEvent.OnProducerPictureEdited(file)) },
                onCancel = { onEvent(HomeEvent.DismissEditProducer) }
            )
        }

        if (uiState.showUploadFailedDialog) {
            AlertDialog(
                onDismissRequest = { /* Prevent dismissing by clicking outside */ },
                title = { Text("Error de Subida") },
                text = { Text("Hubo un problema al enviar las imágenes. Puede continuar, pero las imágenes no se han guardado en el servidor.") },
                confirmButton = {
                    Button(onClick = {
                        onEvent(HomeEvent.DismissUploadFailedDialog)
                        val intent = Intent(context, CensoUI::class.java)
                        context.startActivity(intent)
                    }) {
                        Text("Continuar")
                    }
                }
            )
        }
    }
}



@Composable
fun PagerDotsIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = colorResource(R.color.secundario),
    inactiveColor: Color = Color.Gray,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (index == currentPage) activeColor else inactiveColor)
            )
            if (index < pageCount - 1) {
                Spacer(Modifier.width(dotSpacing))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TabacalerosTheme {
        // This preview will not have a working viewmodel.
        // For a more complete preview, you might need to create a mock ViewModel.
        val context = LocalContext.current
        val factory = HomeViewModelFactory(context)
        val viewModel = factory.create(HomeViewModel::class.java)
        HomeScreen(
            uiState = HomeUiState(),
            onEvent = viewModel::onEvent
        )
    }
}
