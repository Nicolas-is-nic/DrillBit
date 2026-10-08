package com.drillbit.data.net

import com.drillbit.data.BankIndexItem
import com.drillbit.data.BankPayload
import com.drillbit.data.parseBank
import com.drillbit.data.parseBankIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 401 专用异常：调用方据此清除本地凭证并引导重登（方案 D5） */
class UnauthorizedException(message: String) : IOException(message)

/** 同步快照元信息（拉取前比对用） */
data class SyncMeta(
    val uploadedAt: Long,
    val device: String,
)

/** 云端快照（payloadJson 为五类数据的 JSON 文本，结构与方案文档第三节一致） */
data class SyncSnapshot(
    val uploadedAt: Long,
    val device: String,
    val payloadJson: String,
)

/**
 * 个人服务器接口客户端（spec 4.2.2）：
 * GET /api/index、GET /api/banks/{id}、POST /api/notes（笔记上传在 NoteRepository 调用）。
 * 2026-10-07 云同步批次：POST /api/auth/login、POST /api/sync/upload、GET /api/sync/download、GET /api/sync/meta。
 * 鉴权：Authorization: Bearer <token>。
 * 红线：请求与响应体读取全程 Dispatchers.IO；错误信息必须含可读原因。
 */
class ServerApi {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** 拉取题库目录 */
    suspend fun fetchIndex(serverUrl: String, token: String): List<BankIndexItem> =
        withContext(Dispatchers.IO) {
            val body = get("$serverUrl/api/index", token)
            parseBankIndex(body)
        }

    /** 拉取单个题库全量 JSON */
    suspend fun fetchBank(serverUrl: String, token: String, bankId: String): BankPayload =
        withContext(Dispatchers.IO) {
            val body = get("$serverUrl/api/banks/$bankId", token)
            parseBank(body)
        }

    /** POST JSON 并返回响应体文本 */
    suspend fun postJson(url: String, token: String, jsonBody: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody())
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException("服务器返回 ${resp.code}：${reasonFrom(text, resp.code)}")
                }
                text
            }
        }

    /** GET 并返回响应体文本（含错误体的可读原因提取） */
    private fun get(url: String, token: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("服务器返回 ${resp.code}：${reasonFrom(text, resp.code)}")
            }
            return text
        }
    }

    /** 从错误响应体提取可读原因（后端错误统一 {"detail": "..."}），提取失败给出状态码语义 */
    private fun reasonFrom(body: String, code: Int): String = runCatching {
        JSONObject(body).getString("detail")
    }.getOrElse {
        when (code) {
            401 -> "Token 不正确或未配置"
            404 -> "接口不存在，请检查服务器地址"
            else -> "服务器异常"
        }
    }

    // ===== 云同步（2026-10-07）：401 抛 UnauthorizedException，其余非 2xx 抛 IOException =====

    /** 账号登录，成功返回 token（登录端点无需鉴权头） */
    suspend fun login(serverUrl: String, username: String, password: String): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("username", username)
                .put("password", password)
                .toString()
            val request = Request.Builder()
                .url("$serverUrl/api/auth/login")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody())
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IOException("登录失败：${reasonFrom(text, resp.code)}")
                JSONObject(text).getString("token")
            }
        }

    /** 上传全量快照，返回服务器时间戳 uploaded_at */
    suspend fun uploadSnapshot(
        serverUrl: String,
        token: String,
        device: String,
        payloadJson: String,
    ): Long = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("device", device)
            .put("payload", JSONObject(payloadJson))
            .toString()
        val request = Request.Builder()
            .url("$serverUrl/api/sync/upload")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody())
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) throw UnauthorizedException(reasonFrom(text, 401))
            if (!resp.isSuccessful) throw IOException("上传失败：${reasonFrom(text, resp.code)}")
            JSONObject(text).getLong("uploaded_at")
        }
    }

    /** 下载云端快照（云端无快照时抛 IOException，detail 为「云端暂无快照」） */
    suspend fun downloadSnapshot(serverUrl: String, token: String): SyncSnapshot =
        withContext(Dispatchers.IO) {
            val text = authedGet("$serverUrl/api/sync/download", token)
            val o = JSONObject(text)
            SyncSnapshot(
                uploadedAt = o.getLong("uploaded_at"),
                device = o.optString("device"),
                payloadJson = o.getJSONObject("payload").toString(),
            )
        }

    /** 云端元信息；无快照返回 null（拉取前比对用） */
    suspend fun fetchSyncMeta(serverUrl: String, token: String): SyncMeta? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$serverUrl/api/sync/meta")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.code == 404) return@withContext null
                if (resp.code == 401) throw UnauthorizedException(reasonFrom(text, 401))
                if (!resp.isSuccessful) throw IOException("查询失败：${reasonFrom(text, resp.code)}")
                val o = JSONObject(text)
                SyncMeta(uploadedAt = o.getLong("uploaded_at"), device = o.optString("device"))
            }
        }

    /** 带鉴权 GET：401 抛 UnauthorizedException，其余非 2xx 抛 IOException */
    private fun authedGet(url: String, token: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) throw UnauthorizedException(reasonFrom(text, 401))
            if (!resp.isSuccessful) throw IOException(reasonFrom(text, resp.code))
            return text
        }
    }
}

/** 字符串转请求体（UTF-8 JSON，okhttp 扩展函数实现） */
private fun String.toRequestBody(): okhttp3.RequestBody =
    toRequestBody("application/json; charset=utf-8".toMediaType())
