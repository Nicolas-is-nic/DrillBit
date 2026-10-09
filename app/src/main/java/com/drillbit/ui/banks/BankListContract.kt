package com.drillbit.ui.banks

import com.drillbit.ui.components.BannerUi

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.1 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-10-09 分类批次：UiState 增 category；事件增 CategoryChange；MixClick 改带 category 载荷。
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
    val banks: List<BankCard>,       // 已按当前页签分类过滤（2026-10-09 分类批次）
    val category: String,            // 当前页签："knowledge" | "algo"，记忆在 DataStore
    val syncing: Boolean,
    val lastSyncText: String,        // 如「服务器已连接 · 上次同步 今天 08:20」
    val updateDialog: UpdateDialogState?,  // 非空时展示 P3 更新弹窗
    val banner: BannerUi?            // 同步/更新失败的可读错误（非空时展示提示条）
)

sealed interface BankListEvent {
    data object SyncClick : BankListEvent
    data class CategoryChange(val category: String) : BankListEvent  // 切换知识库/算法库页签
    data class BankClick(val bankId: String) : BankListEvent
    data class MixClick(val category: String) : BankListEvent      // 混合抽题（携带当前页签，配置页按分类过滤）
    data object UpdateConfirm : BankListEvent
    data object UpdateCancel : BankListEvent
}
