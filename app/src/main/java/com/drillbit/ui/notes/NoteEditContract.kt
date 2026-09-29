package com.drillbit.ui.notes

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.8 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 注：sourceQuestionText 行按 Kotlin 语法补了行尾逗号（KSP 解析内联注释 + 后续字段时要求显式逗号）。
 */

data class NoteEditUiState(
    val noteId: String,
    val title: String,
    val content: String,
    val sourceQuestionText: String?,  // 来源题目展示行，如「大模型基础 · 第 12 题」；无则空
    val deleteConfirmVisible: Boolean
)

sealed interface NoteEditEvent {
    data class TitleChange(val text: String) : NoteEditEvent
    data class ContentChange(val text: String) : NoteEditEvent
    data object Save : NoteEditEvent
    data object SourceClick : NoteEditEvent    // 跳回来源题目
    data object DeleteClick : NoteEditEvent
    data object DeleteConfirm : NoteEditEvent
    data object DeleteCancel : NoteEditEvent
    data object Back : NoteEditEvent
}
