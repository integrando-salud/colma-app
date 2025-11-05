package com.colma.colmaApp.features.MenuMain

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.features.RegistroSalud.RegistrosSalud
import com.colma.colmaApp.features.login.ui.LoginUI
import com.colma.colmaApp.features.main.ui.MainScreen
import com.colma.colmaApp.features.sesiones.WelcomeActivity
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MenuMain : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColmaAppTheme {
                MenuScreen(
                    onSessionsClick = {
                        val intent = Intent(this@MenuMain, WelcomeActivity::class.java)
                        startActivity(intent)
                    },
                    onHealthRegistersClick = {
                        val intent = Intent(this@MenuMain, RegistrosSalud::class.java)
                        startActivity(intent)
                    },
                    onSalirClick = {
                        val intent = Intent(this@MenuMain, LoginUI::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    title: String = "Bienvenido Lucas",
    onMenuClick: () -> Unit = {},
    onSessionsClick: () -> Unit = {},
    onHealthRegistersClick: () -> Unit = {},
    onSalirClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},         // new parameter
    modifier: Modifier = Modifier
) {
    val signatureColor = colorResource(R.color.principal)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var lastResponse by remember { mutableStateOf<String?>(null) }


    val webhookUrl = "https://webhook.site/12930f51-dcee-43ca-8b6a-84aec1e7164a"

    val dbHelper = remember { com.colma.colmaApp.servicios.database.dbhelper.DBhelper(context.applicationContext) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color.White) {
                // App logo
                Image(
                    painter = painterResource(id = R.drawable.colma_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(12.dp))

                // small helper composable for rows
                @Composable
                fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // context for toasts
                val context = LocalContext.current

                // Drawer items
                Column {
                    DrawerItem("Inicio", Icons.Default.Home) {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Inicio", Toast.LENGTH_SHORT).show()
                    }

                    DrawerItem("Ayuda técnica del promotor de salud", Icons.Default.Call) {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Ayuda técnica", Toast.LENGTH_SHORT).show()
                    }

                    DrawerItem("Enlaces útiles", Icons.Default.Info) {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Enlaces útiles", Toast.LENGTH_SHORT).show()
                    }

                    DrawerItem("Tutorial de Usuario", Icons.Default.Face) {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Manual de Usuario", Toast.LENGTH_SHORT).show()
                    }
                }

                Spacer(Modifier.height(28.dp))

                // existing Salir button (keeps its behavior)
                Button(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSalirClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.principal),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Salir")
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    ){
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.20f), // subtle white background
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(36.dp)
                                .clickable { scope.launch { drawerState.open() } }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Menu",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = signatureColor,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 50.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¿Que te gustaria hacer hoy?",
                        fontSize = 25.sp,
                        color = signatureColor,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(55.dp))

                    Button(
                        onClick = onSessionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp), clip = false)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorResource(id = R.color.light_principal),
                                        colorResource(id = R.color.dark_principal)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // If you want the icon without tint (keeps PNG/vector original colors) use Image:
                            Image(
                                painter = painterResource(id = R.drawable.sesion_logo),
                                contentDescription = "Sesiones",
                                modifier = Modifier
                                    .size(24.dp) // slightly larger for touch affordance
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Mis Sesiones",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onHealthRegistersClick,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0F0F0),
                            contentColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation( // subtle shadow
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_unknown_document_24),
                                contentDescription = "Registros",
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Mis Registros de Salud",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    // push footer to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Footer: Sincronizar datos button with left icon
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // 0) choose one sync timestamp for this operation (local device time)
                                    val syncTs = nowLocal()

                                    // 1) Read DB on IO thread: glucosa, presion, estado_animo
                                    val (glucosaList, presionList, estadoAnimoList) = withContext(Dispatchers.IO) {
                                        val g = dbHelper.getPendingGlucosa(1)   // user id = 1 (adjust as needed)
                                        val p = dbHelper.getPendingPresion(1)
                                        val e = dbHelper.getPendingEstadoAnimoList(1)
                                        Triple(g, p, e)
                                    }

                                    // Build JSON root
                                    val root = JSONObject().apply {
                                        put("deviceTimestamp", System.currentTimeMillis())
                                        put("userId", 1)
                                        put("app", "colmaApp")
                                        // include clientSyncTimestamp top-level so server knows the client timestamp
                                        put("clientSyncTimestamp", syncTs)
                                    }

                                    // glucosa array (include fecha_sincronizacion = syncTs)
                                    val gArray = JSONArray()
                                    val glucosaIdsToMark = mutableListOf<Long>()
                                    glucosaList.forEach { g ->
                                        val gObj = JSONObject()
                                        gObj.put("id_glucosa", g.id)
                                        gObj.put("id_usuario", g.idUsuario ?: JSONObject.NULL)
                                        gObj.put("valor_mgdl", g.valorMgdl ?: JSONObject.NULL)
                                        gObj.put("ayuno", g.ayuno ?: JSONObject.NULL)
                                        gObj.put("fecha_registro", g.fecha_registro ?: JSONObject.NULL)
                                        // send the chosen sync timestamp (client-chosen)
                                        gObj.put("fecha_sincronizacion", syncTs)
                                        gArray.put(gObj)
                                        glucosaIdsToMark.add(g.id)
                                    }
                                    root.put("glucosa", gArray)

                                    // presion array (include fecha_sincronizacion = syncTs)
                                    val pArray = JSONArray()
                                    val presionIdsToMark = mutableListOf<Long>()
                                    presionList.forEach { p ->
                                        val pObj = JSONObject()
                                        pObj.put("id_presion", p.id)
                                        pObj.put("id_usuario", p.idUsuario ?: JSONObject.NULL)
                                        pObj.put("sistolica", p.sistolica ?: JSONObject.NULL)
                                        pObj.put("diastolica", p.diastolica ?: JSONObject.NULL)
                                        pObj.put("fecha_registro", p.fecha_registro ?: JSONObject.NULL)
                                        pObj.put("fecha_sincronizacion", syncTs)
                                        pArray.put(pObj)
                                        presionIdsToMark.add(p.id)
                                    }
                                    root.put("presion", pArray)

                                    // estado_animo array (include fecha_sincronizacion = syncTs)
                                    val eArray = JSONArray()
                                    val estadoIdsToMark = mutableListOf<Long>()
                                    estadoAnimoList.forEach { e ->
                                        val eObj = JSONObject()
                                        eObj.put("id_estado_animo", e.rowid) // use rowid as id
                                        eObj.put("id_paciente", e.idPaciente ?: JSONObject.NULL)
                                        eObj.put("fecha_registro", e.fechaRegistro ?: JSONObject.NULL)
                                        for (i in 1..9) {
                                            val value = e.preguntas.getOrNull(i - 1)
                                            if (value == null) eObj.put("pregunta_$i", JSONObject.NULL)
                                            else eObj.put("pregunta_$i", value)
                                        }
                                        eObj.put("fecha_sincronizacion", syncTs)
                                        eArray.put(eObj)
                                        estadoIdsToMark.add(e.rowid)
                                    }
                                    root.put("estado_animo", eArray)

                                    val jsonToSend = root.toString(2)

                                    // 3) Post JSON
                                    val postResult = postJsonRaw(webhookUrl, jsonToSend)

                                    withContext(Dispatchers.Main) {
                                        postResult.fold(onSuccess = { resp ->
                                            // success path
                                            lastResponse = "Enviado correctamente: $resp"
                                            Toast.makeText(context, "Enviado correctamente", Toast.LENGTH_SHORT).show()

                                            // default: use the client sync timestamp unless server provides specific timestamps
                                            val defaultTimestamp = syncTs

                                            // parse server response — try to prefer server timestamps / status
                                            val glucosaTimestampMap = mutableMapOf<Long, String>()
                                            val presionTimestampMap = mutableMapOf<Long, String>()
                                            val estadoTimestampMap = mutableMapOf<Long, String>()

                                            var serverStatus = "200"
                                            var serverSyncedAt: String? = null

                                            try {
                                                val respStr = resp?.toString() ?: ""
                                                if (respStr.isNotBlank()) {
                                                    val rootResp = org.json.JSONObject(respStr)

                                                    // possible top-level status / code
                                                    serverStatus = when {
                                                        rootResp.has("code") -> rootResp.optString("code", serverStatus)
                                                        rootResp.has("status") -> rootResp.optString("status", serverStatus)
                                                        rootResp.has("statusCode") -> rootResp.optString("statusCode", serverStatus)
                                                        else -> serverStatus
                                                    }

                                                    // server might return a global timestamp (syncedAt)
                                                    if (rootResp.has("syncedAt")) serverSyncedAt = rootResp.optString("syncedAt", null)

                                                    // per-item timestamp arrays (prefer these)
                                                    if (rootResp.has("glucosa")) {
                                                        val ga = rootResp.optJSONArray("glucosa")
                                                        if (ga != null) {
                                                            for (i in 0 until ga.length()) {
                                                                val o = ga.optJSONObject(i) ?: continue
                                                                val id = o.optLong("id_glucosa", -1L)
                                                                val fecha = o.optString("fecha_sincronizacion", null)
                                                                if (id > 0) glucosaTimestampMap[id] = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp
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
                                                                if (id > 0) presionTimestampMap[id] = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp
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
                                                                if (id > 0) estadoTimestampMap[id] = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp
                                                            }
                                                        }
                                                    }

                                                    // if server returned a unified timestamp and we have no per-id, use it
                                                    if (glucosaTimestampMap.isEmpty() && presionTimestampMap.isEmpty() && estadoTimestampMap.isEmpty() && serverSyncedAt != null) {
                                                        val ts = if (serverSyncedAt.isNullOrBlank()) defaultTimestamp else serverSyncedAt
                                                        glucosaIdsToMark.forEach { glucosaTimestampMap[it] = ts }
                                                        presionIdsToMark.forEach { presionTimestampMap[it] = ts }
                                                        estadoIdsToMark.forEach { estadoTimestampMap[it] = ts }
                                                    }

                                                    // fallback: if per-id maps are still empty but server returned lists of ids,
                                                    // mark with defaultTimestamp
                                                    if (glucosaTimestampMap.isEmpty() && rootResp.has("glucosaIds")) {
                                                        val ga = rootResp.optJSONArray("glucosaIds")
                                                        if (ga != null) for (i in 0 until ga.length()) { val id = ga.optLong(i, -1L); if (id > 0) glucosaTimestampMap[id] = defaultTimestamp }
                                                    }
                                                    if (presionTimestampMap.isEmpty() && rootResp.has("presionIds")) {
                                                        val pa = rootResp.optJSONArray("presionIds")
                                                        if (pa != null) for (i in 0 until pa.length()) { val id = pa.optLong(i, -1L); if (id > 0) presionTimestampMap[id] = defaultTimestamp }
                                                    }
                                                    if (estadoTimestampMap.isEmpty() && rootResp.has("estadoIds")) {
                                                        val ea = rootResp.optJSONArray("estadoIds")
                                                        if (ea != null) for (i in 0 until ea.length()) { val id = ea.optLong(i, -1L); if (id > 0) estadoTimestampMap[id] = defaultTimestamp }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }

                                            // Final fallback: mark each id with the client syncTs
                                            if (glucosaTimestampMap.isEmpty()) glucosaIdsToMark.forEach { glucosaTimestampMap[it] = defaultTimestamp }
                                            if (presionTimestampMap.isEmpty()) presionIdsToMark.forEach { presionTimestampMap[it] = defaultTimestamp }
                                            if (estadoTimestampMap.isEmpty()) estadoIdsToMark.forEach { estadoTimestampMap[it] = defaultTimestamp }

                                            // Persist two things on IO:
                                            //  a) insert a row into sincronizacion_table marking this sync attempt as success
                                            //  b) update per-record fecha_sincronizacion maps
                                            val recordTimestampForSyncTable = serverSyncedAt ?: syncTs
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    // insert a summary sync record (success)
                                                    try {
                                                        dbHelper.insertSincronizacion(recordTimestampForSyncTable, serverStatus, null)
                                                    } catch (ie: Exception) { ie.printStackTrace() }

                                                    // update per-row timestamps
                                                    if (glucosaTimestampMap.isNotEmpty()) dbHelper.updateGlucosaSyncTimestamps(glucosaTimestampMap)
                                                    if (presionTimestampMap.isNotEmpty()) dbHelper.updatePresionSyncTimestamps(presionTimestampMap)
                                                    if (estadoTimestampMap.isNotEmpty()) dbHelper.updateEstadoAnimoSyncTimestamps(estadoTimestampMap)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }, onFailure = { err ->
                                            // failure path
                                            lastResponse = "Error: ${err?.message}"
                                            Toast.makeText(context, "Error: ${err?.message}", Toast.LENGTH_LONG).show()

                                            // save failure into sincronizacion_table including the payload for later inspection/resend
                                            val errDesc = (err?.message ?: "network/error")
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    dbHelper.insertSincronizacion(syncTs, errDesc, jsonToSend)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        })
                                    } // end withContext(Main)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        lastResponse = "Error inesperado: ${e.message}"
                                        Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                                        // Save unexpected exceptions to sync table with payload for debugging
                                        val syncTs = nowLocal()
                                        withContext(Dispatchers.IO) {
                                            try {
                                                dbHelper.insertSincronizacion(syncTs, e.message ?: "exception", /* optionally save payload? */ null)
                                            } catch (ie: Exception) { ie.printStackTrace() }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF0F0F0),
                            contentColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_sync_24),
                                contentDescription = "Sincronizar",
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Sincronizar datos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}



suspend fun postJsonRaw(urlString: String, jsonBody: String, timeoutMs: Int = 15000): Result<String> {
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

            // write body
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

fun nowLocal(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    // keep device local timezone
    sdf.timeZone = java.util.TimeZone.getDefault()
    return sdf.format(java.util.Date())
}