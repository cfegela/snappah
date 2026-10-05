# Snappah 📸

A minimal, distraction-free point-and-shoot camera application for Android built with **Jetpack Compose** and **CameraX**.

Snappah strips away the clutter of modern camera apps—no menus, no sliders, no filters, and no intrusive overlays. It delivers an instant, analog-inspired photography experience focused entirely on framing and capturing the shot.

---

## Features

- **Distraction-Free UI**: Pure black OLED-friendly interface with an unadorned viewfinder and a signature tactile shutter button styled to match the app icon.
- **Native Sensor Framing**: 3:4 aspect ratio viewfinder vertically centered with letterboxing, displaying the uncropped native sensor view.
- **WYSIWYG 15% Saturation Boost**:
  - **Live Preview**: Real-time hardware-accelerated 15% saturation boost (`1.15f`) applied directly to the viewfinder at full sensor frame rates.
  - **Capture Pipeline**: Matching 15% saturation boost applied to captured photos prior to saving, preserving authentic colors, orientation, and full EXIF camera metadata.
- **Double-Tap Lens Switch**: Quick double-tap anywhere on the viewfinder to swap between rear and front-facing cameras, complete with haptic feedback and smooth stream transition masking.
- **Multisensory Shutter Feedback**:
  - **Haptics**: Tactile feedback on shutter tap.
  - **Audio**: Vintage mechanical shutter sound played via `SoundPool`.
  - **Visual**: Subtle 60ms shutter flash animation confirming image capture.
  - **Custom Shutter Button**: Custom Compose Canvas shutter button featuring a solid white base, blue accent ring, and blue core mirroring the app icon.
- **Low-Latency Capture**: Utilizes CameraX `CAPTURE_MODE_MINIMIZE_LATENCY` for quick snapshot response.
- **Standard MediaStore Storage**: Photos are saved directly to public device storage under `DCIM/Snappah/SNAP_<timestamp>.jpg` at 98% JPEG quality.
- **Graceful Permission Management**: Integrated runtime permission requesting flow with a fallback screen to prompt or re-grant camera access.

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (JVM target 17)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Camera Pipeline**: [AndroidX CameraX 1.4.0](https://developer.android.com/training/camerax)
  - `androidx.camera:camera-camera2`
  - `androidx.camera:camera-lifecycle`
  - `androidx.camera:camera-view`
- **Image & Color Processing**: Android `ColorMatrix` / `ColorMatrixColorFilter` hardware rendering for both live preview layer and capture bitmap encoding with EXIF preservation
- **Audio**: Android `SoundPool` for low-latency playback of custom shutter audio
- **Build System**: Gradle 8.10.2 (Kotlin DSL)
- **SDK Compatibility**:
  - `minSdk`: **26** (Android 8.0 Oreo)
  - `targetSdk`: **35** (Android 15)
  - `compileSdk`: **35**

---

## Project Structure

```text
snappah/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml           # Camera permissions & portrait activity config
│   │       ├── java/com/example/snappah/
│   │       │   ├── MainActivity.kt           # Lifecycle, permission requests, edge-to-edge
│   │       │   └── ui/
│   │       │       ├── CameraScreen.kt       # Viewfinder, saturation pipeline, shutter button
│   │       │       └── theme/                # Compose theme, typography, and Snappah colors
│   │       └── res/
│   │           ├── drawable/                 # Launcher background and foreground vectors
│   │           ├── raw/shutter_sound.wav     # Mechanical shutter sound effect
│   │           ├── values/                   # Strings, theme configuration
│   │           └── mipmap/                   # Adaptive launcher icon definitions
│   └── build.gradle.kts                      # App-level dependencies and Android configuration
├── gradle/wrapper/                           # Gradle wrapper binaries & configuration
├── build.gradle.kts                          # Root build script
├── settings.gradle.kts                       # Repository & project module settings
├── .gitignore                                # Excludes build artifacts, caches, and local SDK paths
└── README.md                                 # Project documentation
```

---

## Getting Started

### Prerequisites

- **JDK**: Java Development Kit 17 or later
- **Android SDK**: API 35 (Android SDK Platform & Command-line Tools / Android Studio)
- **Device / Emulator**: Physical Android device recommended (with camera hardware) running Android 8.0+ (API 26+)

### Setting up the Environment

Create a `local.properties` file in the project root pointing to your local Android SDK (if not already set in your environment as `ANDROID_HOME` or `ANDROID_SDK_ROOT`):

```properties
sdk.dir=/path/to/your/android-sdk
```

*(Note: `local.properties` is machine-specific and excluded by `.gitignore`)*

### Building the App

To build a debug APK using the Gradle wrapper:

```bash
./gradlew assembleDebug
```

The compiled APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Installing and Running

1. Connect your Android device via USB with USB Debugging enabled.
2. Install directly via Gradle:

```bash
./gradlew installDebug
```

Or install the built APK via `adb`:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch **Snappah** from your app drawer.

---

## Permissions

The app requests:
- `android.permission.CAMERA`: Required to display the camera preview and capture photos.
- `android.permission.WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`): Legacy storage support on Android 9 and lower (Android 10+ uses scoped `MediaStore`).

---

## License

This project is licensed under the terms of the project repository.
