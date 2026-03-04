# AlertyAI 🤖✅

AlertyAI is a premium, AI-powered task management assistant for Android. It combines local efficiency with cloud intelligence, allowing you to manage your life using voice, images, and smart chat.

## 🚀 Key Features

### 🔐 Modern Authentication
- **One-Tap Google Sign-In:** Seamlessly log in using Android's native Credential Manager.
- **Secure JWT Auth:** Communicates with a Python/FastAPI backend via secure tokens.

### 🧠 AI-Driven Task Creation
- **Smart Chat:** Interact with an AI assistant to manage your schedule.
- **Voice-to-Task:** Record your thoughts (minimum 2s) and let Sarvam AI transcribe and categorize them.
- **Image OCR:** Snap a photo or upload a screenshot to extract actionable tasks instantly.
- **Natural Language Parsing:** Simply type "Meeting on Friday at 5pm" and the AI handles the rest.

### 📋 Enhanced Task Management
- **Detailed Scheduling:** Set specific due dates and times with native pickers.
- **Smart Alarms:** Configure alerts with custom reminder offsets (At time, 10m, 30m, 1hr).
- **Location Awareness:** Attach places to your tasks for better context.
- **Subtasks & Checklists:** Break down complex goals into actionable steps with live progress tracking.
- **Backend Sync:** Automatically merges tasks created via Chat or the webapp with your local Room database.

## 🛠 Tech Stack

- **UI:** Jetpack Compose (Material 3)
- **Language:** Kotlin (100%)
- **Architecture:** MVVM with Clean Architecture patterns
- **Database:** Room (Local SQLite + Migration Support)
- **Dependency Injection:** Dagger Hilt
- **Networking:** Retrofit + OkHttp
- **AI Services:** Sarvam AI (Voice), Groq/OpenAI (Chat/Parsing)

## 📲 Installation

1. Download the latest APK: `app/build/outputs/apk/debug/app-debug.apk`
2. Enable "Install from Unknown Sources" on your Android device.
3. Install and launch **AlertyAI**.

## 🏗 Build Instructions

1. **Clone the repository.**
2. **Open in Android Studio** (Ladybug or newer).
3. **Sync Gradle** and ensure you have the Android SDK 34+.
4. **Run the Backend:** Ensure the AlertyAI Python backend is running at `http://0.0.0.0:8000`.
5. **Build APK:**
   ```bash
   ./gradlew assembleDebug
   ```

## 🔐 Security & Privacy
- **Hybrid Storage:** Local tasks stay on-device in Room; synced tasks are stored securely in MongoDB via the backend.
- **OAuth 2.0:** Secure authentication via Google Identity.
- **OCR:** Optimized image processing with ML Kit.

---
*Developed for AlertyAI Ecosystem*
