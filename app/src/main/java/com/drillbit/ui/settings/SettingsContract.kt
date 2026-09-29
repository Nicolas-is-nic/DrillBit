package com.drillbit.ui.settings

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.11 节（逐字复制，禁止改动字段名、类型与顺序）。
 */

data class SettingsUiState(
    val darkMode: Boolean,
    val serverConfigured: Boolean,    // 展示「已配置」/「未配置」
    val updateAvailableText: String?, // 如「2 个有新版本」，无则「已是最新」
    val modelSummary: String,         // 如「gpt-4o-mini · OpenAI 兼容」，未配置则「未配置」
    val lastBackupText: String,
    val versionText: String,          // 如「v0.1.0（1）」
    val hasCrashLog: Boolean          // 「有」/「无」
)

sealed interface SettingsEvent {
    data class DarkModeChange(val on: Boolean) : SettingsEvent
    data object ServerClick : SettingsEvent
    data object CheckUpdate : SettingsEvent
    data object ModelClick : SettingsEvent
    data object BackupClick : SettingsEvent
    data object CrashLogClick : SettingsEvent
}
