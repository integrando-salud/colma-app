package com.colma.colmaApp

import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun `verificacion basica de operacion aritmetica`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `la serializacion de datos de sincronizacion mantiene el orden requerido de atributos`() {
        val request = SyncDataRequest(
            deviceTimestamp = 1785359996664L,
            userId = 13,
            app = "colmaApp",
            clientSyncTimestamp = "2026-07-29 18:19:56",
            sesionesCompletadas = 5,
            glucosa = emptyList(),
            presion = emptyList(),
            estadoAnimo = emptyList(),
            quiz = emptyList(),
            metas = emptyList(),
            multimediaRead = emptyList()
        )
        val json = Gson().toJson(request)
        
        val expectedKeysOrder = listOf(
            "\"app\":",
            "\"clientSyncTimestamp\":",
            "\"deviceTimestamp\":",
            "\"sesiones_completadas\":",
            "\"estado_animo\":",
            "\"glucosa\":",
            "\"metas\":",
            "\"progreso_elementos\":",
            "\"presion\":",
            "\"quiz\":",
            "\"userId\":"
        )
        
        var lastIndex = -1
        for (key in expectedKeysOrder) {
            val index = json.indexOf(key)
            assertTrue("Key $key not found in serialized JSON", index != -1)
            assertTrue("Key $key is out of order", index > lastIndex)
            lastIndex = index
        }
    }
}