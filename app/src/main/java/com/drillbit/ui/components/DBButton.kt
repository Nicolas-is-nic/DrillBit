package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/**
 * 按钮类型：
 * PRIMARY 主色实心；GHOST 主色描边透明底；OFF 禁用态灰底；WARN 危险操作实心。
 */
enum class DBButtonType { PRIMARY, GHOST, OFF, WARN }

/**
 * 通用按钮（高度 44dp，圆角 10dp）。
 *
 * 宽度由调用方给定（Modifier.weight / fillMaxWidth）；enabled = false 时按 OFF 样式呈现。
 */
@Composable
fun DBButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: DBButtonType = DBButtonType.PRIMARY,
    enabled: Boolean = true,
    heightDp: Dp = 44.dp,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(10.dp)
    val effectiveType = if (enabled) type else DBButtonType.OFF
    val background = when (effectiveType) {
        DBButtonType.PRIMARY -> colors.primary
        DBButtonType.WARN -> colors.bad
        DBButtonType.OFF -> colors.off
        DBButtonType.GHOST -> Color.Transparent
    }
    val foreground = when (effectiveType) {
        DBButtonType.PRIMARY -> colors.onPrimary
        DBButtonType.WARN, DBButtonType.OFF -> colors.card
        DBButtonType.GHOST -> colors.primary
    }
    Box(
        modifier = modifier
            .height(heightDp)
            .clip(shape)
            .background(background)
            .then(if (effectiveType == DBButtonType.GHOST) Modifier.border(1.dp, colors.primary, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}
