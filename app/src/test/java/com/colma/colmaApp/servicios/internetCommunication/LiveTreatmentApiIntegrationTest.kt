package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.servicios.internetCommunication.model.ServerTreatmentResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTreatmentApiIntegrationTest {

    private val loginService = LoginService()
    private val treatmentService = TreatmentService()

    private val testUsers = listOf(
        UserData("Sonia Venegas", "03843296", "Argentina", "DBT"),
        UserData("Maria Quintanilla", "50697085", "Argentina", "HTA + DBT"),
        UserData("Francisca Delapaz", "44876430", "Argentina", "HTA"),
        UserData("Erik Becerra", "81306555", "Perú", "DBT"),
        UserData("Ignacio Heaton", "98765432", "Perú", "HTA + DBT"),
        UserData("Guillermo Arribas", "15864485", "Perú", "HTA")
    )

    data class UserData(
        val name: String,
        val dni: String,
        val country: String,
        val condition: String,
        val password: String = "123456"
    )

    @Test
    fun `inspeccionar respuesta real de login y get tratamiento para todos los usuarios de prueba`() = runBlocking {
        println("\n================================================================================")
        println("=== INICIO DE INSPECCIÓN DE ENDPOINTS EN VIVO ===")
        println("================================================================================\n")

        val prettyGson = GsonBuilder().setPrettyPrinting().create()

        for (user in testUsers) {
            println("--------------------------------------------------------------------------------")
            println("Usuario: ${user.name} | DNI: ${user.dni} | País: ${user.country} | Condición: ${user.condition}")
            println("--------------------------------------------------------------------------------")

            // 1. Login
            val loginResponse = loginService.login(user.dni, user.password)
            println("1. Login HTTP Status: ${loginResponse.code()}")
            if (!loginResponse.isSuccessful) {
                println("   ERROR en Login: ${loginResponse.errorBody()?.string()}")
                continue
            }

            val loginBody = loginResponse.body()
            assertNotNull(loginBody)
            val token = loginBody!!.token
            val userId = loginBody.usuario.id
            println("   Token: ${token.take(15)}... | ID Paciente en BD: $userId")

            // 2. GET /api/tratamiento
            val treatmentResponse = treatmentService.getTratamiento(token)
            println("2. GET /api/tratamiento HTTP Status: ${treatmentResponse.code()}")
            if (!treatmentResponse.isSuccessful) {
                println("   ERROR en Tratamiento: ${treatmentResponse.errorBody()?.string()}")
                continue
            }

            val rawJson = treatmentResponse.body()?.string() ?: "{}"
            val jsonObject = Gson().fromJson(rawJson, JsonObject::class.java)
            val tratamientoObj = jsonObject.getAsJsonObject("tratamiento")

            if (tratamientoObj == null) {
                println("   Respuesta vacía o sin objeto 'tratamiento': $rawJson")
                continue
            }

            val tipo = tratamientoObj.get("tipo")?.asString ?: "N/A"
            val nombre = tratamientoObj.get("nombre")?.asString ?: "N/A"
            val semanasArray = tratamientoObj.getAsJsonArray("semanas")
            val totalSemanas = semanasArray?.size() ?: 0

            println("   Tratamiento Tipo: '$tipo' | Nombre: '$nombre' | Semanas: $totalSemanas")

            var totalContentElements = 0
            val typeCounts = mutableMapOf<String, Int>()
            var elementsWithElementId = 0
            var elementsWithMultimediaId = 0

            if (semanasArray != null) {
                for (semanaEl in semanasArray) {
                    val semanaObj = semanaEl.asJsonObject
                    val sesionesArray = semanaObj.getAsJsonArray("sesiones") ?: continue
                    for (sesionEl in sesionesArray) {
                        val sesionObj = sesionEl.asJsonObject
                        val contentArray = sesionObj.getAsJsonArray("content") ?: continue
                        for (itemEl in contentArray) {
                            totalContentElements++
                            val itemObj = itemEl.asJsonObject
                            val type = itemObj.get("type")?.asString ?: "UNKNOWN"
                            typeCounts[type] = (typeCounts[type] ?: 0) + 1

                            if (itemObj.has("tratamiento_elemento_id") && !itemObj.get("tratamiento_elemento_id").isJsonNull) {
                                elementsWithElementId++
                            }
                            if (itemObj.has("tratamiento_multimedia_id") && !itemObj.get("tratamiento_multimedia_id").isJsonNull) {
                                elementsWithMultimediaId++
                            }
                        }
                    }
                }
            }

            println("   Total elementos en 'content': $totalContentElements")
            println("   Distribución por tipo: $typeCounts")
            println("   Elementos con 'tratamiento_elemento_id': $elementsWithElementId")
            println("   Elementos con 'tratamiento_multimedia_id': $elementsWithMultimediaId")

            // Guardar archivo JSON actualizado localmente
            val userFolder = "${user.name.lowercase().replace(" ", "_")}_${user.condition.replace(" + ", "_").replace(" ", "_")}_${if (user.country.contains("per", ignoreCase = true)) "PE" else "AR"}"
            listOf(
                java.io.File("test/api_responses/$userFolder/2_tratamiento_response.json"),
                java.io.File("app/test/api_responses/$userFolder/2_tratamiento_response.json"),
                java.io.File("../test/api_responses/$userFolder/2_tratamiento_response.json")
            ).forEach { file ->
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(prettyGson.toJson(jsonObject))
                } catch (_: Exception) {}
            }

            // Imprimir desglose de Metas en cada Semana
            println("   Detalle de Metas por sesión:")
            if (semanasArray != null) {
                for (semanaEl in semanasArray) {
                    val semanaObj = semanaEl.asJsonObject
                    val semId = semanaObj.get("id")?.asInt ?: 0
                    val sesionesArray = semanaObj.getAsJsonArray("sesiones") ?: continue
                    for (sesionEl in sesionesArray) {
                        val sesionObj = sesionEl.asJsonObject
                        val sesId = sesionObj.get("id")?.asInt ?: 0
                        val contentArray = sesionObj.getAsJsonArray("content") ?: continue
                        val metasInSession = contentArray.map { it.asJsonObject }.filter { it.get("type")?.asString == "METAS" }
                        if (metasInSession.isNotEmpty()) {
                            val metasSummary = metasInSession.map { 
                                "ID ${it.get("tratamiento_elemento_id")?.asInt}: '${it.get("title")?.asString}' (metasId: ${it.get("metasId")?.asString})"
                            }.joinToString(", ")
                            println("     - Semana $semId | Sesión $sesId: $metasSummary")
                        }
                    }
                }
            }
            println()
        }
    }

    @Test
    fun `obtener tratamiento real y generar json de sincronizacion simulado sin enviar al servidor`() = runBlocking {
        println("\n================================================================================")
        println("=== GENERACIÓN DE JSON DE SINCRONIZACIÓN (SOLO LECTURA / SIN ENVÍO) ===")
        println("================================================================================\n")

        val user = testUsers.find { it.name == "Maria Quintanilla" } ?: testUsers.first()
        println("Generando payload de sincronización para usuario: ${user.name} (${user.dni} - ${user.condition} - ${user.country})")

        // 1. Iniciar sesión para obtener token y userId
        val loginResponse = loginService.login(user.dni, user.password)
        assertTrue("Login debe ser exitoso", loginResponse.isSuccessful)
        val loginBody = loginResponse.body()!!
        val token = loginBody.token
        val userId = loginBody.usuario.id
        println("-> Token obtenido. User ID: $userId")

        // 2. Obtener tratamiento real del backend
        val treatmentResponse = treatmentService.getTratamiento(token)
        assertTrue("Tratamiento debe responder exitoso", treatmentResponse.isSuccessful)
        val bodyString = treatmentResponse.body()?.string() ?: "{}"
        val serverResponse = Gson().fromJson(bodyString, ServerTreatmentResponse::class.java)
        val serverTreatment = serverResponse.tratamiento
        assertNotNull("Debe existir el objeto tratamiento", serverTreatment)

        // 3. Extraer IDs de elementos reales del tratamiento
        val semanas = serverTreatment?.semanas ?: emptyList()
        val allContentItems = mutableListOf<com.colma.colmaApp.servicios.internetCommunication.model.ServerContentItem>()
        for (semana in semanas) {
            for (sesion in semana.sesiones ?: emptyList()) {
                allContentItems.addAll(sesion.content ?: emptyList())
            }
        }

        val videoOrDescItems = allContentItems.filter { it.type == "VIDEO" || it.type == "DESCRIPTION" }
        val quizItems = allContentItems.filter { it.type == "QUIZ" }
        val metasItems = allContentItems.filter { it.type == "METAS" }

        println("-> Elementos encontrados en el tratamiento:")
        println("   - Videos/Descripciones: ${videoOrDescItems.size}")
        println("   - Quizzes: ${quizItems.size}")
        println("   - Metas: ${metasItems.size}")

        // 4. Construir elementos simulados completados
        val progresoElementosSimulados = videoOrDescItems.take(3).mapNotNull { item ->
            item.elementoId?.let { elemId ->
                com.colma.colmaApp.servicios.internetCommunication.model.ProgresoElementoData(
                    tratamientoElementoId = elemId,
                    completado = true,
                    fechaEstado = "2026-09-22 09:30:00",
                    fechaSincronizacion = "2026-09-22 09:35:00"
                )
            }
        }

        val quizSimulado = quizItems.firstOrNull()?.let { quizServerItem ->
            val elemId = quizServerItem.elementoId ?: 210
            com.colma.colmaApp.servicios.internetCommunication.model.QuizData(
                tratamientoElementoId = elemId,
                fechaRegistro = "2026-09-22 09:35:00",
                fechaUsuario = "2026-09-22 09:35:00",
                fechaSincronizacion = "2026-09-22 09:40:00",
                idQuiz = 1,
                idUsuario = userId,
                quizType = "COMPLETED",
                sessionId = 1,
                subSessionId = 1,
                puntajeTotal = 1,
                puntajeObtenido = 1,
                porcentajeCorrecto = 100,
                preguntas = listOf(
                    com.colma.colmaApp.servicios.internetCommunication.model.PreguntaData(
                        preguntaIndex = 0,
                        preguntaTexto = quizServerItem.questions?.firstOrNull()?.questionText ?: "¿Cómo se mide la presión arterial?",
                        respuestaUsuario = 1,
                        respuestaUsuarioTexto = quizServerItem.questions?.firstOrNull()?.options?.getOrNull(1) ?: "Sentado y en reposo",
                        respuestaCorrecta = 1,
                        respuestaCorrectaTexto = quizServerItem.questions?.firstOrNull()?.options?.getOrNull(1) ?: "Sentado y en reposo",
                        esCorrecta = true,
                        posiblesRespuestas = quizServerItem.questions?.firstOrNull()?.options ?: listOf("De pie", "Sentado y en reposo")
                    )
                )
            )
        }

        val metaSimulada = metasItems.firstOrNull()?.let { metaServerItem ->
            val elemId = metaServerItem.elementoId ?: 220
            com.colma.colmaApp.servicios.internetCommunication.model.MetaData(
                idMeta = 1,
                idUsuario = userId,
                tratamientoElementoId = elemId,
                semana = 1,
                sesion = 1,
                tipoActividad = "caminar",
                actividad = "Caminar 30 minutos al aire libre",
                lugar = "Parque central",
                acompaniante = "Familiar",
                fechaRegistro = "2026-09-22 09:40:00",
                fechaUsuario = "2026-09-22 09:40:00",
                fechaEdicion = null,
                fechaSincronizacion = "2026-09-22 09:45:00",
                metaCumplida = true,
                fechaObjetivo = "2026-09-28",
                motivoIncumplimiento = null
            )
        }

        val glucosaSimulada = com.colma.colmaApp.servicios.internetCommunication.model.GlucosaData(
            idGlucosa = 1,
            idUsuario = userId,
            valorMgdl = 110.0f,
            ayuno = 1,
            fechaRegistro = "2026-09-22 08:00:00",
            fechaUsuario = "2026-09-22 08:00:00",
            fechaSincronizacion = "2026-09-22 09:45:00",
            advertencia = "NORMAL"
        )

        val presionSimulada = com.colma.colmaApp.servicios.internetCommunication.model.PresionData(
            idPresion = 1,
            idUsuario = userId,
            sistolica = 120.0f,
            diastolica = 80.0f,
            fechaRegistro = "2026-09-22 08:05:00",
            fechaUsuario = "2026-09-22 08:05:00",
            fechaSincronizacion = "2026-09-22 09:45:00",
            advertencia = "NORMAL"
        )

        val estadoAnimoSimulado = com.colma.colmaApp.servicios.internetCommunication.model.EstadoAnimoData(
            idEstadoAnimo = 1,
            idUsuario = userId,
            fechaRegistro = "2026-09-22 08:10:00",
            fechaUsuario = "2026-09-22 08:10:00",
            fechaSincronizacion = "2026-09-22 09:45:00",
            pregunta1 = 1,
            pregunta2 = 1,
            pregunta3 = 0,
            pregunta4 = 0,
            pregunta5 = 0,
            pregunta6 = 0,
            pregunta7 = 0,
            pregunta8 = 0,
            pregunta9 = 0
        )

        // 5. Construir SyncDataRequest
        val syncRequest = com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest(
            deviceTimestamp = System.currentTimeMillis(),
            userId = userId,
            app = "colmaApp",
            clientSyncTimestamp = "2026-09-22 09:45:00",
            sesionesCompletadas = 1,
            glucosa = listOf(glucosaSimulada),
            presion = listOf(presionSimulada),
            estadoAnimo = listOf(estadoAnimoSimulado),
            quiz = listOfNotNull(quizSimulado),
            metas = listOfNotNull(metaSimulada),
            progresoElementos = progresoElementosSimulados
        )

        // 6. Serializar usando Gson con formato legible (pretty print) y adaptador SyncDataRequestAdapter
        val prettyGson = GsonBuilder().setPrettyPrinting().create()
        val serializedJson = prettyGson.toJson(syncRequest)

        println("\n================================================================================")
        println("=== JSON DE SINCRONIZACIÓN GENERADO (ESTRUCTURA EXACTA A ENVIAR) ===")
        println("================================================================================\n")
        println(serializedJson)
        println("\n================================================================================\n")

        // Guardar archivo JSON localmente para inspección
        val outputFile = java.io.File("test/api_responses/simulated_sync_payload_user_${userId}.json")
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(serializedJson)
        println("-> Archivo guardado exitosamente en: ${outputFile.absolutePath}")
    }

    @Test
    fun `diagnostico de elementos y sesiones completadas en el servidor para todos los usuarios`() = runBlocking {
        println("\n================================================================================")
        println("=== DIAGNÓSTICO DE ELEMENTOS Y SESIONES COMPLETADAS EN SERVIDOR ===")
        println("================================================================================\n")

        for (user in testUsers) {
            println("--------------------------------------------------------------------------------")
            println("Usuario: ${user.name} | DNI: ${user.dni} | Condición: ${user.condition} | País: ${user.country}")
            println("--------------------------------------------------------------------------------")

            // 1. Login
            val loginResponse = loginService.login(user.dni, user.password)
            if (!loginResponse.isSuccessful) {
                println("   [!] Login falló: ${loginResponse.code()}")
                continue
            }
            val token = loginResponse.body()!!.token
            val userId = loginResponse.body()!!.usuario.id
            println("   ID Paciente: $userId")

            // 2. Obtener elementos completados en el servidor
            val completadosResponse = try {
                treatmentService.getElementosCompletados(token)
            } catch (e: Exception) {
                null
            }
            val completedElementIds = completadosResponse?.body()?.elementosCompletados?.toSet()
                ?: try {
                    treatmentService.getMultimediaCompletados(token).body()?.completedIds?.toSet()
                } catch (e: Exception) {
                    null
                } ?: emptySet()

            println("   Total IDs completados reportados por GET /api/elementos-completados: ${completedElementIds.size}")
            println("   IDs completados en servidor: $completedElementIds")

            // 3. Obtener tratamiento completo para mapear elementos por sesión
            val treatmentResponse = treatmentService.getTratamiento(token)
            if (!treatmentResponse.isSuccessful) {
                println("   [!] No se pudo obtener tratamiento: ${treatmentResponse.code()}")
                continue
            }
            val bodyString = treatmentResponse.body()?.string() ?: "{}"
            val serverResponse = Gson().fromJson(bodyString, ServerTreatmentResponse::class.java)
            val serverTreatment = serverResponse.tratamiento

            val semanas = serverTreatment?.semanas ?: emptyList()
            var sesionesCompletasCount = 0
            var totalSesionesCount = 0

            for ((wIdx, semana) in semanas.withIndex()) {
                val semanaNum = semana.id ?: (wIdx + 1)
                val sesiones = semana.sesiones ?: emptyList()
                for ((sIdx, sesion) in sesiones.withIndex()) {
                    totalSesionesCount++
                    val sesionNum = sesion.id ?: (sIdx + 1)
                    val contents = sesion.content ?: emptyList()
                    val elementIdsInSession = contents.mapNotNull { it.elementoId }
                    
                    val completedInThisSession = elementIdsInSession.filter { completedElementIds.contains(it) }
                    val missingInThisSession = elementIdsInSession.filter { !completedElementIds.contains(it) }
                    val isSessionFullyCompleted = elementIdsInSession.isNotEmpty() && missingInThisSession.isEmpty()

                    if (isSessionFullyCompleted) {
                        sesionesCompletasCount++
                    }

                    val statusMark = if (isSessionFullyCompleted) "[COMPLETA]" else "[INCOMPLETA]"
                    println("   -> Semana $semanaNum | Sesión $sesionNum: $statusMark (${completedInThisSession.size}/${elementIdsInSession.size} elementos completados)")
                    if (missingInThisSession.isNotEmpty()) {
                        println("      Faltan los siguientes elementos:")
                        for (missingId in missingInThisSession) {
                            val missingItem = contents.find { it.elementoId == missingId }
                            println("        - ID $missingId: [${missingItem?.type}] '${missingItem?.title}'")
                        }
                    }
                }
            }

            println("\n   RESUMEN:")
            println("   - Sesiones completamente finalizadas en servidor: $sesionesCompletasCount de $totalSesionesCount")
            println("--------------------------------------------------------------------------------\n")
        }
    }
}
