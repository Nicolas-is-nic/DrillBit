package com.drillbit.ui.notes

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.9 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class DigestSection(val title: String, val body: String)

data class DigestUiState(
    val metaText: String,             // 如「由 23 条笔记梳理生成 · 09-29 08:05 · 已存入笔记」
    val sections: List<DigestSection>,
    val streaming: Boolean,           // 生成中（正文区显示生成进度文案）
    val footerText: String            // 如「全文共 6 个主题 · 已去除重复表述」
)

sealed interface DigestEvent {
    data object Regenerate : DigestEvent
    data object Back : DigestEvent
}
