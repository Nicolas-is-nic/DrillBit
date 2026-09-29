package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.theme.dbColors

/**
 * 顶栏：
 * - onBack 为空：大标题首页型（题库 / 错题 / 笔记 / 设置）
 * - onBack 非空：二级页型（左返回 + 居中标题 + 右侧动作）
 */
@Composable
fun DbTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = dbColors()
    if (onBack == null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = colors.text2,
                modifier = Modifier
                    .width(14.dp)
                    .clickable(onClick = onBack),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}

/** 顶栏右侧动作按钮（同步 / 全部重考 / 梳理 / 备份 等） */
@Composable
fun DbTopBarAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) colors.primary else colors.text2,
        )
    }
}

/** 顶栏右侧状态文字（进度、模型名等只读信息） */
@Composable
fun DbTopBarInfo(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = dbColors()
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.text2,
        modifier = modifier,
    )
}
