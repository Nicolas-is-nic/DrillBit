package com.drillbit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 本地库。database version 从 1 起步，实体任何变更必须 version+1 + Migration。
 */
@Database(
    entities = [
        BankEntity::class,
        QuestionEntity::class,
        ProgressEntity::class,
        WrongEntity::class,
        NoteEntity::class,
        DeletedQuestionEntity::class,
        FavoriteEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class DrillBitDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun wrongDao(): WrongDao
    abstract fun noteDao(): NoteDao
    abstract fun deletedQuestionDao(): DeletedQuestionDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        /** v2（F1/F2）：新增已删题黑名单与收藏两张独立表，无外键不级联 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_questions` " +
                        "(`questionId` TEXT NOT NULL, `bankId` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`questionId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` " +
                        "(`questionId` TEXT NOT NULL, `bankId` TEXT NOT NULL, `bankName` TEXT NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, PRIMARY KEY(`questionId`))",
                )
            }
        }
        /** v3（recall 批次）：questions 增 recallJson 列（可空，recall 题存揭示数据整包 JSON） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `recallJson` TEXT")
            }
        }
        @Volatile
        private var instance: DrillBitDatabase? = null

        fun get(context: Context): DrillBitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DrillBitDatabase::class.java,
                    "drillbit.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
