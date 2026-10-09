package com.drillbit.data.repo

import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.DeletedQuestionEntity
import com.drillbit.data.db.FavoriteEntity
import com.drillbit.data.db.ProgressEntity
import com.drillbit.data.db.WrongEntity
import com.drillbit.data.parseAnswers
import com.drillbit.data.parseRecall
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

    /** 构建单库顺序刷会话（带断点）；已删题过滤后入会 */
    suspend fun startSingle(bankId: String): QuizSession? = withContext(Dispatchers.IO) {
        val bank = db.bankDao().getById(bankId) ?: return@withContext null
        val deleted = db.deletedQuestionDao().idsByBank(bankId).toSet()
        val questions = db.questionDao().getByBank(bankId).filterNot { it.id in deleted }
        if (questions.isEmpty()) return@withContext null
        var progress = db.progressDao().get(bankId)
        // 已刷完一轮（nextIndex 到达题数）：断点归零从头开新轮，否则重进只剩最后一题，答完即“结束”
        if (progress != null && progress.nextIndex >= questions.size) {
            db.progressDao().deleteByBank(bankId)
            progress = null
        }
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
     * currentPosition 为「会话内位置」（即过滤已删题后的下标，2026-10-09 review F-3 统一域：
     * startSingle 按过滤后列表下标消费断点，此前误传 DB 原始 orderIndex，删题后有洞即错位）。
     */
    suspend fun commitSingleProgress(
        session: QuizSession,
        currentPosition: Int,
        answeredCount: Int,
    ): Unit = withContext(Dispatchers.IO) {
        val bankId = session.bankId ?: return@withContext
        db.progressDao().upsert(
            ProgressEntity(
                bankId = bankId,
                nextIndex = (currentPosition + 1).coerceAtMost(session.questions.size),
                doneCount = session.startDoneCount + answeredCount,
            ),
        )
    }

    /**
     * 层级专项会话（2026-10-09 分类批次）：按 recall 题层级（P0/P1/P2）过滤，
     * 临时会话不动断点与 doneCount（断点仍归全库「继续刷题」管）。
     */
    suspend fun startTier(bankId: String, tier: String): QuizSession? = withContext(Dispatchers.IO) {
        val bank = db.bankDao().getById(bankId) ?: return@withContext null
        val deleted = db.deletedQuestionDao().idsByBank(bankId).toSet()
        val questions = db.questionDao().getByBank(bankId)
            .filterNot { it.id in deleted }
            .filter { parseRecall(it.recallJson)?.tags?.firstOrNull() == tier }
        if (questions.isEmpty()) return@withContext null
        QuizSession(
            mode = QuizMode.TIER,
            title = "${bank.name} · $tier",
            questions = questions.map { SessionQuestion(it, bank.name) },
            startIndex = 0,
            bankId = null,
            startDoneCount = 0,
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
            }.let { all ->
                // 已删题不参与抽题（F1：本地删除，远程不删）
                val deleted = db.deletedQuestionDao().getAllOnce().map { it.questionId }.toSet()
                all.filterNot { it.entity.id in deleted }
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

    /** 读题目的全部正确下标（multi 多元素，single/judge 单元素；过滤越界与重复） */
    fun correctIndices(optionsJson: String, answersJson: String): List<Int> {
        val optionsCount = com.drillbit.data.parseOptions(optionsJson).size
        return parseAnswers(answersJson)
            .filter { it in 0 until optionsCount }
            .distinct()
    }

    /**
     * 本地删题（F1）：入黑名单 + 连带清错题记录与收藏。
     * 题目行本身不删（同步 version+1 全量重建后黑名单过滤仍生效）。
     */
    suspend fun deleteQuestion(questionId: String, bankId: String): Unit = withContext(Dispatchers.IO) {
        db.deletedQuestionDao().insert(
            DeletedQuestionEntity(questionId, bankId, System.currentTimeMillis()),
        )
        db.wrongDao().deleteByQuestionId(questionId)
        db.favoriteDao().deleteByQuestionId(questionId)
    }

    /** 全部已收藏题 id（会话内星标态用，会话开始时加载一次） */
    suspend fun favoriteIds(): Set<String> = withContext(Dispatchers.IO) {
        db.favoriteDao().allIdsOnce().toSet()
    }

    /** toggle 收藏（F2）：返回操作后是否已收藏 */
    suspend fun toggleFavorite(questionId: String, bankId: String, bankName: String): Boolean =
        withContext(Dispatchers.IO) {
            val existing = db.favoriteDao().allIdsOnce().contains(questionId)
            if (existing) {
                db.favoriteDao().deleteByQuestionId(questionId)
                false
            } else {
                db.favoriteDao().insert(FavoriteEntity(questionId, bankId, bankName, System.currentTimeMillis()))
                true
            }
        }
}
