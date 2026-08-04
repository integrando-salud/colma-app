package com.colma.colmaApp

import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSyncDataRequestSerializationOrder() {
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
        println("Serialized JSON: $json")
        
        val expectedKeysOrder = listOf(
            "\"app\":",
            "\"clientSyncTimestamp\":",
            "\"deviceTimestamp\":",
            "\"sesiones_completadas\":",
            "\"estado_animo\":",
            "\"glucosa\":",
            "\"metas\":",
            "\"multimedia_read\":",
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