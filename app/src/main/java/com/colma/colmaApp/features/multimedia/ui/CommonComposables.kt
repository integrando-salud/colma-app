package com.colma.colmaApp.features.multimedia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colma.colmaApp.R
import kotlin.math.max

@Composable
fun NavigationButtons(
    currentIndex: Int,
    maxIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    signatureColor: Color
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentIndex > 0) {
            Button(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.principal),
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Anterior", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = signatureColor,
                contentColor = Color.White
            ),
        ) {
            val isLast = currentIndex == maxIndex
            Text(text = if (isLast) "Finalizar" else "Siguiente", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun FlexibleFooterColumn(
    minHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
            Column(modifier = Modifier.fillMaxWidth()) { footer() }
        }
    ) { measurables, constraints ->
        val contentPlaceable = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val footerPlaceable = measurables[1].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))

        val totalHeight = max(minHeight.roundToPx(), contentPlaceable.height + footerPlaceable.height)

        layout(constraints.maxWidth, totalHeight) {
            contentPlaceable.placeRelative(0, 0)
            footerPlaceable.placeRelative(0, totalHeight - footerPlaceable.height)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Gray,
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = 8.dp,
    spacing: Dp = 8.dp,
    indicatorShape: Shape = CircleShape
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

fun Modifier.scrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = this.drawWithContent {
    drawContent()
    if (state.maxValue > 0) {
        val vpH = size.height
        val tH = (vpH / (state.maxValue + vpH)) * vpH * 0.5f
        val sO = state.value.toFloat()
        val sP = sO / state.maxValue
        val tO = sP * (vpH - tH)
        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), tO),
            size = Size(width.toPx(), tH)
        )
    }
}

fun Modifier.scrollbar(
    state: LazyListState,
    width: Dp = 6.dp,
    color: Color = Color.Gray.copy(alpha = 0.8f)
): Modifier = this.drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isNotEmpty()) {
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItemsCount = visibleItemsInfo.size
        if (visibleItemsCount < totalItemsCount) {
            val vpH = size.height
            val tH = (visibleItemsCount.toFloat() / totalItemsCount) * vpH
            val fVI = visibleItemsInfo.first()
            val fVIdx = fVI.index
            val fVO = fVI.offset.toFloat()
            val iS = fVI.size.toFloat()
            val sP = (fVIdx.toFloat() - (fVO / iS)) / totalItemsCount.toFloat()
            val tO = (sP * vpH).coerceIn(0f, vpH - tH)
            drawRect(
                color = color,
                topLeft = Offset(size.width - width.toPx(), tO),
                size = Size(width.toPx(), tH)
            )
        }
    }
}
