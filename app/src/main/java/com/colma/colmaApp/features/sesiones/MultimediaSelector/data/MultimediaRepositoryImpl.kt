package com.colma.colmaApp.features.sesiones.MultimediaSelector.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.colma.colmaApp.R
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.utils.ResourceUtils

class MultimediaRepositoryImpl(
    private val contentRepository: ContentRepository,
    private val context: Context
) : MultimediaRepository {

    override suspend fun getMultimediaOptions(
        treatmentTypes: List<TreatmentType>,
        subSessionId: Long
    ): Result<List<MultimediaOption>> {
        // Assuming we only care about the first treatment type for now based on previous logic
        val treatmentType = treatmentTypes.firstOrNull() ?: return Result.failure(Exception("No treatment type provided"))

        return contentRepository.getMultimediaOptions(treatmentType, subSessionId).fold(
            onSuccess = { multimediaContents ->
                Result.success(multimediaContents.map { multimediaContent ->
                    MultimediaOption(
                        type = multimediaContent.type,
                        title = multimediaContent.title,
                        description = multimediaContent.description,
                        fullDescription = multimediaContent.fullDescription,
                        iconResId = multimediaContent.iconResId?.let {
                            ResourceUtils.getResourceId(context, it, "drawable")
                        },
                        iconImageVector = multimediaContent.iconImageVector?.let {
                            mapImageVector(it)
                        },
                        contentAssetId = multimediaContent.contentAsset?.let {
                            ResourceUtils.getResourceId(context, it, "raw")
                        },
                        quizId = multimediaContent.quizId,
                        metasId = multimediaContent.metasId
                    )
                })
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun mapImageVector(name: String): ImageVector? {
        // This is a simple mapping. More complex solutions might use reflection or a larger map.
        return when (name) {
            "PlayArrow" -> Icons.Default.PlayArrow
            // Add other ImageVector mappings as needed
            else -> null
        }
    }
}
