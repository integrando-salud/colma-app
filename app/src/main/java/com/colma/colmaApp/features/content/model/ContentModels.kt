package com.colma.colmaApp.features.content.model

import kotlinx.serialization.Serializable
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType

@Serializable
data class ContentDictionary(
    val treatments: List<TreatmentContent>
)

@Serializable
data class TreatmentContent(
    val id: TreatmentType,
    val name: String,
    val sessions: List<SessionContent>
)

@Serializable
data class SessionContent(
    val id: Long,
    val title: String,
    val thumbnail: String, // String representation of resource name (e.g., "missing_photo")
    val duration: String,
    val subSessions: List<SubSessionContent>
)

@Serializable
data class SubSessionContent(
    val id: Long,
    val title: String,
    val duration: String,
    val multimedia: List<MultimediaContent>
)

@Serializable
data class MultimediaContent(
    val type: MultimediaContentType,
    val title: String,
    val description: String,
    val fullDescription: String? = null,
    val iconResId: String? = null, // String representation of resource name (e.g., "outline_trophy_24")
    val iconImageVector: String? = null, // String representation of ImageVector name (e.g., "PlayArrow")
    val contentAsset: String? = null, // String representation of resource name (e.g., "hta_sesion_5_1")
    val quizId: String? = null,
    val metasId: String? = null
)
