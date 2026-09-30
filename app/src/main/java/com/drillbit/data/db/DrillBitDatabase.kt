package com.drillbit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    ],
    version = 1,
    exportSchema = false,
)
abstract class DrillBitDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun wrongDao(): WrongDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: DrillBitDatabase? = null

        fun get(context: Context): DrillBitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DrillBitDatabase::class.java,
                    "drillbit.db",
                ).build().also { instance = it }
            }
    }
}
