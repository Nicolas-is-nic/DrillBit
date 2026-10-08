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
    /** 仅 recall 题：揭示层数据整包 JSON（tags/images/strategy/steps/timeCx/spaceCx/pseudocode/code），其余题型 null */
    val recallJson: String? = null,
    /** 仅 recall 题：题图相对路径列表（下载用，与 recallJson 内 images 一致） */
    val images: List<String> = emptyList(),
)

/** recall 揭示层数据解析结果（VM 渲染与 AI 上下文共用） */
data class RecallData(
    val tags: List<String>,
    val images: List<String>,   // 服务器相对路径（形如 img/lc84-1.png）
    val strategy: String,
    val steps: List<String>,
    val timeCx: String,
    val spaceCx: String,
    val pseudocode: String?,
    val code: String?,
)

/** 解析 recallJson 为结构数据；结构非法返回 null（容错：老数据/异常数据不崩刷题页） */
fun parseRecall(jsonText: String?): RecallData? = runCatching {
    if (jsonText.isNullOrBlank()) return null
    val o = JSONObject(jsonText)
    RecallData(
        tags = o.optJSONArray("tags")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
        images = o.optJSONArray("images")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
        strategy = o.optString("strategy"),
        steps = o.optJSONArray("steps")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
        timeCx = o.optString("timeCx"),
        spaceCx = o.optString("spaceCx"),
        pseudocode = o.optString("pseudocode").ifBlank { null },
        code = o.optString("code").ifBlank { null },
    )
}.getOrNull()

/** 题图相对路径转本地文件：filesDir/img/{bankId}/{文件名}（取 basename，防路径穿越） */
fun recallImageFile(filesDir: java.io.File, bankId: String, relative: String): java.io.File =
    java.io.File(java.io.File(filesDir, "img/$bankId"), relative.substringAfterLast('/'))

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
        val type = q.optString("type", "single")
        if (type == "recall") {
            // recall 题禁止 options/answers；揭示数据整包存 recallJson，图片相对路径单独带出供下载
            val qid = q.getString("id")
            val approach = q.optJSONObject("approach")
                ?: throw IllegalArgumentException("题库 $bankId 中题目 $qid 缺 approach 字段")
            val strategy = approach.optString("strategy").trim()
            val steps = approach.optJSONArray("steps")
                ?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
            require(strategy.isNotEmpty() && steps.isNotEmpty()) {
                "题库 $bankId 中题目 $qid 的 approach.strategy/steps 不能为空"
            }
            val tags = q.optJSONArray("tags")
                ?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
            val images = q.optJSONArray("images")
                ?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
            val recallJson = JSONObject()
                .put("tags", JSONArray(tags))
                .put("images", JSONArray(images))
                .put("strategy", strategy)
                .put("steps", JSONArray(steps))
                .put("timeCx", approach.optString("timeCx"))
                .put("spaceCx", approach.optString("spaceCx"))
                .put("pseudocode", q.optString("pseudocode"))
                .put("code", q.optString("code"))
                .toString()
            QuestionPayload(
                id = qid,
                type = type,
                stem = q.getString("stem"),
                options = emptyList(),
                answers = emptyList(),
                explanation = "",
                weight = q.optInt("weight", 1).coerceIn(1, 5),
                recallJson = recallJson,
                images = images,
            )
        } else {
            QuestionPayload(
                id = q.getString("id"),
                type = type,
                stem = q.getString("stem"),
                options = q.getJSONArray("options").let { opts ->
                    List(opts.length()) { idx -> opts.getString(idx) }
                },
                answers = q.getJSONArray("answers").let { a ->
                    List(a.length()) { idx -> a.getInt(idx) }
                },
                explanation = q.optString("explanation", ""),
                // 权重越界收敛到 1-5（已拍板：静态标注，默认 1）
                weight = q.optInt("weight", 1).coerceIn(1, 5),
            )
        }
    }
    // 结构性校验：非 recall 题答案下标必须落在选项范围内（recall 题无选项无答案，不参与校验）
    questions.filter { it.type != "recall" }.forEach { q ->
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
