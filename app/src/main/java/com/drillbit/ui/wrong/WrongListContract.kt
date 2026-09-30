package com.drillbit.ui.wrong

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.5 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-09-29 拍板方案 A 后补充 WrongDetailUi / detailDialog / DetailDismiss（文档已同步）。
 * 2026-09-30 多选/判断题型支持：correctIndex 改 correctIndices（文档已同步）。
 */

data class WrongItem(
    val questionId: String,
    val stemPreview: String,         // 题干摘要单行
    val bankName: String,
    val dateText: String,
    val countText: String            // 如「重考计数 3/3」
)

data class WrongDetailUi(
    val stem: String,                // 完整题干
    val options: List<String>,       // 全部选项
    val correctIndices: List<Int>,  // 正确项下标（弹层内全部正确项标 ok 色，multi 多元素）
    val explanation: String,
    val countText: String            // 当前重考计数，弹层头部展示
)

data class WrongListUiState(
    val items: List<WrongItem>,
    val summaryText: String,         // 如「共 14 题待清 · 每答对一次，计数减一，减到 0 移出错题集」
    val detailDialog: WrongDetailUi? // 非空时弹层展示该错题完整内容（只读态）
)

sealed interface WrongListEvent {
    data class ItemClick(val questionId: String) : WrongListEvent   // 查库填充 detailDialog
    data object DetailDismiss : WrongListEvent                      // 关闭详情弹层
    data object RetryAll : WrongListEvent
}
