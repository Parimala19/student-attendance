package com.attendance.app.data.repository

import android.os.Build
import com.attendance.app.data.remote.ApiService
import com.attendance.app.data.remote.AttendanceResponse
import com.attendance.app.data.remote.CheckInRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun checkIn(
        studentId: String,
        courseId: String,
        confidenceScore: Float,
        latitude: Double?,
        longitude: Double?
    ): Result<AttendanceResponse> {
        return try {
            val deviceInfo = mapOf(
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "android" to Build.VERSION.RELEASE
            )
            val response = apiService.checkIn(
                CheckInRequest(
                    studentId = studentId,
                    courseId = courseId,
                    confidenceScore = confidenceScore,
                    latitude = latitude,
                    longitude = longitude,
                    deviceInfo = deviceInfo
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val msg = response.errorBody()?.string()?.take(200) ?: response.message()
                Result.Error("Check-in failed (${response.code()}): $msg")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getAttendanceHistory(studentId: String): Result<List<AttendanceResponse>> {
        return try {
            val response = apiService.getAttendanceHistory(studentId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to fetch history: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }
}
