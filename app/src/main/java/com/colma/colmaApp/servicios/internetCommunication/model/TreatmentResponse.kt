package com.colma.colmaApp.servicios.internetCommunication.model

import com.colma.colmaApp.features.content.model.QuizQuestion
import com.google.gson.annotations.SerializedName

data class ServerTreatmentResponse(
    @SerializedName("diagnostico") val diagnostico: String? = null,
    @SerializedName("tratamiento") val tratamiento: ServerTreatment? = null
)

data class ServerTreatment(
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("semanas") val semanas: List<ServerSemana>? = null
)

data class ServerSemana(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("sesiones") val sesiones: List<ServerSesion>? = null
)

data class ServerSesion(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("content") val content: List<ServerContentItem>? = null
)

data class ServerContentItem(
    @SerializedName("tratamiento_elemento_id") val tratamientoElementoId: Int? = null,
    @SerializedName("tratamiento_multimedia_id") val tratamientoMultimediaId: Int? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("video") val video: String? = null,
    @SerializedName("audio") val audio: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("fullDescription") val fullDescription: String? = null,
    @SerializedName("iconResId") val iconResId: String? = null,
    @SerializedName("iconImageVector") val iconImageVector: String? = null,
    @SerializedName("contentAsset") val contentAsset: String? = null,
    @SerializedName("metasId") val metasId: String? = null,
    @SerializedName("metasOptions") val metasOptions: List<String>? = null,
    @SerializedName("questions") val questions: List<QuizQuestion>? = null
) {
    val elementoId: Int?
        get() = tratamientoElementoId ?: tratamientoMultimediaId

    val effectiveLink: String?
        get() = link ?: video ?: audio ?: image
}

data class MultimediaCompletadosResponse(
    @SerializedName("multimedia_completados") val multimediaCompletados: List<Int>? = null,
    @SerializedName("elementos_completados") val elementosCompletados: List<Int>? = null
) {
    val completedIds: List<Int>
        get() = elementosCompletados ?: multimediaCompletados ?: emptyList()
}

data class ElementosCompletadosResponse(
    @SerializedName("elementos_completados") val elementosCompletados: List<Int>? = null
)

data class DatosFuncionalesResponse(
    @SerializedName("quiz") val quiz: List<QuizData>? = null,
    @SerializedName("metas") val metas: List<MetaData>? = null
)



