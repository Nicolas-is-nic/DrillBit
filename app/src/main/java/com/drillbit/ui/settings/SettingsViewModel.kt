package com.drillbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.BuildConfig
import com.drillbit.ServiceLocator
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel：服务器/模型/备份/版本/崩溃日志状态。
 * DarkModeChange 由 MainActivity 直接处理（DataStore 单一归属），不经本 VM。
 */
class SettingsViewModel : ViewModel() {

    /** 检查更新提示（null 表示未检查或已最新） */
    private val updateHint = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
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
                "${settings.llmModel} · " + if (settings.llmType == "anthropic") "Anthropic" else "OpenAI 兼容"
            },
            lastBackupText = if (settings.lastBackupAt > 0) {
                TimeFmt.medium(settings.lastBackupAt)
            } else {
                "--"
            },
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

    /** 检查服务器题库更新（未配置服务器时给出提示） */
    private fun checkUpdate() {
        viewModelScope.launch {
            val settings = ServiceLocator.settingsStore.snapshot()
            if (settings.serverUrl.isBlank()) {
                updateHint.value = "服务器未配置"
                return@launch
            }
            runCatching {
                val remote = ServiceLocator.bankRepository.fetchRemoteIndex(settings)
                val local = ServiceLocator.bankRepository.getLocalBanks().associateBy { it.id }
                remote.count { r -> local[r.id] == null || local[r.id]!!.version < r.version }
            }.onSuccess { count ->
                updateHint.value = if (count > 0) "$count 个有新版本" else "已是最新"
            }.onFailure { e ->
                updateHint.value = "检查失败：${e.message ?: "网络异常"}"
            }
        }
    }
}
