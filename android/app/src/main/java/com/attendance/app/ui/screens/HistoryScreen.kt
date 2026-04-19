package com.attendance.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.remote.AttendanceResponse
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.AuthRepository
import com.attendance.app.data.repository.Result
import com.attendance.app.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            val studentId = authRepository.currentStudentId.first() ?: run {
                _uiState.value = HistoryUiState.Error("Student ID not found. Please sign in again.")
                return@launch
            }

            // Fetch attendance records and course list in parallel
            val attendanceResult = attendanceRepository.getAttendanceHistory(studentId)
            val coursesResult    = studentRepository.getCourses()

            // Build courseId → courseName map (best-effort; falls back to short UUID)
            val courseNames: Map<String, String> = when (coursesResult) {
                is Result.Success -> coursesResult.data.associate { it.id to "${it.code} — ${it.name}" }
                is Result.Error   -> emptyMap()
            }

            when (attendanceResult) {
                is Result.Success -> _uiState.value =
                    HistoryUiState.Success(attendanceResult.data, courseNames)
                is Result.Error   -> _uiState.value =
                    HistoryUiState.Error(attendanceResult.message)
            }
        }
    }
}

sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class  Success(
        val records: List<AttendanceResponse>,
        val courseNames: Map<String, String>
    ) : HistoryUiState()
    data class  Error(val message: String) : HistoryUiState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is HistoryUiState.Loading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }

            is HistoryUiState.Success -> {
                val state = uiState as HistoryUiState.Success
                if (state.records.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No attendance records yet.")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Check in to a course to see your history here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.records) { record ->
                            AttendanceRecordCard(
                                record     = record,
                                courseName = state.courseNames[record.courseId]
                                    ?: "Course ${record.courseId.take(8)}…"
                            )
                        }
                    }
                }
            }

            is HistoryUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        (uiState as HistoryUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.loadHistory() }) { Text("Retry") }
                }
            }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
fun AttendanceRecordCard(record: AttendanceResponse, courseName: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = courseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = record.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text  = formatDateTime(record.checkedInAt),
                style = MaterialTheme.typography.bodyMedium
            )

            record.confidenceScore?.let { score ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Confidence: ${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val bg = when (status.lowercase()) {
        "present" -> MaterialTheme.colorScheme.primaryContainer
        "late"    -> MaterialTheme.colorScheme.tertiaryContainer
        "absent"  -> MaterialTheme.colorScheme.errorContainer
        else      -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when (status.lowercase()) {
        "present" -> MaterialTheme.colorScheme.onPrimaryContainer
        "late"    -> MaterialTheme.colorScheme.onTertiaryContainer
        "absent"  -> MaterialTheme.colorScheme.onErrorContainer
        else      -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text     = status.uppercase(),
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Parses ISO-8601 timestamps produced by FastAPI / PostgreSQL, including
 * fractional seconds (up to microseconds) and the trailing Z.
 * Example: "2026-04-19T06:56:12.981317Z"
 */
fun formatDateTime(isoDateTime: String): String {
    return try {
        val instant = Instant.parse(
            // Ensure the string ends with Z so Instant.parse accepts it
            if (isoDateTime.endsWith("Z") || isoDateTime.contains("+"))
                isoDateTime
            else
                "${isoDateTime}Z"
        )
        DateTimeFormatter
            .ofPattern("MMM dd, yyyy  HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (e: Exception) {
        isoDateTime
    }
}
