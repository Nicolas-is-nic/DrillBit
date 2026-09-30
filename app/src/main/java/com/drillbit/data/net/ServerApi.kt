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

/**
 * 个人服务器接口客户端（spec 4.2.2）：
 * GET /api/index、GET /api/banks/{id}、POST /api/notes（笔记上传在 NoteRepository 调用）。
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
}

/** 字符串转请求体（UTF-8 JSON，okhttp 扩展函数实现） */
private fun String.toRequestBody(): okhttp3.RequestBody =
    toRequestBody("application/json; charset=utf-8".toMediaType())
