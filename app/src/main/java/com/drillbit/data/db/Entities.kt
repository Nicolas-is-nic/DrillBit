package com.drillbit.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地题库元信息。
 * 唯一数据源在服务器，本地为缓存：可删可重拉，更新采用库级全量替换。
 */
@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** 服务器版本号，单调递增；本地版本低于服务器即需更新 */
    val version: Int,
    /** 服务器侧题库更新时间（展示用文本，如 09-27） */
    val updatedAt: String,
    val questionCount: Int,
    /** 本地最近一次拉取成功的毫秒时间戳 */
    val lastSyncAt: Long,
)

/**
 * 题目。options/answers 存 JSON 数组文本（org.json 序列化）。
 * 单题文本量 KB 级，远低于 CursorWindow 2MB 读取上限。
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = BankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bankId", "orderIndex")],
)
data class QuestionEntity(
    /** 全局唯一：bankId:questionId */
    @PrimaryKey val id: String,
    val bankId: String,
    /** 题库内固定顺序（单库顺序刷与断点续刷的基准） */
    val orderIndex: Int,
    /** 题型：single / multi / judge（MVP 仅 single，其余为 schema 预留） */
    val type: String,
    val stem: String,
    val optionsJson: String,
    /** 正确项下标数组，单选取首个 */
    val answersJson: String,
    val explanation: String,
    /** 抽题权重 1-5，默认 1，越大越容易被抽中 */
    val weight: Int,
    /** 仅 recall 题：揭示层数据整包 JSON，其余题型 null（v3 迁移新增列） */
    val recallJson: String? = null,
)

/**
 * 断点续刷进度，仅单库顺序模式使用。
 * 题库更新后直接重置（已拍板），不做题目 id 对齐。
 */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val bankId: String,
    /** 下一题的 orderIndex（0 起） */
    val nextIndex: Int,
    /** 已完成题数（含答错） */
    val doneCount: Int,
)

/**
 * 错题计数器。计数变化仅发生在重考场景（已拍板）：
 * 重考答对 retryCount-1（减到 0 移出），重考答错重置 3；普通刷题不动计数。
 */
@Entity(
    tableName = "wrong",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bankId")],
)
data class WrongEntity(
    /** 对应 questions.id */
    @PrimaryKey val questionId: String,
    val bankId: String,
    /** 剩余重考计数（3 → 0） */
    val retryCount: Int,
    /** 累计答错次数（展示用） */
    val wrongCount: Int,
    val addedAt: Long,
    val lastWrongAt: Long,
)

/**
 * 笔记。不随题库删除级联：内容自成一体，来源仅作展示文字。
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    /** 来源：题目 / AI问答 / 归纳稿 */
    val source: String,
    /** 来源题目 id（可空），用于跳回原题 */
    val sourceQuestionId: String?,
    /** 来源题库名（可空，展示用，题库删除后仍保留文字） */
    val bankName: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 已删题黑名单（F1）。不挂外键：题库更新会全量重建 questions，
 * 删除标记必须独立存活，同步重导后过滤仍生效（远程不删，本地不再出现）。
 */
@Entity(tableName = "deleted_questions")
data class DeletedQuestionEntity(
    /** 同 questions.id（bankId:qid） */
    @PrimaryKey val questionId: String,
    val bankId: String,
    val deletedAt: Long,
)

/**
 * 收藏（F2）。不挂外键（同上）：题库全量重建不清收藏；
 * bankName 存快照，题库删除后列表仍可展示来源。
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    /** 同 questions.id（bankId:qid） */
    @PrimaryKey val questionId: String,
    val bankId: String,
    val bankName: String,
    val addedAt: Long,
)
