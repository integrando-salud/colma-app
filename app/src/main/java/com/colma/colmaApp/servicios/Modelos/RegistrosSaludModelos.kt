package com.colma.colmaApp.servicios.Modelos

class RegistrosSaludModelos {
    data class GlucosaRecord(
        val id: Long,
        val idUsuario: Int?,
        val valorMgdl: Double?,
        val ayuno: Int?,
        val fecha_registro: String?,
        val fecha_sincronizacion: String?
    )

    data class PresionRecord(
        val id: Long,
        val idUsuario: Int?,
        val sistolica: Double?,
        val diastolica: Double?,
        val fecha_registro: String?,
        val fecha_sincronizacion: String?
    )
}