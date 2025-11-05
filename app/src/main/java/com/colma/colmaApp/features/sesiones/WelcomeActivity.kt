package com.colma.colmaApp.features.sesiones

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import com.colma.colmaApp.ui.theme.ColmaAppTheme

@OptIn(ExperimentalMaterial3Api::class)
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColmaAppTheme {
                WelcomePageScreen(
                    onBack = { finish() },
                    onNext = {
                        startActivity(Intent(this@WelcomeActivity, ActionActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomePageScreen(onBack: () -> Unit = {}, onNext: () -> Unit = {}) {
    val signatureColor = colorResource(R.color.principal)
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Información", fontSize = 18.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
            )

            // Your existing welcome content
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomePage() // reuse composable you already have
            }

            // Bottom button row
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = signatureColor, contentColor = Color.White),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Siguiente", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Información importante",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Icon(
                painter = painterResource(R.drawable.outline_unknown_document_24),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFE84589)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sed laoreet ligula, et semper nisi. Donec a elit eget justo rutrum porttitor et non lectus. Praesent ultricies, nulla non tincidunt dapibus, ex est efficitur justo, ac placerat libero velit ac justo. Aenean ornare sem nibh, eu pharetra dolor gravida vitae. Phasellus hendrerit orci id libero auctor tempor. Praesent tincidunt sed lacus nec condimentum. Sed commodo consequat lacus, eu pharetra quam auctor et. Proin sed metus pulvinar, blandit neque ut, iaculis augue. Etiam egestas velit at magna laoreet dapibus. Nulla facilisi. Curabitur in arcu non orci sollicitudin dictum.",
                fontSize = 16.sp,
                textAlign = TextAlign.Left
            )
        }
    }
}