package com.colma.colmaApp.servicios.Modelos

class EstadoAnimoModelo {
    data class EstadoAnimoRecord(
        val rowid: Long,
        val idPaciente: Int?,
        val fechaRegistro: String?,
        val fechaEdicion: String?,               // NEW
        val preguntas: List<Int?>, // length 9 -> pregunta_1..pregunta_9
        val fechaSincronizacion: String?
    )
}