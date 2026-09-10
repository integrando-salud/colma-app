package com.colma.colmaApp.servicios.internetCommunication.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: LoginApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `el login retorna respuesta exitosa y procesa el usuario correctamente`() = runTest {
        val jsonResponse = """
            {
                "token": "fake_jwt_token_12345",
                "usuario": {
                    "id": 1,
                    "documento": "12345678",
                    "nombre": "Juan Perez",
                    "email": "juan@example.com",
                    "sexo": "M",
                    "pais": {
                        "id": 1,
                        "nombre": "Peru"
                    },
                    "centro_salud": {
                        "id": 10,
                        "nombre": "Centro Salud Lima"
                    },
                    "primer_login": "2026-01-01"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
        )

        val response = apiService.login("12345678", "password123")

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("fake_jwt_token_12345", body?.token)
        assertEquals("Juan Perez", body?.usuario?.nombre)
        assertEquals("12345678", body?.usuario?.documento)
        assertEquals("Centro Salud Lima", body?.usuario?.centroSalud?.nombre)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/login", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
    }

    @Test
    fun `el login retorna codigo 401 ante credenciales invalidas o no autorizadas`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Credenciales inválidas"}""")
                .setHeader("Content-Type", "application/json")
        )

        val response = apiService.login("12345678", "wrong_pass")

        assertEquals(401, response.code())
        assertTrue(!response.isSuccessful)
    }
}
