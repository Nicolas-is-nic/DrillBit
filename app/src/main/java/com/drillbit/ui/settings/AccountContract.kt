package com.drillbit.ui.settings

/*
 * 契约来源：agent_docs/双模型分工开发方案.md 第 7.13 节（2026-10-08 账号与同步独立页面批次新增）。
 * 同步语义（B 方案拆分）：备份到云端 = 上传本机全量快照覆盖云端；用云端恢复 = 下载云端快照覆盖本机。
 * 云端只存一份最新快照（后上传覆盖，方案 D1/D2）；备份遇云端新快照、恢复与登出恒弹二次确认。
 */

/** 当前确认弹窗（null=无） */
enum class AccountDialog { BACKUP_OVERWRITE, RESTORE_OVERWRITE, LOGOUT }

data class AccountUiState(
    val loggedIn: Boolean,           // 已登录（authToken 非空）
    val username: String,            // 已登录用户名，未登录为空串
    val busy: Boolean,               // 登录/备份/恢复/登出进行中，禁用全部操作
    val resultText: String?,         // 最近一次操作结果文案（下次操作覆盖，不自动清除）
    val lastSyncText: String,        // 如「10-07 20:11 · 安卓」，从未与云端交互则「从未」
    val dialog: AccountDialog?,      // 非 null 时展示对应确认弹窗
)

sealed interface AccountEvent {
    data class LoginClick(val username: String, val password: String) : AccountEvent
        // 登录成功后自动拉取云端快照（只下载不上传；云端无快照不算失败）
    data object BackupClick : AccountEvent
        // 备份到云端；云端存在本机未拉取的新快照（其他设备上传）时先弹确认
    data object BackupConfirm : AccountEvent    // 确认以本机数据覆盖云端那份新快照
    data object RestoreClick : AccountEvent     // 用云端恢复：恒弹确认（本机未备份的改动会丢失）
    data object RestoreConfirm : AccountEvent
    data object LogoutClick : AccountEvent      // 登出（仅清本地凭证，云端数据保留）：恒弹确认
    data object LogoutConfirm : AccountEvent
    data object DialogCancel : AccountEvent     // 关闭当前弹窗（不做任何操作）
    data object Back : AccountEvent
}
