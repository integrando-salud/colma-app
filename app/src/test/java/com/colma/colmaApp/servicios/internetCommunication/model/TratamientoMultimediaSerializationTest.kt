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
    fun `serializacion de SyncDataRequest convierte multimediaRead a progreso_elementos con tratamiento_elemento_id`() {
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
        assertTrue("JSON debe contener 'progreso_elementos'", json.contains("\"progreso_elementos\""))
        assertTrue("JSON debe contener 'tratamiento_elemento_id'", json.contains("\"tratamiento_elemento_id\":1") || json.contains("\"tratamiento_elemento_id\": 1"))

        val deserialized = Gson().fromJson(json, SyncDataRequest::class.java)
        assertEquals(1, deserialized.progresoElementos.size)
        assertEquals(1, deserialized.progresoElementos[0].tratamientoElementoId)
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
        assertEquals(listOf(12, 18, 31), response.completedIds)
    }

    @Test
    fun `serializacion de SyncDataRequest con progreso_elementos y tratamiento_elemento_id`() {
        val progresoItem = ProgresoElementoData(
            tratamientoElementoId = 208,
            completado = true,
            fechaEstado = "2026-03-31 08:30:00",
            fechaSincronizacion = "2026-03-31 08:30:00"
        )
        val quizItem = QuizData(
            tratamientoElementoId = 210,
            fechaRegistro = "2026-03-31 08:35:00",
            fechaUsuario = "2026-03-31 08:35:00",
            fechaSincronizacion = "2026-03-31 08:35:00",
            preguntas = listOf(
                PreguntaData(
                    preguntaIndex = 0,
                    preguntaTexto = "Pregunta 1",
                    respuestaUsuario = 1,
                    respuestaUsuarioTexto = "Opción B",
                    respuestaCorrecta = 1,
                    respuestaCorrectaTexto = "Opción B",
                    esCorrecta = true,
                    posiblesRespuestas = listOf("Opción A", "Opción B")
                )
            )
        )
        val metaItem = MetaData(
            idMeta = 1,
            idUsuario = 70,
            tratamientoElementoId = 220,
            semana = 1,
            sesion = 3,
            tipoActividad = "caminar",
            actividad = "Caminar 30 min",
            lugar = "Parque",
            acompaniante = "Familiar",
            fechaRegistro = "2026-03-31 09:00:00",
            fechaUsuario = "2026-03-31 09:00:00",
            fechaEdicion = null,
            fechaSincronizacion = "2026-03-31 09:00:00",
            metaCumplida = true,
            fechaObjetivo = "2026-04-05",
            motivoIncumplimiento = null
        )

        val request = SyncDataRequest(
            deviceTimestamp = 1785359996664L,
            userId = 70,
            app = "colmaApp",
            clientSyncTimestamp = "2026-03-31 09:00:00",
            sesionesCompletadas = 1,
            glucosa = emptyList(),
            presion = emptyList(),
            estadoAnimo = emptyList(),
            quiz = listOf(quizItem),
            metas = listOf(metaItem),
            progresoElementos = listOf(progresoItem)
        )

        val json = Gson().toJson(request)
        assertTrue(json.contains("\"progreso_elementos\""))
        assertTrue(json.contains("\"tratamiento_elemento_id\":208") || json.contains("\"tratamiento_elemento_id\": 208"))
        assertTrue(json.contains("\"tratamiento_elemento_id\":210") || json.contains("\"tratamiento_elemento_id\": 210"))
        assertTrue(json.contains("\"tratamiento_elemento_id\":220") || json.contains("\"tratamiento_elemento_id\": 220"))

        val deserialized = Gson().fromJson(json, SyncDataRequest::class.java)
        assertEquals(1, deserialized.progresoElementos.size)
        assertEquals(208, deserialized.progresoElementos[0].tratamientoElementoId)
        assertEquals(210, deserialized.quiz[0].tratamientoElementoId)
        assertEquals(220, deserialized.metas[0].tratamientoElementoId)
    }

    @Test
    fun `deserializacion de ElementosCompletadosResponse y DatosFuncionalesResponse`() {
        val completadosJson = """
        {
            "elementos_completados": [208, 209, 210]
        }
        """.trimIndent()
        val completados = Gson().fromJson(completadosJson, ElementosCompletadosResponse::class.java)
        assertEquals(listOf(208, 209, 210), completados.elementosCompletados)

        val funcionalesJson = """
        {
            "quiz": [
                {
                    "tratamiento_elemento_id": 210,
                    "fecha_registro": "2026-03-31 08:35:00",
                    "fecha_usuario": "2026-03-31 08:35:00",
                    "fecha_sincronizacion": "2026-03-31 08:35:00",
                    "preguntas": []
                }
            ],
            "metas": [
                {
                    "id_meta": 1,
                    "id_usuario": 70,
                    "tratamiento_elemento_id": 220,
                    "tipo_actividad": "caminar",
                    "actividad": "Caminar 30 min",
                    "lugar": "Parque",
                    "acompaniante": "Familiar",
                    "meta_cumplida": true,
                    "fecha_registro": "2026-03-31 09:00:00",
                    "fecha_usuario": "2026-03-31 09:00:00",
                    "fecha_sincronizacion": "2026-03-31 09:00:00",
                    "fecha_objetivo": "2026-04-05"
                }
            ]
        }
        """.trimIndent()
        val funcionales = Gson().fromJson(funcionalesJson, DatosFuncionalesResponse::class.java)
        assertEquals(1, funcionales.quiz?.size)
        assertEquals(210, funcionales.quiz?.get(0)?.tratamientoElementoId)
        assertEquals(1, funcionales.metas?.size)
        assertEquals(220, funcionales.metas?.get(0)?.tratamientoElementoId)
    }
}


