package com.drillbit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore 实例（App 级单例） */
private val Context.dataStore by preferencesDataStore(name = "drillbit_settings")

/**
 * 用户配置快照。llmKey 仅本机存储不上传（红线）。
 */
data class DbSettings(
    val serverUrl: String = "",
    val serverToken: String = "",
    val llmUrl: String = "",
    val llmKey: String = "",
    val llmModel: String = "",
    /** openai / anthropic */
    val llmType: String = "openai",
    val darkMode: Boolean = false,
    val lastBackupAt: Long = 0L,
    /** 账号登录 token（空=未登录，云同步用） */
    val authToken: String = "",
    /** 已登录用户名（null=未登录） */
    val authUser: String? = null,
    /** 上次成功下载导入云端快照的时间（毫秒，0=从未同步） */
    val lastSyncAt: Long = 0L,
    /** 题库页当前分类页签："knowledge" | "algo"（2026-10-09 分类批次） */
    val banksCategory: String = "knowledge",
)

/**
 * 配置仓库（DataStore）。
 * 深色模式归属（已拍板）：本仓库是唯一存储，MainActivity 启动读一次并持有，向下单向传递。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val SERVER_TOKEN = stringPreferencesKey("server_token")
        val LLM_URL = stringPreferencesKey("llm_url")
        val LLM_KEY = stringPreferencesKey("llm_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val LLM_TYPE = stringPreferencesKey("llm_type")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_USER = stringPreferencesKey("auth_user")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val BANKS_CATEGORY = stringPreferencesKey("banks_category")
    }

    /** 配置 Flow（UI 订阅用） */
    val settings: Flow<DbSettings> = context.dataStore.data.map { p ->
        DbSettings(
            serverUrl = p[Keys.SERVER_URL] ?: "",
            serverToken = p[Keys.SERVER_TOKEN] ?: "",
            llmUrl = p[Keys.LLM_URL] ?: "",
            llmKey = p[Keys.LLM_KEY] ?: "",
            llmModel = p[Keys.LLM_MODEL] ?: "",
            llmType = p[Keys.LLM_TYPE] ?: "openai",
            darkMode = p[Keys.DARK_MODE] ?: false,
            lastBackupAt = p[Keys.LAST_BACKUP_AT] ?: 0L,
            authToken = p[Keys.AUTH_TOKEN] ?: "",
            authUser = p[Keys.AUTH_USER],
            lastSyncAt = p[Keys.LAST_SYNC_AT] ?: 0L,
            banksCategory = p[Keys.BANKS_CATEGORY] ?: "knowledge",
        )
    }

    /** 读一次当前快照（启动初始化用，文件访问走 IO 由调用方保证或依赖 DataStore 内部实现） */
    suspend fun snapshot(): DbSettings = settings.first()

    suspend fun setServer(url: String, token: String) {
        context.dataStore.edit { p ->
            p[Keys.SERVER_URL] = url.trim()
            p[Keys.SERVER_TOKEN] = token.trim()
        }
    }

    suspend fun setLlm(url: String, key: String, model: String, type: String) {
        context.dataStore.edit { p ->
            p[Keys.LLM_URL] = url.trim()
            p[Keys.LLM_KEY] = key.trim()
            p[Keys.LLM_MODEL] = model.trim()
            p[Keys.LLM_TYPE] = if (type == "anthropic") "anthropic" else "openai"
        }
    }

    suspend fun setDarkMode(on: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = on }
    }

    suspend fun setLastBackupAt(at: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = at }
    }

    /** 写入/清除账号凭证（logout 传空串与 null） */
    suspend fun setAuth(token: String, user: String?) {
        context.dataStore.edit { p ->
            if (token.isBlank()) p.remove(Keys.AUTH_TOKEN) else p[Keys.AUTH_TOKEN] = token
            if (user == null) p.remove(Keys.AUTH_USER) else p[Keys.AUTH_USER] = user
        }
    }

    suspend fun setLastSyncAt(at: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_AT] = at }
    }

    /** 题库页页签记忆（非法值回落 knowledge） */
    suspend fun setBanksCategory(category: String) {
        context.dataStore.edit {
            it[Keys.BANKS_CATEGORY] = if (category == "algo") "algo" else "knowledge"
        }
    }
}
