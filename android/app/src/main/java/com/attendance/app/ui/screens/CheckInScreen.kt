package com.attendance.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.repository.AuthRepository
import com.attendance.app.data.repository.Result
import com.attendance.app.data.repository.StudentRepository
import com.attendance.app.ml.EmbeddingGenerator
import com.attendance.app.ml.FaceMatcher
import com.attendance.app.ui.components.CameraPreview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

// ── State types ───────────────────────────────────────────────────────────────

sealed class FaceDetectionState {
    data object NoFace : FaceDetectionState()
    data class  Detected(val confidence: Float) : FaceDetectionState()
}

sealed class CheckInUiState {
    data object Idle       : CheckInUiState()
    data object Verifying  : CheckInUiState()   // embedding comparison in progress
    data object Processing : CheckInUiState()   // API call in progress
    data class  Success(val status: String) : CheckInUiState()
    data class  Error(val message: String)  : CheckInUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val embeddingGenerator: EmbeddingGenerator,
    private val faceMatcher: FaceMatcher
) : ViewModel() {

    private val _uiState   = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    private val _faceState = MutableStateFlow<FaceDetectionState>(FaceDetectionState.NoFace)
    val faceState: StateFlow<FaceDetectionState> = _faceState.asStateFlow()

    /** Called on every camera frame from the live ImageAnalysis pipeline. */
    fun onFacesAnalyzed(faces: List<Face>, imageWidth: Int, imageHeight: Int) {
        if (_uiState.value != CheckInUiState.Idle) return
        if (faces.isEmpty()) { _faceState.value = FaceDetectionState.NoFace; return }

        val face     = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
        val faceArea = face.boundingBox.width().toFloat() * face.boundingBox.height().toFloat()
        val imgArea  = imageWidth.toFloat() * imageHeight.toFloat()
        val size     = ((faceArea / imgArea) / 0.3f).coerceIn(0f, 1f)
        val pose     = ((40f - kotlin.math.abs(face.headEulerAngleY)) / 40f).coerceIn(0f, 1f)
        _faceState.value = FaceDetectionState.Detected((size * 0.4f + pose * 0.6f).coerceIn(0.3f, 1f))
    }

    /**
     * Called when the user presses "Check In".
     * [faceBitmap] is the captured (and already face-cropped) frame.
     */
    fun submitCheckIn(courseId: String, faceBitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = CheckInUiState.Verifying

            val studentId = authRepository.currentStudentId.first() ?: run {
                _uiState.value = CheckInUiState.Error("Not logged in. Please sign in again.")
                return@launch
            }

            // 1. Generate embedding for the captured face
            val queryEmbedding = embeddingGenerator.generateEmbedding(faceBitmap)

            // 2. Load stored embeddings for this student from the local DB
            val storedEntities = studentRepository.getLocalEmbeddings(studentId).first()

            val confidence: Float
            if (storedEntities.isEmpty()) {
                // No enrolled face data — allow check-in but note it's unverified
                confidence = (_faceState.value as? FaceDetectionState.Detected)?.confidence ?: 0.8f
            } else {
                // 3. Compare query embedding against every stored embedding
                val storedEmbeddings = storedEntities.map { it.embedding }
                val matchResult = faceMatcher.matchSingleStudent(queryEmbedding, storedEmbeddings)

                if (!matchResult.isMatch) {
                    _uiState.value = CheckInUiState.Error(
                        "Face not recognised (similarity ${(matchResult.similarity * 100).toInt()}%).\n" +
                        "Please re-enroll or try again in better lighting."
                    )
                    return@launch
                }
                confidence = matchResult.similarity
            }

            // 4. Submit attendance to the backend
            _uiState.value = CheckInUiState.Processing
            when (val r = attendanceRepository.checkIn(
                studentId       = studentId,
                courseId        = courseId,
                confidenceScore = confidence,
                latitude        = null,
                longitude       = null
            )) {
                is Result.Success -> _uiState.value = CheckInUiState.Success(r.data.status)
                is Result.Error   -> _uiState.value = CheckInUiState.Error(r.message)
            }
        }
    }

    fun resetState() {
        _faceState.value = FaceDetectionState.NoFace
        _uiState.value   = CheckInUiState.Idle
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CheckInScreen(
    courseId: String,
    onCheckInComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val faceState         by viewModel.faceState.collectAsState()
    val cameraPermission   = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState  = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CheckInUiState.Success -> {
                snackbarHostState.showSnackbar(
                    "Check-in successful: ${(uiState as CheckInUiState.Success).status}"
                )
                kotlinx.coroutines.delay(1200)
                onCheckInComplete()
            }
            is CheckInUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as CheckInUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Check-In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermission.status.isGranted -> {
                    PermissionPrompt { cameraPermission.launchPermissionRequest() }
                }
                uiState is CheckInUiState.Verifying ||
                uiState is CheckInUiState.Processing -> {
                    LoadingOverlay(
                        message = if (uiState is CheckInUiState.Verifying)
                            "Comparing face…" else "Submitting attendance…"
                    )
                }
                uiState is CheckInUiState.Success -> {
                    SuccessView()
                }
                else -> {
                    LiveCameraCheckIn(
                        faceState = faceState,
                        onFaces   = { faces, w, h -> viewModel.onFacesAnalyzed(faces, w, h) },
                        onCapture = { bitmap -> viewModel.submitCheckIn(courseId, bitmap) }
                    )
                }
            }
        }
    }
}

