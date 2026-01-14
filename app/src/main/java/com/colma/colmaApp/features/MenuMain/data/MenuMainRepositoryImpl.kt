package com.colma.colmaApp.features.MenuMain.data

import android.content.Context
import com.colma.colmaApp.features.MenuMain.domain.MenuMainRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuMainRepositoryImpl(
    private val context: Context,
    private val dbHelper: DBhelper
) : MenuMainRepository {

    private val webhookUrl = "https://webhook.site/12930f51-dcee-43ca-8b6a-84aec1e7164a" // TODO: Move to constants/config

    override suspend fun getPendingPastMeta(userId: Int?): RegistrosSaludModelos.MetaRecord? {
        return withContext(Dispatchers.IO) {
            dbHelper.getPendingPastMeta(userId)
        }
    }

    override suspend fun getPendingFutureMetasCount(userId: Int?): Int {
        return withContext(Dispatchers.IO) {
            dbHelper.getPendingFutureMetasCount(userId)
        }
    }

    override suspend fun updateMeta(meta: RegistrosSaludModelos.MetaRecord) {
        withContext(Dispatchers.IO) {
            dbHelper.updateMeta(meta)
        }
    }

        override suspend fun synchronizeData(userId: Int?): Result<String> {

            return withContext(Dispatchers.IO) {

                try {

                    val syncTs = nowLocal()

    

                    val (glucosaList, presionList, estadoAnimoList) = withContext(Dispatchers.IO) {

                        val g = dbHelper.getPendingGlucosa(userId)

                        val p = dbHelper.getPendingPresion(userId)

                        val e = dbHelper.getPendingEstadoAnimoList(userId)

                        Triple(g, p, e)

                    }

    

                    val root = JSONObject().apply {

                        put("deviceTimestamp", System.currentTimeMillis())

                        put("userId", userId)

                        put("app", "colmaApp")

                        put("clientSyncTimestamp", syncTs)

                    }

    

                    val gArray = JSONArray()

                    val glucosaIdsToMark = mutableListOf<Long>()

                    glucosaList.forEach { g ->

                        val gObj = JSONObject()

                        gObj.put("id_glucosa", g.id)

                        gObj.put("id_usuario", g.idUsuario as Any? ?: JSONObject.NULL)

                        gObj.put("valor_mgdl", g.valorMgdl ?: JSONObject.NULL)

                        gObj.put("ayuno", g.ayuno ?: JSONObject.NULL)

                        gObj.put("fecha_registro", g.fecha_registro ?: JSONObject.NULL)

                        gObj.put("fecha_sincronizacion", syncTs)

                        gArray.put(gObj)

                        glucosaIdsToMark.add(g.id)

                    }

                    root.put("glucosa", gArray)

    

                    val pArray = JSONArray()

                    val presionIdsToMark = mutableListOf<Long>()

                    presionList.forEach { p ->

                        val pObj = JSONObject()

                        pObj.put("id_presion", p.id)

                        pObj.put("id_usuario", p.idUsuario as Any? ?: JSONObject.NULL)

                        pObj.put("sistolica", p.sistolica ?: JSONObject.NULL)

                        pObj.put("diastolica", p.diastolica ?: JSONObject.NULL)

                        pObj.put("fecha_registro", p.fecha_registro ?: JSONObject.NULL)

                        pObj.put("fecha_sincronizacion", syncTs)

                        pArray.put(pObj)

                        presionIdsToMark.add(p.id)

                    }

                    root.put("presion", pArray)

    

                    val eArray = JSONArray()

                    val estadoIdsToMark = mutableListOf<Long>()

                    estadoAnimoList.forEach { e ->

                        val eObj = JSONObject()

                        eObj.put("id_estado_animo", e.rowid)

                        eObj.put("id_paciente", e.idPaciente ?: JSONObject.NULL)

                        eObj.put("fecha_registro", e.fechaRegistro ?: JSONObject.NULL)

                        for (i in 1..9) {

                            val value = e.preguntas.getOrNull(i - 1)

                            if (value == null) eObj.put("pregunta_$i", JSONObject.NULL)

                            else eObj.put("pregunta_$i", value as Any)

                        }

                        eObj.put("fecha_sincronizacion", syncTs)

                        eArray.put(eObj)

                        estadoIdsToMark.add(e.rowid)

                    }

                    root.put("estado_animo", eArray)

    

                    val jsonToSend = root.toString(2)

    

                    val postResult = postJsonRaw(webhookUrl, jsonToSend)

    

                    postResult.fold(onSuccess = { resp ->

                        val defaultTimestamp = syncTs

                        val glucosaTimestampMap: MutableMap<Long, String> = mutableMapOf()

                        val presionTimestampMap: MutableMap<Long, String> = mutableMapOf()

                        val estadoTimestampMap: MutableMap<Long, String> = mutableMapOf()

                        var serverStatus = "200"

                        var serverSyncedAt: String? = null

                        try {

                            val respStr = resp?.toString() ?: ""

                            if (respStr.isNotBlank()) {

                                val rootResp = org.json.JSONObject(respStr)

                                serverStatus = when {

                                    rootResp.has("code") -> rootResp.optString("code", serverStatus)

                                    rootResp.has("status") -> rootResp.optString("status", serverStatus)

                                    rootResp.has("statusCode") -> rootResp.optString("statusCode", serverStatus)

                                    else -> serverStatus

                                }

                                if (rootResp.has("syncedAt")) serverSyncedAt = rootResp.optString("syncedAt", null)

                                if (rootResp.has("glucosa")) {

                                    val ga = rootResp.optJSONArray("glucosa")

                                    if (ga != null) {

                                        for (i in 0 until ga.length()) {

                                            val o = ga.optJSONObject(i) ?: continue

                                            val id = o.optLong("id_glucosa", -1L)

                                            val fecha = o.optString("fecha_sincronizacion", null)

                                            if (id > 0) {

                                                val timestampToUse = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp

                                                glucosaTimestampMap.put(id, timestampToUse)

                                            }

                                        }

                                    }

                                }

                                if (rootResp.has("presion")) {

                                    val pa = rootResp.optJSONArray("presion")

                                    if (pa != null) {

                                        for (i in 0 until pa.length()) {

                                            val o = pa.optJSONObject(i) ?: continue

                                            val id = o.optLong("id_presion", -1L)

                                            val fecha = o.optString("fecha_sincronizacion", null)

                                            if (id > 0) {

                                                val timestampToUse = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp

                                                presionTimestampMap.put(id, timestampToUse)

                                            }

                                        }

                                    }

                                }

                                if (rootResp.has("estado_animo")) {

                                    val ea = rootResp.optJSONArray("estado_animo")

                                    if (ea != null) {

                                        for (i in 0 until ea.length()) {

                                            val o = ea.optJSONObject(i) ?: continue

                                            val id = o.optLong("id_estado_animo", -1L)

                                            val fecha = o.optString("fecha_sincronizacion", null)

                                            if (id > 0) {

                                                val timestampToUse = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp

                                                estadoTimestampMap.put(id, timestampToUse)

                                            }

                                        }

                                    }

                                }

                                if (glucosaTimestampMap.isEmpty() && presionTimestampMap.isEmpty() && estadoTimestampMap.isEmpty() && serverSyncedAt != null) {

                                    val ts = if (serverSyncedAt.isNullOrBlank()) defaultTimestamp else serverSyncedAt

                                    glucosaIdsToMark.forEach { glucosaTimestampMap[it] = ts }

                                    presionIdsToMark.forEach { presionTimestampMap[it] = ts }

                                    estadoIdsToMark.forEach { estadoTimestampMap[it] = ts }

                                }

                                /*

                                if (glucosaTimestampMap.isEmpty() && rootResp.has("glucosaIds")) {

                                    val ga = rootResp.optJSONArray("glucosaIds")

                                    if (ga != null) for (i in 0 until ga.length()) {

                                        val id = ga.optLong("id_glucosa", -1L); if (id > 0) glucosaTimestampMap.put(id, defaultTimestamp)

                                    }

                                }

                                */

                                /*

                                if (presionTimestampMap.isEmpty() && rootResp.has("presionIds")) {

                                    val pa = rootResp.optJSONArray("presionIds")

                                    if (pa != null) for (i in 0 until pa.length()) {

                                        val id = pa.optLong("id_presion", -1L); if (id > 0) presionTimestampMap.put(id, defaultTimestamp)

                                    }

                                }

                                */

                                /*

                                if (estadoTimestampMap.isEmpty() && rootResp.has("estadoIds")) {

                                    val ea = rootResp.optJSONArray("estadoIds")

                                    if (ea != null) for (i in 0 until ea.length()) {

                                        val id = ea.optLong("id_estado_animo", -1L); if (id > 0) estadoTimestampMap.put(id, defaultTimestamp)

                                    }

                                }

                                */

                            }

                        } catch (e: Exception) {

                            e.printStackTrace()

                        }

                        if (glucosaTimestampMap.isEmpty()) glucosaIdsToMark.forEach { glucosaTimestampMap[it] = defaultTimestamp }

                        if (presionTimestampMap.isEmpty()) presionIdsToMark.forEach { presionTimestampMap[it] = defaultTimestamp }

                        if (estadoTimestampMap.isEmpty()) estadoIdsToMark.forEach { estadoTimestampMap[it] = defaultTimestamp }

    

                        val recordTimestampForSyncTable = serverSyncedAt ?: syncTs

                        try {

                            dbHelper.insertSincronizacion(recordTimestampForSyncTable, serverStatus, null)

                            if (glucosaTimestampMap.isNotEmpty()) dbHelper.updateGlucosaSyncTimestamps(glucosaTimestampMap)

                            if (presionTimestampMap.isNotEmpty()) dbHelper.updatePresionSyncTimestamps(presionTimestampMap)

                            if (estadoTimestampMap.isNotEmpty()) dbHelper.updateEstadoAnimoSyncTimestamps(estadoTimestampMap)

                        } catch (e: Exception) {

                            e.printStackTrace()

                        }

                        Result.success("Synchronization successful")

                    }, onFailure = { err ->

                        val errDesc = (err?.message ?: "network/error")

                        try {

                            dbHelper.insertSincronizacion(syncTs, errDesc, jsonToSend)

                        } catch (e: Exception) {

                            e.printStackTrace()

                        }

                        Result.failure(err)

                    })

                } catch (e: Exception) {

                    e.printStackTrace()

                    Result.failure(e)

                }

            }

        }

    override suspend fun saveSynchronizationRecord(timestamp: String, status: String, data: String?) {
        withContext(Dispatchers.IO) {
            dbHelper.insertSincronizacion(timestamp, status, data)
        }
    }

    private suspend fun postJsonRaw(
        urlString: String,
        jsonBody: String,
        timeoutMs: Int = 15000
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

                conn.outputStream.use { out ->
                    BufferedOutputStream(out).use { bos ->
                        bos.write(jsonBody.toByteArray(Charsets.UTF_8))
                        bos.flush()
                    }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { br ->
                        val sb = StringBuilder()
                        var line: String?
                        while (br.readLine().also { line = it } != null) sb.append(line)
                        sb.toString()
                    }
                } ?: ""

                conn.disconnect()

                if (code in 200..299) Result.success("HTTP $code: $responseText")
                else Result.failure(Exception("HTTP $code: $responseText"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun nowLocal(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(Date())
    }
}
