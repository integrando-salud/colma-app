package com.codeco.tabacaleros.features.home.viewmodel

import android.net.Uri
import java.io.File

sealed interface UploadState {
    object Idle : UploadState
    object Loading : UploadState
    object Success : UploadState
    data class Error(val message: String) : UploadState
}

data class HomeUiState(
    val dniUri: Uri? = null,
    val producerUri: Uri? = null,
    val uploadState: UploadState = UploadState.Idle,
    val showDniCamera: Boolean = false,
    val showProducerCamera: Boolean = false,
    val editingDniPhoto: Uri? = null,
    val editingProducerPhoto: Uri? = null,
    val uploadFailureCount: Int = 0,
    val showUploadFailedDialog: Boolean = false
)

sealed class HomeEvent {
    data class OnDniUriChanged(val uri: Uri?) : HomeEvent()
    data class OnProducerUriChanged(val uri: Uri?) : HomeEvent()
    object OnContinueClicked : HomeEvent()
    object ResetUploadState : HomeEvent()

    // New events to handle UI interactions from composables
    object TakeDniPictureClicked : HomeEvent()
    object TakeProducerPictureClicked : HomeEvent()
    data class OnDniPictureTaken(val file: File) : HomeEvent()
    data class OnProducerPictureTaken(val file: File) : HomeEvent()
    object DismissDniCamera : HomeEvent()
    object DismissProducerCamera : HomeEvent()

    object EditDniPictureClicked : HomeEvent()
    object EditProducerPictureClicked : HomeEvent()
    data class OnDniPictureEdited(val file: File) : HomeEvent()
    data class OnProducerPictureEdited(val file: File) : HomeEvent()
    object DismissEditDni : HomeEvent()
    object DismissEditProducer : HomeEvent()
    object DismissUploadFailedDialog : HomeEvent()
}
