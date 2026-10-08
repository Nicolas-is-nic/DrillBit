package com.drillbit.ui.settings

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
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBTextField
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.components.SettingRow
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/**
 * 账号与同步二级页（2026-10-08 从设置页拆出）：
 * 未登录 = 登录表单；已登录 = 当前账号 + 备份/恢复双动作 + 上次同步 + 登出。
 * 备份与恢复是两个方向独立的动作（B 方案拆分，替代原「立即同步=上传并下载」）。
 */
@Composable
fun AccountScreen(state: AccountUiState, onEvent: (AccountEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "账号与同步",
                onBack = { onEvent(AccountEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                if (!state.loggedIn) {
                    // 登录表单（输入态留页面本地，不进契约 7.13）
                    var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    DBTextField(label = "用户名", value = username, onChange = { username = it })
                    DBTextField(
                        label = "密码",
                        value = password,
                        onChange = { password = it },
                        password = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    DBButton(
                        text = if (state.busy) "登录中…" else "登录",
                        onClick = { onEvent(AccountEvent.LoginClick(username, password)) },
                        modifier = Modifier.fillMaxWidth(),
                        type = if (username.isBlank() || password.isBlank() || state.busy) {
                            DBButtonType.OFF
                        } else {
                            DBButtonType.PRIMARY
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "登录后本机会自动拉取云端快照；账号在服务器上创建，App 内不提供注册",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text2,
                    )
                } else {
                    SettingRow(title = "当前账号", valueText = state.username)
                    SettingRow(
                        title = "备份到云端",
                        valueText = if (state.busy) "备份中…" else "上传本机全部数据",
                        onClick = { if (!state.busy) onEvent(AccountEvent.BackupClick) },
                    )
                    SettingRow(
                        title = "用云端恢复",
                        valueText = if (state.busy) "恢复中…" else "云端快照覆盖本机",
                        onClick = { if (!state.busy) onEvent(AccountEvent.RestoreClick) },
                    )
                    SettingRow(title = "上次同步", valueText = state.lastSyncText)
                    SettingRow(
                        title = "登出账号",
                        valueText = "云端数据保留",
                        onClick = { if (!state.busy) onEvent(AccountEvent.LogoutClick) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                state.resultText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text2,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "同步机制：云端只保存一份最新快照。备份 = 用本机数据覆盖云端；恢复 = 用云端快照覆盖本机（本机未备份的改动会丢失）。多台设备时以最后备份的一台为准；换设备使用前，先把在用的那台备份一次。",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        when (state.dialog) {
            AccountDialog.BACKUP_OVERWRITE -> BackupOverwriteDialog(onEvent = onEvent)
            AccountDialog.RESTORE_OVERWRITE -> RestoreOverwriteDialog(onEvent = onEvent)
            AccountDialog.LOGOUT -> LogoutConfirmDialog(onEvent = onEvent)
            null -> Unit
        }
    }
}

/** 备份警告：云端有本机未拉取的新快照，继续备份将覆盖它 */
@Composable
private fun BackupOverwriteDialog(onEvent: (AccountEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "云端有新快照",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "云端存在其他设备上传的、本机未拉取的快照。继续备份将用本机数据覆盖云端那份。若本机数据是旧的，请取消并先用云端恢复。",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(AccountEvent.DialogCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "继续备份",
                onClick = { onEvent(AccountEvent.BackupConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 恢复警告：云端快照将覆盖本机，未备份的改动会丢 */
@Composable
private fun RestoreOverwriteDialog(onEvent: (AccountEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "用云端恢复",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "将下载云端快照并覆盖本机的笔记、收藏、进度、错题。本机自上次备份后的改动会丢失，确定继续？",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(AccountEvent.DialogCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "继续恢复",
                onClick = { onEvent(AccountEvent.RestoreConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 登出确认：防手滑 */
@Composable
private fun LogoutConfirmDialog(onEvent: (AccountEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "登出账号",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "仅清除本机登录凭证，云端数据保留；下次登录可重新拉取。确定登出？",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(AccountEvent.DialogCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "登出",
                onClick = { onEvent(AccountEvent.LogoutConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 预览假数据：已登录 */
private fun previewState() = AccountUiState(
    loggedIn = true,
    username = "yang",
    busy = false,
    resultText = "已备份到云端",
    lastSyncText = "10-08 13:40 · 安卓",
    dialog = null,
)

/** 预览假数据：未登录 */
private fun previewLoggedOutState() = AccountUiState(
    loggedIn = false,
    username = "",
    busy = false,
    resultText = null,
    lastSyncText = "从未",
    dialog = null,
)

@Preview(name = "账号与同步 · 已登录 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun AccountPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            AccountScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "账号与同步 · 未登录 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun AccountLoggedOutPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            AccountScreen(state = previewLoggedOutState(), onEvent = {})
        }
    }
}
