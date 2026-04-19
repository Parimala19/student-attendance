# Android Attendance App

Native Android application for Facial Recognition Student Attendance System.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Camera**: CameraX
- **Face Detection**: Google ML Kit
- **Face Recognition**: TensorFlow Lite (MobileFaceNet)
- **Networking**: Retrofit + Moshi
- **Local Storage**: Room + DataStore
- **Background Tasks**: WorkManager
- **Concurrency**: Coroutines + Flow

## Project Structure

```
app/src/main/java/com/attendance/app/
├── AttendanceApp.kt              # Application class with Hilt
├── MainActivity.kt               # Single activity host
├── di/                           # Dependency injection modules
│   ├── AppModule.kt
│   └── NetworkModule.kt
├── data/                         # Data layer
│   ├── local/                    # Room database
│   │   ├── AppDatabase.kt
│   │   ├── EmbeddingDao.kt
│   │   └── EmbeddingEntity.kt
│   ├── remote/                   # API services
│   │   ├── ApiService.kt
│   │   └── ApiModels.kt
│   └── repository/               # Repository implementations
│       ├── AuthRepository.kt
│       ├── StudentRepository.kt
│       └── AttendanceRepository.kt
├── ml/                           # ML components
│   ├── FaceDetectionAnalyzer.kt  # ML Kit face detection
│   ├── EmbeddingGenerator.kt     # TFLite inference
│   ├── FaceMatcher.kt            # Cosine similarity matching
│   └── LivenessDetector.kt       # Blink/head movement detection
├── ui/                           # UI layer
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── screens/                  # Composable screens
│   │   ├── LoginScreen.kt
│   │   ├── CourseSelectionScreen.kt
│   │   ├── EnrollmentScreen.kt
│   │   ├── CheckInScreen.kt
│   │   └── HistoryScreen.kt
│   ├── components/               # Reusable components
│   │   ├── CameraPreview.kt
│   │   └── AttendanceCard.kt
│   └── theme/
│       └── Theme.kt
└── worker/                       # Background tasks
    └── EmbeddingSyncWorker.kt
```

## Setup

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 26+ (minimum SDK)
- Android SDK 35 (target SDK)

### Configuration

1. **API Base URL**: Configure in `app/build.gradle.kts`:
   - Debug: `http://10.0.2.2:8000` (Android emulator localhost)
   - Release: Set your production API URL

2. **TensorFlow Lite Model**:
   - Place `mobilefacenet.tflite` in `app/src/main/assets/`
   - Model expects 112x112 RGB input, outputs 512-dim embedding
   - Uncomment model loading code in `EmbeddingGenerator.kt`

3. **Permissions**: Already configured in `AndroidManifest.xml`:
   - CAMERA (required)
   - INTERNET (required)
   - ACCESS_FINE_LOCATION (optional for location-based check-in)
   - ACCESS_COARSE_LOCATION (optional)

### Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## API Endpoints

The app communicates with FastAPI backend:

- `POST /api/v1/auth/student-login` - Student authentication
- `POST /api/v1/students/register` - New student registration
- `POST /api/v1/students/{id}/embeddings` - Upload face embedding
- `GET /api/v1/students/{id}/embeddings` - Download embeddings
- `POST /api/v1/attendance/check-in` - Mark attendance
- `GET /api/v1/attendance/student/{id}` - Attendance history
- `GET /api/v1/courses/` - List active courses

## Features

### 1. Authentication
- Email/password login
- JWT token storage (encrypted with DataStore)
- Automatic token refresh

### 2. Face Enrollment
- Guided multi-angle capture (front, left, right, up, down)
- Real-time face detection with ML Kit
- TFLite embedding generation (512-dimensional vectors)
- Upload to backend with retry logic

### 3. Check-In
- Live camera preview with face detection overlay
- Liveness detection (blink + head movement)
- Local embedding matching (cosine similarity >= 0.6)
- Automatic attendance submission with confidence score

### 4. Attendance History
- List view of past attendance records
- Status indicators (present/late/absent)
- Confidence scores per check-in

### 5. Background Sync
- WorkManager periodic embedding sync
- Runs every 24 hours
- Handles network failures with exponential backoff

## Security

- JWT tokens encrypted with DataStore
- HTTPS enforced (disable `usesCleartextTraffic` in production)
- ProGuard rules for TFLite and ML Kit
- No credentials stored in code

## Testing

The project structure supports unit and instrumentation tests:

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### ML Kit face detection not working
- Ensure Google Play Services is updated on device
- Check internet connection for model download on first run

### TFLite model not found
- Verify `mobilefacenet.tflite` is in `assets/` directory
- Uncomment model loading code in `EmbeddingGenerator.kt`

### Camera preview black screen
- Check camera permissions granted
- Verify device has working camera
- Check CameraX initialization in logs

### API connection failures
- Emulator: Use `10.0.2.2` instead of `localhost`
- Physical device: Ensure device and backend on same network
- Check API base URL in `BuildConfig.API_BASE_URL`

## License

Proprietary - All rights reserved
