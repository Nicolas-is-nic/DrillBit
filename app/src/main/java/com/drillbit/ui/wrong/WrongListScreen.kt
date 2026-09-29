package com.drillbit.ui.wrong

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
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarAction
import com.drillbit.ui.components.TagChip
import com.drillbit.ui.components.TagChipType
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P8 错题集（Tab2）：错题行带重考计数，顶部「全部重考」发起一次重考 */
@Composable
fun WrongListScreen(state: WrongListUiState, onEvent: (WrongListEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "错题",
                actions = {
                    DbTopBarAction(
                        text = "全部重考",
                        onClick = { onEvent(WrongListEvent.RetryAll) },
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
                    // 空态文案由契约 7.5 指定
                    Text(
                        text = "暂无错题，继续加油",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text2,
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                    )
                } else {
                    state.items.forEach { item ->
                        WrongRow(
                            item = item,
                            onClick = { onEvent(WrongListEvent.ItemClick(item.questionId)) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** 错题行：题干摘要 + 重考计数 + 来源题库与日期 */
@Composable
private fun WrongRow(item: WrongItem, onClick: () -> Unit) {
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
            TagChip(
                text = item.countText,
                type = TagChipType.WARN,
            )
            Text(
                text = "${item.bankName} · ${item.dateText}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.text2,
            )
        }
    }
}

/** 预览假数据：14 题待清，计数各不相同 */
private fun previewState() = WrongListUiState(
    items = listOf(
        WrongItem(
            questionId = "q-1001",
            stemPreview = "自注意力机制的计算复杂度随序列长度如何增长？",
            bankName = "大模型基础",
            dateText = "09-28",
            countText = "重考计数 3/3",
        ),
        WrongItem(
            questionId = "q-1002",
            stemPreview = "ReAct 范式中观察（Observation）的作用是什么？",
            bankName = "Agent 与工具调用",
            dateText = "09-28",
            countText = "重考计数 2/3",
        ),
        WrongItem(
            questionId = "q-1003",
            stemPreview = "LoRA 相比全量微调的主要优势是什么？",
            bankName = "模型微调与部署",
            dateText = "09-27",
            countText = "重考计数 1/3",
        ),
        WrongItem(
            questionId = "q-1004",
            stemPreview = "KV Cache 解决了推理中的什么问题？",
            bankName = "大模型基础",
            dateText = "09-26",
            countText = "重考计数 3/3",
        ),
    ),
    summaryText = "共 14 题待清 · 每答对一次，计数减一，减到 0 移出错题集",
)

/** 预览假数据：空态 */
private fun previewEmptyState() = WrongListUiState(
    items = emptyList(),
    summaryText = "共 0 题待清 · 每答对一次，计数减一，减到 0 移出错题集",
)

@Preview(name = "错题集 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun WrongListPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            WrongListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "错题集 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun WrongListPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            WrongListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "错题集 · 空态 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun WrongListEmptyPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            WrongListScreen(state = previewEmptyState(), onEvent = {})
        }
    }
}
