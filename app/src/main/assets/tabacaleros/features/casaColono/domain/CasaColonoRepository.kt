package com.codeco.tabacaleros.features.casaColono.domain

import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion

interface CasaColonoRepository {
    suspend fun getRadicaciones(): Result<List<Radicacion>>
}
