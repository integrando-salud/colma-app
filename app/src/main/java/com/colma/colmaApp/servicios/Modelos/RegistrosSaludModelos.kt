package com.colma.colmaApp.servicios.Modelos

class RegistrosSaludModelos {
    data class GlucosaRecord(
        val id: Long,
        val idUsuario: Int?,
        val valorMgdl: Double?,
        val fechaRegistro: String?,
        val horaRegistro: String?
    )

    data class PresionRecord(
        val id: Long,
        val idUsuario: Int?,
        val sistolica: Double?,
        val diastolica: Double?,
        val fechaRegistro: String?,
        val horaRegistro: String?
    )
}