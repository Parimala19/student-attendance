# Student Attendance — Face Recognition System

A full-stack student attendance system that uses **face recognition** to mark attendance.  
Students register once, enrol their face, and check in to classes by looking at their phone camera — no cards, no codes.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                         │
│  Kotlin · Jetpack Compose · CameraX · ML Kit · Hilt    │
└──────────────────────┬──────────────────────────────────┘
                       │ REST / JSON
┌──────────────────────▼──────────────────────────────────┐
│                  FastAPI Backend                        │
│  Python 3.13 · SQLAlchemy (async) · Alembic · JWT      │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
┌──────────▼──────────┐   ┌───────────▼──────────────────┐
│  PostgreSQL + pgvector│   │         Redis               │
│  (attendance data,  │   │  (session / cache)           │
│   face embeddings)  │   └──────────────────────────────┘
└─────────────────────┘
           ▲
┌──────────┴──────────────────────────────────────────────┐
│             Streamlit Dashboard (Admin)                 │
│  Course management · Attendance reports · Analytics    │
└─────────────────────────────────────────────────────────┘
```

---

## Components

| Component | Path | Description |
|---|---|---|
| Android App | `android/` | Student-facing mobile app |
| FastAPI Backend | `backend/` | REST API, auth, attendance logic |
| Streamlit Dashboard | `dashboard-streamlit/` | Admin web interface |
| Infrastructure | `infra/` | Docker Compose orchestration |

---

## User Flow

### Student — first time
```
Open app
  └─► Welcome screen
        ├─► [Sign In]     → Email + password → Courses
        └─► [Register]    → Fill details
                              └─► Face Enrolment (5 angles: front/left/right/up/down)
                                    └─► Sign In
```

### Student — daily attendance
```
Sign In → Select course → Camera opens
  └─► Position face in oval (ML Kit detects face live)
        └─► [Check In] → face captured → embedding compared against enrolled data
              ├─ Match ≥ 60%  → Attendance recorded ✓
              └─ No match     → "Face not recognised" error
