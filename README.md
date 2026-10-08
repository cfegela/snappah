# <img src="assets/icon.png" width="40" height="40" alt="Snappah Icon" valign="middle"> Snappah

A minimal, distraction-free point-and-shoot camera app for Android built with **Jetpack Compose** and **CameraX**.

Snappah strips away the clutter of modern camera apps — no menus, no sliders, no filters. Just frame the shot and press the button.

---

## Features

### Photo
- **High-quality capture** via CameraX `CAPTURE_MODE_MAXIMIZE_QUALITY` enabling hardware multi-frame noise reduction (MFNR) on the camera ISP for superior low-light performance
- **WYSIWYG 15% saturation boost** applied to both the live preview and saved JPEG for consistent, vivid color
- **98% quality JPEG** saved to `Pictures/simplah/SNAP_<timestamp>.jpg` via the `MediaStore` API with full EXIF preservation
- **Multisensory shutter feedback**: haptic pulse + vintage mechanical shutter sound + 60ms white flash animation

### Photo Viewer
- **Swipe-to-view**: Swipe left anywhere on the screen to fluidly glide into the viewer screen via a Compose `HorizontalPager`
- **Instant latest photo**: Displays the last captured photo from the current session or queries `MediaStore` for the most recent photo in `Pictures/simplah`
- **Efficient decoding**: Rendered with Android's `ImageDecoder` using target sample downsampling to conserve RAM while preserving full EXIF rotation
- **Upper-right delete button**: Positioned safely in the top-right header to avoid accidental taps; deletes immediately with haptic feedback and automatically displays the next most recent photo (or empty state if none remain)
- **Fluid return**: Swipe right, tap the top-left back button, or use the system back gesture (`BackHandler`) to return instantly to the camera
- **Gesture safety**: Pager swiping is locked during active video recording to prevent accidental navigation

### Video
- **Long-press** the shutter button to start recording
- **Tap** the shutter button to stop
- Video saved to `Pictures/simplah/SNAP_VID_<timestamp>.mp4` at the highest quality available
- **Beep on start and stop** so you always know recording state without looking at the screen
- Button turns **red** while recording and snaps back to **blue** the instant you stop

### Viewfinder & Controls
- **3:4 aspect ratio** native sensor viewfinder, vertically centered with letterboxing
- **Single-tap** anywhere on the viewfinder for instant focus and exposure metering (with a compact circular animated reticle, haptic feedback, and a 3-second auto-reset)
- **Upper-right selfie camera toggle**: Dedicated button positioned in the upper-right corner above the viewfinder to seamlessly flip between rear and front cameras (smoothly hidden during video recording)
- Pure black OLED-friendly interface with minimal chrome
- Off-screen camera preview preloaded (`beyondViewportPageCount = 1`) for zero startup lag or black flicker when swiping back from the viewer

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language | Kotlin (JVM target 17) |
| UI | Jetpack Compose + Material 3 |
| Camera | AndroidX CameraX 1.6.2 (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-video`) |
| Color processing | Android `ColorMatrix` / `ColorMatrixColorFilter` on hardware layer |
| Audio | `SoundPool` (shutter click) · `ToneGenerator` (record start/stop beeps) |
| Build | Gradle 8.11.1 (Kotlin DSL) · Android Gradle Plugin 8.9.1 |

**SDK targets**

| | API | Android version |
| :--- | :--- | :--- |
| `minSdk` | 26 | Android 8.0 Oreo |
| `targetSdk` / `compileSdk` | 36 | Android 16 |

---

## Permissions

| Permission | Why |
| :--- | :--- |
| `CAMERA` | Preview and capture |
| `RECORD_AUDIO` | Video recording with audio |
| `READ_MEDIA_IMAGES` *(minSdkVersion 33)* | View and manage previously captured photos on Android 13+ |
| `READ_EXTERNAL_STORAGE` *(maxSdkVersion 32)* | View previously captured photos on Android 12 and below |
| `WRITE_EXTERNAL_STORAGE` *(maxSdkVersion 28)* | Legacy storage on Android 9 and below |

All permissions are requested at runtime on first launch. Camera access is required; microphone access is required for video with audio.

---

## Compatibility

Snappah runs on any Android device released since late 2017:

- **Google Pixel** 2 through Pixel 10 (Pro, XL, Fold, a-series)
- **Samsung Galaxy** S8 / Note 8 and newer (S-series, Z Fold / Z Flip, A-series)
- **Motorola, OnePlus, Xiaomi, Nothing Phone, Sony Xperia, Asus, Oppo, Vivo** — any model on Android 8.0+

**Storage**: Android 10+ uses the scoped `MediaStore` API with no extra permission prompts for newly captured photos. Android 8–9 uses the legacy `WRITE_EXTERNAL_STORAGE` path.

---

## Project Structure

```text
snappah/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml           # Permissions & portrait activity config
│   │   ├── java/com/example/snappah/
│   │   │   ├── MainActivity.kt           # Lifecycle, runtime permissions, edge-to-edge
│   │   │   └── ui/
│   │   │       ├── CameraScreen.kt       # Viewfinder, photo/video capture, pager container
│   │   │       ├── PhotoViewerScreen.kt  # MediaStore query, image viewer, delete action
│   │   │       └── theme/                # Compose theme & Snappah brand colors
│   │   └── res/
│   │       ├── raw/shutter_sound.wav     # Mechanical shutter sound effect
│   │       ├── drawable/                 # Launcher icon vectors
│   │       ├── mipmap/                   # Adaptive icon definitions
│   │       └── values/                   # Strings & theme config
│   └── build.gradle.kts                  # Dependencies & Android config
├── assets/
│   ├── icon.png
│   └── icon.svg
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Getting Started

### Prerequisites

- **JDK 17+** — Homebrew: `brew install openjdk@17`
- **Android SDK** (API 36) — via Android Studio or command-line tools
- **Physical Android device** with USB Debugging enabled (recommended; camera hardware required)

### Environment

Set your SDK path in `local.properties` (git-ignored):

```properties
sdk.dir=/path/to/your/android-sdk
```

### Build & Install

```bash
# Build debug APK
JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew assembleDebug

# Build and install directly to a connected device
JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew installDebug
```

Or install a pre-built APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

This project is licensed under the terms of the project repository.
