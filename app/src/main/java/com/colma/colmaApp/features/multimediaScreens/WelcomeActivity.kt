package com.colma.colmaApp.features.multimediaScreens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.colma.colmaApp.features.MenuMain.MenuMain
import com.colma.colmaApp.ui.theme.ColmaAppTheme

@OptIn(ExperimentalMaterial3Api::class)
class DescriptionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        setContent {
            ColmaAppTheme {
                WelcomePageScreen(
                    onBack = { finish() },
                    onNext = {
                        startActivity(Intent(this@DescriptionActivity, ActionActivity::class.java))
                    },
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomePageScreen(onBack: () -> Unit = {}, onNext: () -> Unit = {}, onFinish: () -> Unit = {}) {
    val signatureColor = colorResource(R.color.principal)
    Surface(color = Color.White, modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Información", fontSize = 18.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = signatureColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
                    onClick = {
                        onFinish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = signatureColor, contentColor = Color.White),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Terminar", fontWeight = FontWeight.SemiBold)
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin facilisis nisi sit amet justo suscipit semper. Ut vitae nisl quis mauris iaculis tempus et eget eros. Aenean facilisis, urna at viverra feugiat, massa enim ultrices tortor, vitae blandit mauris est a quam. Nulla sit amet pulvinar dolor. Nunc vel quam massa. Mauris ac risus vel lacus pellentesque vestibulum. Pellentesque mauris nisi, sodales vel ornare sed, posuere eget magna. Praesent quam ante, bibendum eu condimentum sit amet, faucibus non ante. Suspendisse interdum varius cursus. Donec ut semper sem, nec consequat risus. Nam commodo faucibus arcu non dapibus. Suspendisse bibendum mauris sit amet ante ornare laoreet non nec ante. Sed vulputate condimentum metus, vitae consequat lacus sagittis et. In id purus lacinia nunc imperdiet volutpat. Duis eu libero vestibulum, malesuada justo ac, blandit lacus. Donec ornare mauris non est semper vulputate.\n" +
                        "\n" +
                        "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Praesent elit ante, rhoncus et dui eleifend, volutpat dictum mauris. Phasellus fringilla dolor ligula. Vivamus ultricies nisi et tortor hendrerit egestas. Nulla ultrices quam ipsum, vitae luctus libero facilisis tempus. Integer fermentum turpis sed leo tempor vehicula. Aenean suscipit ornare odio.",
                fontSize = 16.sp,
                textAlign = TextAlign.Left
            )
        }
    }
}