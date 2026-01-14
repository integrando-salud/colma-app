package com.codeco.tabacaleros.features.DNI.viewmodel

import com.codeco.tabacaleros.features.DNI.model.ProductorInfo

data class DniUiState(
    val documentNumber: String = "",
    val productorInfo: ProductorInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTouched: Boolean = false,
    val phoneNumber: String = "",
    val phoneVerificationResult: Boolean? = null,
    val showWarningDialog: Boolean = false,
    val dialogMessage: String = "",
    val selectedCasaColonoName: String? = null
)

sealed class DniEvent {
    data class DocumentNumberChanged(val number: String) : DniEvent()
    data class PhoneNumberChanged(val phone: String) : DniEvent()
    data class PhoneVerificationChanged(val isVerified: Boolean) : DniEvent()
    object ContinueClicked : DniEvent()
    object ScreenTouched: DniEvent()
    object WarningDialogDismissed : DniEvent()
}
