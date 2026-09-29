package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/**
 * 遮罩弹窗容器：全屏遮罩 + 居中卡片（16dp 圆角）。
 *
 * 弹窗的关闭由内部按钮事件驱动（对照设计稿），调用方把本组件放在页面根 Box 内即可。
 */
@Composable
fun ScrimModal(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = dbColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .padding(horizontal = 15.dp, vertical = 16.dp),
            content = content,
        )
    }
}
