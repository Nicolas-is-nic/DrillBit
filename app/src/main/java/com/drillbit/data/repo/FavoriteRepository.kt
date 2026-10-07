package com.drillbit.data.repo

import com.drillbit.data.db.DrillBitDatabase
import com.drillbit.data.db.FavoriteEntity
import com.drillbit.model.QuizSession
import com.drillbit.model.SessionQuestion
import com.drillbit.ui.quiz.QuizMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * 收藏仓库（F2，2026-10-06 拍板）：
 * 列表、只读详情、收藏刷题会话（按收藏时间正序，无计数无惩罚可重刷）。
 * favorites 表不挂外键：题库全量重建不影响收藏。
 */
class FavoriteRepository(private val db: DrillBitDatabase) {

    /** 收藏行（收藏 + 关联题目；题目可能已被题库更新移除，可空） */
    data class FavoriteListItem(
        val favorite: FavoriteEntity,
        val question: com.drillbit.data.db.QuestionEntity?,
    )

    /** 收藏列表 Flow（按收藏时间正序） */
    fun observeList(): Flow<List<FavoriteListItem>> =
        combine(db.favoriteDao().observeAll(), db.bankDao().observeAll()) { favorites, _ ->
            favorites.map { fav ->
                FavoriteListItem(fav, db.questionDao().getById(fav.questionId))
            }
        }

    /** 只读详情（题目不存在时返回 null，点击无反应） */
    suspend fun getDetail(questionId: String): WrongRepository.WrongDetail? =
        withContext(Dispatchers.IO) {
            val q = db.questionDao().getById(questionId) ?: return@withContext null
            val options = com.drillbit.data.parseOptions(q.optionsJson)
            WrongRepository.WrongDetail(
                stem = q.stem,
                options = options,
                correctIndices = com.drillbit.data.parseAnswers(q.answersJson)
                    .filter { it in options.indices }
                    .distinct(),
                explanation = q.explanation,
                retryCount = 0,
            )
        }

    /** 构建收藏刷题会话：按收藏时间正序（复用错题集临时会话机制） */
    suspend fun startFavoriteSession(): QuizSession? = withContext(Dispatchers.IO) {
        val favorites = db.favoriteDao().getAllOnce()
        if (favorites.isEmpty()) return@withContext null
        val items = favorites.mapNotNull { fav ->
            db.questionDao().getById(fav.questionId)?.let { SessionQuestion(it, fav.bankName) }
        }
        if (items.isEmpty()) return@withContext null
        QuizSession(
            mode = QuizMode.FAVORITE,
            title = "我的收藏",
            questions = items,
            startIndex = 0,
            bankId = null,
            startDoneCount = 0,
        )
    }
}
