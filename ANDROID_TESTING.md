# Android App Testing Guide

## Current Status

⚠️ **The Android app is scaffolded but source code is not yet implemented.**

The project structure, Gradle configuration, and build system are ready, but the Kotlin source files need to be created.

## Options for Testing

### Option 1: Complete Android Implementation (Estimated 8-12 hours)

Build the full Android app with:
- Authentication screens
- Face capture with CameraX
- ML Kit face detection
- TensorFlow Lite embedding generation
- Check-in flow
- Attendance history

**Pros:**
- Complete mobile experience
- Real face recognition testing
- Production-ready app

**Cons:**
- Significant time investment
- Requires ML model integration
- Complex camera/ML Kit setup

### Option 2: Use Streamlit Dashboard on Mobile (Quick Demo)

Access the Streamlit dashboard from a mobile browser.

**Steps:**
1. Start backend services: `./quick-start.sh`
2. Find your computer's IP: `ipconfig getifaddr en0`
3. On mobile browser: `http://YOUR_IP:8501`
4. Login and test features

**Pros:**
- Immediate testing
- No app build required
- Works on any device/platform

**Cons:**
- Not native mobile experience
- Limited camera integration
- Desktop-first UI

### Option 3: Build Minimal Android App (Estimated 2-4 hours)

Create a simplified Android app with:
- Login screen
- Course list
- Manual check-in (no face recognition)
- History view

**Pros:**
- Native Android experience
- Faster development
- Still demonstrates concept

**Cons:**
- No face recognition (core feature missing)
- Limited functionality

### Option 4: Use Existing Tools for Demo

Create a demo video or presentation showing:
- Backend API working (Swagger UI)
- Streamlit dashboard functionality
- Mock face recognition flow

**Pros:**
- Fastest option
- Professional presentation
- Focus on architecture/design

**Cons:**
- Not interactive
- Can't test edge cases
- Less impressive for stakeholders

## Recommendation for Demo

### Best Approach: Hybrid Testing

1. **Backend + Dashboard Testing** (Ready Now)
   - Use `./quick-start.sh` to start services
   - Test all features via Streamlit dashboard
   - Create students, courses, attendance records
   - Demonstrate reporting and analytics

2. **Mobile UI Demo** (Quick)
   - Access Streamlit on mobile browser
   - Show responsive design
   - Demonstrate mobile workflow

3. **Future Enhancement** (Post-Demo)
   - Build full Android app if project moves forward
   - Implement face recognition
   - Add offline capabilities

## Android Emulator Setup (For Future Development)

### Prerequisites

```bash
# Check if Android Studio is installed
ls "/Applications/Android Studio.app" || echo "Android Studio not found"

# Check Java version (needs 17+)
java -version

# Check Android SDK
echo $ANDROID_HOME
```

### Create Emulator

1. **Open Android Studio**
```bash
open "/Applications/Android Studio.app"
```

2. **Create AVD:**
   - Tools → Device Manager → Create Device
   - Select: **Pixel 8 Pro** (or similar modern device)
   - System Image: **API 34 (Android 14)** - Download if needed
   - Advanced Settings:
     - RAM: 4096 MB
     - Internal Storage: 2048 MB
     - Camera: Emulated (for face testing)
   - Click "Finish"

3. **Launch Emulator:**
```bash
# List available emulators
emulator -list-avds

# Start specific emulator
emulator -avd Pixel_8_Pro_API_34 &

# Or from Android Studio Device Manager
```

### Configure Network for Testing

**Emulator Network Setup:**

The emulator uses `10.0.2.2` to access host machine's localhost:

```kotlin
// In ApiConfig.kt or build.gradle.kts
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
```

**Test Connectivity:**

```bash
# From terminal, connect to emulator
adb shell

# Inside emulator shell
curl http://10.0.2.2:8000/health

# Should return: {"status":"ok"}
```

## Physical Device Setup

### Enable Developer Mode

1. **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Enter PIN/password
4. Go back → **Developer Options** now visible
5. Enable **USB Debugging**

### Connect Device

```bash
# Connect phone via USB

# Check device is recognized
adb devices

# Should show:
# List of devices attached
# ABC123XYZ    device

# If unauthorized, check phone for USB debugging prompt
```

