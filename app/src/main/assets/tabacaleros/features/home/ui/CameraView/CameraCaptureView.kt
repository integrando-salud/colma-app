package com.codeco.tabacaleros.features.home.ui.CameraView

import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import com.codeco.tabacaleros.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.Surface
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    onImageCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permiso de camara denegado", Toast.LENGTH_SHORT).show()

        }
    }

    LaunchedEffect(granted, requestedOnce) {
        if (!granted && !requestedOnce) {
            requestedOnce = true
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val rotation = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            (context as? android.app.Activity)
                ?.windowManager
                ?.defaultDisplay
                ?.rotation
                ?: Surface.ROTATION_0
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (granted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        scaleType = PreviewView.ScaleType.FILL_CENTER

                        val cameraProviderFut = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFut.addListener({
                            try {
                                val cameraProvider = cameraProviderFut.get()
                                val previewUseCase = Preview.Builder()
                                    .setTargetRotation(rotation)
                                    .build()
                                    .also { it.setSurfaceProvider(this.surfaceProvider) }

                                val captureUseCase = ImageCapture.Builder()
                                    .setTargetRotation(rotation)
                                    .build()

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    previewUseCase,
                                    captureUseCase
                                )
                                imageCapture = captureUseCase
                            } catch (exc: Exception) {
                                Log.e("CameraScreen", "Failed to bind camera use cases", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission is required to preview and take photos.\nThe system permission dialog will appear automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (!granted) {
                    Toast.makeText(context, "Requesting camera permission...", Toast.LENGTH_SHORT).show()
                    return@FloatingActionButton
                }

                val ic = imageCapture ?: run {
                    Toast.makeText(context, "Camera is not ready yet", Toast.LENGTH_SHORT).show()
                    return@FloatingActionButton
                }

                val photoFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                ic.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraScreen", "Photo capture failed", exc)
                            Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }

                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            onImageCaptured(photoFile)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(100.dp)
                .padding(bottom = 24.dp),
            containerColor = colorResource(R.color.principal)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_camera_alt_24),
                contentDescription = "Take photo",
                modifier = Modifier.size(50.dp),
                tint = Color.hsl(0f, 0f, 0.95f)
            )
        }
    }
}
