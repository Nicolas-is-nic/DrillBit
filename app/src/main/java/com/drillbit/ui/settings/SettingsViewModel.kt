package com.drillbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.BuildConfig
import com.drillbit.ServiceLocator
import com.drillbit.data.net.UnauthorizedException
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel：服务器/模型/备份/版本/崩溃日志/账号与同步状态。
 * DarkModeChange 由 MainActivity 直接处理（DataStore 单一归属），不经本 VM。
 * 云同步语义（方案 D 节）：SyncNow 先上传后下载；云端有其他设备的新快照时先弹覆盖警告；
 * 401 统一清凭证并提示重登（D5）。
 */
class SettingsViewModel : ViewModel() {

    /** 检查更新提示（null 表示未检查或已最新） */
    private val updateHint = MutableStateFlow<String?>(null)

    /** 同步进行中 */
    private val syncBusy = MutableStateFlow(false)

    /** 同步/登录操作结果文案（null=无） */
    private val syncResult = MutableStateFlow<String?>(null)

    /** 覆盖警告弹窗（true=展示） */
    private val confirmOverwrite = MutableStateFlow(false)

    /** 警告弹窗暂存的云端元信息（弹窗确认后直接用） */
    private var pendingCloudNewer = false

    val state: StateFlow<SettingsUiState> = combine(
        ServiceLocator.settingsStore.settings,
        updateHint,
        syncBusy,
        syncResult,
        confirmOverwrite,
    ) { settings, hint, busy, result, confirm ->
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
            accountUser = settings.authUser,
            lastSyncText = if (settings.lastSyncAt > 0) {
                "${TimeFmt.medium(settings.lastSyncAt)} · 安卓"
            } else {
                "未同步"
            },
            syncBusy = busy,
            syncResultText = result,
            confirmOverwrite = confirm,
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
            accountUser = null,
            lastSyncText = "未同步",
            syncBusy = false,
            syncResultText = null,
            confirmOverwrite = false,
        ),
    )

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.CheckUpdate -> checkUpdate()
            is SettingsEvent.AccountLogin -> login(event.username, event.password)
            SettingsEvent.AccountLogout -> logout()
            SettingsEvent.SyncNow -> syncNow()
            SettingsEvent.SyncOverwriteConfirm -> {
                confirmOverwrite.value = false
                if (pendingCloudNewer) {
                    pendingCloudNewer = false
                    doUploadAndDownload()
                }
            }
            SettingsEvent.SyncOverwriteCancel -> {
                pendingCloudNewer = false
                confirmOverwrite.value = false
            }
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

    /** 登录：成功后自动拉取云端快照（B5） */
    private fun login(username: String, password: String) {
        if (syncBusy.value || username.isBlank() || password.isBlank()) return
        syncBusy.value = true
        viewModelScope.launch {
            runCatching {
                ServiceLocator.syncRepository.login(username.trim(), password)
                // 登录成功自动拉一次；云端无快照（新账号首台设备）不算失败
                runCatching { ServiceLocator.syncRepository.downloadAndImport() }
                    .onSuccess { syncResult.value = "登录成功，$it" }
                    .onFailure { syncResult.value = "登录成功（云端暂无快照）" }
            }.onSuccess {
                // 结果文案已在内部写入
            }.onFailure { e ->
                syncResult.value = when (e) {
                    is UnauthorizedException -> "登录已失效，请重新登录"
                    else -> "登录失败：${e.message ?: "网络异常"}"
                }
                // 登录接口本身 401 是密码错误，不清凭证；后续同步 401 才清
                if (e is UnauthorizedException) ServiceLocator.syncRepository.logout()
            }
            syncBusy.value = false
        }
    }

    private fun logout() {
        viewModelScope.launch {
            ServiceLocator.syncRepository.logout()
            syncResult.value = "已登出（云端数据保留）"
        }
    }

    /** 立即同步：云端有其他设备新快照先警告（B6），否则直接上传后下载 */
    private fun syncNow() {
        if (syncBusy.value) return
        viewModelScope.launch {
            runCatching { ServiceLocator.syncRepository.meta() }
                .onSuccess { meta ->
                    if (meta == null) {
                        // 云端无快照（首台设备）：直接上传，无需下载
                        doUploadOnly()
                    } else {
                        val settings = ServiceLocator.settingsStore.snapshot()
                        if (meta.uploadedAt > settings.lastSyncAt) {
                            // 云端有本机未拉取的新快照（来自其他设备），先确认再覆盖
                            pendingCloudNewer = true
                            confirmOverwrite.value = true
                        } else {
                            doUploadAndDownload()
                        }
                    }
                }
                .onFailure { e -> handleSyncError(e) }
        }
    }

    /** 仅上传（云端无快照时） */
    private fun doUploadOnly() {
        syncBusy.value = true
        viewModelScope.launch {
            runCatching { ServiceLocator.syncRepository.upload() }
                .onSuccess { syncResult.value = "已上传云端快照" }
                .onFailure { e -> handleSyncError(e) }
            syncBusy.value = false
        }
    }

    /** 先上传后下载（常规立即同步） */
    private fun doUploadAndDownload() {
        syncBusy.value = true
        viewModelScope.launch {
            runCatching {
                ServiceLocator.syncRepository.upload()
                ServiceLocator.syncRepository.downloadAndImport()
            }.onSuccess { text ->
                syncResult.value = "同步完成，$text"
            }.onFailure { e -> handleSyncError(e) }
            syncBusy.value = false
        }
    }

    /** 同步类错误统一处理：401 清凭证（D5），其余给可读原因 */
    private fun handleSyncError(e: Throwable) {
        if (e is UnauthorizedException) {
            viewModelScope.launch { ServiceLocator.syncRepository.logout() }
            syncResult.value = "登录已失效，请重新登录"
        } else {
            syncResult.value = "同步失败：${e.message ?: "网络异常"}"
        }
    }
}
