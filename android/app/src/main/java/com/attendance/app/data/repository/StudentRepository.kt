package com.attendance.app.data.repository

import com.attendance.app.data.local.EmbeddingDao
import com.attendance.app.data.local.EmbeddingEntity
import com.attendance.app.data.remote.ApiService
import com.attendance.app.data.remote.CourseDto
import com.attendance.app.data.remote.EmbeddingUploadRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

@Singleton
class StudentRepository @Inject constructor(
    private val apiService: ApiService,
    private val embeddingDao: EmbeddingDao
) {

    suspend fun uploadEmbedding(
        studentId: String,
        embedding: FloatArray,
        captureAngle: String
    ): Result<Unit> {
        return try {
            val response = apiService.uploadEmbedding(
                studentId,
                EmbeddingUploadRequest(
                    embedding = embedding.toList(),
                    captureAngle = captureAngle
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val embeddingResponse = response.body()!!
                embeddingDao.insertEmbedding(
                    EmbeddingEntity(
                        id = embeddingResponse.id,
                        studentId = embeddingResponse.studentId,
                        embedding = embeddingResponse.embedding.toFloatArray(),
                        captureAngle = embeddingResponse.captureAngle ?: "front",
                        timestamp = System.currentTimeMillis()
                    )
                )
                Result.Success(Unit)
            } else {
                Result.Error("Upload failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun syncEmbeddings(studentId: String): Result<Unit> {
        return try {
            val response = apiService.getStudentEmbeddings(studentId)
            if (response.isSuccessful && response.body() != null) {
                val embeddings = response.body()!!.map { dto ->
                    EmbeddingEntity(
                        id = dto.id,
                        studentId = dto.studentId,
                        embedding = dto.embedding.toFloatArray(),
                        captureAngle = dto.captureAngle ?: "front",
                        timestamp = System.currentTimeMillis()
                    )
                }
                embeddingDao.deleteEmbeddingsByStudentId(studentId)
                embeddingDao.insertEmbeddings(embeddings)
                Result.Success(Unit)
            } else {
                Result.Error("Sync failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    fun getLocalEmbeddings(studentId: String): Flow<List<EmbeddingEntity>> {
        return embeddingDao.getEmbeddingsByStudentId(studentId)
    }

    fun getAllLocalEmbeddings(): Flow<List<EmbeddingEntity>> {
        return embeddingDao.getAllEmbeddings()
    }

    suspend fun getCourses(): Result<List<CourseDto>> {
        return try {
            val response = apiService.getCourses()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to fetch courses: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }
}
