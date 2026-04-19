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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.repository.AuthRepository
import com.attendance.app.data.repository.Result
import com.attendance.app.data.repository.StudentRepository
import com.attendance.app.ml.EmbeddingGenerator
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
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
    private val embeddingGenerator: EmbeddingGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<EnrollmentUiState>(EnrollmentUiState.Idle)
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    private val captureAngles = listOf("front", "left", "right", "up", "down")
    private var currentAngleIndex = 0
    private val capturedEmbeddings = mutableListOf<Pair<String, FloatArray>>()

    fun getCurrentAngle(): String = captureAngles.getOrNull(currentAngleIndex) ?: "front"
    fun getProgress(): Float = currentAngleIndex.toFloat() / captureAngles.size

    fun captureFace(faceBitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = EnrollmentUiState.Processing

            // EmbeddingGenerator handles the TFLite → pixel fallback internally
            val embedding = embeddingGenerator.generateEmbedding(faceBitmap)

            capturedEmbeddings.add(Pair(getCurrentAngle(), embedding))
            currentAngleIndex++

            if (currentAngleIndex >= captureAngles.size) {
                uploadEmbeddings()
            } else {
                _uiState.value = EnrollmentUiState.Capturing(getCurrentAngle())
            }
        }
    }

    private fun uploadEmbeddings() {
        viewModelScope.launch {
            _uiState.value = EnrollmentUiState.Uploading

            val studentId = authRepository.currentStudentId.first() ?: run {
                _uiState.value = EnrollmentUiState.Error("Student ID not found. Please log in first.")
                return@launch
            }

            var success = true
            for ((angle, embedding) in capturedEmbeddings) {
                when (studentRepository.uploadEmbedding(studentId, embedding, angle)) {
                    is Result.Success -> {}
                    is Result.Error   -> { success = false; break }
                }
            }

            _uiState.value = if (success) EnrollmentUiState.Success
            else EnrollmentUiState.Error("Failed to upload face embeddings. Check your connection.")
        }
    }

    fun resetState() {
        currentAngleIndex = 0
        capturedEmbeddings.clear()
        _uiState.value = EnrollmentUiState.Capturing(getCurrentAngle())
    }

    init { resetState() }
}

sealed class EnrollmentUiState {
    data object Idle       : EnrollmentUiState()
    data class  Capturing(val angle: String) : EnrollmentUiState()
    data object Processing : EnrollmentUiState()
    data object Uploading  : EnrollmentUiState()
    data object Success    : EnrollmentUiState()
    data class  Error(val message: String) : EnrollmentUiState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EnrollmentScreen(
    onEnrollmentComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnrollmentViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val cameraPermission   = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState  = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is EnrollmentUiState.Success -> {
                snackbarHostState.showSnackbar("Face enrollment completed!")
                onEnrollmentComplete()
            }
            is EnrollmentUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as EnrollmentUiState.Error).message)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Enrollment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!cameraPermission.status.isGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required for face enrollment",
                    textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Progress bar
                LinearProgressIndicator(
                    progress = { viewModel.getProgress() },
                    modifier = Modifier.fillMaxWidth()
                )

                when (uiState) {
                    is EnrollmentUiState.Capturing -> {
                        val angle = (uiState as EnrollmentUiState.Capturing).angle
                        val total = 5
                        val done  = (viewModel.getProgress() * total).toInt()
                        Text(
                            text = "Step ${done + 1} of $total — Look $angle",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = angleInstruction(angle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            EnrollmentCameraView(
                                onCapture = { bitmap -> viewModel.captureFace(bitmap) }
                            )
                        }
                    }

                    is EnrollmentUiState.Processing,
                    is EnrollmentUiState.Uploading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (uiState is EnrollmentUiState.Uploading)
                                    "Uploading face data…"
                                else
                                    "Processing capture…"
                            )
                        }
                    }

                    is EnrollmentUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Enrollment complete!",
                                style = MaterialTheme.typography.headlineMedium)
                        }
                    }

                    is EnrollmentUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                (uiState as EnrollmentUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                            Button(onClick = { viewModel.resetState() }) {
                                Text("Retry")
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

private fun angleInstruction(angle: String) = when (angle) {
    "front" -> "Face directly at the camera"
    "left"  -> "Turn your head slightly to the left"
    "right" -> "Turn your head slightly to the right"
    "up"    -> "Tilt your head slightly upward"
    "down"  -> "Tilt your head slightly downward"
    else    -> "Position your face in the oval"
}

// ── Camera capture composable ─────────────────────────────────────────────────

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun EnrollmentCameraView(
    onCapture: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val cameraController = remember {
        try {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            }
        } catch (e: Exception) {
            null
        }
    }

    if (cameraController == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera not available on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text("Configure the emulator to use Webcam0 in AVD Manager,\nor tap Skip in the top bar.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // ML Kit face detector for live preview feedback
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f)
                .build()
        )
    }
    var faceReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
            faceDetector.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = cameraController,
            modifier   = Modifier.fillMaxSize()
        )

        // Oval guide overlay
        val borderColor = if (faceReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp, 260.dp)
                .border(3.dp, borderColor, RoundedCornerShape(100.dp))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (faceReady) "Face detected — press Capture" else "Position your face in the oval",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    // Take a photo, detect face, crop it, pass bitmap to ViewModel
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onCaptureSuccess(proxy: ImageProxy) {
                                val mediaImage = proxy.image
                                if (mediaImage == null) { proxy.close(); return }
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage, proxy.imageInfo.rotationDegrees
                                )
                                faceDetector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        val bitmap = proxy.toBitmap()
                                        proxy.close()
                                        val faceBitmap = cropFace(bitmap, faces.firstOrNull())
                                        onCapture(faceBitmap)
                                        faceReady = false
                                    }
                                    .addOnFailureListener {
                                        // No face detected — pass the full frame anyway
                                        val bitmap = proxy.toBitmap()
                                        proxy.close()
                                        onCapture(bitmap)
                                        faceReady = false
                                    }
                            }
                            override fun onError(exc: ImageCaptureException) {
                                exc.printStackTrace()
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text("Capture", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    // Continuous face-presence check using a lightweight preview analyser
    LaunchedEffect(Unit) {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f)
                .build()
        )
        // Poll the preview every ~500 ms using ImageAnalysis
        cameraController.setEnabledUseCases(
            CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
        )
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        cameraController.setImageAnalysisAnalyzer(executor) { proxy ->
            @Suppress("DEPRECATION")
            val mediaImage = proxy.image
            if (mediaImage != null) {
                val img = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                detector.process(img)
                    .addOnSuccessListener { faces -> faceReady = faces.isNotEmpty() }
                    .addOnCompleteListener { proxy.close() }
            } else {
                proxy.close()
            }
        }
    }
}

/**
 * Crops the detected face region from a bitmap, or returns the full bitmap if
 * no face was found.
 */
private fun cropFace(bitmap: Bitmap, face: Face?): Bitmap {
    if (face == null) return bitmap
    val box = face.boundingBox
    val padding = (box.width() * 0.2f).toInt()
    val safeRect = Rect(
        (box.left   - padding).coerceAtLeast(0),
        (box.top    - padding).coerceAtLeast(0),
        (box.right  + padding).coerceAtMost(bitmap.width),
        (box.bottom + padding).coerceAtMost(bitmap.height)
    )
    return Bitmap.createBitmap(
        bitmap,
        safeRect.left, safeRect.top,
        safeRect.width(), safeRect.height()
    )
}
