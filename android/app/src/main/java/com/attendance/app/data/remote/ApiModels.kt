package com.attendance.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Auth ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

/** Matches FastAPI TokenResponse: access_token / refresh_token / token_type */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "access_token")  val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type")    val tokenType: String = "bearer"
)

// ── Student registration ──────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "student_number") val studentNumber: String,
    @Json(name = "full_name")      val fullName: String,
    val email: String,
    val password: String
)

/** Matches FastAPI StudentResponse */
@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val id: String,
    @Json(name = "student_number") val studentNumber: String,
    @Json(name = "full_name")      val fullName: String,
    val email: String?,
    @Json(name = "created_at")     val createdAt: String
)

// ── Face embeddings ───────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class EmbeddingUploadRequest(
    val embedding: List<Float>,
    @Json(name = "capture_angle") val captureAngle: String?
)

@JsonClass(generateAdapter = true)
data class EmbeddingResponse(
    val id: String,
    @Json(name = "student_id")    val studentId: String,
    val embedding: List<Float>,
    @Json(name = "capture_angle") val captureAngle: String?,
    @Json(name = "created_at")    val createdAt: String
)

// ── Attendance check-in ───────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CheckInRequest(
    @Json(name = "student_id")       val studentId: String,
    @Json(name = "course_id")        val courseId: String,
    @Json(name = "confidence_score") val confidenceScore: Float,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "device_info")      val deviceInfo: Map<String, String>? = null
)

/** Matches FastAPI AttendanceResponse */
@JsonClass(generateAdapter = true)
data class AttendanceResponse(
    val id: String,
    @Json(name = "student_id")       val studentId: String,
    @Json(name = "course_id")        val courseId: String,
    @Json(name = "checked_in_at")    val checkedInAt: String,
    val status: String,
    @Json(name = "confidence_score") val confidenceScore: Float?
)

// ── Courses ───────────────────────────────────────────────────────────────────

/** Matches FastAPI CourseResponse */
@JsonClass(generateAdapter = true)
data class CourseDto(
    val id: String,
    val name: String,
    val code: String,
    @Json(name = "teacher_id") val teacherId: String?,
    val schedule: Map<String, String>?
)

// ── Misc ──────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiError(
    val detail: String
)
