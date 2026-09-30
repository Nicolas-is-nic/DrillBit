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
}
