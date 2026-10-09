package com.drillbit.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
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
 * 2026-10-09 分类批次：页签（知识库/算法库）记忆在 DataStore，列表按分类过滤。
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

    /** 同步/更新结果提示（成功与失败都上浮，非空展示提示条） */
    private val banner = MutableStateFlow<BannerUi?>(null)

    /** 列表与进度合并（避免 combine 超五路） */
    private val banksWithProgress = combine(
        repo.observeBanks(),
        repo.observeProgress(),
    ) { banks, progress -> banks to progress.associateBy { it.bankId } }

    private val baseState = combine(
        banksWithProgress,
        syncing,
        pendingUpdates,
        updateDialog,
        ServiceLocator.settingsStore.settings,
    ) { (banks, progressMap), syncingNow, pending, dialog, settings ->
        BankListUiState(
            banks = banks
                .filter { it.category == settings.banksCategory }
                .map { b ->
                    BankCard(
                        bankId = b.id,
                        name = b.name,
                        questionCount = b.questionCount,
                        doneCount = progressMap[b.id]?.doneCount ?: 0,
                        hasUpdate = pending.containsKey(b.id),
                        updatedAtText = TimeFmt.short(b.lastSyncAt),
                    )
                },
            category = settings.banksCategory,
            syncing = syncingNow,
            lastSyncText = if (banks.isNotEmpty()) {
                val last = banks.maxOfOrNull { it.lastSyncAt } ?: 0L
                if (last > 0) "上次同步 ${TimeFmt.medium(last)}" else "尚未同步"
            } else {
                "尚未同步"
            },
            updateDialog = dialog,
            banner = null,
        )
    }

    val state: StateFlow<BankListUiState> = combine(baseState, banner) { s, b ->
        s.copy(banner = b)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BankListUiState(
            banks = emptyList(),
            category = "knowledge",
            syncing = false,
            lastSyncText = "尚未同步",
            updateDialog = null,
            banner = null,
        ),
    )

    fun onEvent(event: BankListEvent) {
        when (event) {
            BankListEvent.SyncClick -> sync()
            is BankListEvent.CategoryChange -> switchCategory(event.category)
            BankListEvent.UpdateConfirm -> confirmUpdate()
            BankListEvent.UpdateCancel -> updateDialog.value = null
            else -> Unit
        }
    }

    /** 页签切换：写 DataStore，列表经 settings flow 自动刷新 */
    private fun switchCategory(category: String) {
        if (category != "knowledge" && category != "algo") return
        viewModelScope.launch {
            runCatching { ServiceLocator.settingsStore.setBanksCategory(category) } // review F-26
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
            banner.value = null
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
                    .onFailure { e ->
                        // 网络失败要给用户可读反馈，不能静默吞掉
                        banner.value = BannerUi(
                            "同步失败：" + BankRepository.readableError(e as? Exception ?: RuntimeException(e)),
                            BannerType.WARN,
                        )
                    }
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
        if (syncing.value) return
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
                banner.value = BannerUi("更新完成", BannerType.OK)
            } else {
                // 失败也要关弹窗并上浮错误，否则用户看不到任何反应
                updateDialog.value = null
                banner.value = BannerUi("更新失败：$failed", BannerType.WARN)
            }
            syncing.value = false
        }
    }
}
