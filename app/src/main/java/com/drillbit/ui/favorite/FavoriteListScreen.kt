package com.drillbit.ui.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarAction
import com.drillbit.ui.components.ExplainCard
import com.drillbit.ui.components.OptionRow
import com.drillbit.ui.components.OptionState
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.components.TagChip
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P20 我的收藏（F2）：收藏行 + 顶部「刷收藏」发起可反复刷的收藏会话（契约 7.15） */
@Composable
fun FavoriteListScreen(state: FavoriteListUiState, onEvent: (FavoriteListEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "我的收藏",
                onBack = { onEvent(FavoriteListEvent.Back) },
                actions = {
                    DbTopBarAction(
                        text = "刷收藏",
                        onClick = { onEvent(FavoriteListEvent.StartQuiz) },
                    )
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = state.summaryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                if (state.items.isEmpty()) {
                    // 空态文案由契约 7.15 指定
                    Text(
                        text = "暂无收藏，刷题时点亮星标即可收藏",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2,
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                    )
                } else {
                    state.items.forEach { item ->
                        FavoriteRow(
                            item = item,
                            onClick = { onEvent(FavoriteListEvent.ItemClick(item.questionId)) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        // 方案 A 同错题集：只读详情就地弹层
        state.detailDialog?.let { detail ->
            FavoriteDetailDialog(detail = detail, onEvent = onEvent)
        }
    }
}

/** 收藏行：题干摘要 + 收藏标记 + 来源题库与收藏日期 */
@Composable
private fun FavoriteRow(item: FavoriteItem, onClick: () -> Unit) {
    val colors = dbColors()
    DBCard(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = onClick,
    ) {
        Text(
            text = item.stemPreview,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TagChip(text = "已收藏")
            Text(
                text = "${item.bankName} · ${item.dateText}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.text2,
            )
        }
    }
}

/** 收藏详情弹层：完整题面 + 全部正确项标绿 + 解析（只读态，无计数） */
@Composable
private fun FavoriteDetailDialog(detail: FavoriteDetailUi, onEvent: (FavoriteListEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = detail.stem,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        detail.options.forEachIndexed { index, option ->
            OptionRow(
                label = ('A' + index).toString(),
                text = option,
                state = if (index in detail.correctIndices) OptionState.GOOD else OptionState.DISABLED,
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        ExplainCard(title = "解析", body = detail.explanation)
        Spacer(Modifier.height(12.dp))
        DBButton(
            text = "关闭",
            onClick = { onEvent(FavoriteListEvent.DetailDismiss) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 预览假数据：3 题收藏，来源与日期各不相同 */
private fun previewState() = FavoriteListUiState(
    items = listOf(
        FavoriteItem(
            questionId = "q-2001",
            stemPreview = "ReAct 范式中观察（Observation）的作用是什么？",
            bankName = "Agent 与工具调用",
            dateText = "10-06",
        ),
        FavoriteItem(
            questionId = "q-2002",
            stemPreview = "关于位置编码，下列说法正确的有哪些？",
            bankName = "大模型基础",
            dateText = "10-05",
        ),
        FavoriteItem(
            questionId = "q-2003",
            stemPreview = "LoRA 相比全量微调的主要优势是什么？",
            bankName = "模型微调与部署",
            dateText = "10-05",
        ),
    ),
    summaryText = "共 3 题已收藏 · 可反复刷",
    detailDialog = null,
)

@Preview(name = "收藏 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun FavoritePreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            FavoriteListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "收藏 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun FavoritePreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            FavoriteListScreen(state = previewState(), onEvent = {})
        }
    }
}
