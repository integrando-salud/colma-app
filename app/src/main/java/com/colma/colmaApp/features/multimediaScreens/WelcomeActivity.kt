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
import com.colma.colmaApp.ui.theme.ColmaAppTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape // Added for indicator shape
import androidx.compose.ui.unit.Dp // Added for indicator parameters
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
        window.navigationBarColor = AndroidColor.WHITE

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // Dark status bar (principal color), so light icons
            isAppearanceLightNavigationBars = true // White navigation bar, so dark icons
        }

        val displayContent = intent.getStringExtra("displayContent") ?: "No content provided."

        setContent {
            ColmaAppTheme {
                WelcomePageScreen(
                    displayContent = displayContent,
                    onBack = { finish() },
                    onNext = {
                        startActivity(Intent(this@WelcomeActivity, ActionActivity::class.java))
                    },
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WelcomePageScreen(displayContent: String, onBack: () -> Unit = {}, onNext: () -> Unit = {}, onFinish: () -> Unit = {}) {
    val signatureColor = colorResource(R.color.principal)
    val paragraphs = remember {
        displayContent.split("\n\n").filter { it.isNotBlank() }
    }
    val pagerState = rememberPagerState(pageCount = { paragraphs.size })
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
                .padding(horizontal = 16.dp, vertical = 8.dp), // Adjust vertical padding for pager
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (paragraphs.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        WelcomePage(displayContent = paragraphs[page])
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        pageCount = paragraphs.size,
                        activeColor = signatureColor,
                        inactiveColor = Color.LightGray,
                        modifier = Modifier
                            .padding(top = 8.dp)
                    )
                } else {
                    WelcomePage(displayContent = "No content provided.")
                }
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
                
                // NOTE: This WelcomePage composable will now receive a single paragraph string
                @Composable
                fun WelcomePage(displayContent: String) {
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
                                displayContent,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Left
                            )
                        }
                    }
                }
                
                @OptIn(ExperimentalFoundationApi::class)
                @Composable
                fun HorizontalPagerIndicator(
                    pagerState: androidx.compose.foundation.pager.PagerState,
                    pageCount: Int,
                    modifier: Modifier = Modifier,
                    activeColor: Color = Color.White,
                    inactiveColor: Color = Color.Gray,
                    indicatorWidth: Dp = 8.dp,
                    indicatorHeight: Dp = 8.dp,
                    spacing: Dp = 8.dp,
                    indicatorShape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier,
                    ) {
                        repeat(pageCount) { index ->
                            val color = if (pagerState.currentPage == index) activeColor else inactiveColor
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = spacing / 2)
                                    .size(indicatorWidth, indicatorHeight)
                                    .background(color = color, shape = indicatorShape)
                            )
                        }
                    }
                }