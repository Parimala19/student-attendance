# Testing Guide - Attendance System

Complete guide for testing the full stack locally.

## Architecture

```
┌─────────────────┐
│  Android App    │
│  (Emulator or   │
│   Physical)     │
└────────┬────────┘
         │ HTTP
         ↓
┌─────────────────┐     ┌─────────────────┐
│  Streamlit      │────→│  FastAPI        │
│  Dashboard      │     │  Backend        │
│  Port: 8501     │     │  Port: 8000     │
└─────────────────┘     └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    ↓                         ↓
            ┌──────────────┐         ┌──────────────┐
            │  PostgreSQL  │         │    Redis     │
            │  + pgvector  │         │  Port: 6379  │
            │  Port: 5432  │         └──────────────┘
            └──────────────┘
```

## Step 1: Start Backend Services

### Option A: Docker Compose (Recommended)

```bash
# Navigate to infra directory
cd attendance-app/infra

# Start all services
docker-compose up --build

# Or run in background
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f dashboard

# Stop services
docker-compose down
```

**Services Started:**
- PostgreSQL + pgvector: `localhost:5432`
- Redis: `localhost:6379`
- FastAPI Backend: `localhost:8000`
- Streamlit Dashboard: `localhost:8501`

### Option B: Run Services Individually

```bash
# Terminal 1: Start PostgreSQL
docker run -d --name attendance-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=attendance \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Terminal 2: Start Redis
docker run -d --name attendance-redis \
  -p 6379:6379 \
  redis:7-alpine

# Terminal 3: Start Backend (requires Python 3.13)
cd attendance-app/backend
export ATTENDANCE_DATABASE_URL="postgresql+asyncpg://postgres:postgres@localhost:5432/attendance"
export ATTENDANCE_REDIS_URL="redis://localhost:6379/0"
export ATTENDANCE_JWT_SECRET_KEY="dev-secret-key"
export ATTENDANCE_DEBUG="true"

# With uv (if working)
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Or with python
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Terminal 4: Start Streamlit
cd attendance-app/dashboard-streamlit
pip install -r requirements.txt
streamlit run app.py
```

## Step 2: Verify Backend

```bash
# Health check
curl http://localhost:8000/health

# API docs (Swagger UI)
open http://localhost:8000/docs

# Create admin user (if not exists)
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "password": "password123",
    "full_name": "Admin User",
    "role": "admin"
  }'
```

## Step 3: Test Streamlit Dashboard

```bash
# Open dashboard
open http://localhost:8501

# Login credentials
Email: admin@test.com
Password: password123
```

**Test Flow:**
1. Login with admin credentials
2. Navigate to Students → Create a test student
3. Navigate to Courses → Create a test course
4. Check Dashboard for overview
5. View Reports

## Step 4: Android App Testing

### Option A: Android Emulator (Recommended for Development)

**Prerequisites:**
- Android Studio installed
- Android SDK (API 34)
- Emulator configured

**Setup Emulator:**

1. **Create AVD (Android Virtual Device):**
```bash
# Open Android Studio
# Tools → Device Manager → Create Device
# Choose: Pixel 8 Pro (or any modern device)
# System Image: API 34 (Android 14)
# AVD Name: Attendance_Test
```

2. **Network Configuration:**

For emulator to access localhost services, use special IP:
- `10.0.2.2` = Your host machine's localhost

**Update API URL in Android app:**

Edit `attendance-app/android/app/src/main/java/com/attendance/data/remote/ApiConfig.kt`:
```kotlin
object ApiConfig {
    // For emulator
    const val BASE_URL = "http://10.0.2.2:8000"

    // For physical device on same network
    // const val BASE_URL = "http://YOUR_COMPUTER_IP:8000"
}
```

3. **Run App:**
```bash
cd attendance-app/android

# Run on emulator
./gradlew installDebug

# Or from Android Studio
# Click "Run" button (green triangle)
```

