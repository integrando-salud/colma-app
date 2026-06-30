package com.colma.colmaApp.features.content.domain

interface TreatmentDownloadRepository {
    suspend fun downloadAndSaveTreatment(
        token: String,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): Result<Unit>
    suspend fun isTreatmentDownloaded(): Boolean
    suspend fun clearDownloadedTreatment()
}
