package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/** 信息统计块：卡片内若干「键 — 值」行（题库详情、备份状态） */
@Composable
fun StatBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        content = content,
    )
}

/**
 * 统计行：左侧键（次要文字）+ 右侧值（主文字加粗）。
 *
 * divider = true 时在该行下方画细线，用于分隔多行。
 */
@Composable
fun StatRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
) {
    val colors = dbColors()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelMedium,
                color = colors.text2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
        }
        if (divider) {
            HorizontalDivider(thickness = 1.dp, color = colors.line)
        }
    }
}
