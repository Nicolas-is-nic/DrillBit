package com.drillbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.BuildConfig
import com.drillbit.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel：服务器/模型/备份/版本/崩溃日志状态。
 * DarkModeChange 由 MainActivity 直接处理（DataStore 单一归属），不经本 VM。
 * 账号与同步状态已移至 AccountViewModel（2026-10-08 拆分批次）。
 */
class SettingsViewModel : ViewModel() {

    /** 检查更新提示（null 表示未检查或已最新） */
    private val updateHint = MutableStateFlow<String?>(null)

    val state = combine(
        ServiceLocator.settingsStore.settings,
        updateHint,
    ) { settings, hint ->
        SettingsUiState(
            darkMode = settings.darkMode,
            serverConfigured = settings.serverUrl.isNotBlank(),
            updateAvailableText = hint,
            modelSummary = if (settings.llmModel.isBlank()) {
                "未配置"
            } else {
                "${settings.llmModel} · ${if (settings.llmType == "anthropic") "Anthropic" else "OpenAI 兼容"}"
            },
            lastBackupText = if (settings.lastBackupAt > 0) {
                com.drillbit.util.TimeFmt.medium(settings.lastBackupAt)
            } else {
                "--"
            },
            accountText = settings.authUser ?: "未登录",
            versionText = "v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
            hasCrashLog = ServiceLocator.hasPendingCrashLog(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            darkMode = false,
            serverConfigured = false,
            updateAvailableText = null,
            modelSummary = "未配置",
            lastBackupText = "--",
            accountText = "未登录",
            versionText = "v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
            hasCrashLog = false,
        ),
    )

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.CheckUpdate -> checkUpdate()
            else -> Unit // 其余事件由导航层处理
        }
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            runCatching {
                val settings = ServiceLocator.settingsStore.snapshot()
                ServiceLocator.bankRepository.checkUpdates(settings)
            }.onSuccess { updates ->
                updateHint.value = if (updates.isEmpty()) "已是最新" else "${updates.size} 个有新版本"
            }.onFailure {
                updateHint.value = "检查失败"
            }
        }
    }
}
