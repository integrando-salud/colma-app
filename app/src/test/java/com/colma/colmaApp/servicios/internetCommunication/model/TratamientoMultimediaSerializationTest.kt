package com.colma.colmaApp.servicios.internetCommunication.model

import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.google.gson.Gson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TratamientoMultimediaSerializationTest {

    @Test
    fun `deserializacion de ServerTreatmentResponse extrae correctamente tratamiento_multimedia_id`() {
        val serverJson = """
        {
            "tratamiento": {
                "tipo": "HTA",
                "nombre": "Hipertensión arterial",
                "semanas": [
                    {
                        "id": 1,
                        "title": "Semana 1",
                        "sesiones": [
                            {
                                "id": 1,
                                "title": "Sesión 1",
                                "content": [
                                    {
                                        "tratamiento_multimedia_id": 1,
                                        "type": "VIDEO",
                                        "title": "video-1.mp4",
                                        "link": "http://localhost/api/multimedia/tratamientos/PE/HTA/semana-1/sesion-1/video-1.mp4"
                                    }
                                ]
                            },
                            {
                                "id": 3,
                                "title": "Sesión 3",
                                "content": [
                                    {
                                        "tratamiento_multimedia_id": 3,
                                        "type": "AUDIO",
                                        "title": "audio-1.mp3",
                                        "link": "http://localhost/api/multimedia/tratamientos/PE/HTA/semana-1/sesion-3/audio-1.mp3"
                                    },
                                    {
                                        "tratamiento_multimedia_id": 4,
                                        "type": "IMAGEN",
                                        "title": "imagen-1.png",
                                        "link": "http://localhost/api/multimedia/tratamientos/PE/HTA/semana-1/sesion-3/imagen-1.png"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

        val response = Gson().fromJson(serverJson, ServerTreatmentResponse::class.java)
        assertNotNull(response.tratamiento)
        val semana1 = response.tratamiento?.semanas?.first()
        assertNotNull(semana1)
        val sesion1 = semana1?.sesiones?.first()
        val item1 = sesion1?.content?.first()
        assertEquals(1, item1?.tratamientoMultimediaId)
        assertEquals("VIDEO", item1?.type)

        val sesion3 = semana1?.sesiones?.get(1)
        val audioItem = sesion3?.content?.get(0)
        val imageItem = sesion3?.content?.get(1)
        assertEquals(3, audioItem?.tratamientoMultimediaId)
        assertEquals(4, imageItem?.tratamientoMultimediaId)
    }

    @Test
    fun `serializacion y deserializacion de MultimediaContent preserva tratamiento_multimedia_id`() {
        val multimedia = MultimediaContent(
            type = MultimediaContentType.VIDEO,
            title = "Video 1",
            description = "Descripción del video",
            contentAsset = "/path/to/video-1.mp4",
            tratamientoMultimediaId = 42
        )

        val jsonString = Json.encodeToString(multimedia)
        assertTrue(jsonString.contains("\"tratamiento_multimedia_id\":42") || jsonString.contains("\"tratamiento_multimedia_id\": 42"))

        val decoded = Json.decodeFromString<MultimediaContent>(jsonString)
        assertEquals(42, decoded.tratamientoMultimediaId)
        assertEquals(MultimediaContentType.VIDEO, decoded.type)
        assertEquals("Video 1", decoded.title)
    }

    @Test
    fun `serializacion de SyncDataRequest incluye tratamiento_multimedia_id en multimedia_read`() {
        val multimediaReadItem = MultimediaReadData(
            idMultimediaRead = 1,
            idUsuario = 10,
            treatmentId = "HTA",
            weekId = 1,
            sessionId = 1,
            multimediaType = "VIDEO",
            multimediaUniqueKey = "LOCAL_HTA_1_1_video1",
            fechaRegistro = "2026-09-11 10:00:00",
            fechaSincronizacion = "2026-09-11 10:30:00",
            tratamientoMultimediaId = 1
        )

        val request = SyncDataRequest(
            deviceTimestamp = 1785359996664L,
            userId = 10,
            app = "colmaApp",
            clientSyncTimestamp = "2026-09-11 10:30:00",
            sesionesCompletadas = 1,
            glucosa = emptyList(),
            presion = emptyList(),
            estadoAnimo = emptyList(),
            quiz = emptyList(),
            metas = emptyList(),
            multimediaRead = listOf(multimediaReadItem)
        )

        val json = Gson().toJson(request)
        assertTrue("JSON debe contener 'tratamiento_multimedia_id'", json.contains("\"tratamiento_multimedia_id\":1"))

        val deserialized = Gson().fromJson(json, SyncDataRequest::class.java)
        assertEquals(1, deserialized.multimediaRead.size)
        assertEquals(1, deserialized.multimediaRead[0].tratamientoMultimediaId)
        assertEquals("LOCAL_HTA_1_1_video1", deserialized.multimediaRead[0].multimediaUniqueKey)
    }

    @Test
    fun `deserializacion de MultimediaCompletadosResponse extrae lista de ids completados`() {
        val json = """
        {
            "multimedia_completados": [12, 18, 31]
        }
        """.trimIndent()

        val response = Gson().fromJson(json, MultimediaCompletadosResponse::class.java)
        assertNotNull(response)
        assertEquals(listOf(12, 18, 31), response.multimediaCompletados)
    }
}

