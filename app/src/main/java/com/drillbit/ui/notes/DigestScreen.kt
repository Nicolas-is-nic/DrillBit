package com.drillbit.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P14 知识点归纳稿：由全部笔记梳理生成，按主题分组展示 */
@Composable
fun DigestScreen(state: DigestUiState, onEvent: (DigestEvent) -> Unit) {
    val colors = dbColors()
    // 查看全文弹窗为页面内本地状态，不进契约（事件语义已在页面内消化）
    var showFull by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "知识点归纳稿",
                onBack = { onEvent(DigestEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = state.metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                // 生成中：正文区显示进度文案（契约 7.9 streaming）
                if (state.streaming) {
                    Banner(
                        text = "正在梳理全部笔记，请稍候",
                        modifier = Modifier.padding(bottom = 10.dp),
                        type = BannerType.INFO,
                    )
                }
                state.sections.forEach { section ->
                    DBCard(
                        modifier = Modifier.padding(bottom = 10.dp),
                        accent = true,
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.text,
                        )
                        Spacer(Modifier.height(7.dp))
                        // 归纳稿正文为模型生成的 Markdown：富渲染（列表/加粗/行内代码），配色自动跟随主题
                        com.mikepenz.markdown.m3.Markdown(
                            content = section.body.replace(Regex("\\n{3,}"), "\\n\\n"),
                            typography = com.mikepenz.markdown.m3.markdownTypography(
                                text = MaterialTheme.typography.bodySmall.copy(lineHeight = 21.sp),
                            ),
                        )
                    }
                }
                Text(
                    text = state.footerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            HorizontalDivider(thickness = 1.dp, color = colors.line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
            ) {
                DBButton(
                    text = "重新梳理",
                    onClick = { onEvent(DigestEvent.Regenerate) },
                    modifier = Modifier.width(96.dp),
                    type = DBButtonType.GHOST,
                )
                Spacer(Modifier.width(10.dp))
                DBButton(
                    text = "查看全文",
                    onClick = { showFull = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (showFull) {
            ScrimModal {
                Text(
                    text = "归纳稿全文",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                )
                Spacer(Modifier.height(8.dp))
                com.mikepenz.markdown.m3.Markdown(
                    content = state.sections.joinToString("\n\n") { it.title + "\n" + it.body }
                        .replace(Regex("\\n{3,}"), "\\n\\n"),
                    typography = com.mikepenz.markdown.m3.markdownTypography(
                        text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    ),
                )
                Spacer(Modifier.height(12.dp))
                DBButton(
                    text = "关闭",
                    onClick = { showFull = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 预览假数据：已生成（含 6 个主题，此处展示前 3 个） */
private fun previewState() = DigestUiState(
    metaText = "由 23 条笔记梳理生成 · 09-29 08:05 · 已存入笔记",
    sections = listOf(
        DigestSection(
            title = "一、注意力与复杂度",
            body = "自注意力平方复杂度源于 n×n 分数矩阵；优化路线分稀疏注意力、线性注意力、分块三支；" +
                "KV Cache 省的是重复前向计算，不改变复杂度量级。",
        ),
        DigestSection(
            title = "二、Agent 与工具调用",
            body = "ReAct 循环为思考、行动、观察三步；观察把工具结果回灌上下文；工具描述质量直接决定调用成功率。",
        ),
        DigestSection(
            title = "三、微调与部署",
            body = "LoRA 只训练低秩矩阵，显存与存储开销大幅下降；QLoRA 进一步量化主干；" +
                "合并权重后可像原模型一样推理。",
        ),
    ),
    streaming = false,
    footerText = "全文共 6 个主题 · 已去除重复表述",
)

/** 预览假数据：生成中（尚无分组内容） */
private fun previewStreamingState() = DigestUiState(
    metaText = "正在基于 23 条笔记生成归纳稿",
    sections = emptyList(),
    streaming = true,
    footerText = "",
)

@Preview(name = "归纳稿 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun DigestPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            DigestScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "归纳稿 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun DigestPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            DigestScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "归纳稿 · 生成中 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun DigestStreamingPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            DigestScreen(state = previewStreamingState(), onEvent = {})
        }
    }
}
