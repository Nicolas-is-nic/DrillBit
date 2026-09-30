package com.drillbit.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.BankIndexItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.drillbit.data.repo.BankRepository
import com.drillbit.util.TimeFmt

/**
 * 题库列表 ViewModel：同步（服务器/本地测试导入）、更新弹窗、列表数据。
 * 契约另一侧：产出 BankListUiState，消费 BankListEvent（跳转类由导航层处理）。
 */
class BankListViewModel : ViewModel() {

    private val repo: BankRepository = ServiceLocator.bankRepository

    /** 同步进行中 */
    private val syncing = MutableStateFlow(false)

    /** 有新版本的库：bankId -> 弹窗行文案（如「新增 12 题」） */
    private val pendingUpdates = MutableStateFlow<Map<String, String>>(emptyMap())

    /** 更新弹窗内容（非空即展示） */
    private val updateDialog = MutableStateFlow<UpdateDialogState?>(null)

    /** 服务器目录全量（弹窗列出「无变化」项用） */

    val state: StateFlow<BankListUiState> = combine(
        repo.observeBanks(),
        repo.observeProgress(),
        syncing,
        pendingUpdates,
        updateDialog,
    ) { banks, progress, syncingNow, pending, dialog ->
        val progressMap = progress.associateBy { it.bankId }
        val last = banks.maxOfOrNull { it.lastSyncAt } ?: 0L
        BankListUiState(
            banks = banks.map { b ->
                BankCard(
                    bankId = b.id,
                    name = b.name,
                    questionCount = b.questionCount,
                    doneCount = progressMap[b.id]?.doneCount ?: 0,
                    hasUpdate = pending.containsKey(b.id),
                    updatedAtText = TimeFmt.short(b.lastSyncAt),
                )
            },
            syncing = syncingNow,
            lastSyncText = if (last > 0) "上次同步 ${TimeFmt.medium(last)}" else "尚未同步",
            updateDialog = dialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BankListUiState(
            banks = emptyList(),
            syncing = false,
            lastSyncText = "尚未同步",
            updateDialog = null,
        ),
    )

    fun onEvent(event: BankListEvent) {
        when (event) {
            BankListEvent.SyncClick -> sync()
            BankListEvent.UpdateConfirm -> confirmUpdate()
            BankListEvent.UpdateCancel -> updateDialog.value = null
            else -> Unit
        }
    }

    /**
     * 同步：已配置服务器走「目录对比 → 弹窗确认 → 逐库下载」；
     * 未配置服务器走开发期路径（本地测试题库导入）。
     */
    private fun sync() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            val settings = ServiceLocator.settingsStore.snapshot()
            if (settings.serverUrl.isBlank()) {
                runCatching { repo.importTestBanks(ServiceLocator.appContext()) }
                    .onFailure { e ->
                        updateDialog.value = null
                        pendingUpdates.value = emptyMap()
                        syncing.value = false
                        // 数据异常无法继续，保留提示给下次同步
                        android.util.Log.w("BankList", "导入失败", e)
                    }
            } else {
                runCatching { prepareUpdateDialog(settings) }
                    .onFailure { /* 网络错误时弹窗不放，状态行由数据刷新体现 */ }
            }
            syncing.value = false
        }
    }

    /** 拉取目录并构造更新弹窗（无差异时不弹） */
    private suspend fun prepareUpdateDialog(settings: com.drillbit.data.DbSettings) {
        val remote = repo.fetchRemoteIndex(settings)
        val local = repo.getLocalBanks().associateBy { it.id }
        val deltaMap = mutableMapOf<String, String>()
        val items = remote.map { r ->
            val ours = local[r.id]
            val delta = when {
                ours == null -> "新题库 · ${r.questionCount} 题"
                r.version > ours.version -> {
                    val d = r.questionCount - ours.questionCount
                    if (d > 0) "新增 $d 题" else "有新版本"
                }
                else -> null
            }
            if (delta != null) deltaMap[r.id] = delta
            UpdateItem(name = r.name, deltaText = delta ?: "无变化")
        }
        pendingUpdates.value = deltaMap
        updateDialog.value = if (deltaMap.isEmpty()) {
            null
        } else {
            UpdateDialogState(
                totalDeltaText = "检查完成，共 ${deltaMap.size} 个题库存在新版本。",
                items = items,
            )
        }
    }

    /** 确认更新：逐库事务替换 */
    private fun confirmUpdate() {
        val targets = pendingUpdates.value.keys.toList()
        if (targets.isEmpty()) {
            updateDialog.value = null
            return
        }
        viewModelScope.launch {
            syncing.value = true
            val settings = ServiceLocator.settingsStore.snapshot()
            var failed: String? = null
            for (bankId in targets) {
                runCatching { repo.downloadAndReplace(settings, bankId) }
                    .onFailure { e ->
                        failed = BankRepository.readableError(e as? Exception ?: RuntimeException(e))
                    }
                if (failed != null) break
            }
            if (failed == null) {
                pendingUpdates.value = emptyMap()
                updateDialog.value = null
            }
            syncing.value = false
        }
    }
}
