package com.drillbit.ui.settings

import com.drillbit.ui.components.BannerUi

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.13 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 说明：BannerUi 已定义在 ui/components（Banner.kt），此处直接引用。
 */

data class ServerConfigUiState(
    val url: String,
    val token: String,
    val testing: Boolean,
    val testResult: BannerUi?         // 如「连通性正常 · 题库接口与笔记接口均可用」
)

sealed interface ServerConfigEvent {
    data class UrlChange(val text: String) : ServerConfigEvent
    data class TokenChange(val text: String) : ServerConfigEvent
    data object Test : ServerConfigEvent
    data object Save : ServerConfigEvent
    data object Back : ServerConfigEvent
}
