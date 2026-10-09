package com.drillbit.data.repo

import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.QuestionEntity
import com.drillbit.data.db.WrongEntity
import com.drillbit.model.QuizSession
import com.drillbit.model.SessionQuestion
import com.drillbit.ui.quiz.QuizMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 错题行（错题计数 + 关联题目 + 所属题库名）。
 */
data class WrongListItem(
    val wrong: WrongEntity,
    val question: QuestionEntity?,
    val bankName: String,
)

/**
 * 错题仓库：列表、详情、重考会话、计数状态机（已拍板：计数变化仅重考场景）。
 */
class WrongRepository(private val db: DrillBitDatabase) {

    /** 错题列表 Flow（按最近答错时间倒序，逐条关联题目实体与题库名；已删题不出现） */
    fun observeList(): Flow<List<WrongListItem>> = db.wrongDao().observeAll().map { wrongs ->
        val bankNames = db.bankDao().getAllOnce().associate { it.id to it.name }
        val deleted = db.deletedQuestionDao().getAllOnce().map { it.questionId }.toSet()
        wrongs
            .filterNot { it.questionId in deleted }
            .sortedByDescending { it.lastWrongAt }
            .mapNotNull { w ->
                // 孤儿错题（父题未同步，外键已去）不进列表，题库同步后自动恢复（review F-2 配套）
                val q = db.questionDao().getById(w.questionId) ?: return@mapNotNull null
                WrongListItem(w, q, bankNames[q.bankId] ?: "")
            }
    }

    /** 错题详情弹层数据（方案 A：完整题面 + 解析 + 当前计数） */
    data class WrongDetail(
        val stem: String,
        val options: List<String>,
        val correctIndices: List<Int>,
        val explanation: String,
        val retryCount: Int,
    )

    suspend fun getDetail(questionId: String): WrongDetail? = withContext(Dispatchers.IO) {
        val wrong = db.wrongDao().getAllOnce().find { it.questionId == questionId } ?: return@withContext null
        val q = db.questionDao().getById(questionId) ?: return@withContext null
        val options = com.drillbit.data.parseOptions(q.optionsJson)
        WrongDetail(
            stem = q.stem,
            options = options,
            correctIndices = com.drillbit.data.parseAnswers(q.answersJson)
                .filter { it in options.indices }
                .distinct(),
            explanation = q.explanation,
            retryCount = wrong.retryCount,
        )
    }

    /** 构建重考会话：全部错题按入集先后正序；已删题过滤 */
    suspend fun startRetrySession(): QuizSession? = withContext(Dispatchers.IO) {
        val deleted = db.deletedQuestionDao().getAllOnce().map { it.questionId }.toSet()
        val wrongs = db.wrongDao().getAllOnce().sortedBy { it.addedAt }.filterNot { it.questionId in deleted }
        if (wrongs.isEmpty()) return@withContext null
        val bankNames = db.bankDao().getAllOnce().associate { it.id to it.name }
        val items = wrongs.mapNotNull { w ->
            db.questionDao().getById(w.questionId)?.let { SessionQuestion(it, bankNames[it.bankId] ?: "") }
        }
        if (items.isEmpty()) return@withContext null
        QuizSession(
            mode = QuizMode.RETRY,
            title = "错题重考",
            questions = items,
            startIndex = 0,
            bankId = null,
            startDoneCount = 0,
        )
    }

    /**
     * 重考场景计数状态机（已拍板仅此场景变化）：
     * 答对 retryCount-1，减到 0 删除（移出错题集）；答错重置 3。
     * 返回（变化前计数文案, 变化后文案，如 "2/3" 到 "1/3"；移出时 after 为 "已移出"）。
     */
    suspend fun recordRetryResult(
        questionId: String,
        bankId: String,
        isCorrect: Boolean,
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val existing = db.wrongDao().getAllOnce().find { it.questionId == questionId }
        val now = System.currentTimeMillis()
        if (existing == null) {
            // 不在错题集却出现在重考（数据被清过）：按新错题入集
            db.wrongDao().upsert(
                WrongEntity(questionId, bankId, 3, 1, now, now),
            )
            "0/3" to "3/3"
        } else if (isCorrect) {
            val before = "${existing.retryCount}/3"
            val after = existing.retryCount - 1
            if (after <= 0) {
                db.wrongDao().deleteByQuestionId(questionId)
                before to "已移出错题集"
            } else {
                db.wrongDao().upsert(existing.copy(retryCount = after))
                before to "$after/3"
            }
        } else {
            db.wrongDao().upsert(
                existing.copy(retryCount = 3, wrongCount = existing.wrongCount + 1, lastWrongAt = now),
            )
            "${existing.retryCount}/3" to "3/3"
        }
    }
}
