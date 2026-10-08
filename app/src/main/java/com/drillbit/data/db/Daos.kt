package com.drillbit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bank: BankEntity)

    @Query("SELECT * FROM banks ORDER BY name")
    fun observeAll(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE id = :bankId")
    suspend fun getById(bankId: String): BankEntity?

    @Query("SELECT * FROM banks")
    suspend fun getAllOnce(): List<BankEntity>

    @Query("DELETE FROM banks WHERE id = :bankId")
    suspend fun deleteById(bankId: String)
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE bankId = :bankId ORDER BY orderIndex")
    suspend fun getByBank(bankId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getById(questionId: String): QuestionEntity?

    @Query("DELETE FROM questions WHERE bankId = :bankId")
    suspend fun deleteByBank(bankId: String)
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE bankId = :bankId")
    suspend fun get(bankId: String): ProgressEntity?

    @Query("SELECT * FROM progress")
    suspend fun getAllOnce(): List<ProgressEntity>

    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("DELETE FROM progress WHERE bankId = :bankId")
    suspend fun deleteByBank(bankId: String)

    @Query("DELETE FROM progress")
    suspend fun deleteAll()
}

@Dao
interface WrongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wrong: WrongEntity)

    @Query("SELECT * FROM wrong")
    fun observeAll(): Flow<List<WrongEntity>>

    @Query("SELECT * FROM wrong")
    suspend fun getAllOnce(): List<WrongEntity>

    @Query("DELETE FROM wrong WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: String)

    @Query("DELETE FROM wrong")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM wrong")
    fun observeCount(): Flow<Int>
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

/** 已删题黑名单（F1） */
@Dao
interface DeletedQuestionDao {
    @Query("SELECT * FROM deleted_questions")
    suspend fun getAllOnce(): List<DeletedQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deleted: DeletedQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deleted: List<DeletedQuestionEntity>)

    @Query("SELECT questionId FROM deleted_questions WHERE bankId = :bankId")
    suspend fun idsByBank(bankId: String): List<String>

    @Query("DELETE FROM deleted_questions")
    suspend fun deleteAll()
}

/** 收藏（F2） */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt ASC")
    suspend fun getAllOnce(): List<FavoriteEntity>

    @Query("SELECT questionId FROM favorites")
    suspend fun allIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
