package com.colma.colmaApp.features.sesiones.SessionSteps.domain

import androidx.compose.ui.graphics.vector.ImageVector

import com.colma.colmaApp.features.content.model.QuizQuestion // Import QuizQuestion

enum class MultimediaContentType {
    DESCRIPTION,
    VIDEO,
    AUDIO,
    METAS,
    QUIZ
}

enum class TreatmentType { HTA, DIABETES, DBT_HTA }

data class MultimediaOption(
    val type: MultimediaContentType,
    val title: String,
    val description: String, // Short description for the card
    val fullDescription: String? = null, // Long description for WelcomeActivity
    val iconResId: Int? = null, // For drawable resources (e.g., R.drawable.xxx)
    val iconImageVector: ImageVector? = null, // For ImageVector icons (e.g., Icons.Default.PlayArrow)
    val contentAssetId: Int? = null, // For raw resource IDs (e.g., R.raw.xxx)
    val questions: List<QuizQuestion>? = null, // New field for embedded quiz questions
    val metasId: String? = null, // New field for metas identification
    val descriptionAudios: List<Int>? = null, // Audio resource IDs for description paragraphs
    val isCompleted: Boolean = false, // Indicates if the resource has been viewed/completed
    val localFilePath: String? = null,
    val localDescriptionAudios: List<String>? = null,
    val localDescriptionImages: List<String>? = null
)
