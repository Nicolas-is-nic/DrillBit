package com.drillbit.ui.banks

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.1 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class BankCard(
    val bankId: String,
    val name: String,
    val questionCount: Int,
    val doneCount: Int,
    val hasUpdate: Boolean,
    val updatedAtText: String
)

data class UpdateItem(val name: String, val deltaText: String)

data class UpdateDialogState(
    val totalDeltaText: String,      // 如「共 2 个题库存在新版本，预计下载 17 道新题」
    val items: List<UpdateItem>      // 含「无变化」项
)

data class BankListUiState(
    val banks: List<BankCard>,
    val syncing: Boolean,
    val lastSyncText: String,        // 如「服务器已连接 · 上次同步 今天 08:20」
    val updateDialog: UpdateDialogState?   // 非空时展示 P3 更新弹窗
)

sealed interface BankListEvent {
    data object SyncClick : BankListEvent
    data class BankClick(val bankId: String) : BankListEvent
    data object MixClick : BankListEvent
    data object UpdateConfirm : BankListEvent
    data object UpdateCancel : BankListEvent
}
