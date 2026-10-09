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
    version = 5,
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
        /** v4（2026-10-09 分类批次）：banks 增排序列与分类列（带默认值，旧数据自动归位） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `banks` ADD COLUMN `sortKey` INTEGER NOT NULL DEFAULT 9999")
                db.execSQL("ALTER TABLE `banks` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'knowledge'")
            }
        }
        /**
         * v5（2026-10-09 review F-2）：wrong 表去外键重建（与 favorites/deleted 同构）。
         * 迁移保数据：建新表（无外键）→ 拷贝 → 换名 → 重建 bankId 索引。
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wrong_new` (" +
                        "`questionId` TEXT NOT NULL, `bankId` TEXT NOT NULL, " +
                        "`retryCount` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, `lastWrongAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`questionId`))",
                )
                db.execSQL("INSERT INTO `wrong_new` SELECT * FROM `wrong`")
                db.execSQL("DROP TABLE `wrong`")
                db.execSQL("ALTER TABLE `wrong_new` RENAME TO `wrong`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wrong_bankId` ON `wrong` (`bankId`)")
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
    }
}
