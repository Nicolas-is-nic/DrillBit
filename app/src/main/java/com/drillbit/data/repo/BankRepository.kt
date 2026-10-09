package com.drillbit.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.drillbit.data.BankIndexItem
import com.drillbit.data.BankPayload
import com.drillbit.data.DbSettings
import com.drillbit.data.db.BankEntity
import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.ProgressEntity
import com.drillbit.data.db.QuestionEntity
import com.drillbit.data.net.ServerApi
import com.drillbit.ServiceLocator
import com.drillbit.data.toJsonText
import com.drillbit.data.toIntJsonText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * 题库仓库：拉取/增量更新/删除/断点重置（spec 4.3.2）。
 * 更新语义：库级增量（只下载有版本变化的库），库内事务全量替换；失败不落半截。
 */
class BankRepository(
    private val db: DrillBitDatabase,
    private val api: ServerApi,
) {

    /** 本地题库列表 Flow */
    fun observeBanks(): Flow<List<BankEntity>> = db.bankDao().observeAll()

    /** 全部断点进度 Flow（题库列表拼接 doneCount 用） */
    fun observeProgress(): Flow<List<ProgressEntity>> = db.progressDao().observeAll()

    suspend fun getBank(bankId: String): BankEntity? = db.bankDao().getById(bankId)

    /** 本地库全量（列表页 diff 用） */
    suspend fun getLocalBanks(): List<BankEntity> = db.bankDao().getAllOnce()

    /** 服务器目录全量（列表页 diff 用） */
    suspend fun fetchRemoteIndex(settings: DbSettings): List<BankIndexItem> =
        api.fetchIndex(settings.serverUrl, settings.serverToken)

    /** 断点清零（从头重刷） */
    suspend fun resetProgress(bankId: String) {
        db.progressDao().deleteByBank(bankId)
    }

    /**
     * 检查服务器更新：返回「本地已有但服务器版本更高」的条目（弹窗数据源）。
     * 服务器新库（本地没有的）也返回，标记为待新增。
     */
    suspend fun checkUpdates(settings: DbSettings): List<BankIndexItem> =
        withContext(Dispatchers.IO) {
            val remote = api.fetchIndex(settings.serverUrl, settings.serverToken)
            val local = db.bankDao().getAllOnce().associateBy { it.id }
            remote.filter { item ->
                val ours = local[item.id]
                ours == null || ours.version < item.version
            }
        }

    /** 下载并替换指定题库（事务内：删旧题目 → 插新 → 更新元信息 → 断点重置为 0）。
     *  recall 题图随库一并下载到本地（离线可刷）；单张失败不阻塞题库更新，UI 显示未下载占位。
     */
    suspend fun downloadAndReplace(settings: DbSettings, bankId: String): Unit =
        withContext(Dispatchers.IO) {
            val payload = api.fetchBank(settings.serverUrl, settings.serverToken, bankId)
            downloadImages(settings, payload)
            replaceInTransaction(payload)
            cleanupStaleImages(payload.id, payload.questions.flatMap { it.images })
        }

    /** 删除本地题库：banks 级联删 questions/wrong；progress 无外键手动删；题图目录连带删除 */
    suspend fun deleteBank(bankId: String): Unit = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.progressDao().deleteByBank(bankId)
            db.bankDao().deleteById(bankId)
        }
        imgDir(bankId).deleteRecursively()
    }

    /**
     * 从 assets 导入测试题库（开发期路径：未配置服务器时代替拉取）。
     * 版本相同跳过，结构非法抛异常由 ViewModel 转为可读提示。
     */
    suspend fun importTestBanks(context: Context): Int = withContext(Dispatchers.IO) {
        val text = context.assets.open("test_banks.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("banks")
        var imported = 0
        for (i in 0 until arr.length()) {
            val payloadText = arr.getJSONObject(i).toString()
            val payload = com.drillbit.data.parseBank(payloadText)
            val ours = db.bankDao().getById(payload.id)
            if (ours == null || ours.version < payload.version) {
                copyAssetImages(context, payload)
                replaceInTransaction(payload)
                cleanupStaleImages(payload.id, payload.questions.flatMap { it.images })
                imported++
            }
        }
        imported
    }

    /** 事务内全量替换一个题库（下载路径与导入路径共用） */
    private suspend fun replaceInTransaction(payload: BankPayload) {
        db.withTransaction {
            // 先写题库行：questions 有外键指向 banks，全新库先插题目会触发外键约束回滚
            db.bankDao().upsert(
                BankEntity(
                    id = payload.id,
                    name = payload.name,
                    version = payload.version,
                    updatedAt = payload.updatedAt,
                    questionCount = payload.questions.size,
                    lastSyncAt = System.currentTimeMillis(),
                    sortKey = payload.sortKey,
                    category = payload.category,
                ),
            )
            db.questionDao().deleteByBank(payload.id)
            db.questionDao().insertAll(
                payload.questions.mapIndexed { index, q ->
                    QuestionEntity(
                        id = "${payload.id}:${q.id}",
                        bankId = payload.id,
                        orderIndex = index,
                        type = q.type,
                        stem = q.stem,
                        optionsJson = q.options.toJsonText(),
                        answersJson = q.answers.toIntJsonText(),
                        explanation = q.explanation,
                        weight = q.weight,
                        recallJson = q.recallJson,
                    )
                },
            )
            // 已拍板：题库更新后断点直接重置，从头刷
            db.progressDao().deleteByBank(payload.id)
        }
    }

    // ===== 题图本地管理（recall 批次）：filesDir/img/{bankId}/，文件名取相对路径 basename =====

    /** 题库图片目录 */
    private fun imgDir(bankId: String): File =
        File(File(ServiceLocator.appContext().filesDir, "img"), bankId)

    /** 从服务器下载该库全部题图（逐张 best-effort：失败跳过不阻塞，重试依赖该库 version 再加一） */
    private suspend fun downloadImages(settings: DbSettings, payload: BankPayload) {
        val dir = imgDir(payload.id).apply { mkdirs() }
        payload.questions.flatMap { it.images }
            .map { it.substringAfterLast('/') }
            .distinct()
            .forEach { filename ->
                runCatching {
                    val bytes = api.fetchImage(settings.serverUrl, settings.serverToken, payload.id, filename)
                    val tmp = File(dir, "$filename.tmp")
                    tmp.writeBytes(bytes)
                    tmp.renameTo(File(dir, filename))
                }
            }
    }

    /** 从 assets 复制测试库题图（开发期路径，与下载路径产物一致） */
    private fun copyAssetImages(context: android.content.Context, payload: BankPayload) {
        val dir = imgDir(payload.id).apply { mkdirs() }
        payload.questions.flatMap { it.images }
            .map { it.substringAfterLast('/') }
            .distinct()
            .forEach { filename ->
                runCatching {
                    val tmp = File(dir, "$filename.tmp")
                    context.assets.open("test_banks/img/${payload.id}/$filename").use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                    }
                    tmp.renameTo(File(dir, filename))
                }
            }
    }

    /** 清理该库目录下已不被引用的旧图（题库全量替换后调用） */
    private fun cleanupStaleImages(bankId: String, keepRelatives: List<String>) {
        val dir = imgDir(bankId)
        if (!dir.exists()) return
        val keep = keepRelatives.map { it.substringAfterLast('/') }.toSet()
        dir.listFiles()?.forEach { f -> if (f.name !in keep) f.delete() }
    }

    companion object {
        /** 版本对比失败等 IO 异常转可读文案 */
        fun readableError(e: Exception): String = when (e) {
            is IOException -> "网络异常：${e.message ?: "连接失败"}"
            is IllegalArgumentException -> "题库数据异常：${e.message}"
            else -> "操作失败：${e.message ?: "未知错误"}"
        }
    }
}
