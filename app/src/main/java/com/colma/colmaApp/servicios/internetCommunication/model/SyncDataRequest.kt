package com.colma.colmaApp.servicios.internetCommunication.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

@JsonAdapter(SyncDataRequestAdapter::class)
data class SyncDataRequest(
    val deviceTimestamp: Long,
    val userId: Int,
    val app: String,
    val clientSyncTimestamp: String,
    @SerializedName("sesiones_completadas") val sesionesCompletadas: Int = 0,
    val glucosa: List<GlucosaData>,
    val presion: List<PresionData>,
    @SerializedName("estado_animo") val estadoAnimo: List<EstadoAnimoData>,
    val quiz: List<QuizData>,
    val metas: List<MetaData>,
    @SerializedName("multimedia_read") val multimediaRead: List<MultimediaReadData>
)

class SyncDataRequestAdapter : JsonSerializer<SyncDataRequest>, JsonDeserializer<SyncDataRequest> {
    override fun serialize(
        src: SyncDataRequest,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("app", src.app)
        jsonObject.addProperty("clientSyncTimestamp", src.clientSyncTimestamp)
        jsonObject.addProperty("deviceTimestamp", src.deviceTimestamp)
        jsonObject.addProperty("sesiones_completadas", src.sesionesCompletadas)
        jsonObject.add("estado_animo", context.serialize(src.estadoAnimo))
        jsonObject.add("glucosa", context.serialize(src.glucosa))
        jsonObject.add("metas", context.serialize(src.metas))
        jsonObject.add("multimedia_read", context.serialize(src.multimediaRead))
        jsonObject.add("presion", context.serialize(src.presion))
        jsonObject.add("quiz", context.serialize(src.quiz))
        jsonObject.addProperty("userId", src.userId)
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SyncDataRequest {
        val obj = json.asJsonObject
        val deviceTimestamp = obj.get("deviceTimestamp")?.asLong ?: 0L
        val userId = obj.get("userId")?.asInt ?: 0
        val app = obj.get("app")?.asString ?: ""
        val clientSyncTimestamp = obj.get("clientSyncTimestamp")?.asString ?: ""
        val sesionesCompletadas = (obj.get("sesiones_completadas") ?: obj.get("sesionesCompletadas"))?.asInt ?: 0
        
        val glucosa: List<GlucosaData> = context.deserialize<Array<GlucosaData>>(obj.get("glucosa"), Array<GlucosaData>::class.java)?.toList() ?: emptyList()
        val presion: List<PresionData> = context.deserialize<Array<PresionData>>(obj.get("presion"), Array<PresionData>::class.java)?.toList() ?: emptyList()
        val estadoAnimo: List<EstadoAnimoData> = context.deserialize<Array<EstadoAnimoData>>(obj.get("estado_animo") ?: obj.get("estadoAnimo"), Array<EstadoAnimoData>::class.java)?.toList() ?: emptyList()
        val quiz: List<QuizData> = context.deserialize<Array<QuizData>>(obj.get("quiz"), Array<QuizData>::class.java)?.toList() ?: emptyList()
        val metas: List<MetaData> = context.deserialize<Array<MetaData>>(obj.get("metas"), Array<MetaData>::class.java)?.toList() ?: emptyList()
        val multimediaRead: List<MultimediaReadData> = context.deserialize<Array<MultimediaReadData>>(obj.get("multimedia_read") ?: obj.get("multimediaRead"), Array<MultimediaReadData>::class.java)?.toList() ?: emptyList()

        return SyncDataRequest(
            deviceTimestamp = deviceTimestamp,
            userId = userId,
            app = app,
            clientSyncTimestamp = clientSyncTimestamp,
            sesionesCompletadas = sesionesCompletadas,
            glucosa = glucosa,
            presion = presion,
            estadoAnimo = estadoAnimo,
            quiz = quiz,
            metas = metas,
            multimediaRead = multimediaRead
        )
    }
}

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
