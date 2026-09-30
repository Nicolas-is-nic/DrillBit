package com.drillbit.data.repo

import com.drillbit.data.DbSettings
import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.NoteEntity
import com.drillbit.data.net.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * 笔记仓库：CRUD、全量梳理提示词、备份上传（spec 4.2.2 全量 POST）。
 */
class NoteRepository(
    private val db: DrillBitDatabase,
    private val api: ServerApi,
) {

    fun observeNotes(): Flow<List<NoteEntity>> = db.noteDao().observeAll()

    suspend fun getNote(noteId: Long): NoteEntity? = db.noteDao().getById(noteId)

    /** 保存（noteId<=0 插入，否则更新），返回 noteId */
    suspend fun saveNote(
        noteId: Long,
        title: String,
        content: String,
        source: String,
        sourceQuestionId: String?,
        bankName: String?,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = if (noteId > 0) db.noteDao().getById(noteId) else null
        val note = NoteEntity(
            id = if (noteId > 0) noteId else 0,
            title = title,
            content = content,
            source = source,
            sourceQuestionId = sourceQuestionId,
            bankName = bankName,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        db.noteDao().upsert(note)
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        db.noteDao().deleteById(noteId)
    }

    suspend fun count(): Int = db.noteDao().count()

    /**
     * 全量笔记梳理提示词（spec 4.3.4）：system 要求按主题分组归纳，
     * user 携带全部笔记标题与正文。
     */
    suspend fun buildSummarizePrompt(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val notes = db.noteDao().getAllOnce()
        if (notes.isEmpty()) return@withContext null
        val system = "你是一个知识点整理助手。用户会提供全部学习笔记，请输出一篇结构化的知识点归纳稿：" +
            "按主题分组（每组一个小标题，格式「一、主题名」），组内提炼要点、去除重复表述，" +
            "保留关键技术细节，全部使用中文，不要输出与归纳无关的客套话。"
        val sb = StringBuilder()
        notes.forEachIndexed { index, note ->
            sb.append("【笔记${index + 1}】").append(note.title).append('\n')
                .append(note.content).append("\n\n")
        }
        system to sb.toString()
    }

    /**
     * 全量备份（spec 4.2.2）：POST /api/notes，body 为全部笔记数组。
     * 返回成功上传条数；失败抛 IOException（含可读原因）。
     */
    suspend fun backup(settings: DbSettings): Int = withContext(Dispatchers.IO) {
        val notes = db.noteDao().getAllOnce()
        if (notes.isEmpty()) return@withContext 0
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("source", n.source)
                    .put("bankName", n.bankName ?: "")
                    .put("updatedAt", n.updatedAt),
            )
        }
        api.postJson("${settings.serverUrl.trim().trimEnd('/')}/api/notes", settings.serverToken, arr.toString())
        notes.size
    }

    companion object {
        fun readableError(e: Exception): String = when (e) {
            is IOException -> "备份失败：${e.message ?: "网络异常"}"
            else -> "备份失败：${e.message ?: "未知错误"}"
        }
    }
}
