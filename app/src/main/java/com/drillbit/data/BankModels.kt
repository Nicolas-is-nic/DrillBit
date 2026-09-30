package com.drillbit.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 题库 JSON 数据模型与解析（服务器响应与本地测试题库共用同一结构，spec 4.2.3）。
 * 解析容错：未知字段忽略（向后兼容）；结构性缺失（无答案、下标越界、weight 越界）抛异常由调用方处理。
 */
data class BankIndexItem(
    val id: String,
    val name: String,
    val version: Int,
    val updatedAt: String,
    val questionCount: Int,
)

data class QuestionPayload(
    val id: String,
    val type: String,
    val stem: String,
    val options: List<String>,
    val answers: List<Int>,
    val explanation: String,
    val weight: Int,
)

data class BankPayload(
    val id: String,
    val name: String,
    val version: Int,
    val updatedAt: String,
    val questions: List<QuestionPayload>,
)

/** 解析 GET /api/index 的响应（数组） */
fun parseBankIndex(jsonText: String): List<BankIndexItem> {
    val arr = JSONArray(jsonText)
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        BankIndexItem(
            id = o.getString("id"),
            name = o.getString("name"),
            version = o.getInt("version"),
            updatedAt = o.optString("updatedAt", ""),
            questionCount = o.optInt("questionCount", 0),
        )
    }
}

/** 解析单个题库全量 JSON */
fun parseBank(jsonText: String): BankPayload {
    val o = JSONObject(jsonText)
    val bankId = o.getString("id")
    val questionsArr = o.getJSONArray("questions")
    val questions = List(questionsArr.length()) { i ->
        val q = questionsArr.getJSONObject(i)
        val answers = q.getJSONArray("answers").let { a ->
            List(a.length()) { idx -> a.getInt(idx) }
        }
        QuestionPayload(
            id = q.getString("id"),
            type = q.optString("type", "single"),
            stem = q.getString("stem"),
            options = q.getJSONArray("options").let { opts ->
                List(opts.length()) { idx -> opts.getString(idx) }
            },
            answers = answers,
            explanation = q.optString("explanation", ""),
            // 权重越界收敛到 1-5（已拍板：静态标注，默认 1）
            weight = q.optInt("weight", 1).coerceIn(1, 5),
        )
    }
    // 结构性校验：答案下标必须落在选项范围内
    questions.forEach { q ->
        require(q.answers.isNotEmpty() && q.answers.all { it in q.options.indices }) {
            "题库 $bankId 中题目 ${q.id} 的答案下标非法"
        }
        require(q.options.isNotEmpty()) { "题库 $bankId 中题目 ${q.id} 无选项" }
    }
    return BankPayload(
        id = bankId,
        name = o.getString("name"),
        version = o.getInt("version"),
        updatedAt = o.optString("updatedAt", ""),
        questions = questions,
    )
}

/** 序列化选项/答案为 JSON 数组文本（入库用） */
fun List<String>.toJsonText(): String = JSONArray(this).toString()

fun List<Int>.toIntJsonText(): String = JSONArray(this).toString()

/** 反序列化选项 */
fun parseOptions(jsonText: String): List<String> {
    val arr = JSONArray(jsonText)
    return List(arr.length()) { arr.getString(it) }
}

/** 反序列化答案下标 */
fun parseAnswers(jsonText: String): List<Int> {
    val arr = JSONArray(jsonText)
    return List(arr.length()) { arr.getInt(it) }
}
