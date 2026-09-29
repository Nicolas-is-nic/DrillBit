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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.StatBox
import com.drillbit.ui.components.StatRow
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P15 笔记备份：单向备份到个人服务器，页面只回答「上次何时传的、传了多少条、这次成没成」 */
@Composable
fun BackupScreen(state: BackupUiState, onEvent: (BackupEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "笔记备份",
                onBack = { onEvent(BackupEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                StatBox {
                    StatRow(key = "上次备份", value = state.lastBackupText, divider = true)
                    StatRow(key = "已备份条数", value = "${state.backedCount} 条", divider = true)
                    StatRow(key = "服务器", value = state.serverHost)
                }
                Spacer(Modifier.height(11.dp))
                state.resultBanner?.let { banner ->
                    Banner(
                        text = banner.text,
                        modifier = Modifier.padding(bottom = 10.dp),
                        type = banner.type,
                    )
                }
                Text(
                    text = "单向备份：只把手机上的笔记传到服务器保存，不会从服务器拉回覆盖本地",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
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
                    text = if (state.uploading) "上传中…" else "立即上传备份",
                    onClick = { onEvent(BackupEvent.Upload) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.uploading,
                )
            }
        }
    }
}

/** 预览假数据：上次备份成功 */
private fun previewState() = BackupUiState(
    lastBackupText = "09-28 21:10",
    backedCount = 23,
    serverHost = "note.example.com",
    uploading = false,
    resultBanner = BannerUi(text = "上次备份成功，共上传 23 条笔记", type = BannerType.OK),
)

/** 预览假数据：上传中 */
private fun previewUploadingState() = previewState().copy(uploading = true, resultBanner = null)

/** 预览假数据：上传失败（含原因） */
private fun previewFailedState() = previewState().copy(
    resultBanner = BannerUi(text = "备份失败：连接超时，请检查服务器地址与网络后重试", type = BannerType.WARN),
)

/** 预览假数据：从未备份 */
private fun previewNeverState() = BackupUiState(
    lastBackupText = "--",
    backedCount = 0,
    serverHost = "未配置",
    uploading = false,
    resultBanner = null,
)

@Preview(name = "笔记备份 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BackupPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BackupScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记备份 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun BackupPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BackupScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记备份 · 上传中 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BackupUploadingPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BackupScreen(state = previewUploadingState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记备份 · 失败 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BackupFailedPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BackupScreen(state = previewFailedState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记备份 · 从未备份 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun BackupNeverPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            BackupScreen(state = previewNeverState(), onEvent = {})
        }
    }
}
