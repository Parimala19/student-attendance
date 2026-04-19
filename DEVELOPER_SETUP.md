# Developer Setup Guide — Student Attendance System

Welcome! This guide walks you through setting up the entire attendance system on your laptop
from scratch. It assumes you are new to software development and explains every step in plain
English. Read each section carefully before running any command.

---

## Table of Contents

1. [How the System Works — Big Picture](#1-how-the-system-works--big-picture)
2. [What You Will Install](#2-what-you-will-install)
3. [Install the Prerequisites](#3-install-the-prerequisites)
   - 3.1 [Homebrew — the macOS package manager](#31-homebrew--the-macos-package-manager)
   - 3.2 [Git — version control](#32-git--version-control)
   - 3.3 [Docker Desktop — run services without installing them](#33-docker-desktop--run-services-without-installing-them)
   - 3.4 [JDK 17 — Java runtime for Android tooling](#34-jdk-17--java-runtime-for-android-tooling)
   - 3.5 [Android Studio — IDE for the mobile app](#35-android-studio--ide-for-the-mobile-app)
4. [Get the Source Code](#4-get-the-source-code)
5. [Start the Backend (Server-side Services)](#5-start-the-backend-server-side-services)
   - 5.1 [What Docker Compose does](#51-what-docker-compose-does)
   - 5.2 [Run the stack](#52-run-the-stack)
   - 5.3 [Verify every service](#53-verify-every-service)
6. [Set Up Android Studio](#6-set-up-android-studio)
   - 6.1 [Install the Android SDK](#61-install-the-android-sdk)
   - 6.2 [Create a virtual device (emulator)](#62-create-a-virtual-device-emulator)
   - 6.3 [Enable webcam in the emulator (required for face recognition)](#63-enable-webcam-in-the-emulator-required-for-face-recognition)
7. [Build and Install the Android App](#7-build-and-install-the-android-app)
8. [Test the Full System End-to-End](#8-test-the-full-system-end-to-end)
   - 8.1 [Create a course in the admin dashboard](#81-create-a-course-in-the-admin-dashboard)
   - 8.2 [Register as a new student](#82-register-as-a-new-student)
   - 8.3 [Enrol your face](#83-enrol-your-face)
   - 8.4 [Sign in as the student](#84-sign-in-as-the-student)
   - 8.5 [Take attendance (check in)](#85-take-attendance-check-in)
   - 8.6 [View attendance history on the phone](#86-view-attendance-history-on-the-phone)
   - 8.7 [Verify attendance in the admin dashboard](#87-verify-attendance-in-the-admin-dashboard)
9. [Run the Automated Tests](#9-run-the-automated-tests)
10. [Reading Logs and Debugging](#10-reading-logs-and-debugging)
11. [Daily Development Workflow](#11-daily-development-workflow)
12. [Troubleshooting Common Problems](#12-troubleshooting-common-problems)

---

## 1. How the System Works — Big Picture

Before touching any code it helps to understand what each piece does.

```
Your Laptop
│
├── Docker (runs server-side services in containers)
│   ├── FastAPI Backend  (port 8000)  ← the "brain"; stores data, checks identities
│   ├── Streamlit Dashboard (port 8501) ← admin web UI; manage courses, view reports
│   ├── PostgreSQL Database (port 5432) ← stores students, courses, attendance records
│   └── Redis Cache        (port 6379)  ← fast temporary storage for sessions
│
└── Android Emulator / Physical Phone
    └── Attendance App (Kotlin)
        ├── Students register an account and enrol their face (5 angles)
        ├── Students select a course and take attendance by looking at the camera
        └── The app compares the live face against the stored face data
```

**Flow in plain English:**

1. An admin opens the Streamlit dashboard and creates a "CS101" course.
2. A student opens the Android app, registers (name, email, password), then photographs their face from 5 angles. Those photos are converted into a 512-number "face fingerprint" (embedding) and saved to the database.
3. Every day the student opens the app, selects CS101, and looks at the camera. The app generates a fresh face fingerprint, compares it to the stored one, and if they match (≥ 60 % similarity) it records "Present" in the database.
4. The admin checks the dashboard and sees who attended.

---

## 2. What You Will Install

| Tool | Version needed | What it does |
|---|---|---|
| Homebrew | latest | macOS package manager — installs everything else easily |
| Git | any recent | Downloads the source code and tracks changes |
| Docker Desktop | 4.x or newer | Runs the backend services (database, API, dashboard) in isolated containers |
| JDK 17 | exactly 17 | Java Development Kit — required by Android build tools |
| Android Studio | Hedgehog or newer | IDE that builds and runs the Android app |
| Android SDK | API 35 | Android platform libraries used when compiling |

> **Why containers instead of installing PostgreSQL/Redis directly?**
> Containers are isolated boxes. You do not change any system settings and you can tear everything down with one command. It is much safer for a development machine.

---

## 3. Install the Prerequisites

Open **Terminal** (press `⌘ Space`, type `Terminal`, press Enter).

### 3.1 Homebrew — the macOS package manager

Homebrew lets you install developer tools with a single command instead of hunting for
installers online.

```bash
# Paste this entire line and press Enter
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

The installer will ask for your Mac password (nothing is displayed while you type — that
is normal). Follow any on-screen prompts. When it finishes, run:

```bash
brew --version
# Expected output: Homebrew 4.x.x  (any version is fine)
```

> **Apple Silicon (M1/M2/M3 Mac)?** Homebrew installs to `/opt/homebrew`. The installer
> will print a message telling you to add it to your PATH. Run the two `eval` and `export`
> lines it prints, then close and reopen Terminal.

---

### 3.2 Git — version control

Git lets you download (clone) the source code and later pull updates.

```bash
brew install git
```

Verify:

```bash
git --version
# Expected: git version 2.x.x
```

Configure your identity (Git uses this for commit history):

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

---

### 3.3 Docker Desktop — run services without installing them

Docker Desktop runs the database, API, and dashboard inside lightweight virtual containers
so you do not have to install PostgreSQL, Redis, or Python on your Mac.

1. Download from: **https://www.docker.com/products/docker-desktop/**
   - Choose "Mac with Apple Chip" if you have an M1/M2/M3, otherwise "Mac with Intel Chip".
2. Open the downloaded `.dmg` file and drag **Docker** into your Applications folder.
3. Launch Docker from Applications. You will see a whale icon in your menu bar.
4. Accept the license agreement and wait for "Docker Desktop is running" (green dot).

Verify in Terminal:

```bash
docker --version
# Expected: Docker version 25.x.x or newer

docker compose version
# Expected: Docker Compose version v2.x.x
```

> **Why the whale icon matters:** Docker must be running (whale visible in menu bar) every
> time you want to work on this project. If the whale is not there, open Docker from
> Applications first.

---

### 3.4 JDK 17 — Java runtime for Android tooling

Android Studio's build system (Gradle) requires Java 17.

```bash
brew install --cask temurin@17
```

Verify:

```bash
java -version
# Expected first line: openjdk version "17.x.x"
```

> **Important:** If you see version 11 or 21, Android builds may fail. Use `brew` to
> install Temurin 17 specifically as shown above.

---

### 3.5 Android Studio — IDE for the mobile app

1. Download from: **https://developer.android.com/studio**
   - Click "Download Android Studio" — it will auto-detect your Mac type.
2. Open the `.dmg` and drag **Android Studio** into Applications.
3. Launch Android Studio.
4. On first launch a **Setup Wizard** appears. Follow these steps exactly:
   - "Install Type": choose **Standard**
   - Accept all license agreements (scroll down in each dialog before clicking Accept)
   - Let it download the SDK components (this can take 10–20 minutes on a slow connection)
5. When you reach the "Welcome to Android Studio" screen, setup is complete.

---

## 4. Get the Source Code

A "repository" (repo) is a folder that contains all the project files and their history.
`git clone` downloads a copy to your machine.

```bash
# 1. Create a folder to keep your code organised
mkdir -p ~/code
cd ~/code

# 2. Clone (download) the repository
git clone https://github.com/Parimala19/student-attendance.git

# 3. Enter the project folder
cd student-attendance
```

You should now see these top-level folders:

```
student-attendance/
├── android/          ← the Android mobile app
├── backend/          ← the FastAPI server
├── dashboard-streamlit/ ← the admin web UI
├── infra/            ← Docker Compose configuration
└── README.md
```

---

## 5. Start the Backend (Server-side Services)

### 5.1 What Docker Compose does

`infra/docker-compose.yml` is a recipe that tells Docker to start four services:

| Service | Role |
|---|---|
| `db` | PostgreSQL 16 + pgvector — stores all data |
| `redis` | Redis 7 — caches sessions |
| `backend` | FastAPI — the REST API the app talks to |
| `dashboard` | Streamlit — admin web interface |

Docker builds the backend and dashboard from the source code in `backend/` and
`dashboard-streamlit/`. The database and Redis use pre-built images from Docker Hub.

### 5.2 Run the stack

Make sure Docker Desktop is running (whale in menu bar), then:

```bash
# From the root of the project
cd ~/code/student-attendance

# Build images and start all four services in the background (-d = detached)
docker compose -f infra/docker-compose.yml up --build -d
```

What happens step-by-step:
1. Docker downloads PostgreSQL and Redis images from the internet (first time only, ~200 MB).
2. Docker builds the backend image from `backend/Dockerfile` (installs Python dependencies).
3. Docker builds the dashboard image from `dashboard-streamlit/Dockerfile`.
4. All four containers start. The backend waits for the database to be healthy before starting.
5. The backend automatically runs database migrations (creates all the tables).
6. An admin account (`admin@test.com` / `password123`) is seeded if it does not exist.

**This first build takes 3–5 minutes.** Subsequent starts take about 10 seconds.

Check that all containers are running:

```bash
docker compose -f infra/docker-compose.yml ps
```

Expected output — every service should say `running` or `Up`:

```
NAME                    STATUS          PORTS
infra-backend-1         Up              0.0.0.0:8000->8000/tcp
infra-dashboard-1       Up              0.0.0.0:8501->8501/tcp
infra-db-1              Up (healthy)    0.0.0.0:5432->5432/tcp
infra-redis-1           Up (healthy)    0.0.0.0:6379->6379/tcp
```

If any service shows `Exiting` or `Error`, see [Section 12 — Troubleshooting](#12-troubleshooting-common-problems).

### 5.3 Verify every service

Run each of these commands and confirm you get the expected response.

**Backend API health check:**

```bash
curl http://localhost:8000/health
# Expected: {"status":"ok"}
```

**Swagger API documentation (human-readable interactive docs):**

```bash
open http://localhost:8000/docs
# Should open a web page listing all API endpoints
```

**Streamlit admin dashboard:**

```bash
open http://localhost:8501
# Should open a login page in your browser
```

Login with:
- Email: `admin@test.com`
- Password: `password123`

---

## 6. Set Up Android Studio

### 6.1 Install the Android SDK

The Android SDK is a collection of tools and libraries that let you compile code for Android
devices. Android Studio manages it for you.

1. Open Android Studio.
2. Go to **Android Studio → Settings** (macOS: `⌘,`).
3. Navigate to **Languages & Frameworks → Android SDK**.
4. Under **SDK Platforms** tab:
   - Check **Android 15 (API 35)**.
   - Click **Apply** → **OK** → let it download.
5. Under **SDK Tools** tab:
   - Make sure **Android SDK Build-Tools 35** is checked.
   - Make sure **Android Emulator** is checked.
   - Make sure **Android SDK Platform-Tools** is checked.
   - Click **Apply** if anything was unchecked.

### 6.2 Create a virtual device (emulator)

An emulator is a software simulation of an Android phone that runs on your laptop. You do
not need a physical Android device to test the app.

1. In Android Studio, go to **Tools → Device Manager**.
2. Click **+** (or "Create Device").
3. **Choose a device definition:**
   - Category: Phone
   - Select **Pixel 8** (or any recent Pixel model)
   - Click **Next**
4. **Select a system image:**
   - Click the **Recommended** tab
   - Select **API 35 (Android 15, Google APIs)**
   - If you see a download icon next to it, click it and wait for the download to finish
   - Click **Next**
5. **AVD Configuration:**
   - AVD Name: `Pixel8_API35` (or leave the default)
   - Leave all other settings as-is
   - Click **Finish**

### 6.3 Enable webcam in the emulator (required for face recognition)

By default the emulator uses a fake animated camera. For face recognition to work you need
to point it at your laptop's real webcam.

1. In Device Manager, click the **pencil icon** (Edit) next to your emulator.
2. Click **Show Advanced Settings** at the bottom left.
3. Scroll down to the **Camera** section:
   - **Front Camera**: change from `Emulated` to `Webcam0`
   - **Back Camera**: change from `Emulated` to `Webcam0`
4. Click **Finish** to save.
5. **Important:** After changing camera settings you must do a "Cold Boot" (not a normal start).
   - In Device Manager, click the **▼** arrow next to the play button
   - Choose **Cold Boot Now**

> **What is a Cold Boot?** A normal emulator boot resumes from a saved state (like waking a
> sleeping computer). A Cold Boot starts completely fresh and picks up hardware configuration
> changes like the webcam setting.

---

## 7. Build and Install the Android App

### Step 1 — Open the project in Android Studio

```bash
# In Terminal
open -a "Android Studio" ~/code/student-attendance/android
```

Android Studio will open and immediately start **Gradle Sync** — this downloads all the
Android libraries the app depends on. A progress bar appears at the bottom. **Wait for it
to complete** (first time: 3–5 minutes depending on internet speed).

You will know it is done when you see **"Gradle sync finished"** in the status bar and no
spinning indicator.

### Step 2 — Verify the API URL

The app needs to know where the backend is running. For the emulator, the magic address
`10.0.2.2` always maps to your laptop's localhost.

Open `android/app/build.gradle.kts` in Android Studio. Confirm these lines exist:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
```

This is already configured correctly for the emulator. If you are using a **physical phone**
on the same Wi-Fi, see [Physical device setup](#option-b-physical-android-device) at the
end of this section.

### Step 3 — Start the emulator

1. In Device Manager click the **▶ play button** next to your `Pixel8_API35` emulator.
2. Wait for the Android lock screen to appear (takes 1–3 minutes on first start).
3. Click the screen and drag up to unlock.

### Step 4 — Run the app

In Android Studio, click the **green ▶ Run button** in the toolbar (or press `Control + R`).

Android Studio will:
1. Compile the Kotlin source code into an APK (Android Package file).
2. Install the APK on the running emulator.
3. Launch the app automatically.

You should see the **Welcome screen** with "Sign In" and "Register" buttons.

> **Build takes too long?** The first build compiles everything from scratch and can take
> 3–5 minutes. Subsequent builds only recompile changed files and take 10–30 seconds.

---

### Option B: Physical Android Device

If you prefer to test on a real phone instead of the emulator:

**1. Enable Developer Options on the phone:**
- Go to **Settings → About Phone**
- Tap **Build Number** exactly 7 times rapidly
- You will see a toast: "You are now a developer!"

**2. Enable USB Debugging:**
- Go to **Settings → Developer Options** (now visible)
- Toggle **USB Debugging** ON

**3. Connect the phone to your Mac with a USB cable.**
- On the phone, tap **Allow** when it asks "Allow USB Debugging from this computer?"

**4. Verify the phone is detected:**

```bash
adb devices
# Expected output:
# List of devices attached
# ABC123XYZ        device
```

If you see `unauthorized` instead of `device`, check the phone for another permission
dialog and tap Allow.

**5. Find your Mac's IP address:**

```bash
ipconfig getifaddr en0
# Example output: 192.168.1.105
```

**6. Update the API URL** in `android/app/build.gradle.kts`:

```kotlin
// Change this line inside the debug block:
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.105:8000\"")
// Replace 192.168.1.105 with your actual IP from the command above
```

**7. Rebuild and install:**

```bash
cd ~/code/student-attendance/android
./gradlew clean installDebug
```

> **Important:** Your phone and Mac must be on the **same Wi-Fi network**. A corporate
> network that isolates devices from each other will not work; use a home network or a
> personal hotspot.

---

## 8. Test the Full System End-to-End

This section walks through the entire user journey from admin setup to attendance recording.

### 8.1 Create a course in the admin dashboard

Before a student can check in, a course must exist.

1. Open **http://localhost:8501** in your browser.
2. Log in: email `admin@test.com`, password `password123`.
3. Navigate to **Courses** in the sidebar.
4. Click **Add Course** (or the equivalent button) and fill in:
   - Course ID: `CS101`
   - Course Name: `Introduction to Computer Science`
   - Teacher: (leave default or enter a teacher name)
5. Save the course.

### 8.2 Register as a new student

On the Android emulator:

1. Tap **Register** on the Welcome screen.
2. Fill in the registration form:
   - **Student Number:** `STU001`
   - **Full Name:** `Test Student`
   - **Email:** `student@test.com`
   - **Password:** `password123`
3. Tap **Register**.
4. If registration is successful, the app immediately moves to the Face Enrolment screen.

> **Why face enrolment happens right after registration?** The system needs a reference of
> your face before it can recognise you during check-in. Without enrolment, check-in cannot
> work.

### 8.3 Enrol your face

The enrolment screen captures 5 photos of your face from different angles so the system can
recognise you in various lighting conditions and positions.

1. Grant the camera permission when the popup appears. Tap **Allow**.
2. Position your face inside the oval guide on screen.
3. Wait for the guide to turn green — this means a face has been detected.
4. Tap the **Capture** button.
5. The app will prompt you for each angle in sequence:
   - **Front** — look straight at the camera
   - **Left** — turn your head slightly left
   - **Right** — turn your head slightly right
   - **Up** — tilt your chin up slightly
   - **Down** — tilt your chin down slightly
6. After all 5 captures, tap **Done / Save**.

> **What happens behind the scenes?**
> Each captured photo is cropped to just the face, then a mathematical "fingerprint"
> (512 numbers) is generated for it using TensorFlow Lite. These fingerprints are uploaded
> to the backend and stored in the database. They are also cached on the device for offline
> use.

### 8.4 Sign in as the student

After enrolment, the app navigates to the Login screen.

1. Enter email: `student@test.com`
2. Enter password: `password123`
3. Tap **Sign In**.
4. You should land on the **Course Selection** screen listing `CS101`.

### 8.5 Take attendance (check in)

1. Tap **CS101** on the Course Selection screen.
2. The **Check-In** screen opens with a live camera preview.
3. Position your face inside the oval. Wait for the "Face detected" indicator.
4. Tap **Check In**.
5. The app will:
   - Capture a photo of your face
   - Generate a fresh face fingerprint
   - Compare it against your 5 stored fingerprints using cosine similarity
   - If the best match score ≥ 0.60 (60%), record attendance as "Present"
6. You will see a success message with the confidence score (e.g., "Checked in — 87%").

> **Check-in failing with "Face not recognised"?**
> - Ensure you are in good lighting.
> - Hold the camera at eye level.
> - If you are on the emulator, make sure the webcam is configured (Section 6.3) and you
>   did a Cold Boot.

### 8.6 View attendance history on the phone

1. On the Course Selection screen, tap the **History** icon or button (usually in the
   top-right or bottom navigation).
2. You will see a list of past check-ins showing:
   - Course name
   - Date and time
   - Status (Present / Late)
   - Confidence score

### 8.7 Verify attendance in the admin dashboard

1. Open **http://localhost:8501**.
2. Go to **Attendance** in the sidebar.
3. Select course **CS101**.
4. You should see the check-in record for `Test Student` with a timestamp and confidence
   score.

Congratulations — you have just run the full end-to-end flow!

---

## 9. Run the Automated Tests

The backend has a test suite that verifies every API endpoint works correctly. Run it after
making code changes to catch regressions early.

### Set up the Python test environment

The tests run against Python installed inside a virtual environment — an isolated Python
install that does not affect your Mac's system Python.

```bash
cd ~/code/student-attendance/backend

# Install uv (a fast Python package manager)
brew install uv

# Create a virtual environment and install all dependencies
uv sync
```

### Run unit tests (do not require Docker)

Unit tests check individual functions in isolation. They use a lightweight in-memory SQLite
database instead of the real PostgreSQL.

```bash
cd ~/code/student-attendance/backend
uv run pytest tests/ -v --ignore=tests/integration
```

Expected output: a list of test names each ending in `PASSED`.

### Run integration tests (require the Docker stack to be running)

Integration tests send real HTTP requests to the running backend and verify the responses.
The Docker stack must be up before running these.

```bash
# Make sure Docker stack is running (see Section 5.2)
curl http://localhost:8000/health   # should return {"status":"ok"}

# Run integration tests
cd ~/code/student-attendance/backend
uv run pytest tests/integration -v
```

### What do the tests cover?

| Test file | What it checks |
|---|---|
| `test_auth.py` | Login, token generation, invalid credentials |
| `test_students.py` | Register a student, fetch student data |
| `test_courses.py` | Create and list courses |
| `test_attendance.py` | Check-in endpoint, history retrieval |
| `integration/test_backend_http.py` | Full HTTP round-trips against running backend |

---

## 10. Reading Logs and Debugging

Logs are the most important debugging tool. When something goes wrong, always check the
logs first.

### Backend API logs

```bash
# Live log stream (press Ctrl+C to stop)
docker compose -f infra/docker-compose.yml logs -f backend
```

You will see every HTTP request, database query, and error. Look for lines starting with
`ERROR` or `CRITICAL`.

### Streamlit dashboard logs

```bash
docker compose -f infra/docker-compose.yml logs -f dashboard
```

### All services at once

```bash
docker compose -f infra/docker-compose.yml logs -f
```

### Android app logs

The Android app writes detailed logs that you can read in real-time.

**In Android Studio:**
1. Click **Logcat** at the bottom of the screen.
2. In the search bar, type `com.attendance.app` to filter to just the app's logs.
3. Reproduce the issue and watch the red `E` (error) lines appear.

**From Terminal:**

```bash
# Show all logs from the app
adb logcat | grep "com.attendance.app"

# Show only error-level logs
adb logcat *:E

# Filter to specific tag (e.g., face detection)
adb logcat | grep "FaceDetection"
adb logcat | grep "CheckIn"
adb logcat | grep "EmbeddingGenerator"
```

### Inspecting the database directly

Sometimes you need to look at what is actually stored in the database.

```bash
# Open a PostgreSQL prompt inside the database container
docker compose -f infra/docker-compose.yml exec db psql -U postgres -d attendance

# Useful queries (type these at the postgres=# prompt):
\dt                          -- list all tables
SELECT * FROM students;      -- see all registered students
SELECT * FROM courses;       -- see all courses
SELECT * FROM attendance_records ORDER BY created_at DESC LIMIT 10;
SELECT * FROM face_embeddings;  -- see stored face data
\q                           -- quit
```

### Testing API endpoints manually

The backend has interactive documentation at **http://localhost:8000/docs**. You can click
any endpoint, fill in the parameters, and click "Execute" to see the real response — no
Android app needed. This is very useful for isolating whether a bug is in the app or the
backend.

---

## 11. Daily Development Workflow

Once everything is set up, your daily workflow will be:

### Morning — start everything up

```bash
# 1. Make sure Docker Desktop is running (whale in menu bar)

# 2. Start the backend services (fast after the first build)
cd ~/code/student-attendance
docker compose -f infra/docker-compose.yml up -d

# 3. Start the Android emulator
# In Android Studio: Tools → Device Manager → ▶ play button

# 4. Run the app
# In Android Studio: click the green ▶ Run button
```

### After changing backend code

When you edit Python files in `backend/`, you need to rebuild and restart the backend
container:

```bash
docker compose -f infra/docker-compose.yml build backend
docker compose -f infra/docker-compose.yml up -d backend
```

> **Why rebuild?** Docker caches a snapshot of your code. Simply restarting the container
> uses the old snapshot. You must rebuild to bake the new code into the image.

### After changing Android code

Android Studio will hot-reload most changes automatically when you click **Apply Changes**
(lightning bolt icon). For more substantial changes (new dependencies, manifest changes),
do a full rebuild with the green ▶ Run button.

### End of day — shut down

```bash
docker compose -f infra/docker-compose.yml down
# The emulator can just be closed
```

Your database data is preserved in a Docker volume named `pgdata` — it persists across
`down` / `up` cycles.

### To wipe all data and start fresh

```bash
docker compose -f infra/docker-compose.yml down -v
# -v removes volumes, including the database — everything is deleted
docker compose -f infra/docker-compose.yml up --build -d
```

---

## 12. Troubleshooting Common Problems

### Docker Issues

---

**Problem:** `docker compose up` fails with "Cannot connect to Docker daemon"

**Cause:** Docker Desktop is not running.

**Fix:** Open Docker Desktop from Applications and wait for the green "running" indicator,
then try again.

---

**Problem:** The `backend` container exits immediately with code 1

**Fix:** Read the logs:

```bash
docker compose -f infra/docker-compose.yml logs backend
```

Common sub-causes:
- **"could not connect to server"** — database is not yet healthy; wait 30 seconds and
  run `up -d` again.
- **"password authentication failed"** — the database URL in docker-compose.yml does not
  match the credentials. Make sure you have not edited those values.

---

**Problem:** Port already in use — `bind: address already in use`

Another application is using port 8000, 8501, 5432, or 6379.

```bash
# Find what is using port 8000
lsof -i :8000

# Kill it (replace 12345 with the actual PID shown)
kill -9 12345
```

Then run `docker compose up -d` again.

---

### Android Build Issues

---

**Problem:** "SDK location not found" when opening the project

**Fix:**

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > ~/code/student-attendance/android/local.properties
```

Then in Android Studio: **File → Sync Project with Gradle Files**.

---

**Problem:** Gradle sync fails with "Could not resolve …"

**Cause:** Network issue or stale cache.

**Fix:**

```bash
cd ~/code/student-attendance/android
./gradlew --refresh-dependencies clean
```

Then sync again in Android Studio.

---

**Problem:** "Unsupported class file major version" during build

**Cause:** Wrong Java version (not 17).

**Fix:**

```bash
java -version  # should say 17.x
```

If it shows something else:

```bash
brew install --cask temurin@17
```

Then in Android Studio: **File → Project Structure → SDK Location → JDK Location** →
set to the Temurin 17 path (usually `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`).

---

**Problem:** App installs but shows a white/blank screen on launch

**Cause:** The app crashed before rendering anything.

**Fix:** Check Logcat for a red `FATAL EXCEPTION` line. The most common causes are:
- **Camera permission not granted**: Go to Settings → Apps → Attendance → Permissions →
  enable Camera and Microphone.
- **Emulator camera not configured**: Revisit Section 6.3 and do a Cold Boot.

---

### Android App + Backend Connectivity

---

**Problem:** App shows "Cannot connect to server" or requests time out

**Step 1:** Verify the backend is reachable from inside the emulator:

```bash
adb shell
# Now you are inside the emulator's shell
curl http://10.0.2.2:8000/health
# Expected: {"status":"ok"}
exit
```

**Step 2:** If Step 1 fails, confirm the backend container is running:

```bash
docker compose -f infra/docker-compose.yml ps
curl http://localhost:8000/health
```

**Step 3:** Check your Mac's firewall. Go to **System Settings → Network → Firewall** and
make sure it is not blocking incoming connections, or add an exception for Docker.

---

**Problem:** Registration succeeds but check-in says "Face not recognised"

**Cause:** Either the enrolment was not saved correctly, or the live face does not match
well enough (lighting, angle, distance).

**Debugging steps:**

1. Check that embeddings were saved:

```bash
docker compose -f infra/docker-compose.yml exec db psql -U postgres -d attendance \
  -c "SELECT student_id, capture_angle, created_at FROM face_embeddings ORDER BY created_at DESC LIMIT 10;"
```

You should see 5 rows (one per angle) for the student.

2. If no rows: the enrolment upload failed. Check the backend logs during enrolment.

3. If rows exist: the similarity score is below 0.60. Try:
   - Better lighting (face a bright light source)
   - Hold the phone/emulator camera at face height
   - Re-enrol (clear app data, register again)

---

**Problem:** Attendance history is empty after a successful check-in

**Check the backend directly:**

```bash
docker compose -f infra/docker-compose.yml exec db psql -U postgres -d attendance \
  -c "SELECT * FROM attendance_records ORDER BY created_at DESC LIMIT 5;"
```

If records exist in the database but not in the app, it is a display bug — check the
Android Logcat for errors in `HistoryViewModel`.

---

### Streamlit Dashboard

---

**Problem:** Dashboard shows a blank page or "Connection refused"

```bash
# Check if the container is running
docker compose -f infra/docker-compose.yml ps dashboard

# View dashboard logs
docker compose -f infra/docker-compose.yml logs dashboard

# Restart it
docker compose -f infra/docker-compose.yml restart dashboard
```

---

**Problem:** Login to dashboard fails with "Invalid credentials"

The admin account is seeded automatically on first backend startup. If the backend restarted
before the seed ran:

```bash
docker compose -f infra/docker-compose.yml logs backend | grep -i "seed\|admin"
```

If the seed failed, restart the backend:

```bash
docker compose -f infra/docker-compose.yml restart backend
```

Wait 15 seconds, then try logging in again.

---

## Quick Reference Card

```bash
# Start everything
docker compose -f infra/docker-compose.yml up --build -d

# Stop everything
docker compose -f infra/docker-compose.yml down

# View live logs
docker compose -f infra/docker-compose.yml logs -f

# Rebuild backend after code changes
docker compose -f infra/docker-compose.yml build backend && \
docker compose -f infra/docker-compose.yml up -d backend

# Open database shell
docker compose -f infra/docker-compose.yml exec db psql -U postgres -d attendance

# Run backend tests
cd backend && uv run pytest -v

# View Android logs
adb logcat | grep "com.attendance.app"

# URLs
# API docs:     http://localhost:8000/docs
# Dashboard:    http://localhost:8501
# Admin:        admin@test.com / password123
```

---

You are now fully set up. If you hit a problem not listed here, read the logs first — they
almost always tell you exactly what went wrong.
