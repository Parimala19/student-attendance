package com.attendance.app.data.repository

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.attendance.app.data.remote.ApiService
import com.attendance.app.data.remote.LoginRequest
import com.attendance.app.data.remote.LoginResponse
import com.attendance.app.data.remote.RegisterRequest
import com.attendance.app.data.remote.RegisterResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ACCESS_TOKEN_KEY  = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val STUDENT_ID_KEY    = stringPreferencesKey("student_id")
        private val STUDENT_NAME_KEY  = stringPreferencesKey("student_name")
        private val STUDENT_EMAIL_KEY = stringPreferencesKey("student_email")
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY] != null
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[STUDENT_ID_KEY]
    }

    suspend fun login(email: String, password: String): AuthResult<LoginResponse> {
        return try {
            val response = apiService.studentLogin(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                val studentId = decodeSubFromJwt(loginResponse.accessToken)
                saveAuthData(loginResponse, email, studentId)
                AuthResult.Success(loginResponse)
            } else {
                val code = response.code()
                val msg  = response.errorBody()?.string()?.take(200) ?: response.message()
                AuthResult.Error("Login failed ($code): $msg")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun register(
        studentNumber: String,
        fullName: String,
        email: String,
        password: String
    ): AuthResult<RegisterResponse> {
        return try {
            val response = apiService.registerStudent(
                RegisterRequest(studentNumber, fullName, email, password)
            )
            if (response.isSuccessful && response.body() != null) {
                val reg = response.body()!!
                // Persist student identity so face enrollment can use the ID
                // immediately after registration (before the student logs in).
                dataStore.edit { prefs ->
                    prefs[STUDENT_ID_KEY]    = reg.id
                    prefs[STUDENT_NAME_KEY]  = reg.fullName
                    prefs[STUDENT_EMAIL_KEY] = reg.email ?: email
                }
                AuthResult.Success(reg)
            } else {
                val code = response.code()
                val msg  = response.errorBody()?.string()?.take(200) ?: response.message()
                AuthResult.Error("Registration failed ($code): $msg")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun saveAuthData(
        loginResponse: LoginResponse,
        email: String,
        studentId: String?
    ) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY]  = loginResponse.accessToken
            prefs[REFRESH_TOKEN_KEY] = loginResponse.refreshToken
            prefs[STUDENT_EMAIL_KEY] = email
            studentId?.let { prefs[STUDENT_ID_KEY] = it }
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    fun getStudentName(): Flow<String?>  = dataStore.data.map { it[STUDENT_NAME_KEY] }
    fun getStudentEmail(): Flow<String?> = dataStore.data.map { it[STUDENT_EMAIL_KEY] }

    /**
     * Decodes the `sub` claim from a JWT without a third-party library.
     * The JWT payload is the second base64url-encoded segment.
     */
    private fun decodeSubFromJwt(token: String): String? {
        return try {
            val payload = token.split(".").getOrNull(1) ?: return null
            val bytes   = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            val json    = String(bytes, Charsets.UTF_8)
            Regex(""""sub"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}
