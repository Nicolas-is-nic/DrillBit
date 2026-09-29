package com.drillbit.ui.banks

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.2 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class BankDetailUiState(
    val bankId: String,
    val name: String,
    val total: Int,
    val done: Int,
    val lastSyncText: String,
    val serverVersionText: String,   // 如「v3 · 有新版本 v4」或「v3 · 已是最新」
    val hasUpdate: Boolean,
    val deleteConfirmVisible: Boolean
)

sealed interface BankDetailEvent {
    data object ContinueClick : BankDetailEvent    // 继续刷题·第 N 题（done>=total 时文案为「从头刷题」）
    data object RestartClick : BankDetailEvent     // 从头重刷
    data object CheckUpdateClick : BankDetailEvent
    data object DeleteClick : BankDetailEvent
    data object DeleteConfirm : BankDetailEvent
    data object DeleteCancel : BankDetailEvent
    data object Back : BankDetailEvent
}
