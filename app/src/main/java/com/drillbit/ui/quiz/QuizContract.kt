package com.drillbit.ui.quiz

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.4 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

enum class QuizMode { SINGLE, MIX, RETRY }
enum class QuizPhase { ANSWERING, ANSWERED }

data class QuestionUi(
    val stem: String,
    val options: List<String>,
    val sourceBankName: String?      // 仅 MIX 模式非空，展示「来自：xxx」标签
)

data class AnsweredUi(
    val selectedIndex: Int,
    val correctIndex: Int,
    val isCorrect: Boolean,
    val explanation: String,
    val wrongBannerText: String?,     // 答错时非空，如「已加入错题集 · 重考计数 3/3」
    val countBannerText: String?,     // 仅 RETRY 模式非空，如「本题重考计数 2/3 · 本次答对后变为 1/3」
)

data class QuizUiState(
    val mode: QuizMode,
    val title: String,               // 题库名 / 「混合卷」 / 「错题重考」
    val currentIndex: Int,           // 从 1 起
    val totalCount: Int,             // 单库模式为题库总题数；混合/重考为本次总数
    val correctCount: Int,           // 本次答对题数（完成页展示）
    val progress: Float,             // 0..1
    val phase: QuizPhase,
    val question: QuestionUi,
    val answered: AnsweredUi?,       // phase=ANSWERED 时非空
    val finished: Boolean            // true 时展示完成页：本次共 N 题、答对 M 题
)

sealed interface QuizEvent {
    data class OptionClick(val index: Int) : QuizEvent
    data object Next : QuizEvent
    data object AskAi : QuizEvent
    data object Back : QuizEvent
}
