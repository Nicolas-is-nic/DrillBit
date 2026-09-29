package com.drillbit.ui.notes

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.7 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class NoteCard(
    val noteId: String,
    val title: String,
    val sourceText: String,          // 如「来自题目 · 大模型基础 · 09-28」或「大模型梳理生成 · 含 6 个主题 · 09-25」
    val isDigest: Boolean            // true 时卡片左侧主色竖条
)

data class NoteListUiState(
    val items: List<NoteCard>,
    val summaryText: String,         // 如「共 23 条 · 上次备份 09-28 21:10」
    val summarizing: Boolean         // 梳理生成中（顶部按钮转「梳理中…」）
)

sealed interface NoteListEvent {
    data class NoteClick(val noteId: String) : NoteListEvent
    data object NewNote : NoteListEvent         // 列表右下角或顶部新建入口，路由 noteEdit/new
    data object Summarize : NoteListEvent
    data object Backup : NoteListEvent          // 顶部动作区进入备份页
}