```

### Admin
```
Open Streamlit dashboard (http://localhost:8501)
  └─► Log in with admin credentials
        ├─► Manage courses
        ├─► View real-time attendance per course
        └─► Export attendance reports
```

---

## Technology Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt DI |
| Camera | CameraX (preview, image analysis, image capture) |
| Face detection | ML Kit Face Detection |
| Face embeddings | TFLite (MobileFaceNet) with pixel-descriptor fallback |
| Networking | Retrofit + Moshi (snake_case ↔ camelCase) |
| Local DB | Room (cached embeddings) |
| Auth storage | DataStore Preferences |
| Background sync | WorkManager |

### Backend
| Layer | Technology |
|---|---|
| Framework | FastAPI (async) |
| ORM | SQLAlchemy 2 (asyncpg) |
| Migrations | Alembic |
| Auth | JWT (python-jose) + bcrypt |
| Vector search | pgvector |
| Task queue | Redis |
| Runtime | Python 3.13 + Uvicorn |

### Infrastructure
- **Docker Compose** — single command to start all services
- **PostgreSQL 16 + pgvector** — relational data + face embedding vectors
- **Redis 7** — caching / session storage

---

## New Developer? Start Here

If you are setting up this project for the first time, read the full step-by-step guide:

**[DEVELOPER_SETUP.md](./DEVELOPER_SETUP.md)** — covers every tool you need to install,
how to start the backend, how to build the Android app, and how to run the full end-to-end
test. Written for someone new to the project.

---

## Quick Start (experienced developers)

### Prerequisites
- Docker & Docker Compose
- Android Studio Hedgehog or later (for the mobile app)
- Android SDK 35, JDK 17+

### 1. Start the backend stack
```bash
cd infra
docker compose up -d
```

Services started:
| Service | URL |
|---|---|
| FastAPI backend | http://localhost:8000 |
| API docs (Swagger) | http://localhost:8000/docs |
| Streamlit dashboard | http://localhost:8501 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

A default **admin** account is seeded automatically on first startup:
- Email: `admin@test.com`
- Password: `password123`

### 2. Build and run the Android app

**Option A — Android Studio**
1. Open the `android/` folder in Android Studio
2. Create an emulator (Pixel 8, API 35) or connect a physical device
3. *(For webcam in emulator)* Edit AVD → Advanced Settings → Camera → set both to `Webcam0` → Cold Boot
4. Run `▶`

**Option B — Command line**
```bash
cd android
./gradlew installDebug
```

### 3. Configure backend URL (if needed)

Edit `android/app/build.gradle.kts`:
```kotlin
// Emulator talking to host machine
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")

// Physical device on same Wi-Fi
buildConfigField("String", "API_BASE_URL", "\"http://192.168.x.x:8000/\"")
```

---

## Backend API Reference

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/students/register` | Register a new student |
| POST | `/api/v1/auth/student-login` | Student sign-in → JWT |
| POST | `/api/v1/auth/login` | Admin / teacher sign-in |
| GET | `/api/v1/auth/me` | Current user profile |

### Students & Face Embeddings
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/students/{id}/embeddings` | Upload face embedding for enrolment |
| GET | `/api/v1/students/{id}/embeddings` | Fetch student's stored embeddings |

### Courses
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/courses/` | List all courses |
| POST | `/api/v1/courses/` | Create a course (admin) |

### Attendance
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/attendance/check-in` | Submit attendance with confidence score |
| GET | `/api/v1/attendance/student/{id}` | Student's attendance history |
| GET | `/api/v1/attendance/course/{id}` | Course attendance (admin) |

Full interactive docs: **http://localhost:8000/docs**

---

## Face Recognition Details

The system uses a two-phase approach:

**Enrolment (Registration)**
- Student captures 5 angles: front, left, right, up, down
- Each frame is processed by ML Kit to detect and crop the face
- A 512-float embedding is generated per angle (TFLite MobileFaceNet when available; normalised pixel descriptor as fallback)
- All embeddings are uploaded to the backend and cached locally in Room

**Verification (Check-in)**
- Student's face is detected live via ML Kit image analysis
- On capture, a fresh 512-float embedding is generated from the cropped face
- Cosine similarity is computed against every stored embedding for that student
- If best similarity ≥ **0.6** → attendance is recorded with the similarity as the confidence score
- If no match → "Face not recognised" error with the similarity percentage shown

> **Using the emulator?** Configure the AVD to use `Webcam0` (your laptop camera) in AVD Manager → Edit → Advanced Settings → Camera. Then cold-boot the emulator.

---

## Project Structure

```
attendance-app/
├── android/                    # Android mobile app
│   └── app/src/main/java/com/attendance/app/
│       ├── data/
│       │   ├── local/          # Room DB (embedding cache)
│       │   ├── remote/         # Retrofit API models & service
│       │   └── repository/     # Auth, Student, Attendance repos
│       ├── di/                 # Hilt DI modules
│       ├── ml/                 # Face detection, embedding, matching
│       ├── ui/
│       │   ├── navigation/     # NavHost + Screen routes
│       │   └── screens/        # Welcome, Login, Register, Enrolment,
│       │                       #   CourseSelection, CheckIn, History
│       └── worker/             # Background embedding sync
│
├── backend/                    # FastAPI backend
│   ├── app/
│   │   ├── api/                # Route handlers (auth, courses, attendance…)
│   │   ├── core/               # Config, DB engine, security utils
│   │   └── models/             # SQLAlchemy ORM models
│   ├── alembic/                # DB migrations
│   └── tests/                  # Unit + integration tests
│
├── dashboard-streamlit/        # Admin Streamlit web app
│   └── app.py
│
└── infra/
    └── docker-compose.yml      # Full stack orchestration
```

---

## Running Tests

### Backend unit tests
```bash
cd backend
uv run pytest
```

### Backend integration tests (requires running stack)
```bash
cd backend
uv run pytest tests/integration
```

---

## Environment Variables (Backend)

| Variable | Default | Description |
|---|---|---|
| `ATTENDANCE_DATABASE_URL` | (required) | PostgreSQL asyncpg connection string |
| `ATTENDANCE_REDIS_URL` | (required) | Redis connection string |
| `ATTENDANCE_JWT_SECRET_KEY` | (required) | JWT signing secret |
| `ADMIN_EMAIL` | `admin@test.com` | Seeded admin email |
| `ADMIN_PASSWORD` | `password123` | Seeded admin password |
| `ATTENDANCE_DEBUG` | `false` | Enable SQLAlchemy query logging |

---

## Known Limitations / Roadmap

- [ ] Replace pixel-descriptor fallback with a bundled MobileFaceNet TFLite model
- [ ] Add liveness detection (anti-spoofing) using blink / head-movement checks
- [ ] Offline check-in queue (sync when network returns)
- [ ] Push notifications for attendance confirmation
- [ ] Course enrolment management in the mobile app
- [ ] Multi-tenancy (multiple institutions)

---

## License

MIT
