package com.drillbit.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 混合抽题配置 ViewModel：勾选题库、加减号题数（默认 10 / 下限 10 / 步进 10）、
 * Start 时按题目权重抽 N 题建混合卷会话（塞 SessionHolder 后由导航进入刷题页）。
 * 2026-10-09 分类批次：构造参数 category 过滤勾选列表（从哪个页签发起就只列该分类的库）。
 */
class MixConfigViewModel(private val category: String) : ViewModel() {

    private val repo: QuizRepository = ServiceLocator.quizRepository

    /** 已勾选题库 id */
    private val selected = MutableStateFlow<Set<String>>(emptySet())

    /** 当前题数（默认 10，下限 10，步进 10） */
    private val count = MutableStateFlow(DEFAULT_COUNT)

    val state: StateFlow<MixConfigUiState> = combine(
        ServiceLocator.bankRepository.observeBanks(),
        selected,
        count,
    ) { banks, selectedSet, countNow ->
        MixConfigUiState(
            banks = banks
                .filter { it.category == category }
                .map { b ->
                    BankOption(
                        bankId = b.id,
                        name = b.name,
                        questionCount = b.questionCount,
                        selected = b.id in selectedSet,
                    )
                },
            count = countNow,
            minCount = MIN_COUNT,
            step = STEP,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MixConfigUiState(
            banks = emptyList(),
            count = DEFAULT_COUNT,
            minCount = MIN_COUNT,
            step = STEP,
        ),
    )

    fun onEvent(event: MixConfigEvent) {
        when (event) {
            is MixConfigEvent.ToggleBank -> toggle(event.bankId)
            MixConfigEvent.Minus -> count.value = (count.value - STEP).coerceAtLeast(MIN_COUNT)
            MixConfigEvent.Plus -> count.value += STEP
            else -> Unit // Start/Back 由导航层处理
        }
    }

    private fun toggle(bankId: String) {
        selected.value = selected.value.toMutableSet().apply {
            if (!add(bankId)) remove(bankId)
        }
    }

    /** Start 点击时由导航层调用：建混合卷塞 SessionHolder；未勾选返回 false（按钮本应置灰） */
    suspend fun buildSession(): Boolean {
        val ids = selected.value.toList()
        if (ids.isEmpty()) return false
        val session = repo.buildMixSession(ids, count.value)
        if (session == null) return false
        com.drillbit.model.SessionHolder.pending = session
        return true
    }

    class Factory(private val category: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MixConfigViewModel(category) as T
    }

    companion object {
        const val DEFAULT_COUNT = 10
        const val MIN_COUNT = 10
        const val STEP = 10
    }
}
