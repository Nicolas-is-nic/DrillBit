package com.drillbit.ui.settings

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.11 节（逐字复制，禁止改动字段名、类型与顺序）。
 * 2026-10-07 云同步批次：新增账号与同步分组（accountUser/lastSyncText/syncBusy/syncResultText/
 * confirmOverwrite 与 AccountLogin/AccountLogout/SyncNow/SyncOverwriteConfirm/SyncOverwriteCancel）。
 */

data class SettingsUiState(
    val darkMode: Boolean,
    val serverConfigured: Boolean,    // 展示「已配置」/「未配置」
    val updateAvailableText: String?, // 如「2 个有新版本」，无则「已是最新」
    val modelSummary: String,         // 如「gpt-4o-mini · OpenAI 兼容」，未配置则「未配置」
    val lastBackupText: String,
    val versionText: String,          // 如「v0.1.0（1）」
    val hasCrashLog: Boolean,         // 「有」/「无」
    val accountUser: String?,         // 已登录用户名，null=未登录（账号与同步分组）
    val lastSyncText: String,         // 如「10-07 20:11 · 安卓」，未同步则「未同步」
    val syncBusy: Boolean,            // 同步进行中（禁用按钮与登录表单）
    val syncResultText: String?,      // 同步/登录操作结果提示条文案，展示后不清除（下次操作覆盖）
    val confirmOverwrite: Boolean     // true 时弹「云端将覆盖本地较新数据」确认弹窗（SyncNow 检测到风险时置位）
)

sealed interface SettingsEvent {
    data class DarkModeChange(val on: Boolean) : SettingsEvent
    data object ServerClick : SettingsEvent
    data object CheckUpdate : SettingsEvent
    data object ModelClick : SettingsEvent
    data object BackupClick : SettingsEvent
    data object CrashLogClick : SettingsEvent
    data class AccountLogin(val username: String, val password: String) : SettingsEvent  // 登录，成功自动拉取云端快照
    data object AccountLogout : SettingsEvent                                              // 登出（仅清本地凭证，云端数据保留）
    data object SyncNow : SettingsEvent                                                    // 立即同步：先上传后下载；云端较旧时先弹覆盖警告
    data object SyncOverwriteConfirm : SettingsEvent                                      // 确认用云端覆盖本地较新数据
    data object SyncOverwriteCancel : SettingsEvent                                       // 取消覆盖弹窗
}
