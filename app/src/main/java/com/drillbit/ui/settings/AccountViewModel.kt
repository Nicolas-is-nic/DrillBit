package com.drillbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.net.UnauthorizedException
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 账号与同步页 ViewModel（2026-10-08 从设置页拆出，同步动作拆为备份/恢复两个独立语义）：
 * - 登录：成功后自动拉取云端快照（引导路径，只下载）
 * - 备份到云端：上传本机全量快照；云端有本机未拉取的新快照时先确认
 * - 用云端恢复：下载快照覆盖本机（恒确认）
 * - 登出：清本地凭证（恒确认）；云端数据保留
 * 401 统一处理：清凭证并提示重新登录（方案 D5）。
 */
class AccountViewModel : ViewModel() {

    /** 操作进行中（登录/备份/恢复/登出） */
    private val busy = MutableStateFlow(false)

    /** 操作结果文案（null=无） */
    private val result = MutableStateFlow<String?>(null)

    /** 当前确认弹窗（null=无） */
    private val dialog = MutableStateFlow<AccountDialog?>(null)

    val state = combine(
        ServiceLocator.settingsStore.settings,
        busy,
        result,
        dialog,
    ) { s, isBusy, resultText, currentDialog ->
        AccountUiState(
            loggedIn = s.authToken.isNotBlank(),
            username = s.authUser.orEmpty(),
            busy = isBusy,
            resultText = resultText,
            lastSyncText = if (s.lastSyncAt > 0) {
                "${TimeFmt.medium(s.lastSyncAt)} · 安卓"
            } else {
                "从未"
            },
            dialog = currentDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountUiState(
            loggedIn = false,
            username = "",
            busy = false,
            resultText = null,
            lastSyncText = "从未",
            dialog = null,
        ),
    )

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.LoginClick -> login(event.username, event.password)
            AccountEvent.BackupClick -> backupClick()
            AccountEvent.BackupConfirm -> {
                dialog.value = null
                doBackup()
            }
            AccountEvent.RestoreClick -> {
                if (!busy.value) dialog.value = AccountDialog.RESTORE_OVERWRITE
            }
            AccountEvent.RestoreConfirm -> {
                dialog.value = null
                doRestore()
            }
            AccountEvent.LogoutClick -> {
                if (!busy.value) dialog.value = AccountDialog.LOGOUT
            }
            AccountEvent.LogoutConfirm -> {
                dialog.value = null
                doLogout()
            }
            AccountEvent.DialogCancel -> dialog.value = null
            AccountEvent.Back -> Unit // 导航层处理
        }
    }

    /** 登录：成功后自动拉取云端快照（云端无快照的新账号首台设备不算失败） */
    private fun login(username: String, password: String) {
        if (busy.value || username.isBlank() || password.isBlank()) return
        busy.value = true
        viewModelScope.launch {
            runCatching {
                ServiceLocator.syncRepository.login(username.trim(), password)
                runCatching { ServiceLocator.syncRepository.downloadAndImport() }
                    .onSuccess { result.value = "登录成功，$it" }
                    .onFailure { e ->
                        // 区分云端确无快照与导入失败（2026-10-09 review F-2：统一显示「无快照」误导排障）
                        val msg = e.message.orEmpty()
                        result.value = if (msg.contains("云端暂无快照")) {
                            "登录成功（云端暂无快照）"
                        } else {
                            "登录成功，但快照导入失败：$msg。请先在题库页同步题库，再回本页用云端恢复"
                        }
                    }
            }.onFailure { e ->
                result.value = when (e) {
                    is UnauthorizedException -> "登录已失效，请重新登录"
                    else -> "登录失败：${e.message ?: "网络异常"}"
                }
                // 登录接口本身 401 是密码错误，不清凭证；后续同步 401 才清
                if (e is UnauthorizedException) ServiceLocator.syncRepository.logout()
            }
            busy.value = false
        }
    }

    /** 备份入口：云端有本机未拉取的新快照（其他设备传的）先确认，否则直接上传 */
    private fun backupClick() {
        if (busy.value) return
        viewModelScope.launch {
            runCatching { ServiceLocator.syncRepository.meta() }
                .onSuccess { meta ->
                    if (meta == null) {
                        doBackup()
                    } else {
                        val settings = ServiceLocator.settingsStore.snapshot()
                        if (meta.uploadedAt > settings.lastSyncAt) {
                            dialog.value = AccountDialog.BACKUP_OVERWRITE
                        } else {
                            doBackup()
                        }
                    }
                }
                .onFailure { e -> handleError(e, "备份失败") }
        }
    }

    private fun doBackup() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            runCatching { ServiceLocator.syncRepository.upload() }
                .onSuccess { result.value = "已备份到云端" }
                .onFailure { e -> handleError(e, "备份失败") }
            busy.value = false
        }
    }

    private fun doRestore() {
        busy.value = true
        viewModelScope.launch {
            runCatching { ServiceLocator.syncRepository.downloadAndImport() }
                .onSuccess { result.value = "已从云端恢复，$it" }
                .onFailure { e -> handleError(e, "恢复失败") }
            busy.value = false
        }
    }

    private fun doLogout() {
        viewModelScope.launch {
            ServiceLocator.syncRepository.logout()
            result.value = "已登出（云端数据保留）"
        }
    }

    /** 401 清凭证并提示重登（D5），其余给可读原因 */
    private fun handleError(e: Throwable, prefix: String) {
        if (e is UnauthorizedException) {
            viewModelScope.launch { ServiceLocator.syncRepository.logout() }
            result.value = "登录已失效，请重新登录"
        } else {
            result.value = "$prefix：${e.message ?: "网络异常"}"
        }
    }
}
