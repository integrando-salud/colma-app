package com.colma.colmaApp.servicios.internetCommunication.model

import com.google.gson.annotations.SerializedName

data class SyncDataRequest(
    val deviceTimestamp: Long,
    val userId: Int,
    val app: String,
    val clientSyncTimestamp: String,
    val glucosa: List<GlucosaData>,
    val presion: List<PresionData>,
    @SerializedName("estado_animo") val estadoAnimo: List<EstadoAnimoData>,
    val quiz: List<QuizData>,
    val metas: List<MetaData>,
    @SerializedName("multimedia_read") val multimediaRead: List<MultimediaReadData>
)

data class GlucosaData(
    @SerializedName("id_glucosa") val idGlucosa: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("valor_mgdl") val valorMgdl: Float,
    val ayuno: Int,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_usuario") val fechaUsuario: String,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String,
    val advertencia: String
)

data class PresionData(
    @SerializedName("id_presion") val idPresion: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    val sistolica: Float,
    val diastolica: Float,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_usuario") val fechaUsuario: String,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String,
    val advertencia: String
)

data class EstadoAnimoData(
    @SerializedName("id_estado_animo") val idEstadoAnimo: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_usuario") val fechaUsuario: String,
    @SerializedName("pregunta_1") val pregunta1: Int,
    @SerializedName("pregunta_2") val pregunta2: Int,
    @SerializedName("pregunta_3") val pregunta3: Int,
    @SerializedName("pregunta_4") val pregunta4: Int,
    @SerializedName("pregunta_5") val pregunta5: Int,
    @SerializedName("pregunta_6") val pregunta6: Int,
    @SerializedName("pregunta_7") val pregunta7: Int,
    @SerializedName("pregunta_8") val pregunta8: Int,
    @SerializedName("pregunta_9") val pregunta9: Int,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String
)

data class QuizData(
    @SerializedName("id_quiz") val idQuiz: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("quiz_type") val quizType: String,
    val sessionId: Int,
    val subSessionId: Int?,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_usuario") val fechaUsuario: String,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String,
    val preguntas: List<PreguntaData>,
    @SerializedName("puntaje_total") val puntajeTotal: Int,
    @SerializedName("puntaje_obtenido") val puntajeObtenido: Int,
    @SerializedName("porcentaje_correcto") val porcentajeCorrecto: Int
)

data class PreguntaData(
    @SerializedName("pregunta_index") val preguntaIndex: Int,
    @SerializedName("pregunta_texto") val preguntaTexto: String,
    @SerializedName("respuesta_usuario") val respuestaUsuario: Int,
    @SerializedName("respuesta_usuario_texto") val respuestaUsuarioTexto: String,
    @SerializedName("respuesta_correcta") val respuestaCorrecta: Int,
    @SerializedName("respuesta_correcta_texto") val respuestaCorrectaTexto: String,
    @SerializedName("es_correcta") val esCorrecta: Boolean,
    @SerializedName("posibles_respuestas") val posiblesRespuestas: List<String>
)

data class MetaData(
    @SerializedName("id_meta") val idMeta: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    val semana: Int,
    val sesion: Int,
    @SerializedName("tipo_actividad") val tipoActividad: String,
    val actividad: String,
    val lugar: String,
    val acompaniante: String?,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_usuario") val fechaUsuario: String,
    @SerializedName("fecha_edicion") val fechaEdicion: String?,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String,
    @SerializedName("meta_cumplida") val metaCumplida: Boolean,
    @SerializedName("fecha_objetivo") val fechaObjetivo: String,
    @SerializedName("motivo_incumplimiento") val motivoIncumplimiento: String?
)

data class MultimediaReadData(
    @SerializedName("id_multimedia_read") val idMultimediaRead: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("treatment_id") val treatmentId: String,
    @SerializedName("week_id") val weekId: Int,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("multimedia_type") val multimediaType: String,
    @SerializedName("multimedia_unique_key") val multimediaUniqueKey: String,
    @SerializedName("fecha_registro") val fechaRegistro: String,
    @SerializedName("fecha_sincronizacion") val fechaSincronizacion: String
)
