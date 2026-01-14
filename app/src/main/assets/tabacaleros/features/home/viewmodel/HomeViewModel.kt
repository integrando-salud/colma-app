package com.codeco.tabacaleros.features.home.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeco.tabacaleros.features.home.domain.HomeRepository
import com.codeco.tabacaleros.servicios.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val UPLOAD_FAILURE_THRESHOLD = 2

    fun onEvent(event: HomeEvent) {
        when(event) {
            is HomeEvent.OnDniUriChanged -> _uiState.update { it.copy(dniUri = event.uri) }
            is HomeEvent.OnProducerUriChanged -> _uiState.update { it.copy(producerUri = event.uri) }
            is HomeEvent.OnContinueClicked -> subirImagenes()
            is HomeEvent.ResetUploadState -> _uiState.update { it.copy(uploadState = UploadState.Idle) }
            is HomeEvent.TakeDniPictureClicked -> _uiState.update { it.copy(showDniCamera = true) }
            is HomeEvent.TakeProducerPictureClicked -> _uiState.update { it.copy(showProducerCamera = true) }
            is HomeEvent.OnDniPictureTaken -> {
                viewModelScope.launch {
                    val correctedUri = correctImageOrientation(event.file)
                    _uiState.update { it.copy(dniUri = correctedUri, showDniCamera = false) }
                }
            }
            is HomeEvent.OnProducerPictureTaken -> {
                viewModelScope.launch {
                    val correctedUri = correctImageOrientation(event.file)
                    _uiState.update { it.copy(producerUri = correctedUri, showProducerCamera = false) }
                }
            }
            is HomeEvent.DismissDniCamera -> _uiState.update { it.copy(showDniCamera = false) }
            is HomeEvent.DismissProducerCamera -> _uiState.update { it.copy(showProducerCamera = false) }
            is HomeEvent.EditDniPictureClicked -> _uiState.update { it.copy(editingDniPhoto = _uiState.value.dniUri) }
            is HomeEvent.EditProducerPictureClicked -> _uiState.update { it.copy(editingProducerPhoto = _uiState.value.producerUri) }
            is HomeEvent.OnDniPictureEdited -> {
                val newUri = Uri.fromFile(event.file)
                    .buildUpon()
                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                    .build()
                _uiState.update { it.copy(dniUri = newUri, editingDniPhoto = null) }
            }
            is HomeEvent.OnProducerPictureEdited -> {
                val newUri = Uri.fromFile(event.file)
                    .buildUpon()
                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                    .build()
                _uiState.update { it.copy(producerUri = newUri, editingProducerPhoto = null) }
            }
            is HomeEvent.DismissEditDni -> _uiState.update { it.copy(editingDniPhoto = null) }
            is HomeEvent.DismissEditProducer -> _uiState.update { it.copy(editingProducerPhoto = null) }
            is HomeEvent.DismissUploadFailedDialog -> _uiState.update { it.copy(showUploadFailedDialog = false) }
        }
    }

    private fun subirImagenes() {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadState = UploadState.Loading) }
            val dniUriValue = _uiState.value.dniUri
            val producerUriValue = _uiState.value.producerUri
            val productorInfo = sessionManager.producerInfoFlow.first()
            val productorId = productorInfo?.idProductor

            if (dniUriValue == null || producerUriValue == null) {
                _uiState.update { it.copy(uploadState = UploadState.Error("Debe seleccionar ambas imágenes")) }
                return@launch
            }

            if (productorId == null) {
                _uiState.update { it.copy(uploadState = UploadState.Error("No se pudo obtener el ID del productor")) }
                return@launch
            }

            val dniFile = uriToFile(dniUriValue, "dni_image.jpg")
            val producerFile = uriToFile(producerUriValue, "producer_image.jpg")

            if (dniFile == null || producerFile == null) {
                _uiState.update { it.copy(uploadState = UploadState.Error("Error al procesar las imágenes")) }
                return@launch
            }

            homeRepository.uploadImages(productorId, dniFile, producerFile)
                .onSuccess {
                    _uiState.update { it.copy(uploadState = UploadState.Success, uploadFailureCount = 0) }
                    Log.d("HomeViewModel", "Upload successful: $it")
                }
                .onFailure {
                    val newFailureCount = _uiState.value.uploadFailureCount + 1
                    val errorMessage = "Error en la subida: ${it.message}"

                    if (newFailureCount >= UPLOAD_FAILURE_THRESHOLD) {
                        _uiState.update { s ->
                            s.copy(
                                uploadState = UploadState.Idle,
                                showUploadFailedDialog = true,
                                uploadFailureCount = 0
                            )
                        }
                    } else {
                        _uiState.update { s ->
                            s.copy(
                                uploadState = UploadState.Error(errorMessage),
                                uploadFailureCount = newFailureCount
                            )
                        }
                    }
                    Log.e("HomeViewModel", errorMessage, it)
                }
        }
    }

    private fun uriToFile(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun correctImageOrientation(file: File): Uri {
        return withContext(Dispatchers.IO) {
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: // If bitmap fails to decode, return original file uri and hope for the best
                return@withContext Uri.fromFile(file)

            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val finalBitmap = if (degrees != 0f) {
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(
                    originalBitmap,
                    0, 0,
                    originalBitmap.width, originalBitmap.height,
                    matrix,
                    true
                )
            } else {
                originalBitmap
            }

            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            Uri.fromFile(file)
                .buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
        }
    }
}