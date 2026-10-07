package com.drillbit.ui.favorite

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.15 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-10-06 F2 新增（与错题集契约同构，无计数）。
 */

data class FavoriteItem(
    val questionId: String,
    val stemPreview: String,       // 题干摘要单行
    val bankName: String,
    val dateText: String,          // 收藏时间 MM-dd
)

data class FavoriteDetailUi(       // 只读详情弹层（方案 A 同错题集，无计数）
    val stem: String,
    val options: List<String>,
    val correctIndices: List<Int>, // 弹层内全部正确项标 ok 色
    val explanation: String,
)

data class FavoriteListUiState(
    val items: List<FavoriteItem>,
    val summaryText: String,       // 如「共 8 题已收藏 · 可反复刷」
    val detailDialog: FavoriteDetailUi?,
)

sealed interface FavoriteListEvent {
    data class ItemClick(val questionId: String) : FavoriteListEvent   // 查库填充 detailDialog
    data object DetailDismiss : FavoriteListEvent                      // 关闭详情弹层
    data object StartQuiz : FavoriteListEvent                          // 生成收藏会话（SessionHolder，导航层处理）
    data object Back : FavoriteListEvent                               // 返回（二级页顶栏返回锥）
}
