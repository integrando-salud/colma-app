package com.colma.colmaApp.servicios.Modelos

class RegistrosSaludModelos {
    data class GlucosaRecord(
        val id: Long,
        val idUsuario: Int?,
        val valorMgdl: Double?,
        val ayuno: Int?,
        val fecha_registro: String?,
        val fecha_sincronizacion: String?,
        val fecha_hora_edicion: String? // ISO string
    )

    data class PresionRecord(
        val id: Long,
        val idUsuario: Int?,
        val sistolica: Double?,
        val diastolica: Double?,
        val fecha_registro: String?,
        val fecha_sincronizacion: String?,
        val fecha_hora_edicion: String?, // epoch millis
    )

    data class MetaRecord(
        val id_meta: Long,
        val id_usuario: Int?,
        val semana: Int?,
        val sesion: Int?,
        val tipo_actividad: String?,
        val actividad: String?,
        val lugar: String?,
        val acompañante: String?,
        val fecha_regisitro: String?,
        val fecha_edicion: String?,
        val fecha_sincronizacion: String?,
        val meta_cumplida: Int?,
        val fecha_objetivo: String?,
        val motivo_incumplimiento: String? = null
    )

    data class MultimediaReadRecord(
        val id_multimedia_read: Long,
        val id_usuario: Int?,
        val treatment_id: String?,
        val week_id: Int?,
        val session_id: Int?,
        val multimedia_type: String?,
        val multimedia_unique_key: String?,
        val fecha_registro: String?,
        val fecha_sincronizacion: String?
    )
}