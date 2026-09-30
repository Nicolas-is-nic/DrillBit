package com.drillbit.model

import com.drillbit.data.db.QuestionEntity
import com.drillbit.ui.quiz.QuizMode

/**
 * 会话内一题的运行时形态（entity 之外补充展示所需信息）。
 */
data class SessionQuestion(
    val entity: QuestionEntity,
    val bankName: String,
)

/**
 * 做题会话（spec 4.2.1）：三种模式（单库/混合/重考）统一为内存队列 + 游标。
 * 单库模式游标每题落库（断点续刷）；混合与重考为临时会话，退出即弃。
 * 会话由所属 ViewModel 持有（主线程），跨页传递经 SessionHolder。
 */
class QuizSession(
    val mode: QuizMode,
    val title: String,
    val questions: List<SessionQuestion>,
    /** 起始游标（单库断点续刷时非 0） */
    val startIndex: Int,
    /** 单库模式的题库 id（断点写回用），混合/重考为 null */
    val bankId: String?,
    /** 起始时已刷题数（单库断点续刷累计用） */
    val startDoneCount: Int,
)

/**
 * 跨页会话持有器：混合抽题配置页建卷 → 刷题页消费；错题集发起重考 → 刷题页消费。
 * 内存态，进程被杀即失效（可接受：临时会话本就允许丢弃）。
 */
object SessionHolder {
    @Volatile
    var pending: QuizSession? = null

    /** 取走待开会话（导航后一次性消费） */
    fun take(): QuizSession? = pending.also { pending = null }
}
