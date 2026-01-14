package com.codeco.tabacaleros.features.home.ui.subViews

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codeco.tabacaleros.R
import androidx.compose.ui.res.colorResource

@Composable
fun DNI(
    modifier: Modifier = Modifier,
    dniUri: Uri?,
    onTakeDniPicture: () -> Unit,
    onEditDniPicture: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "FOTO DNI",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.principal)
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.backgroundLight))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = colorResource(R.color.accent),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            dniUri != null -> {
                                AsyncImage(
                                    model = dniUri,
                                    contentDescription = "DNI foto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Icon(
                                    painter = painterResource(R.drawable.idcard),
                                    contentDescription = "Placeholder",
                                    modifier = Modifier.size(130.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "La imagen debe ser nitida y completa",
                        color = Color.Black,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    if (dniUri != null) {
                        Button(
                            onClick = { onEditDniPicture() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.principal),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Editar Foto")
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { onTakeDniPicture() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .background(
                                color = colorResource(R.color.accent),
                                shape = CircleShape
                            )
                            .size(90.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_camera_alt_24),
                            contentDescription = "Tomar foto",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }
        }
    }
}
