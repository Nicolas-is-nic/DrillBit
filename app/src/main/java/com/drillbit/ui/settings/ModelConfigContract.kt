package com.drillbit.ui.settings

import com.drillbit.ui.components.BannerUi

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.12 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 说明：BannerUi 已定义在 ui/components（Banner.kt），此处直接引用。
 */

enum class ApiType { OPENAI, ANTHROPIC }

data class ModelConfigUiState(
    val apiType: ApiType,
    val url: String,
    val apiKey: String,
    val modelName: String,
    val testing: Boolean,
    val testResult: BannerUi?         // 如「连接测试通过 · 延迟 620 毫秒」ok 态
)

sealed interface ModelConfigEvent {
    data class TypeChange(val type: ApiType) : ModelConfigEvent
    data class UrlChange(val text: String) : ModelConfigEvent
    data class KeyChange(val text: String) : ModelConfigEvent
    data class NameChange(val text: String) : ModelConfigEvent
    data object Test : ModelConfigEvent
    data object Save : ModelConfigEvent
    data object Back : ModelConfigEvent
}
