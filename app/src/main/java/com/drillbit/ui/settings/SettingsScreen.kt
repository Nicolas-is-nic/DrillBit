package com.drillbit.ui.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.spike.SpikeTest
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.SettingRow
import com.drillbit.ui.components.SettingSwitchRow
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/**
 * P16 设置（Tab4）：分组顺序为 外观 / 服务器与题库 / AI 模型 / 笔记 / 关于。
 *
 * 深色模式开关是一键切换整套配色的入口；版本号为只读行（无箭头）。
 */
@Composable
fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    val colors = dbColors()
    // ===== S0 验证（临时）：F3 通知 / F4-L2 钉屏真机验证入口，定案后删除 =====
    val context = LocalContext.current
    var pinned by remember { mutableStateOf(false) }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            SpikeTest.scheduleNotification(context)
            Toast.makeText(context, "已预约：5 分钟后弹通知，现在去杀掉 App", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "通知权限被拒：这本身也是测试结果，F3 需重新评估", Toast.LENGTH_LONG).show()
        }
    }
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
                SectionTitle(text = "S0 验证（临时）")
                SettingRow(
                    title = "立即通知测试",
                    valueText = "验证通知通道",
                    onClick = {
                        SpikeTest.postNotification(context)
                        Toast.makeText(context, "已发出：退到后台后下拉状态栏查看", Toast.LENGTH_LONG).show()
                    },
                )
                SettingRow(
                    title = "短时通知测试",
                    valueText = "2 分钟后 · 不杀 App",
                    onClick = {
                        SpikeTest.scheduleNotification(context, 2 * 60 * 1000)
                        Toast.makeText(context, "已预约 2 分钟：退到后台等待，不要杀掉 App", Toast.LENGTH_LONG).show()
                    },
                )
                SettingRow(
                    title = "定时通知测试",
                    valueText = "预约 5 分钟后通知",
                    onClick = {
                        val granted = Build.VERSION.SDK_INT < 33 ||
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            SpikeTest.scheduleNotification(context)
                            Toast.makeText(context, "已预约：5 分钟后弹通知，现在去杀掉 App", Toast.LENGTH_LONG).show()
                        } else {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                SettingRow(
                    title = "钉屏测试",
                    valueText = if (pinned) "已钉屏，点击解除" else "点击进入钉屏",
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            pinned = SpikeTest.togglePin(activity, pinned)
                        }
                    },
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
    versionText = "v0.1.0（1）",
    hasCrashLog = false,
)

/** 预览假数据：全部未配置（首次安装） */
private fun previewFreshState() = SettingsUiState(
    darkMode = false,
    serverConfigured = false,
    updateAvailableText = null,
    modelSummary = "未配置",
    lastBackupText = "--",
    versionText = "v0.1.0（1）",
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
