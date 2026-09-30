package com.drillbit.data.repo

import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.ProgressEntity
import com.drillbit.data.db.WrongEntity
import com.drillbit.data.parseAnswers
import com.drillbit.model.QuizSession
import com.drillbit.model.SessionQuestion
import com.drillbit.ui.quiz.QuizMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 做题仓库：会话构建、作答落库、错题入集。
 * 计数规则（已拍板）：错题计数变化仅发生在重考场景；普通刷题答错仅入集/累计次数。
 */
class QuizRepository(private val db: DrillBitDatabase) {

    /** 构建单库顺序刷会话（带断点） */
    suspend fun startSingle(bankId: String): QuizSession? = withContext(Dispatchers.IO) {
        val bank = db.bankDao().getById(bankId) ?: return@withContext null
        val questions = db.questionDao().getByBank(bankId)
        if (questions.isEmpty()) return@withContext null
        val progress = db.progressDao().get(bankId)
        QuizSession(
            mode = QuizMode.SINGLE,
            title = bank.name,
            questions = questions.map { SessionQuestion(it, bank.name) },
            startIndex = progress?.nextIndex?.coerceIn(0, questions.lastIndex) ?: 0,
            bankId = bankId,
            startDoneCount = progress?.doneCount ?: 0,
        )
    }

    /**
     * 单库模式作答落库：推进断点（nextIndex/doneCount）。
     * 返回更新后的进度实体供 UI 显示。
     */
    suspend fun commitSingleProgress(
        session: QuizSession,
        currentOrderIndex: Int,
        answeredCount: Int,
    ): Unit = withContext(Dispatchers.IO) {
        val bankId = session.bankId ?: return@withContext
        db.progressDao().upsert(
            ProgressEntity(
                bankId = bankId,
                nextIndex = (currentOrderIndex + 1).coerceAtMost(session.questions.size),
                doneCount = session.startDoneCount + answeredCount,
            ),
        )
    }

    /** 构建混合抽题会话：勾选库全部题目按权重抽 N 题（spec 4.3.1 ES 算法） */
    suspend fun buildMixSession(bankIds: List<String>, count: Int): QuizSession? =
        withContext(Dispatchers.IO) {
            val banks = db.bankDao().getAllOnce().filter { it.id in bankIds }
            if (banks.isEmpty()) return@withContext null
            val nameById = banks.associate { it.id to it.name }
            val candidates = bankIds.flatMap { id ->
                db.questionDao().getByBank(id).map { SessionQuestion(it, nameById[id] ?: "") }
            }
            if (candidates.isEmpty()) return@withContext null
            QuizSession(
                mode = QuizMode.MIX,
                title = "混合卷",
                questions = com.drillbit.model.weightedSample(
                    candidates,
                    weightOf = { it.entity.weight },
                    count = count,
                ),
                startIndex = 0,
                bankId = null,
                startDoneCount = 0,
            )
        }

    /**
     * 答错入错题集：新错题 retryCount=3、wrongCount=1；
     * 已在错题集则不动计数（已拍板），仅 wrongCount+1 并刷新 lastWrongAt。
     * 返回当前计数文案（如「3/3」）。
     */
    suspend fun recordWrong(questionId: String, bankId: String): String =
        withContext(Dispatchers.IO) {
            val existing = db.wrongDao().getAllOnce().find { it.questionId == questionId }
            val now = System.currentTimeMillis()
            val updated = if (existing == null) {
                WrongEntity(
                    questionId = questionId,
                    bankId = bankId,
                    retryCount = 3,
                    wrongCount = 1,
                    addedAt = now,
                    lastWrongAt = now,
                )
            } else {
                existing.copy(wrongCount = existing.wrongCount + 1, lastWrongAt = now)
            }
            db.wrongDao().upsert(updated)
            "${updated.retryCount}/3"
        }

    /** 读题目的正确下标（单选取首个） */
    fun correctIndex(optionsJson: String, answersJson: String): Int {
        val optionsCount = com.drillbit.data.parseOptions(optionsJson).size
        return parseAnswers(answersJson).firstOrNull()?.coerceIn(0, optionsCount - 1) ?: 0
    }
}
