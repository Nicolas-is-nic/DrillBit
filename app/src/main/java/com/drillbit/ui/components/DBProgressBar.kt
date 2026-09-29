package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/**
 * 线性进度条：槽用 chip 色、进度用主色。
 *
 * thin = true 时为页面顶部通栏细条（3dp，无圆角），否则为卡片内粗条（4dp，圆角 2dp）。
 */
@Composable
fun DBProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thin: Boolean = false,
) {
    val colors = dbColors()
    val shape = if (thin) RectangleShape else RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (thin) 3.dp else 4.dp)
            .clip(shape)
            .background(colors.chip),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(colors.primary),
        )
    }
}
