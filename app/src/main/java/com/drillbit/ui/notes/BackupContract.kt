package com.drillbit.ui.notes

import com.drillbit.ui.components.BannerUi

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.10 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 说明：BannerType / BannerUi / Banner 已定义在 ui/components（Banner.kt），此处直接引用。
 */

data class BackupUiState(
    val lastBackupText: String,       // 「--」表示从未备份
    val backedCount: Int,
    val serverHost: String,
    val uploading: Boolean,
    val resultBanner: BannerUi?       // 上传结果（成功 ok / 失败 warn，含原因）
)

sealed interface BackupEvent {
    data object Upload : BackupEvent
    data object Back : BackupEvent
}
