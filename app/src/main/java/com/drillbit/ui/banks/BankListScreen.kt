package com.drillbit.ui.banks

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBProgressBar
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarAction
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.components.TagChip
import com.drillbit.ui.components.TagChipType
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P1 题库列表（Tab1 首页）：题库卡片列表 + 混合抽题入口 + 更新弹窗（P3） */
@Composable
fun BankListScreen(state: BankListUiState, onEvent: (BankListEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "题库",
                actions = {
                    DbTopBarAction(
                        text = if (state.syncing) "同步中…" else "同步",
                        onClick = { onEvent(BankListEvent.SyncClick) },
                        enabled = !state.syncing,
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
                    text = state.lastSyncText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                state.banner?.let { b ->
                    Banner(
                        text = b.text,
                        type = b.type,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                state.banks.forEach { bank ->
                    BankListRow(
                        bank = bank,
                        onClick = { onEvent(BankListEvent.BankClick(bank.bankId)) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                DBButton(
                    text = "混合抽题",
                    onClick = { onEvent(BankListEvent.MixClick) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        state.updateDialog?.let { dialog ->
            UpdateDialog(dialog = dialog, onEvent = onEvent)
        }
    }
}

/** 题库卡片：名称 + 题数进度 + 版本标记 + 更新时间 */
@Composable
private fun BankListRow(bank: BankCard, onClick: () -> Unit) {
    val colors = dbColors()
    DBCard(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = onClick,
    ) {
        Text(
            text = bank.name,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${bank.questionCount} 题 · 已刷 ${bank.doneCount} 题",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(9.dp))
        DBProgressBar(
            progress = if (bank.questionCount > 0) {
                bank.doneCount.toFloat() / bank.questionCount
            } else {
                0f
            },
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TagChip(
                text = if (bank.hasUpdate) "有新版本" else "已是最新",
                type = if (bank.hasUpdate) TagChipType.OK else TagChipType.NORMAL,
            )
            Text(
                text = bank.updatedAtText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text2,
            )
        }
    }
}

/** P3 更新弹窗：逐库列出增量题数与无变化项 */
@Composable
private fun UpdateDialog(dialog: UpdateDialogState, onEvent: (BankListEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "题库更新",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = dialog.totalDeltaText,
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(6.dp))
        dialog.items.forEachIndexed { index, item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.text,
                    )
                    Text(
                        text = item.deltaText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                    )
                }
                if (index != dialog.items.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = colors.line)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(BankListEvent.UpdateCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "开始更新",
                onClick = { onEvent(BankListEvent.UpdateConfirm) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 预览假数据：题库列表（含一个新版本） */
private fun previewState() = BankListUiState(
    banks = listOf(
        BankCard("bank-llm", "大模型基础", 128, 42, true, "更新于 09-27"),
        BankCard("bank-agent", "Agent 与工具调用", 96, 60, false, "更新于 09-24"),
        BankCard("bank-tune", "模型微调与部署", 64, 0, false, "更新于 09-20"),
    ),
    syncing = false,
    lastSyncText = "服务器已连接 · 上次同步 今天 08:20",
    updateDialog = null,
    banner = null,
)

/** 预览假数据：更新弹窗展开 */
private fun previewDialogState() = previewState().copy(
    updateDialog = UpdateDialogState(
        totalDeltaText = "检查完成，共 2 个题库存在新版本，预计下载 17 道新题。",
        items = listOf(
            UpdateItem("大模型基础", "新增 12 题"),
            UpdateItem("Agent 与工具调用", "新增 5 题"),
            UpdateItem("模型微调与部署", "无变化"),
        ),
    ),
)

/** 预览假数据：空列表 */
private fun previewEmptyState() = previewState().copy(banks = emptyList())

@Preview(name = "题库列表 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankListPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "题库列表 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun BankListPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "题库列表 · 更新弹窗 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankListDialogPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankListScreen(state = previewDialogState(), onEvent = {})
        }
    }
}

@Preview(name = "题库列表 · 空列表 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankListEmptyPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankListScreen(state = previewEmptyState(), onEvent = {})
        }
    }
}
