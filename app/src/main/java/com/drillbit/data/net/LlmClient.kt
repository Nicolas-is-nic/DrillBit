package com.drillbit.data.net

import com.drillbit.data.DbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 大模型客户端（spec 4.2.1，SSE 流式）：
 * OpenAI 兼容协议解析 choices[].delta.content；Anthropic 协议解析 content_block_delta 事件。
 * 请求与响应体读取全程 Dispatchers.IO（红线）。
 */
class LlmClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // 流式响应整体读取放宽到 5 分钟（长回答）
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    /**
     * 流式对话：返回增量文本 Flow；Flow 正常结束=回复完成，抛异常=失败/中断。
     */
    fun chatStream(settings: DbSettings, system: String, user: String): Flow<String> =
        channelFlow {
            withContext(Dispatchers.IO) {
                val isAnthropic = settings.llmType == "anthropic"
                val url = if (isAnthropic) {
                    trimUrl(settings.llmUrl) + "/v1/messages"
                } else {
                    trimUrl(settings.llmUrl) + "/chat/completions"
                }
                val body = if (isAnthropic) {
                    JSONObject()
                        .put("model", settings.llmModel)
                        .put("system", system)
                        .put("max_tokens", 2048)
                        .put("stream", true)
                        .put(
                            "messages",
                            JSONArray().put(JSONObject().put("role", "user").put("content", user)),
                        )
                } else {
                    JSONObject()
                        .put("model", settings.llmModel)
                        .put("stream", true)
                        .put(
                            "messages",
                            JSONArray()
                                .put(JSONObject().put("role", "system").put("content", system))
                                .put(JSONObject().put("role", "user").put("content", user)),
                        )
                }
                val builder = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                if (isAnthropic) {
                    builder.header("x-api-key", settings.llmKey)
                    builder.header("anthropic-version", "2023-06-01")
                } else {
                    builder.header("Authorization", "Bearer ${settings.llmKey}")
                }
                client.newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val errBody = resp.body?.string().orEmpty()
                        throw IOException("模型服务返回 ${resp.code}：${errorReason(errBody, resp.code)}")
                    }
                    // 真·流式：okio source 逐行读取 SSE，每读到一段增量立即下发
                    val source = resp.body?.source() ?: throw IOException("响应体为空")
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        val delta = parseSseLine(isAnthropic, line) ?: continue
                        if (delta.isNotEmpty()) send(delta)
                    }
                }
            }
            close()
        }.flowOn(Dispatchers.IO)

    /**
     * 连通性测试：发送一条极短消息，返回耗时毫秒；失败抛异常。
     */
    suspend fun testConnection(settings: DbSettings): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val isAnthropic = settings.llmType == "anthropic"
        val url = if (isAnthropic) {
            trimUrl(settings.llmUrl) + "/v1/messages"
        } else {
            trimUrl(settings.llmUrl) + "/chat/completions"
        }
        val body = if (isAnthropic) {
            JSONObject()
                .put("model", settings.llmModel)
                .put("max_tokens", 8)
                .put(
                    "messages",
                    JSONArray().put(JSONObject().put("role", "user").put("content", "回复：ok")),
                )
        } else {
            JSONObject()
                .put("model", settings.llmModel)
                .put("max_tokens", 8)
                .put(
                    "messages",
                    JSONArray().put(JSONObject().put("role", "user").put("content", "回复：ok")),
                )
        }
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (isAnthropic) {
            builder.header("x-api-key", settings.llmKey)
            builder.header("anthropic-version", "2023-06-01")
        } else {
            builder.header("Authorization", "Bearer ${settings.llmKey}")
        }
        client.newCall(builder.build()).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("模型服务返回 ${resp.code}：${errorReason(respBody, resp.code)}")
            }
        }
        System.currentTimeMillis() - start
    }

    /** url 去尾部斜杠（用户配置可能带或不带结尾 /） */
    private fun trimUrl(url: String): String = url.trim().trimEnd('/')

    /** 从错误响应体提取可读原因 */
    private fun errorReason(body: String, code: Int): String = runCatching {
        val o = JSONObject(body)
        o.optJSONObject("error")?.optString("message")?.takeIf { it.isNotEmpty() }
            ?: o.optString("message")
    }.getOrNull()?.ifEmpty { null } ?: when (code) {
        401 -> "密钥不正确或未授权"
        404 -> "接口不存在，请检查地址是否包含正确的版本路径"
        429 -> "请求过于频繁或额度不足"
        else -> "服务异常"
    }

    /**
     * 单行 SSE 解析：返回增量文本；无关行返回 null。
     * OpenAI: "data: {...}" 中 choices[0].delta.content；"data: [DONE]" 结束
     * Anthropic: "data: {...}" 中 type=content_block_delta 时 delta.text
     */
    private fun parseSseLine(isAnthropic: Boolean, line: String): String? {
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload == "[DONE]" || payload.isEmpty()) return null
        return runCatching {
            val o = JSONObject(payload)
            if (isAnthropic) {
                if (o.optString("type") == "content_block_delta") {
                    o.optJSONObject("delta")?.optString("text").orEmpty()
                } else {
                    null
                }
            } else {
                o.getJSONArray("choices")
                    .optJSONObject(0)
                    ?.optJSONObject("delta")
                    ?.optString("content")
                    ?.takeIf { it.isNotEmpty() && it != "null" }
            }
        }.getOrNull()
    }
}
