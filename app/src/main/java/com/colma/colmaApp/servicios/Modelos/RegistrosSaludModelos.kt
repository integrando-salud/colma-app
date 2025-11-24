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
        val fecha_hora_edicion: String? // epoch millis
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
        val fecha_objetivo: String?
    )
}