**Test Flow:**
1. App opens → Shows login/registration
2. Register a test student
3. Login with credentials
4. View courses
5. Test face capture (emulator camera)
6. Test check-in flow

### Option B: Physical Device

**Prerequisites:**
- Android phone (Android 10+)
- USB debugging enabled
- Computer and phone on same WiFi network

**Setup:**

1. **Enable Developer Options:**
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable "USB Debugging"

2. **Find Computer IP:**
```bash
# macOS
ipconfig getifaddr en0

# Linux
hostname -I | awk '{print $1}'

# Windows
ipconfig | findstr IPv4
```

3. **Update API URL:**

Edit `ApiConfig.kt`:
```kotlin
const val BASE_URL = "http://YOUR_COMPUTER_IP:8000"
// Example: "http://192.168.1.100:8000"
```

4. **Build and Install APK:**
```bash
cd attendance-app/android

# Build debug APK
./gradlew assembleDebug

# APK location
# app/build/outputs/apk/debug/app-debug.apk

# Install via USB
./gradlew installDebug

# Or manually
adb install app/build/outputs/apk/debug/app-debug.apk
```

5. **Alternative: Build Release APK**
```bash
# For sharing or testing on multiple devices
./gradlew assembleRelease

# APK location
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## Step 5: End-to-End Testing

### Test Scenario 1: Student Registration
1. **Android App:** Register new student (with photo)
2. **Backend:** Check logs for embedding generation
3. **Dashboard:** Verify student appears in Students page

### Test Scenario 2: Course Enrollment
1. **Dashboard:** Create a new course
2. **Dashboard:** Enroll student in course
3. **Android App:** Refresh - course should appear

### Test Scenario 3: Attendance Check-in
1. **Android App:** Select course
2. **Android App:** Capture face → Check-in
3. **Dashboard:** View Attendance page → Record should appear
4. **Dashboard:** Check Reports → Statistics updated

## Troubleshooting

### Backend Won't Start
```bash
# Check if ports are in use
lsof -i :8000
lsof -i :5432
lsof -i :6379

# Kill processes if needed
kill -9 <PID>

# Check database
docker exec -it attendance-db psql -U postgres -d attendance -c "\dt"
```

### Android App Can't Connect
```bash
# Verify backend is accessible
curl http://localhost:8000/health

# For emulator - test from inside emulator
adb shell
curl http://10.0.2.2:8000/health

# For physical device - test from phone browser
http://YOUR_COMPUTER_IP:8000/health

# Check firewall settings
# macOS: System Preferences → Security & Privacy → Firewall
# Allow Python/uvicorn through firewall
```

### Streamlit Won't Load
```bash
# Check if running
curl http://localhost:8501

# Check logs
docker-compose logs dashboard

# Restart
docker-compose restart dashboard
```

### Database Issues
```bash
# Reset database
docker-compose down -v
docker-compose up -d db

# Run migrations
cd attendance-app/backend
alembic upgrade head
```

## Performance Tips

1. **Use Docker Compose for stability** - All services auto-restart
2. **Keep emulator running** - Faster than restarting
3. **Use hot reload** - Backend and Streamlit support live reload
4. **Monitor logs** - `docker-compose logs -f` for debugging

## Testing Checklist

- [ ] All containers start successfully
- [ ] Backend health check passes
- [ ] Streamlit dashboard loads
- [ ] Can login to dashboard
- [ ] Can create students and courses
- [ ] Android app connects to backend
- [ ] Student registration works
- [ ] Face capture works (even with emulator)
- [ ] Check-in flow completes
- [ ] Attendance records appear in dashboard
- [ ] Reports show correct statistics

## Recommendation

**For Development:** Use Android Emulator
- Faster iteration
- Easier debugging
- No USB cable needed
- Can use `10.0.2.2` for localhost

**For Demo/UAT:** Use Physical Device
- Real camera for face recognition
- Better performance
- Actual user experience
- Can test GPS features

**Best Practice:** Test on both!
