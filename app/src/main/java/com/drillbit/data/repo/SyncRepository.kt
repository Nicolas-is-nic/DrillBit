package com.drillbit.data.repo

import androidx.room.withTransaction
import com.drillbit.data.SettingsStore
import com.drillbit.data.db.DeletedQuestionEntity
import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.FavoriteEntity
import com.drillbit.data.db.NoteEntity
import com.drillbit.data.db.ProgressEntity
import com.drillbit.data.db.WrongEntity
import com.drillbit.data.net.ServerApi
import com.drillbit.data.net.SyncMeta
import com.drillbit.data.net.UnauthorizedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 云同步仓库（方案见 agent_docs/账号与云同步方案.md）：
 * 登录/登出、全量快照上传、下载导入（事务清表重插）。
 * 数据包五类字段与方案第三节逐字一致（iOS 对齐基准）。
 * 语义：后上传覆盖（D1/D2）；401 抛 UnauthorizedException 由上层清凭证（D5）。
 */
class SyncRepository(
    private val db: DrillBitDatabase,
    private val api: ServerApi,
    private val store: SettingsStore,
) {

    companion object {
        /** 设备名（D4：用户可辨识的自定义设备名） */
        const val DEVICE_NAME = "安卓"
    }

    /** 已登录且服务器已配置时返回 (serverUrl, token)，否则抛异常 */
    private suspend fun requireAuth(): Pair<String, String> {
        val s = store.snapshot()
        if (s.serverUrl.isBlank()) throw IllegalStateException("未配置服务器地址，请先到「服务器地址」填写")
        if (s.authToken.isBlank()) throw UnauthorizedException("未登录")
        return s.serverUrl to s.authToken
    }

    /** 账号登录：成功后持久化凭证 */
    suspend fun login(username: String, password: String) {
        val s = store.snapshot()
        if (s.serverUrl.isBlank()) throw IllegalStateException("未配置服务器地址，请先到「服务器地址」填写")
        val token = api.login(s.serverUrl, username, password)
        store.setAuth(token, username)
    }

    /** 登出：仅清本地凭证，云端数据保留 */
    suspend fun logout() {
        store.setAuth("", null)
    }

    /** 云端元信息；未登录抛异常，云端无快照返回 null */
    suspend fun meta(): SyncMeta? {
        val (url, token) = requireAuth()
        return api.fetchSyncMeta(url, token)
    }

    /** 上传全量快照，返回 uploaded_at；成功即记 lastSyncAt（本机与云端最近一次交互时刻，2026-10-08 备份/恢复拆分批次） */
    suspend fun upload(): Long {
        val (url, token) = requireAuth()
        val at = api.uploadSnapshot(url, token, DEVICE_NAME, exportSnapshot())
        store.setLastSyncAt(at)
        return at
    }

    /**
     * 下载并导入云端快照（全量覆盖本地五表，事务内清表重插）。
     * 返回导入结果文案；云端无快照时抛 IllegalStateException。
     */
    suspend fun downloadAndImport(): String = withContext(Dispatchers.IO) {
        val (url, token) = requireAuth()
        val snap = api.downloadSnapshot(url, token)
        var noteCount = 0
        var favCount = 0
        var delCount = 0
        var progCount = 0
        var wrongCount = 0
        db.withTransaction {
            db.noteDao().deleteAll()
            db.favoriteDao().deleteAll()
            db.deletedQuestionDao().deleteAll()
            db.progressDao().deleteAll()
            db.wrongDao().deleteAll()

            val payload = JSONObject(snap.payloadJson)

            payload.optJSONArray("notes")?.let { arr ->
                val notes = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    NoteEntity(
                        id = o.getLong("id"),
                        title = o.optString("title"),
                        content = o.optString("content"),
                        source = o.optString("source"),
                        sourceQuestionId = o.nullableString("sourceQuestionId"),
                        bankName = o.nullableString("bankName"),
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                    )
                }
                notes.forEach { db.noteDao().upsert(it) }
                noteCount = notes.size
            }

            payload.optJSONArray("favorites")?.let { arr ->
                val items = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FavoriteEntity(
                        questionId = o.getString("questionId"),
                        bankId = o.getString("bankId"),
                        bankName = o.optString("bankName"),
                        addedAt = o.optLong("addedAt"),
                    )
                }
                db.favoriteDao().insertAll(items)
                favCount = items.size
            }

            payload.optJSONArray("deletedQuestions")?.let { arr ->
                val items = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DeletedQuestionEntity(
                        questionId = o.getString("questionId"),
                        bankId = o.getString("bankId"),
                        deletedAt = o.optLong("deletedAt"),
                    )
                }
                db.deletedQuestionDao().insertAll(items)
                delCount = items.size
            }

            payload.optJSONArray("progress")?.let { arr ->
                (0 until arr.length()).forEach { i ->
                    val o = arr.getJSONObject(i)
                    db.progressDao().upsert(
                        ProgressEntity(
                            bankId = o.getString("bankId"),
                            nextIndex = o.getInt("nextIndex"),
                            doneCount = o.getInt("doneCount"),
                        ),
                    )
                }
                progCount = arr.length()
            }

            payload.optJSONArray("wrongs")?.let { arr ->
                (0 until arr.length()).forEach { i ->
                    val o = arr.getJSONObject(i)
                    db.wrongDao().upsert(
                        WrongEntity(
                            questionId = o.getString("questionId"),
                            bankId = o.getString("bankId"),
                            retryCount = o.getInt("retryCount"),
                            wrongCount = o.getInt("wrongCount"),
                            addedAt = o.optLong("addedAt"),
                            lastWrongAt = o.optLong("lastWrongAt"),
                        ),
                    )
                }
                wrongCount = arr.length()
            }
        }
        store.setLastSyncAt(snap.uploadedAt)
        "已导入 笔记$noteCount · 收藏$favCount · 删题$delCount · 进度$progCount · 错题$wrongCount"
    }

    /** 组全量快照 JSON（字段名与方案第三节一致；全程 IO 线程） */
    private suspend fun exportSnapshot(): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()

        val notes = JSONArray()
        db.noteDao().getAllOnce().forEach { n ->
            notes.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("source", n.source)
                    .put("sourceQuestionId", n.sourceQuestionId ?: JSONObject.NULL)
                    .put("bankName", n.bankName ?: JSONObject.NULL)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt),
            )
        }
        payload.put("notes", notes)

        val favorites = JSONArray()
        db.favoriteDao().getAllOnce().forEach { f ->
            favorites.put(
                JSONObject()
                    .put("questionId", f.questionId)
                    .put("bankId", f.bankId)
                    .put("bankName", f.bankName)
                    .put("addedAt", f.addedAt),
            )
        }
        payload.put("favorites", favorites)

        val deleted = JSONArray()
        db.deletedQuestionDao().getAllOnce().forEach { d ->
            deleted.put(
                JSONObject()
                    .put("questionId", d.questionId)
                    .put("bankId", d.bankId)
                    .put("deletedAt", d.deletedAt),
            )
        }
        payload.put("deletedQuestions", deleted)

        val progress = JSONArray()
        db.progressDao().getAllOnce().forEach { p ->
            progress.put(
                JSONObject()
                    .put("bankId", p.bankId)
                    .put("nextIndex", p.nextIndex)
                    .put("doneCount", p.doneCount),
            )
        }
        payload.put("progress", progress)

        val wrongs = JSONArray()
        db.wrongDao().getAllOnce().forEach { w ->
            wrongs.put(
                JSONObject()
                    .put("questionId", w.questionId)
                    .put("bankId", w.bankId)
                    .put("retryCount", w.retryCount)
                    .put("wrongCount", w.wrongCount)
                    .put("addedAt", w.addedAt)
                    .put("lastWrongAt", w.lastWrongAt),
            )
        }
        payload.put("wrongs", wrongs)

        payload.toString()
    }
}

/** 可空字符串字段读取：JSON null 或缺键返回 null */
private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key)
