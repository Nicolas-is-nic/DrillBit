package com.drillbit.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
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
 * Continue/Restart 的导航跳转由 MainActivity 处理，本 VM 只承担数据侧动作。
 */
class BankDetailViewModel(private val bankId: String) : ViewModel() {

    private val repo: BankRepository = ServiceLocator.bankRepository

    private val deleteConfirmVisible = MutableStateFlow(false)

    val state: StateFlow<BankDetailUiState> = combine(
        repo.observeBanks(),
        repo.observeProgress(),
        deleteConfirmVisible,
    ) { banks, progress, confirmVisible ->
        val bank = banks.find { it.id == bankId }
        val done = progress.find { it.bankId == bankId }?.doneCount ?: 0
        BankDetailUiState(
            bankId = bankId,
            name = bank?.name ?: "题库详情",
            total = bank?.questionCount ?: 0,
            done = done,
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
            lastSyncText = "--",
            serverVersionText = "--",
            hasUpdate = false,
            deleteConfirmVisible = false,
        ),
    )

    fun onEvent(event: BankDetailEvent) {
        when (event) {
            BankDetailEvent.RestartClick -> resetProgress()
            BankDetailEvent.DeleteClick -> deleteConfirmVisible.value = true
            BankDetailEvent.DeleteCancel -> deleteConfirmVisible.value = false
            BankDetailEvent.DeleteConfirm -> deleteBank()
            else -> Unit
        }
    }

    /** 从头重刷：断点清零（已刷进度重置，错题不受影响） */
    private fun resetProgress() {
        viewModelScope.launch { repo.resetProgress(bankId) }
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
