package com.drillbit.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.parseRecall
import com.drillbit.data.repo.BankRepository
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 题库详情 ViewModel：统计信息、删除二次确认、从头重刷（断点清零）。
 * 2026-10-09 分类批次：算法库计算 P0/P1/P2 层级题数（tiers），TierStartClick 经
 * startTier 建层级专项临时会话（塞 SessionHolder 后由导航层进入刷题页）。
 * Continue/Restart 的导航跳转由 MainActivity 处理，本 VM 只承担数据侧动作。
 */
class BankDetailViewModel(private val bankId: String) : ViewModel() {

    private val repo: BankRepository = ServiceLocator.bankRepository
    private val quizRepo = ServiceLocator.quizRepository

    private val deleteConfirmVisible = MutableStateFlow(false)

    /** 层级专项入口（仅算法库非空；一次性加载，页内不刷新） */
    private val tiers = MutableStateFlow<List<TierEntry>>(emptyList())

    init {
        viewModelScope.launch { loadTiers() }
    }

    private suspend fun loadTiers() {
        val bank = repo.getBank(bankId) ?: return
        if (bank.category != "algo") return
        val counts = LinkedHashMap<String, Int>()
        ServiceLocator.database.questionDao().getByBank(bankId).forEach { q ->
            val tier = parseRecall(q.recallJson)?.tags?.firstOrNull() ?: return@forEach
            counts[tier] = (counts[tier] ?: 0) + 1
        }
        val order = listOf("P0", "P1", "P2")
        tiers.value = order.filter { counts.containsKey(it) }.map { TierEntry(it, counts[it] ?: 0) }
    }

    val state: StateFlow<BankDetailUiState> = combine(
        repo.observeBanks(),
        repo.observeProgress(),
        deleteConfirmVisible,
        tiers,
    ) { banks, progress, confirmVisible, tierList ->
        val bank = banks.find { it.id == bankId }
        val done = progress.find { it.bankId == bankId }?.doneCount ?: 0
        BankDetailUiState(
            bankId = bankId,
            name = bank?.name ?: "题库详情",
            total = bank?.questionCount ?: 0,
            done = done,
            tiers = tierList,
            lastSyncText = bank?.let { TimeFmt.medium(it.lastSyncAt) } ?: "--",
            serverVersionText = "v${bank?.version ?: 0} · 已是最新",
            hasUpdate = false,
            deleteConfirmVisible = confirmVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BankDetailUiState(
            bankId = bankId,
            name = "题库详情",
            total = 0,
            done = 0,
            tiers = emptyList(),
            lastSyncText = "--",
            serverVersionText = "--",
            hasUpdate = false,
            deleteConfirmVisible = false,
        ),
    )

    fun onEvent(event: BankDetailEvent) {
        when (event) {
            BankDetailEvent.DeleteClick -> deleteConfirmVisible.value = true
            BankDetailEvent.DeleteCancel -> deleteConfirmVisible.value = false
            BankDetailEvent.DeleteConfirm -> deleteBank()
            else -> Unit
        }
    }

    /** 层级专项开刷：建临时会话塞 SessionHolder，成功返回 true（导航层跳 quiz?mode=tier） */
    suspend fun startTier(tier: String): Boolean {
        val session = quizRepo.startTier(bankId, tier) ?: return false
        com.drillbit.model.SessionHolder.pending = session
        return true
    }

    /** 从头重刷：断点清零（已刷进度重置，错题不受影响）；suspend 供导航层等落库后再跳转 */
    suspend fun restart() {
        repo.resetProgress(bankId)
    }

    private fun deleteBank() {
        viewModelScope.launch {
            repo.deleteBank(bankId)
            deleteConfirmVisible.value = false
        }
    }

    class Factory(private val bankId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BankDetailViewModel(bankId) as T
    }
}
