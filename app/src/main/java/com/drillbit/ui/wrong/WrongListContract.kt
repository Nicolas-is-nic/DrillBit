package com.drillbit.ui.wrong

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.5 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class WrongItem(
    val questionId: String,
    val stemPreview: String,         // 题干摘要单行
    val bankName: String,
    val dateText: String,
    val countText: String            // 如「重考计数 3/3」
)

data class WrongListUiState(
    val items: List<WrongItem>,
    val summaryText: String          // 如「共 14 题待清 · 每答对一次，计数减一，减到 0 移出错题集」
)

sealed interface WrongListEvent {
    data class ItemClick(val questionId: String) : WrongListEvent   // 展开该题完整内容（复用答题卡布局，只读态）
    data object RetryAll : WrongListEvent
}