// ── Camera view with capture ──────────────────────────────────────────────────

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun LiveCameraCheckIn(
    faceState: FaceDetectionState,
    onFaces: (List<Face>, Int, Int) -> Unit,
    onCapture: (Bitmap) -> Unit
) {
    val context          = LocalContext.current
    val cameraController = remember {
        try {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            }
        } catch (e: Exception) { null }
    }

    if (cameraController == null) {
        NoCameraFallback(onCapture = { onCapture(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)) })
        return
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetector     = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build()
        )
    }
    val captureDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.1f)
                .build()
        )
    }

    LaunchedEffect(Unit) {
        cameraController.setImageAnalysisAnalyzer(analysisExecutor) { proxy ->
            val mediaImage = proxy.image ?: run { proxy.close(); return@setImageAnalysisAnalyzer }
            val img = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            val w = proxy.width; val h = proxy.height
            faceDetector.process(img)
                .addOnSuccessListener { faces -> onFaces(faces, w, h) }
                .addOnCompleteListener { proxy.close() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            analysisExecutor.shutdown()
            faceDetector.close()
            captureDetector.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(controller = cameraController, modifier = Modifier.fillMaxSize())

        // Oval guide
        val ovalColor = when (faceState) {
            is FaceDetectionState.Detected ->
                if (faceState.confidence >= 0.65f) Color(0xFF4CAF50) else Color(0xFFFFC107)
            FaceDetectionState.NoFace -> Color.White.copy(alpha = 0.6f)
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(220.dp, 280.dp)
                .border(3.dp, ovalColor, RoundedCornerShape(110.dp))
        )

        // Bottom panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (faceState) {
                FaceDetectionState.NoFace -> Text(
                    "Position your face inside the oval",
                    color = Color.White, textAlign = TextAlign.Center
                )
                is FaceDetectionState.Detected -> {
                    Text(
                        "Face detected — ready to check in",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { faceState.confidence },
                        modifier = Modifier.fillMaxWidth(),
                        color = ovalColor
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val canCapture = faceState is FaceDetectionState.Detected &&
                    (faceState as FaceDetectionState.Detected).confidence >= 0.65f

            Button(
                onClick = {
                    // Take a photo, detect face, crop, pass to ViewModel
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onCaptureSuccess(proxy: ImageProxy) {
                                val mediaImage = proxy.image
                                if (mediaImage == null) { proxy.close(); return }
                                val input = InputImage.fromMediaImage(
                                    mediaImage, proxy.imageInfo.rotationDegrees
                                )
                                captureDetector.process(input)
                                    .addOnSuccessListener { faces ->
                                        val bmp  = proxy.toBitmap()
                                        proxy.close()
                                        onCapture(cropFace(bmp, faces.firstOrNull()))
                                    }
                                    .addOnFailureListener {
                                        val bmp = proxy.toBitmap()
                                        proxy.close()
                                        onCapture(bmp)
                                    }
                            }
                            override fun onError(e: ImageCaptureException) { e.printStackTrace() }
                        }
                    )
                },
                enabled  = canCapture,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (canCapture) "Check In" else "Waiting for face…")
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun cropFace(bitmap: Bitmap, face: Face?): Bitmap {
    if (face == null) return bitmap
    val pad  = (face.boundingBox.width() * 0.2f).toInt()
    val rect = Rect(
        (face.boundingBox.left   - pad).coerceAtLeast(0),
        (face.boundingBox.top    - pad).coerceAtLeast(0),
        (face.boundingBox.right  + pad).coerceAtMost(bitmap.width),
        (face.boundingBox.bottom + pad).coerceAtMost(bitmap.height)
    )
    return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera permission is required for face check-in", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SuccessView() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Check-in successful!", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NoCameraFallback(onCapture: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera not available", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Configure Webcam0 in AVD Manager and cold-boot the emulator.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCapture) { Text("Check In (Test — no camera)") }
    }
}
