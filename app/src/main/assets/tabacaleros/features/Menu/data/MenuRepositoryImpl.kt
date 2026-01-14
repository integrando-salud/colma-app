package com.codeco.tabacaleros.features.Menu.data

import com.codeco.tabacaleros.features.Menu.domain.MenuRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.model.Component
import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion
import kotlinx.coroutines.flow.first

class MenuRepositoryImpl(
    private val sessionManager: SessionManager
) : MenuRepository {

    override suspend fun clearProducerInfo() {
        sessionManager.clearProducerInfo()
    }

    override suspend fun getComponents(): List<Component>? {
        return sessionManager.componentsFlow.first()
    }

    override suspend fun getSelectedRadicacion(): Radicacion? {
        return sessionManager.selectedRadicacionFlow.first()
    }
}
