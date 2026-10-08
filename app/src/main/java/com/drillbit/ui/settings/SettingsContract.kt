package com.drillbit.ui.settings

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.11 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-10-07 云同步批次：新增账号与同步分组（accountUser/lastSyncText/syncBusy/syncResultText/
 * confirmOverwrite 与 AccountLogin/AccountLogout/SyncNow/SyncOverwriteConfirm/SyncOverwriteCancel）。
 * 2026-10-08 账号页拆分批次：账号与同步分组移入独立二级页（契约 7.13），本页仅留入口行
 * accountText + AccountClick；同步语义拆为备份/恢复两动作（详见 7.13）。
 */

data class SettingsUiState(
    val darkMode: Boolean,
    val serverConfigured: Boolean,    // 展示「已配置」/「未配置」
    val updateAvailableText: String?, // 如「2 个有新版本」，无则「已是最新」
    val modelSummary: String,         // 如「gpt-4o-mini · OpenAI 兼容」，未配置则「未配置」
    val lastBackupText: String,
    val accountText: String,          // 账号与同步入口右侧文案：用户名或「未登录」
    val versionText: String,          // 如「v0.1.0（1）」
    val hasCrashLog: Boolean,         // 「有」/「无」
)

sealed interface SettingsEvent {
    data class DarkModeChange(val on: Boolean) : SettingsEvent
    data object ServerClick : SettingsEvent
    data object CheckUpdate : SettingsEvent
    data object ModelClick : SettingsEvent
    data object BackupClick : SettingsEvent
    data object AccountClick : SettingsEvent   // 进入账号与同步二级页（7.13）
    data object CrashLogClick : SettingsEvent
}
