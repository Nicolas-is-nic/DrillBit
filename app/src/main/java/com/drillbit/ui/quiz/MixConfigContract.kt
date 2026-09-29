package com.drillbit.ui.quiz

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.3 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class BankOption(
    val bankId: String,
    val name: String,
    val questionCount: Int,
    val selected: Boolean
)

data class MixConfigUiState(
    val banks: List<BankOption>,
    val count: Int,                  // 当前题数，默认 10
    val minCount: Int,               // 固定 10
    val step: Int                    // 固定 10
)

sealed interface MixConfigEvent {
    data class ToggleBank(val bankId: String) : MixConfigEvent
    data object Minus : MixConfigEvent
    data object Plus : MixConfigEvent
    data object Start : MixConfigEvent             // 未勾选任何库时开始按钮置灰
    data object Back : MixConfigEvent
}
