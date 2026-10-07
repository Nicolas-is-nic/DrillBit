package com.drillbit.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.data.db.QuestionEntity
import com.drillbit.data.parseOptions
import com.drillbit.data.repo.QuizRepository
import com.drillbit.model.QuizSession
import com.drillbit.model.SessionHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 刷题页 ViewModel（三模式共用，spec 4.2.1）：
 * - SINGLE：QuizRepository 构建带断点会话，每题落库推进断点
 * - MIX / RETRY：消费 SessionHolder 中的临时会话（配置页/错题集塞入），退出即弃
 * 错题计数规则（已拍板）：仅重考场景答对减 1、答错重置 3；单库/混合答错仅入集。
 */
class QuizViewModel(private val mode: QuizMode, private val bankId: String) : ViewModel() {

    private val repo: QuizRepository = ServiceLocator.quizRepository

    private var session: QuizSession? = null
    private var cursor: Int = 0
    private var correctCount: Int = 0

    /** 会话内收藏 id 集合（F2）：会话开始时加载一次，星标 toggle 时增量维护 */
    private var favoriteIds: Set<String> = emptySet()

    private val stateFlow = MutableStateFlow(
        QuizUiState(
            mode = mode,
            title = defaultTitle(mode),
            currentIndex = 0,
            totalCount = 0,
            correctCount = 0,
            progress = 0f,
            phase = QuizPhase.ANSWERING,
            question = QuestionUi(
                stem = "",
                options = emptyList(),
                type = QuestionType.SINGLE,
                isFavorite = false,
                sourceBankName = null,
            ),
            answered = null,
            selectedIndices = emptyList(),
            confirmDelete = false,
            finished = false,
        ),
    )
    val state: StateFlow<QuizUiState> = stateFlow.asStateFlow()

    init {
        viewModelScope.launch { loadSession() }
    }

