package com.codeco.tabacaleros.features.Menu.domain

import com.codeco.tabacaleros.servicios.internetCommunication.model.Component
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion

interface MenuRepository {
    suspend fun clearProducerInfo()
    suspend fun getComponents(): List<Component>?
    suspend fun getSelectedRadicacion(): Radicacion?
}