### Configure API URL for Physical Device

**Find your computer's IP:**

```bash
# macOS
ipconfig getifaddr en0

# Example output: 192.168.1.100
```

**Update Android app config:**

```kotlin
// For physical device on same WiFi network
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8000\"")
```

**Test from phone browser:**
- Open Chrome on phone
- Navigate to: `http://192.168.1.100:8000/docs`
- Should see FastAPI Swagger UI

## Building Android App (When Source Code is Ready)

### Debug Build

```bash
cd attendance-app/android

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk

# Install to connected device/emulator
./gradlew installDebug

# Or run directly
./gradlew installDebug
adb shell am start -n com.attendance.app/.MainActivity
```

### Release Build (For Distribution)

```bash
# Build release APK (unsigned)
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install APK Manually

```bash
# To emulator
adb -e install app/build/outputs/apk/debug/app-debug.apk

# To physical device
adb -d install app/build/outputs/apk/debug/app-debug.apk

# To specific device (if multiple connected)
adb -s ABC123XYZ install app/build/outputs/apk/debug/app-debug.apk
```

## Testing Workflow (Once App is Built)

### 1. Start Backend Services

```bash
./quick-start.sh
```

### 2. Configure App

Update API URL in app for your testing method:
- Emulator: `http://10.0.2.2:8000`
- Physical Device: `http://YOUR_COMPUTER_IP:8000`

### 3. Build and Install

```bash
cd android
./gradlew installDebug
```

### 4. Test Flow

1. **Registration:**
   - Open app
   - Tap "Register"
   - Enter student details
   - Skip face capture (or mock it)
   - Complete registration

2. **Login:**
   - Enter email/password
   - Verify JWT token stored

3. **View Courses:**
   - See list of available courses
   - (Must be created in dashboard first)

4. **Check-In:**
   - Select a course
   - Trigger check-in
   - Verify attendance recorded

5. **View History:**
   - See past attendance records
   - Check status and timestamps

### 5. Verify in Dashboard

Open `http://localhost:8501`:
- Students page → New student appears
- Attendance page → Check-in record visible
- Reports → Statistics updated

## Troubleshooting

### Emulator Won't Start

```bash
# Check running emulators
adb devices

# Kill all emulators
adb kill-server
adb start-server

# Check system resources (emulator needs 4GB+ RAM free)
```

### Build Errors

```bash
# Sync Gradle
./gradlew --refresh-dependencies

# Clear cache
./gradlew clean
rm -rf .gradle build

# Check Java version
java -version  # Should be 17+

# Check Android SDK
echo $ANDROID_HOME
```

### App Can't Connect to Backend

```bash
# Test from emulator shell
adb shell
curl http://10.0.2.2:8000/health

# Check firewall (macOS)
# System Preferences → Security & Privacy → Firewall
# Allow Python/uvicorn

# Check backend logs
docker-compose -f infra/docker-compose.yml logs backend
```

### ADB Not Found

```bash
# Add to PATH (macOS)
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools

# Add to ~/.zshrc or ~/.bash_profile to make permanent
```

## Next Steps

### For Immediate Demo

1. ✅ Run `./quick-start.sh`
2. ✅ Test via Streamlit dashboard
3. ✅ Access on mobile browser for mobile demo
4. ✅ Prepare presentation slides

### For Full Android Development

1. ⏳ Implement authentication screens
2. ⏳ Integrate CameraX for face capture
3. ⏳ Add ML Kit face detection
4. ⏳ Implement TFLite inference
5. ⏳ Build check-in flow
6. ⏳ Add offline support

### For Production

1. ⏳ Sign APK with release keystore
2. ⏳ Enable ProGuard/R8 obfuscation
3. ⏳ Remove debug features
4. ⏳ Test on multiple devices
5. ⏳ Upload to Google Play Console

## Recommendation

**For this demo project, I recommend:**

1. **Use Streamlit Dashboard** for full feature testing (Ready now)
2. **Access on mobile browser** to show mobile experience
3. **Create demo video** showing the Android app concept
4. **Build full Android app** only if project gets funded/approved

This approach lets you demonstrate the complete system immediately while keeping the door open for native mobile development later.
