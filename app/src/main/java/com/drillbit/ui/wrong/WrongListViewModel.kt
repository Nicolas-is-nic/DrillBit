package com.drillbit.ui.wrong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.WrongRepository
import com.drillbit.model.SessionHolder
import com.drillbit.util.TimeFmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 错题集 ViewModel：列表、详情弹层（方案 A）、发起重考（建会话塞 SessionHolder）。
 * RetryAll 的导航由 MainActivity 在 onReady 回调中处理。
 */
class WrongListViewModel : ViewModel() {

    private val repo: WrongRepository = ServiceLocator.wrongRepository

    /** 详情弹层内容（非空即展示） */
    private val detailDialog = MutableStateFlow<WrongDetailUi?>(null)

    val state: StateFlow<WrongListUiState> = combine(
        repo.observeList(),
        detailDialog,
    ) { list, dialog ->
        WrongListUiState(
            items = list.map { item ->
                WrongItem(
                    questionId = item.wrong.questionId,
                    stemPreview = item.question?.stem?.let { stem ->
                        if (stem.length > 40) stem.take(40) + "…" else stem
                    } ?: "题目已删除",
                    bankName = item.bankName.ifEmpty { "--" },
                    dateText = TimeFmt.short(item.wrong.lastWrongAt),
                    countText = "重考计数 ${item.wrong.retryCount}/3",
                )
            },
            summaryText = "共 ${list.size} 题待清 · 重考中每答对一次计数减一，减到 0 移出错题集",
            detailDialog = dialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WrongListUiState(
            items = emptyList(),
            summaryText = "共 0 题待清 · 重考中每答对一次计数减一，减到 0 移出错题集",
            detailDialog = null,
        ),
    )

    fun onEvent(event: WrongListEvent) {
        when (event) {
            is WrongListEvent.ItemClick -> openDetail(event.questionId)
            WrongListEvent.DetailDismiss -> detailDialog.value = null
            WrongListEvent.RetryAll -> Unit // 导航层调 startRetry 处理
        }
    }

    private fun openDetail(questionId: String) {
        viewModelScope.launch {
            detailDialog.value = repo.getDetail(questionId)?.let { d ->
                WrongDetailUi(
                    stem = d.stem,
                    options = d.options,
                    correctIndex = d.correctIndex,
                    explanation = d.explanation,
                    countText = "${d.retryCount}/3",
                )
            }
        }
    }

    /** 发起重考：建会话塞 SessionHolder；onReady(false) 表示无错题（不导航） */
    fun startRetry(onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val session = repo.startRetrySession()
            if (session == null) {
                onReady(false)
            } else {
                SessionHolder.pending = session
                onReady(true)
            }
        }
    }
}
