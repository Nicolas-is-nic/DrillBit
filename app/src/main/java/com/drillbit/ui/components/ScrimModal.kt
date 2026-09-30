package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/**
 * 遮罩弹窗容器：全屏遮罩 + 居中卡片（16dp 圆角）。
 *
 * 卡片高度上限为屏高 85% 且内容可滚动：长内容（如保存笔记预填全文）
 * 不再把底部按钮推出屏幕外导致用户无法确认（真机问题 3 的修复）。
 * 弹窗的关闭由内部按钮事件驱动（对照设计稿），调用方把本组件放在页面根 Box 内即可。
 */
@Composable
fun ScrimModal(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = dbColors()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 键盘弹出时整体上避，防止悬浮键盘盖住弹窗底部按钮（review m-1）
            // 注：曾加 imePadding 避让键盘，但卓易通容器 IME inset 回报异常，
            // 把整个弹窗层（连遮罩）挤出屏幕导致点击保存无任何反应，已回退；
            // 键盘遮挡由弹窗自身滚动能力兜底（限高+verticalScroll）
            .background(colors.scrim)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.85f)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .padding(horizontal = 15.dp, vertical = 16.dp),
            content = content,
        )
    }
}