    private suspend fun loadSession() {
        val s = when (mode) {
            QuizMode.SINGLE -> repo.startSingle(bankId)
            QuizMode.MIX, QuizMode.RETRY, QuizMode.FAVORITE -> SessionHolder.take()
        }
        session = s
        if (s == null || s.questions.isEmpty()) {
            // 无会话（如临时会话被进程回收）：直接完成态退出
            stateFlow.value = stateFlow.value.copy(finished = true, totalCount = 0)
            return
        }
        cursor = s.startIndex
        correctCount = 0
        favoriteIds = repo.favoriteIds()
        publishCurrent()
    }

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.OptionClick -> onOptionClick(event.index)
            QuizEvent.ConfirmClick -> confirmAnswer()
            QuizEvent.Next -> next()
            QuizEvent.FavoriteClick -> toggleFavorite()
            QuizEvent.DeleteClick ->
                stateFlow.value = stateFlow.value.copy(confirmDelete = true)
            QuizEvent.DeleteConfirm -> deleteCurrentQuestion()
            QuizEvent.DeleteCancel ->
                stateFlow.value = stateFlow.value.copy(confirmDelete = false)
            else -> Unit // AskAi / Back 由导航层处理
        }
    }

    /** 当前题的实体 id（AskAi 跳问答带上下文用） */
    fun currentQuestionId(): String = session?.questions?.getOrNull(cursor)?.entity?.id.orEmpty()

    /** 题库 type 字符串映射契约枚举（7.4），未识别值按 SINGLE */
    private fun mapType(type: String): QuestionType = when (type) {
        "multi" -> QuestionType.MULTI
        "judge" -> QuestionType.JUDGE
        else -> QuestionType.SINGLE
    }

    /** 选项点击：切换答前已选，不立即判定。multi 点已选取消/未选追加；single/judge 点未选替换、点已选取消（契约 7.4） */
    private fun onOptionClick(index: Int) {
        val s = session ?: return
        if (stateFlow.value.phase == QuizPhase.ANSWERED) return
        val sq = s.questions.getOrNull(cursor) ?: return
        val current = stateFlow.value.selectedIndices
        val next = when {
            index in current -> current - index
            mapType(sq.entity.type) == QuestionType.MULTI -> current + index
            else -> listOf(index)
        }
        stateFlow.value = stateFlow.value.copy(selectedIndices = next)
    }

    /** 确认作答（所有题型）：已选项进入判定（空选为按钮禁用的兜底，直接忽略） */
    private fun confirmAnswer() {
        if (stateFlow.value.phase == QuizPhase.ANSWERED) return
        val selected = stateFlow.value.selectedIndices
        if (selected.isEmpty()) return
        answer(selected)
    }

    private fun answer(selectedIndices: List<Int>) {
        val s = session ?: return
        if (stateFlow.value.phase == QuizPhase.ANSWERED) return
        val sq = s.questions.getOrNull(cursor) ?: return
        val q: QuestionEntity = sq.entity
        val correctIndices = repo.correctIndices(q.optionsJson, q.answersJson)
        val isCorrect = selectedIndices.toSet() == correctIndices.toSet()
        if (isCorrect) correctCount++

        viewModelScope.launch {
            var wrongBanner: String? = null
            var countBanner: String? = null
            if (mode == QuizMode.RETRY) {
                // 重考场景计数状态机（已拍板：仅此场景计数变化）
                val (before, after) = ServiceLocator.wrongRepository
                    .recordRetryResult(q.id, q.bankId, isCorrect)
                if (isCorrect) {
                    countBanner = "本题重考计数 $before · 本次答对后变为 $after"
                } else {
                    wrongBanner = "答错 · 重考计数已重置为 $after"
                    countBanner = "重考计数 $before → $after"
                }
            } else if (!isCorrect) {
                // 普通场景：答错仅入集/累计，不动计数（已拍板）
                val countText = repo.recordWrong(q.id, q.bankId)
                wrongBanner = "已加入错题集 · 重考计数 $countText"
            }
            if (mode == QuizMode.SINGLE) {
                repo.commitSingleProgress(s, q.orderIndex, answeredCountInSession())
            }
            stateFlow.value = stateFlow.value.copy(
                phase = QuizPhase.ANSWERED,
                answered = AnsweredUi(
                    selectedIndices = selectedIndices,
                    correctIndices = correctIndices,
                    isCorrect = isCorrect,
                    explanation = q.explanation,
                    wrongBannerText = wrongBanner,
                    countBannerText = countBanner,
                ),
                selectedIndices = emptyList(),
            )
        }
    }

    /** toggle 收藏当前题（F2）：更新内存集合与题目星标态 */
    private fun toggleFavorite() {
        val s = session ?: return
        val sq = s.questions.getOrNull(cursor) ?: return
        val q = sq.entity
        viewModelScope.launch {
            val favored = repo.toggleFavorite(q.id, q.bankId, sq.bankName)
            favoriteIds = if (favored) favoriteIds + q.id else favoriteIds - q.id
            stateFlow.value = stateFlow.value.copy(
                question = stateFlow.value.question.copy(isFavorite = q.id in favoriteIds),
            )
        }
    }

    /** 确认删除当前题（F1）：入黑名单+连带清错题/收藏，然后跳下一题（末题则完成页） */
    private fun deleteCurrentQuestion() {
        val s = session ?: return
        val sq = s.questions.getOrNull(cursor) ?: return
        val q = sq.entity
        viewModelScope.launch {
            repo.deleteQuestion(q.id, q.bankId)
            stateFlow.value = stateFlow.value.copy(confirmDelete = false)
            // 删除仅答后可达，phase 必为 ANSWERED，next() 直接复用
            next()
        }
    }

    private fun next() {
        val s = session ?: return
        if (stateFlow.value.phase != QuizPhase.ANSWERED) return
        if (cursor >= s.questions.lastIndex) {
            // 本轮完成：单库模式进度已在最后一题提交时落库
            stateFlow.value = stateFlow.value.copy(
                finished = true,
                correctCount = correctCount,
            )
            return
        }
        cursor++
        publishCurrent()
    }

    /** 根据游标刷新「作答前」整页 state */
    private fun publishCurrent() {
        val s = session ?: return
        val sq = s.questions.getOrNull(cursor) ?: return
        val q = sq.entity
        val currentIndex = if (mode == QuizMode.SINGLE) q.orderIndex + 1 else cursor + 1
        stateFlow.value = QuizUiState(
            mode = mode,
            title = s.title,
            currentIndex = currentIndex,
            totalCount = s.questions.size,
            correctCount = correctCount,
            progress = if (s.questions.isEmpty()) 0f
            else (currentIndex.toFloat() / s.questions.size).coerceIn(0f, 1f),
            phase = QuizPhase.ANSWERING,
            question = QuestionUi(
                stem = q.stem,
                options = parseOptions(q.optionsJson),
                type = mapType(q.type),
                isFavorite = q.id in favoriteIds,
                sourceBankName = if (mode == QuizMode.MIX) sq.bankName else null,
            ),
            answered = null,
            selectedIndices = emptyList(),
            confirmDelete = false,
            finished = false,
        )
    }

    /** 本会话内已作答题数（含当前题，用于单库 doneCount 累计） */
    private fun answeredCountInSession(): Int = cursor - (session?.startIndex ?: 0) + 1

    private fun defaultTitle(mode: QuizMode): String = when (mode) {
        QuizMode.SINGLE -> "刷题"
        QuizMode.MIX -> "混合卷"
        QuizMode.RETRY -> "错题重考"
        QuizMode.FAVORITE -> "我的收藏"
    }

    class Factory(
        private val mode: QuizMode,
        private val bankId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(mode, bankId) as T
    }
}
