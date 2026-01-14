package com.codeco.tabacaleros.features.home.ui.CameraView

import android.graphics.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codeco.tabacaleros.R
import java.io.File
import java.io.FileOutputStream

private data class FilterOption(
    val name: String,
    val composeMatrix: ComposeColorMatrix,
    val androidMatrix: android.graphics.ColorMatrix
)

@Composable
fun EditPhotoScreen(
    photoFile: File,
    onApply: (File) -> Unit,
    onCancel: () -> Unit
) {

    val filters = remember {
        listOf(

            FilterOption(
                "Nada",
                composeMatrix = ComposeColorMatrix(),
                androidMatrix = android.graphics.ColorMatrix()
            ),


            FilterOption(
                "Gris",
                composeMatrix = ComposeColorMatrix().apply { setToSaturation(0f) },
                androidMatrix = android.graphics.ColorMatrix().apply { setSaturation(0f) }
            ),


            FilterOption(
                "Sepia",
                composeMatrix = ComposeColorMatrix(
                    floatArrayOf(
                        0.393f,0.769f,0.189f,0f,0f,
                        0.349f,0.686f,0.168f,0f,0f,
                        0.272f,0.534f,0.131f,0f,0f,
                        0f,    0f,    0f,    1f,0f
                    )
                ),
                androidMatrix = android.graphics.ColorMatrix(
                    floatArrayOf(
                        0.393f,0.769f,0.189f,0f,0f,
                        0.349f,0.686f,0.168f,0f,0f,
                        0.272f,0.534f,0.131f,0f,0f,
                        0f,    0f,    0f,    1f,0f
                    )
                )
            ),


            FilterOption(
                "Invert",
                composeMatrix = ComposeColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f,-1f, 0f, 0f, 255f,
                        0f, 0f,-1f, 0f, 255f,
                        0f, 0f, 0f, 1f,   0f
                    )
                ),
                androidMatrix = android.graphics.ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f,-1f, 0f, 0f, 255f,
                        0f, 0f,-1f, 0f, 255f,
                        0f, 0f, 0f, 1f,   0f
                    )
                )
            )
        )
    }


    var rotationDegrees by rememberSaveable { mutableStateOf(0f) }
    var selectedFilterIdx by rememberSaveable { mutableStateOf(0) }

    val filtersForRow = filters.filter { it.name != "Invert" }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        AsyncImage(
            model = photoFile,
            contentDescription = "Editing photo",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees },
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(
                filters[selectedFilterIdx].composeMatrix
            )
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filtersForRow.forEachIndexed { idx, filter ->
                val isSelected = (selectedFilterIdx == filters.indexOf(filter))
                OutlinedButton(
                    onClick = { selectedFilterIdx = filters.indexOf(filter) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        // green when selected, white otherwise
                        containerColor = if (isSelected) Color(0xFF689F38) else Color.Transparent,
                        // white text when selected, green otherwise
                        contentColor   = if (isSelected) Color.White else Color(0xFF689F38)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(filter.name)
                }
            }

            IconButton(
                onClick = {
                    rotationDegrees = (rotationDegrees + 90f) % 360f
                },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterVertically),

                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF689F38),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_sync_24),
                    contentDescription = "Rotate",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor   = Color(0xFF67310A)
                )) {
                Text("Cancelar")
            }
            Button(onClick = {

                val orig = BitmapFactory.decodeFile(photoFile.absolutePath)

                val rotated = Bitmap.createBitmap(
                    orig, 0, 0,
                    orig.width, orig.height,
                    Matrix().apply { postRotate(rotationDegrees) },
                    true
                )

                val outputBmp = Bitmap.createBitmap(
                    rotated.width, rotated.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(outputBmp)
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(
                        filters[selectedFilterIdx].androidMatrix
                    )
                }
                canvas.drawBitmap(rotated, 0f, 0f, paint)

                FileOutputStream(photoFile).use { out ->
                    outputBmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                onApply(photoFile)
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF67310A),
                    contentColor   = Color.White
                ),
                ) {
                Text("Aplicar")
            }
        }
    }
}