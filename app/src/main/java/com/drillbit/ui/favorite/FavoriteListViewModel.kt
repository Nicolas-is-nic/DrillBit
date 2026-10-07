package com.drillbit.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.repo.FavoriteRepository
import com.drillbit.model.SessionHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 我的收藏 ViewModel（F2，契约 7.15）：列表、只读详情弹层、发起收藏刷题（建会话塞 SessionHolder）。
 * StartQuiz 的导航由 MainActivity 在 onReady 回调中处理。
 */
class FavoriteListViewModel : ViewModel() {

    private val repo: FavoriteRepository = ServiceLocator.favoriteRepository

    /** 详情弹层内容（非空即展示） */
    private val detailDialog = MutableStateFlow<FavoriteDetailUi?>(null)

    val state: StateFlow<FavoriteListUiState> = combine(
        repo.observeList(),
        detailDialog,
    ) { list, dialog ->
        FavoriteListUiState(
            items = list.map { item ->
                FavoriteItem(
                    questionId = item.favorite.questionId,
                    stemPreview = item.question?.stem?.let { stem ->
                        if (stem.length > 40) stem.take(40) + "…" else stem
                    } ?: "题目已删除",
                    bankName = item.favorite.bankName.ifBlank { "--" },
                    dateText = com.drillbit.util.TimeFmt.short(item.favorite.addedAt),
                )
            },
            summaryText = "共 ${list.size} 题已收藏 · 可反复刷",
            detailDialog = dialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoriteListUiState(
            items = emptyList(),
            summaryText = "共 0 题已收藏 · 可反复刷",
            detailDialog = null,
        ),
    )

    fun onEvent(event: FavoriteListEvent) {
        when (event) {
            is FavoriteListEvent.ItemClick -> openDetail(event.questionId)
            FavoriteListEvent.DetailDismiss -> detailDialog.value = null
            FavoriteListEvent.StartQuiz -> Unit // 导航层调 startQuiz 处理
            FavoriteListEvent.Back -> Unit // 导航层 popBackStack
        }
    }

    private fun openDetail(questionId: String) {
        viewModelScope.launch {
            detailDialog.value = repo.getDetail(questionId)?.let { d ->
                FavoriteDetailUi(
                    stem = d.stem,
                    options = d.options,
                    correctIndices = d.correctIndices,
                    explanation = d.explanation,
                )
            }
        }
    }

    /** 发起收藏刷题：建会话塞 SessionHolder；onReady(false) 表示无可刷题（不导航） */
    fun startQuiz(onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val session = repo.startFavoriteSession()
            if (session == null) {
                onReady(false)
            } else {
                SessionHolder.pending = session
                onReady(true)
            }
        }
    }
}
