package com.drillbit.ui.banks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.components.StatBox
import com.drillbit.ui.components.StatRow
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P2 题库详情：题数 / 进度 / 上次拉取时间 / 删除入口，主按钮承接断点续刷 */
@Composable
fun BankDetailScreen(state: BankDetailUiState, onEvent: (BankDetailEvent) -> Unit) {
    val colors = dbColors()
    // done >= total 时主按钮文案改为「从头刷题」（契约 7.2 注释）
    val continueText = if (state.done >= state.total) {
        "从头刷题"
    } else {
        "继续刷题 · 第 ${state.done + 1} 题"
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = state.name,
                onBack = { onEvent(BankDetailEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                StatBox {
                    StatRow(key = "题目总数", value = "${state.total} 题", divider = true)
                    StatRow(key = "已刷进度", value = "${state.done} / ${state.total}", divider = true)
                    StatRow(key = "上次拉取", value = state.lastSyncText, divider = true)
                    StatRow(key = "服务器版本", value = state.serverVersionText)
                }
                Spacer(Modifier.height(11.dp))
                DBButton(
                    text = continueText,
                    onClick = { onEvent(BankDetailEvent.ContinueClick) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DBButton(
                        text = "从头重刷",
                        onClick = { onEvent(BankDetailEvent.RestartClick) },
                        modifier = Modifier.weight(1f),
                        type = DBButtonType.GHOST,
                    )
                    Spacer(Modifier.width(9.dp))
                    DBButton(
                        text = "检查更新",
                        onClick = { onEvent(BankDetailEvent.CheckUpdateClick) },
                        modifier = Modifier.weight(1f),
                        type = if (state.hasUpdate) DBButtonType.PRIMARY else DBButtonType.GHOST,
                    )
                }
                Text(
                    text = "删除本地题库",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.bad,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(BankDetailEvent.DeleteClick) }
                        .padding(vertical = 12.dp),
                )
                Text(
                    text = "删除只清本地数据，需要时可重新拉取",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        if (state.deleteConfirmVisible) {
            DeleteConfirmDialog(onEvent = onEvent)
        }
    }
}

/** 删除二次确认弹窗 */
@Composable
private fun DeleteConfirmDialog(onEvent: (BankDetailEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "删除本地题库",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "删除只清本地数据，需要时可重新拉取。",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(BankDetailEvent.DeleteCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "删除",
                onClick = { onEvent(BankDetailEvent.DeleteConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 预览假数据：有新版可更新 */
private fun previewState() = BankDetailUiState(
    bankId = "bank-llm",
    name = "大模型基础",
    total = 128,
    done = 42,
    lastSyncText = "09-27 21:40",
    serverVersionText = "v3 · 有新版本 v4",
    hasUpdate = true,
    deleteConfirmVisible = false,
)

/** 预览假数据：已刷完且已是最新 */
private fun previewFinishedState() = BankDetailUiState(
    bankId = "bank-agent",
    name = "Agent 与工具调用",
    total = 96,
    done = 96,
    lastSyncText = "09-24 08:05",
    serverVersionText = "v2 · 已是最新",
    hasUpdate = false,
    deleteConfirmVisible = false,
)

@Preview(name = "题库详情 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankDetailPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankDetailScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "题库详情 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun BankDetailPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankDetailScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "题库详情 · 删除确认 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankDetailDeletePreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankDetailScreen(state = previewState().copy(deleteConfirmVisible = true), onEvent = {})
        }
    }
}

@Preview(name = "题库详情 · 已刷完 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BankDetailFinishedPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BankDetailScreen(state = previewFinishedState(), onEvent = {})
        }
    }
}
