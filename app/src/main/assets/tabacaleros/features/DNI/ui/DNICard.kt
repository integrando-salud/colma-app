package com.codeco.tabacaleros.features.DNI.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeco.tabacaleros.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.colorResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.codeco.tabacaleros.features.DNI.model.ProductorInfo

@Composable
fun DNICard(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes logoRes: Int? = R.drawable.tabaco_logo,
    cardBackground: Color = colorResource(id = R.color.backgroundLight),
    accentColor: Color = colorResource(id = R.color.principal),
    initialNumber: String = "",
    onNumberChange: (String) -> Unit = {},
    onNumberSubmitted: (String) -> Unit,
    productorInfo: ProductorInfo? = null,
    onCallPhone: ((String) -> Unit)? = null,
    onPhoneNumberChange: (String) -> Unit = {},
    phoneVerifiedStatus: Boolean? = null,
    onVerificationChange: (Boolean) -> Unit = {},
    fromConsulta: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    fun sanitize(input: String): String = input.filter { it.isDigit() || it == '-' || it == ' ' }

    var rawNumber by rememberSaveable { mutableStateOf(initialNumber) }
    val digitsOnly = remember(rawNumber) { rawNumber.filter { it.isDigit() } }

    LaunchedEffect(digitsOnly) { onNumberChange(digitsOnly) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (logoRes != null) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(88.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rawNumber,
                onValueChange = { incoming ->
                    rawNumber = sanitize(incoming)
                },
                label = { Text("Número") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onNumberSubmitted(digitsOnly)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    cursorColor = accentColor,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor = accentColor
                )
            )


            productorInfo?.let { info ->

                Spacer(modifier = Modifier.height(50.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = info.nombre,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )


                }
                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {

                    info.estado?.let { estado ->
                        Spacer(modifier = Modifier.width(8.dp))
                        val (estadoText, backgroundColor) = when (estado) {
                            "Normal" -> "Activo" to Color(0xFF4CAF50)
                            "Pre-alta" -> "Pre-alta" to Color(0xFFFFC107) // Amber color for Pre-alta
                            else -> "Inactivo" to Color.Red
                        }
                        Text(
                            text = estadoText,
                            color = Color.White, // Set text color to white for better contrast
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(backgroundColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                }

                Spacer(modifier = Modifier.height(4.dp))
                if(!fromConsulta) {
                    var editablePersonPhone by rememberSaveable { mutableStateOf("+54 9 9999 999999") }
                    onPhoneNumberChange(editablePersonPhone)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editablePersonPhone,
                            onValueChange = { editablePersonPhone = it
                                            onPhoneNumberChange(it)},
                            label = { Text("Teléfono") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                cursorColor = accentColor,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                focusedLabelColor = accentColor
                            )
                        )
                        onCallPhone?.let {
                            if (editablePersonPhone.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { it(editablePersonPhone) }) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call phone")
                                }
                            }
                        }
                    }
                    phoneVerifiedStatus?.let { isVerified ->
                        val statusText = if (isVerified) "Verificado" else "No Verificado"
                        val statusColor = if (isVerified) Color.Green else Color.Red
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onVerificationChange(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Verificar")
                        }
                        Button(
                            onClick = { onVerificationChange(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Omitir")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
			
        }
    }
}