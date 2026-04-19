package com.attendance.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.app.data.repository.AuthRepository
import com.attendance.app.data.repository.Result
import com.attendance.app.data.repository.StudentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class EmbeddingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val studentId = authRepository.currentStudentId.first()
            if (studentId == null) {
                return Result.failure()
            }

            when (studentRepository.syncEmbeddings(studentId)) {
                is com.attendance.app.data.repository.Result.Success -> {
                    Result.success()
                }
                is com.attendance.app.data.repository.Result.Error -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "embedding_sync_work"
    }
}
