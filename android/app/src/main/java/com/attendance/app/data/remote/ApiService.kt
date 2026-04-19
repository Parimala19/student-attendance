package com.attendance.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/v1/auth/student-login")
    suspend fun studentLogin(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/students/register")
    suspend fun registerStudent(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/v1/students/{studentId}/embeddings")
    suspend fun uploadEmbedding(
        @Path("studentId") studentId: String,
        @Body request: EmbeddingUploadRequest
    ): Response<EmbeddingResponse>

    @GET("api/v1/students/{studentId}/embeddings")
    suspend fun getStudentEmbeddings(
        @Path("studentId") studentId: String
    ): Response<List<EmbeddingResponse>>

    @POST("api/v1/attendance/check-in")
    suspend fun checkIn(
        @Body request: CheckInRequest
    ): Response<AttendanceResponse>

    @GET("api/v1/attendance/student/{studentId}")
    suspend fun getAttendanceHistory(
        @Path("studentId") studentId: String
    ): Response<List<AttendanceResponse>>

    @GET("api/v1/courses/")
    suspend fun getCourses(): Response<List<CourseDto>>
}
