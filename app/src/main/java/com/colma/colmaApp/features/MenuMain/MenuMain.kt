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

    val webhookUrl = "https://webhook.site/5ccfb264-270b-4f56-b7a0-7d3452246c16"

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
                                    // 1) Read DB on IO thread
                                    val (glucosaList, presionList) = withContext(Dispatchers.IO) {
                                        val g = dbHelper.getPendingGlucosa(1)   // user id = 1
                                        val p = dbHelper.getPendingPresion(1)
                                        g to p
                                    }

                                    // Build JSON
                                    val root = JSONObject().apply {
                                        put("deviceTimestamp", System.currentTimeMillis())
                                        put("userId", 1)
                                        put("app", "colmaApp")
                                    }

                                    // glucosa array (match new schema)
                                    val gArray = JSONArray()
                                    val glucosaIdsToMark = mutableListOf<Long>()
                                    glucosaList.forEach { g ->
                                        val gObj = JSONObject()
                                        gObj.put("id_glucosa", g.id) // your model id field
                                        gObj.put("id_usuario", g.idUsuario ?: JSONObject.NULL)
                                        gObj.put("valor_mgdl", g.valorMgdl ?: JSONObject.NULL)
                                        gObj.put("ayuno", g.ayuno ?: JSONObject.NULL)
                                        gObj.put("fecha_registro", g.fecha_registro ?: JSONObject.NULL)
                                        gArray.put(gObj)

                                        // collect ids that are not marked as sent (optionally only include envio_sn check)
                                        // assuming your model doesn't include enviado_sn field; if it does, check it before adding
                                        glucosaIdsToMark.add(g.id)
                                    }
                                    root.put("glucosa", gArray)

                                    // presion array
                                    val pArray = JSONArray()
                                    val presionIdsToMark = mutableListOf<Long>()
                                    presionList.forEach { p ->
                                        val pObj = JSONObject()
                                        pObj.put("id_presion", p.id)
                                        pObj.put("id_usuario", p.idUsuario ?: JSONObject.NULL)
                                        pObj.put("sistolica", p.sistolica ?: JSONObject.NULL)
                                        pObj.put("diastolica", p.diastolica ?: JSONObject.NULL)
                                        pObj.put("fecha_registro", p.fecha_registro ?: JSONObject.NULL)
                                        pArray.put(pObj)

                                        presionIdsToMark.add(p.id)
                                    }
                                    root.put("presion", pArray)

                                    val jsonToSend = root.toString(2)

                                    // 3) Post JSON
                                    val postResult = postJsonRaw(webhookUrl, jsonToSend)

                                    withContext(Dispatchers.Main) {
                                        postResult.fold(onSuccess = { resp ->
                                            lastResponse = "Enviado correctamente: $resp"
                                            Toast.makeText(context, "Enviado correctamente", Toast.LENGTH_SHORT).show()

                                            // Determine per-id timestamps from server response if provided,
                                            // otherwise use current time for all returned ids.
                                            val glucosaTimestampMap = mutableMapOf<Long, String>()
                                            val presionTimestampMap = mutableMapOf<Long, String>()
                                            val defaultTimestamp = nowLocal()

                                            try {
                                                // Attempt to parse server response if it's JSON
                                                val respStr = resp?.toString() ?: ""
                                                if (respStr.isNotBlank()) {
                                                    try {
                                                        val root = org.json.JSONObject(respStr)

                                                        // Option 1: server returns arrays with per-id timestamps:
                                                        // { "glucosa": [ { "id_glucosa": 123, "fecha_sincronizacion": "..." }, ... ], "presion": [...] }
                                                        if (root.has("glucosa")) {
                                                            val ga = root.optJSONArray("glucosa")
                                                            if (ga != null) {
                                                                for (i in 0 until ga.length()) {
                                                                    val o = ga.optJSONObject(i) ?: continue
                                                                    val id = o.optLong("id_glucosa", -1L)
                                                                    val fecha = o.optString("fecha_sincronizacion", null)
                                                                    if (id > 0) {
                                                                        glucosaTimestampMap[id] = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (root.has("presion")) {
                                                            val pa = root.optJSONArray("presion")
                                                            if (pa != null) {
                                                                for (i in 0 until pa.length()) {
                                                                    val o = pa.optJSONObject(i) ?: continue
                                                                    val id = o.optLong("id_presion", -1L)
                                                                    val fecha = o.optString("fecha_sincronizacion", null)
                                                                    if (id > 0) {
                                                                        presionTimestampMap[id] = if (!fecha.isNullOrBlank()) fecha else defaultTimestamp
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // Option 2: server returns single timestamp for all
                                                        // { "syncedAt": "2025-10-12T10:00:00Z" }
                                                        if (glucosaTimestampMap.isEmpty() && presionTimestampMap.isEmpty() && root.has("syncedAt")) {
                                                            val s = root.optString("syncedAt", defaultTimestamp)
                                                            val ts = if (s.isNullOrBlank()) defaultTimestamp else s
                                                            glucosaIdsToMark.forEach { glucosaTimestampMap[it] = ts }
                                                            presionIdsToMark.forEach { presionTimestampMap[it] = ts }
                                                        }

                                                        // Option 3: server returns list of ids only, use default timestamp
                                                        // e.g. { "glucosaIds": [123,124], "presionIds":[...] }
                                                        if (glucosaTimestampMap.isEmpty() && root.has("glucosaIds")) {
                                                            val ga = root.optJSONArray("glucosaIds")
                                                            if (ga != null) {
                                                                for (i in 0 until ga.length()) {
                                                                    val id = ga.optLong(i, -1L)
                                                                    if (id > 0) glucosaTimestampMap[id] = defaultTimestamp
                                                                }
                                                            }
                                                        }
                                                        if (presionTimestampMap.isEmpty() && root.has("presionIds")) {
                                                            val pa = root.optJSONArray("presionIds")
                                                            if (pa != null) {
                                                                for (i in 0 until pa.length()) {
                                                                    val id = pa.optLong(i, -1L)
                                                                    if (id > 0) presionTimestampMap[id] = defaultTimestamp
                                                                }
                                                            }
                                                        }
                                                    } catch (je: Exception) {
                                                        // not JSON or unexpected shape — we'll fallback to marking all with now
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }

                                            // If maps are still empty, mark each id with default timestamp
                                            if (glucosaTimestampMap.isEmpty()) {
                                                glucosaIdsToMark.forEach { glucosaTimestampMap[it] = defaultTimestamp }
                                            }
                                            if (presionTimestampMap.isEmpty()) {
                                                presionIdsToMark.forEach { presionTimestampMap[it] = defaultTimestamp }
                                            }

                                            // Now persist to DB on IO dispatcher
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        if (glucosaTimestampMap.isNotEmpty()) {
                                                            dbHelper.updateGlucosaSyncTimestamps(glucosaTimestampMap)
                                                        }
                                                        if (presionTimestampMap.isNotEmpty()) {
                                                            dbHelper.updatePresionSyncTimestamps(presionTimestampMap)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }, onFailure = { err ->
                                            lastResponse = "Error: ${err.message}"
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                        })
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        lastResponse = "Error inesperado: ${e.message}"
                                        Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
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
                        ),
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
    // Do NOT call sdf.timeZone = ... — leaving it unset uses the device timezone
    return sdf.format(java.util.Date())
}