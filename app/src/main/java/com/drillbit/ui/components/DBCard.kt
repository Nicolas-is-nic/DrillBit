package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/** 卡片圆角档位（设计稿统一值：大卡 16dp） */
private val CardShape = RoundedCornerShape(16.dp)

/**
 * 通用卡片容器：卡片底色 + 1dp 细线描边 + 16dp 圆角。
 *
 * accent = true 时左侧叠一条主色竖条，用于归纳稿一类的「生成物」卡片。
 * onClick 非空时整卡可点（点击波纹限制在圆角内）。
 */
@Composable
fun DBCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = dbColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.line, CardShape)
            .then(
                if (accent) {
                    Modifier.drawBehind {
                        drawRect(color = colors.primary, size = Size(3.dp.toPx(), size.height))
                    }
                } else {
                    Modifier
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        content = content,
    )
}
