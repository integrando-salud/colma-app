package com.colma.colmaApp.servicios.internetCommunication

import com.colma.colmaApp.servicios.internetCommunication.model.ServerTreatmentResponse
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTreatmentApiIntegrationTest {

    private val loginService = LoginService()
    private val treatmentService = TreatmentService()

    @Test
    fun `test real de login, obtencion de tratamiento con tratamiento_multimedia_id y multimedia-completados`() = runBlocking {
        val dni = "15864485" // Guillermo Arribas - HTA (Perú)
        val password = "123456"

        println("==================================================")
        println("1. Intentando Login para DNI: $dni ...")
        val loginResponse = loginService.login(dni, password)
        println("Login Response Code: ${loginResponse.code()}")
        assertTrue("Login debe ser exitoso", loginResponse.isSuccessful)

        val loginBody = loginResponse.body()
        assertNotNull("Login body no debe ser nulo", loginBody)
        val token = loginBody!!.token
        println("Token obtenido: ${token.take(15)}...")
        println("Usuario: ${loginBody.usuario.nombre} (${loginBody.usuario.pais.nombre})")

        println("\n2. Consultando GET /api/tratamiento ...")
        val treatmentResponse = treatmentService.getTratamiento(token)
        println("Tratamiento Response Code: ${treatmentResponse.code()}")
        assertTrue("Tratamiento debe ser exitoso", treatmentResponse.isSuccessful)

        val rawJson = treatmentResponse.body()?.string() ?: ""
        println("Tratamiento JSON preview (primeros 500 chars):\n${rawJson.take(500)}...")

        val parsedTreatment = Gson().fromJson(rawJson, ServerTreatmentResponse::class.java)
        assertNotNull("El objeto tratamiento no debe ser nulo", parsedTreatment.tratamiento)

        val semanas = parsedTreatment.tratamiento?.semanas ?: emptyList()
        println("Total de semanas recibidas: ${semanas.size}")
        assertTrue("Debe recibir semanas", semanas.isNotEmpty())

        var totalItems = 0
        var itemsWithMultimediaId = 0
        for (semana in semanas) {
            for (sesion in (semana.sesiones ?: emptyList())) {
                for (contentItem in (sesion.content ?: emptyList())) {
                    totalItems++
                    if (contentItem.tratamientoMultimediaId != null) {
                        itemsWithMultimediaId++
                    }
                }
            }
        }

        println("Total de items multimedia en el tratamiento: $totalItems")
        println("Total de items con tratamiento_multimedia_id presente: $itemsWithMultimediaId")
        assertTrue("Los items multimedia deben incluir tratamiento_multimedia_id", itemsWithMultimediaId > 0)

        println("\n3. Consultando GET /api/multimedia-completados ...")
        val completadosResponse = treatmentService.getMultimediaCompletados(token)
        println("Multimedia Completados Response Code: ${completadosResponse.code()}")
        assertTrue("Endpoint multimedia-completados debe responder exitosamente", completadosResponse.isSuccessful)

        val completadosBody = completadosResponse.body()
        assertNotNull("Body de multimedia-completados no debe ser nulo", completadosBody)
        val listaCompletados = completadosBody?.multimediaCompletados ?: emptyList()
        println("Lista de multimedia_completados recibida del backend: $listaCompletados")
        println("==================================================")
    }

    @Test
    fun `test con paciente DBT de Peru`() = runBlocking {
        val dni = "81306555" // Erika Becerra - DBT (Perú)
        val password = "123456"

        val loginResponse = loginService.login(dni, password)
        assertTrue(loginResponse.isSuccessful)
        val token = loginResponse.body()!!.token

        val treatmentResponse = treatmentService.getTratamiento(token)
        assertTrue(treatmentResponse.isSuccessful)
        val rawJson = treatmentResponse.body()?.string() ?: ""
        val parsed = Gson().fromJson(rawJson, ServerTreatmentResponse::class.java)
        assertNotNull(parsed.tratamiento)

        val completadosResponse = treatmentService.getMultimediaCompletados(token)
        assertTrue(completadosResponse.isSuccessful)
        println("Erika Becerra - multimedia_completados: ${completadosResponse.body()?.multimediaCompletados}")
    }

    @Test
    fun `test con paciente DBT y HTA de Peru`() = runBlocking {
        val dni = "12253242" // Denisse Molina - DBT + HTA (Perú)
        val password = "123456"

        val loginResponse = loginService.login(dni, password)
        assertTrue(loginResponse.isSuccessful)
        val token = loginResponse.body()!!.token

        val treatmentResponse = treatmentService.getTratamiento(token)
        assertTrue(treatmentResponse.isSuccessful)
        val rawJson = treatmentResponse.body()?.string() ?: ""
        val parsed = Gson().fromJson(rawJson, ServerTreatmentResponse::class.java)
        assertNotNull(parsed.tratamiento)

        val completadosResponse = treatmentService.getMultimediaCompletados(token)
        assertTrue(completadosResponse.isSuccessful)
        println("Denisse Molina - multimedia_completados: ${completadosResponse.body()?.multimediaCompletados}")
    }

    @Test
    fun `test con paciente HTA de Argentina`() = runBlocking {
        val dni = "88888888" // María González - HTA (Argentina)
        val password = "123456"

        val loginResponse = loginService.login(dni, password)
        assertTrue(loginResponse.isSuccessful)
        val token = loginResponse.body()!!.token
        println("María González (Argentina) Token: ${token.take(15)}...")

        val treatmentResponse = treatmentService.getTratamiento(token)
        assertTrue(treatmentResponse.isSuccessful)
        val rawJson = treatmentResponse.body()?.string() ?: ""
        val parsed = Gson().fromJson(rawJson, ServerTreatmentResponse::class.java)
        assertNotNull(parsed.tratamiento)

        val completadosResponse = treatmentService.getMultimediaCompletados(token)
        assertTrue(completadosResponse.isSuccessful)
        println("María González - multimedia_completados: ${completadosResponse.body()?.multimediaCompletados}")
    }

    @Test
    fun `test con paciente DBT y HTA de Argentina`() = runBlocking {
        val dni = "77777777" // Pedro Flores - DBT + HTA (Argentina)
        val password = "123456"

        val loginResponse = loginService.login(dni, password)
        assertTrue(loginResponse.isSuccessful)
        val token = loginResponse.body()!!.token
        println("Pedro Flores (Argentina) Token: ${token.take(15)}...")

        val treatmentResponse = treatmentService.getTratamiento(token)
        assertTrue(treatmentResponse.isSuccessful)
        val rawJson = treatmentResponse.body()?.string() ?: ""
        val parsed = Gson().fromJson(rawJson, ServerTreatmentResponse::class.java)
        assertNotNull(parsed.tratamiento)

        val completadosResponse = treatmentService.getMultimediaCompletados(token)
        assertTrue(completadosResponse.isSuccessful)
        println("Pedro Flores - multimedia_completados: ${completadosResponse.body()?.multimediaCompletados}")
    }
}

