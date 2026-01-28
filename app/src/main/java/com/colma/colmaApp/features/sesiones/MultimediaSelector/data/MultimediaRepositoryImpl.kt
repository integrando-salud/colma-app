package com.colma.colmaApp.features.sesiones.MultimediaSelector.data

import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.delay

class MultimediaRepositoryImpl : MultimediaRepository {

    override suspend fun getMultimediaOptions(
        treatmentTypes: List<TreatmentType>,
        subSessionId: Long
    ): Result<List<MultimediaOption>> {
        return try {
            delay(200) // Simulate I/O delay
            val options = treatmentTypes.flatMap { type ->
                getOptionsForTreatment(type, subSessionId)
            }
            Result.success(options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOptionsForTreatment(treatmentType: TreatmentType, subSessionId: Long): List<MultimediaOption> {
        return when (treatmentType) {
            TreatmentType.HTA -> when (subSessionId) {
                1L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Introducción",
                        description = "Ver el vídeo de introducción a la sesión",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.hta_sesion_5_1 // Video 1 asset
                    ),

                    )
                2L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Explicativo",
                        description = "Ver el vídeo explicativo de la sesión",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.hta_sesion_5_2 // Video 2 asset
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.QUIZ,
                        title = "Cuestionario Módulo 5",
                        description = "Responde las preguntas del módulo 5 de HTA",
                        iconResId = R.drawable.outline_trophy_24,
                        quizId = "hta"
                    ),

                    )
                3L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Principal",
                        description = "Primer video de la sesión",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.hta_sesion_5_3 // Video A asset
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Complementario",
                        description = "Segundo video de la sesión",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.hta_sesion_5_3_experiencia // Video B asset
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.METAS,
                        title = "Metas",
                        description = "Configura tus metas para esta sesión",
                        iconResId = R.drawable.outline_trophy_24,
                        metasId = "hta"
                    ),

                    )
                else -> listOf( // Default HTA options

                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Genérico",
                        description = "Ver el vídeo genérico de la sesión",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.exercise
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.AUDIO,
                        title = "Audio Genérico",
                        description = "Escuchar el audio genérico de la sesión",
                        iconResId = R.drawable.outline_headphones_24,
                        contentAssetId = R.raw.audio_explicativo
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.METAS,
                        title = "Metas",
                        description = "Configura tus metas para esta sesión",
                        iconResId = R.drawable.outline_trophy_24,
                        metasId = "hta"
                    )
                )
            }
            TreatmentType.DIABETES -> when (subSessionId) {
                1L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.DESCRIPTION,
                        title = "Descripción (Diabetes)",
                        description = "Conoce más sobre la diabetes.", // Short description for the card
                        fullDescription = """
                        ¿Cómo puedes saber si tus niveles de glucosa están controlados o no?
                        La única forma confiable de medir tu glucosa es analizar tu sangre. Algunas aplicaciones de celular dicen que pueden hacerlo con la cámara, ¡pero eso no es cierto!
                        Solo los glucómetros y los análisis de sangre realizados por profesionales pueden darte resultados reales.
                        Si te haces la prueba en ayunas y tu glucosa sale 126 o más, significa que está alta.
                        
                        Y si en cualquier momento del día el resultado sale 200 o más, también es una señal de que tu glucosa está elevada.
                        Además, tu médico puede pedirte un análisis de sangre para revisar la hemoglobina glicosilada 👨🏻‍⚕️ Este resultado muestra cómo han estado tus niveles de glucosa durante los últimos dos o tres meses. Si el valor es de 6.5% o más, significa que tu glucosa ha estado elevada de forma constante 🚨

                        Es muy importante que midas tu glucosa frecuentemente para ver si está controlada. Los médicos te pueden recomendar que te hagas una prueba de hemoglobina glicosilada cada tres meses.
                        
                        Hoy aprendiste qué es la diabetes y cómo saber si tu glucosa está alta. Cuidar tu glucosa es cuidar de ti. Con hábitos y constancia, puedes mantenerte fuerte y saludable cada día ✨
                    """.trimIndent(), // Long description for WelcomeActivity
                        iconResId = R.drawable.outline_unknown_document_24
                    ),

                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Introducción (Diabetes)",
                        description = "Ver el vídeo de introducción a la sesión de diabetes",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.diabetes_sesion_1_1 // Placeholder for diabetes video
                    ),


                    )
                2L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Explicativo (Diabetes)",
                        description = "Ver el vídeo explicativo de la sesión de diabetes",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.diabetes_sesion_2_1 // Placeholder for diabetes video
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.QUIZ,
                        title = "Cuestionario Módulo 5",
                        description = "Responde las preguntas del módulo 5 de Diabetes",
                        iconResId = R.drawable.outline_trophy_24,
                        quizId = "diabetes"
                    ),

                    )
                3L -> listOf(
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Principal (Diabetes)",
                        description = "Primer video de la sesión de diabetes",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.diabetes_sesion_3_1 // Placeholder for diabetes video
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Complementario (Diabetes)",
                        description = "Segundo video de la sesión de diabetes",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.diabetes_sesion_3_3 // Placeholder for diabetes video
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.METAS,
                        title = "Metas (Diabetes)",
                        description = "Configura tus metas para esta sesión de diabetes",
                        iconResId = R.drawable.outline_trophy_24,
                        metasId = "diabetes"
                    ),

                    )
                else -> listOf( // Default Diabetes options

                    MultimediaOption(
                        type = MultimediaContentType.VIDEO,
                        title = "Video Genérico (Diabetes)",
                        description = "Ver el vídeo genérico de la sesión de diabetes",
                        iconImageVector = Icons.Default.PlayArrow,
                        contentAssetId = R.raw.exercise
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.AUDIO,
                        title = "Audio Genérico (Diabetes)",
                        description = "Escuchar el audio genérico de la sesión de diabetes",
                        iconResId = R.drawable.outline_headphones_24,
                        contentAssetId = R.raw.audio_explicativo
                    ),
                    MultimediaOption(
                        type = MultimediaContentType.METAS,
                        title = "Metas (Diabetes)",
                        description = "Configura tus metas para esta sesión de diabetes",
                        iconResId = R.drawable.outline_trophy_24,
                        metasId = "diabetes"
                    )
                )
            }
        }
    }
}
