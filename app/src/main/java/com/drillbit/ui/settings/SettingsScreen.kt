package com.drillbit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.SettingRow
import com.drillbit.ui.components.SettingSwitchRow
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/**
 * P16 设置（Tab4）：分组顺序为 外观 / 服务器与题库 / 账号与同步（入口行）/ AI 模型 / 笔记 / 关于。
 *
 * 深色模式开关是一键切换整套配色的入口；版本号为只读行（无箭头）。
 * 账号与同步已拆为独立二级页（2026-10-08），本页仅保留入口行。
 */
@Composable
fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(title = "设置")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                SectionTitle(text = "外观")
                SettingSwitchRow(
                    title = "深色模式",
                    checked = state.darkMode,
                    onCheckedChange = { on -> onEvent(SettingsEvent.DarkModeChange(on)) },
                )
                Text(
                    text = "一键切换整套配色，只换颜色不改布局",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 6.dp),
                )
                SectionTitle(text = "服务器与题库")
                SettingRow(
                    title = "服务器地址",
                    valueText = if (state.serverConfigured) "已配置" else "未配置",
                    onClick = { onEvent(SettingsEvent.ServerClick) },
                )
                SettingRow(
                    title = "检查题库更新",
                    valueText = state.updateAvailableText ?: "已是最新",
                    onClick = { onEvent(SettingsEvent.CheckUpdate) },
                )
                SectionTitle(text = "账号与同步")
                SettingRow(
                    title = "账号与同步",
                    valueText = state.accountText,
                    onClick = { onEvent(SettingsEvent.AccountClick) },
                )
                SectionTitle(text = "AI 模型")
                SettingRow(
                    title = "模型配置",
                    valueText = state.modelSummary,
                    onClick = { onEvent(SettingsEvent.ModelClick) },
                )
                SectionTitle(text = "笔记")
                SettingRow(
                    title = "笔记备份",
                    valueText = state.lastBackupText,
                    onClick = { onEvent(SettingsEvent.BackupClick) },
                )
                SectionTitle(text = "关于")
                SettingRow(
                    title = "版本",
                    valueText = state.versionText,
                )
                SettingRow(
                    title = "崩溃日志",
                    valueText = if (state.hasCrashLog) "有" else "无",
                    onClick = { onEvent(SettingsEvent.CrashLogClick) },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** 设置分组标题 */
@Composable
private fun SectionTitle(text: String) {
    val colors = dbColors()
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.text2,
        modifier = Modifier.padding(top = 12.dp, bottom = 7.dp),
    )
}

/** 预览假数据：已配置服务器与模型、有题可更、上次备份成功 */
private fun previewState() = SettingsUiState(
    darkMode = false,
    serverConfigured = true,
    updateAvailableText = "2 个有新版本",
    modelSummary = "gpt-4o-mini · OpenAI 兼容",
    lastBackupText = "09-28 21:10",
    accountText = "yang",
    versionText = "v0.1.0（27）",
    hasCrashLog = false,
)

/** 预览假数据：全部未配置（首次安装） */
private fun previewFreshState() = SettingsUiState(
    darkMode = false,
    serverConfigured = false,
    updateAvailableText = null,
    modelSummary = "未配置",
    lastBackupText = "--",
    accountText = "未登录",
    versionText = "v0.1.0（27）",
    hasCrashLog = true,
)

@Preview(name = "设置 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun SettingsPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            SettingsScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "设置 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun SettingsPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            SettingsScreen(state = previewState().copy(darkMode = true), onEvent = {})
        }
    }
}

@Preview(name = "设置 · 未配置 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun SettingsFreshPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            SettingsScreen(state = previewFreshState(), onEvent = {})
        }
    }
}
