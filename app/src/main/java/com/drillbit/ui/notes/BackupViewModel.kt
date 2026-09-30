package com.drillbit.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.NoteRepository
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 笔记备份 ViewModel（spec 4.2.2 全量单次 POST，单向备份）。
 */
class BackupViewModel : ViewModel() {

    private val repo: NoteRepository = ServiceLocator.noteRepository

    private val stateFlow = MutableStateFlow(
        BackupUiState(
            lastBackupText = "--",
            backedCount = 0,
            serverHost = "未配置",
            uploading = false,
            resultBanner = null,
        ),
    )
    val state: StateFlow<BackupUiState> = stateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = ServiceLocator.settingsStore.snapshot()
            val count = repo.count()
            stateFlow.value = stateFlow.value.copy(
                lastBackupText = if (settings.lastBackupAt > 0) {
                    TimeFmt.medium(settings.lastBackupAt)
                } else {
                    "--"
                },
                backedCount = count,
                serverHost = hostOf(settings.serverUrl),
            )
        }
    }

    fun onEvent(event: BackupEvent) {
        when (event) {
            BackupEvent.Upload -> upload()
            else -> Unit // Back 由导航层处理
        }
    }

    private fun upload() {
        if (stateFlow.value.uploading) return
        viewModelScope.launch {
            stateFlow.value = stateFlow.value.copy(uploading = true, resultBanner = null)
            val settings = ServiceLocator.settingsStore.snapshot()
            if (settings.serverUrl.isBlank()) {
                stateFlow.value = stateFlow.value.copy(
                    uploading = false,
                    resultBanner = BannerUi("服务器未配置，请先到「设置 - 服务器配置」填写", BannerType.WARN),
                )
                return@launch
            }
            runCatching { repo.backup(settings) }
                .onSuccess { count ->
                    val now = System.currentTimeMillis()
                    ServiceLocator.settingsStore.setLastBackupAt(now)
                    stateFlow.value = stateFlow.value.copy(
                        uploading = false,
                        lastBackupText = TimeFmt.medium(now),
                        backedCount = count,
                        resultBanner = BannerUi("备份成功，共上传 $count 条笔记", BannerType.OK),
                    )
                }
                .onFailure { e ->
                    stateFlow.value = stateFlow.value.copy(
                        uploading = false,
                        resultBanner = BannerUi(NoteRepository.readableError(e as? Exception ?: RuntimeException(e)), BannerType.WARN),
                    )
                }
        }
    }

    /** 从 url 提取 host（如 https://note.example.com/api -> note.example.com） */
    private fun hostOf(url: String): String {
        if (url.isBlank()) return "未配置"
        return runCatching {
            val uri = java.net.URI(url.trim())
            uri.host ?: url
        }.getOrDefault(url)
    }
}
