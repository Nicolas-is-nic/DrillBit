package com.drillbit.ui.quiz

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.4 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-09-30 多选/判断题型支持：新增 QuestionType；AnsweredUi 单选 Int 改 List；
 * QuizUiState 增 selectedIndices；QuizEvent 增 ConfirmClick（文档已同步）。
 * 2026-10-06 F1/F2：QuizMode 增 FAVORITE；QuestionUi 增 isFavorite；QuizUiState 增
 * confirmDelete；QuizEvent 增 FavoriteClick/DeleteClick/DeleteConfirm/DeleteCancel（文档已同步）。
 * 2026-10-08 recall 批次：QuestionType 增 RECALL；QuestionUi 增 tags/images/recall（RecallUi）；
 * QuizEvent 增 RememberedClick/ForgotClick（文档已同步）。
 */

enum class QuizMode { SINGLE, MIX, RETRY, FAVORITE }
enum class QuizPhase { ANSWERING, ANSWERED }
enum class QuestionType { SINGLE, MULTI, JUDGE, RECALL }

data class QuestionUi(
    val stem: String,
    val options: List<String>,
    val type: QuestionType,         // VM 由题库 type 字符串映射，未识别值按 SINGLE
    val isFavorite: Boolean,        // 当前题收藏态，题干旁星标渲染
    val sourceBankName: String? = null, // 仅 MIX 模式非空，展示「来自：xxx」标签
    val tags: List<String> = emptyList(),      // 仅 RECALL 题非空：弱提示标签（题眼/难度），chip 渲染
    val images: List<String> = emptyList(),    // 仅 RECALL 题可非空：题图本地文件绝对路径，按序竖排
    val recall: RecallUi? = null,   // 仅 RECALL 题非空：揭示层数据（ANSWERED 阶段渲染）
)

/** RECALL 题揭示层数据（2026-10-08 recall 批次新增） */
data class RecallUi(
    val strategy: String,           // 策略一句话，主色加粗
    val steps: List<String>,        // 关键步骤 1-4 条
    val timeCx: String,             // 时间复杂度短文本
    val spaceCx: String,            // 空间复杂度短文本
    val pseudocode: String?,        // 伪代码（可空）
    val code: String?,              // Python 核心代码（可空）
)

data class AnsweredUi(
    val selectedIndices: List<Int>, // 用户选择（single/judge 恒为单元素）
    val correctIndices: List<Int>,  // 正确答案（multi 多元素）
    val isCorrect: Boolean,         // 集合全等才 true（漏选/错选均 false）
    val explanation: String,
    val wrongBannerText: String?,   // 答错时非空，如「已加入错题集 · 重考计数 3/3」；RECALL 题在 ForgotClick 后填充
    val countBannerText: String?,   // 仅 RETRY 模式非空，如「本题重考计数 2/3 · 本次答对后变为 1/3」
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
    val selectedIndices: List<Int>,  // 答前已选项（保留点击顺序）；multi 可多项，single/judge 至多 1 项；ANSWERED 后清空
    val confirmDelete: Boolean,      // true 时弹「确认删除本题」弹窗（答后 DeleteClick 置位）
    val finished: Boolean            // true 时展示完成页：本次共 N 题、答对 M 题
)

sealed interface QuizEvent {
    data class OptionClick(val index: Int) : QuizEvent
        // 点选切换已选：multi 点已选取消/未选追加；single/judge 点未选替换、点已选取消（均不立即判定）
    data object ConfirmClick : QuizEvent
        // 确认作答进入 ANSWERED（所有题型）。无载荷（VM 已持 selectedIndices）；
        // selectedIndices 为空时按钮置 OFF 禁用，VM 兜底忽略该事件
    data object RememberedClick : QuizEvent
        // RECALL 自评「记住了」：视为答对（RETRY 模式走减计数路径），直接下一题不出横幅
    data object ForgotClick : QuizEvent
        // RECALL 自评「没记住」：视为答错入集（RETRY 模式重置 3），填充 wrongBannerText 后展示「下一题」
    data object Next : QuizEvent
    data object AskAi : QuizEvent
    data object FavoriteClick : QuizEvent   // 星标点击：toggle 收藏当前题（答前答后均可）
    data object DeleteClick : QuizEvent     // 答后「删除本题」按钮：置 confirmDelete=true 弹确认
    data object DeleteConfirm : QuizEvent   // 确认删除：入 DeletedQuestion 表+连带清错题/收藏，跳下一题（末题则完成页）
    data object DeleteCancel : QuizEvent    // 取消删除弹窗
    data object Back : QuizEvent
}
