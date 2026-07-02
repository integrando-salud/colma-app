package com.colma.colmaApp.features.content.domain

interface TreatmentDownloadRepository {
    suspend fun downloadAndSaveTreatment(
        token: String,
        userId: Int,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): Result<Unit>
    suspend fun isTreatmentDownloaded(userId: Int): Boolean
    suspend fun clearDownloadedTreatment(userId: Int)
}
