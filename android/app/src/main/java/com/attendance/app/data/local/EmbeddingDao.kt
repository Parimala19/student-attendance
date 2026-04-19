package com.attendance.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM embeddings WHERE studentId = :studentId")
    fun getEmbeddingsByStudentId(studentId: String): Flow<List<EmbeddingEntity>>

    @Query("SELECT * FROM embeddings")
    fun getAllEmbeddings(): Flow<List<EmbeddingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<EmbeddingEntity>)

    @Query("DELETE FROM embeddings WHERE studentId = :studentId")
    suspend fun deleteEmbeddingsByStudentId(studentId: String)

    @Query("DELETE FROM embeddings")
    suspend fun deleteAllEmbeddings()

    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getEmbeddingsCount(): Int
}
