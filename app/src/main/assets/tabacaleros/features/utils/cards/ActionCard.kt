package com.codeco.tabacaleros.features.utils.cards
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeco.tabacaleros.R


@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    @DrawableRes iconRes: Int,
    iconContentDescription: String? = null,
    onClick: () -> Unit = {},
    accentColor: Color = colorResource(R.color.accent),
    containerColor: Color = colorResource(R.color.backgroundLight),
    titleColor: Color = colorResource(R.color.texto),
    subtitleColor: Color = Color.hsl(0f, 0f, 0.50f)
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .height(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(color = accentColor, shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = iconContentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                AutoResizeText(
                    text = title,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (subtitle.isNotEmpty()) {
                    AutoResizeText(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = subtitleColor,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    var scaledTextStyle by remember { mutableStateOf(style) }

    Text(
        text,
        modifier = modifier,
        style = scaledTextStyle,
        softWrap = false,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.95f)
            }
        }
    )
}

