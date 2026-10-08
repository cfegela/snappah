# <img src="assets/icon.png" width="40" height="40" alt="Snappah Icon" valign="middle"> Snappah

A minimal, distraction-free point-and-shoot camera app for Android built with **Jetpack Compose** and **CameraX**.

Snappah strips away the clutter of modern camera apps — no menus, no sliders, no filters. Just frame the shot and press the button.

---

## Technical Architecture

Snappah is designed around a reactive, lifecycle-aware architecture built on AndroidX CameraX and Jetpack Compose.

```
                    ┌──────────────────────────────────────────────┐
                    │               MainActivity                   │
                    │   (Edge-to-Edge, Portrait Locked, Permissions)│
                    └──────────────────────┬───────────────────────┘
                                           │
                                           ▼
                    ┌──────────────────────────────────────────────┐
                    │                 CameraScreen                 │
                    │     (HorizontalPager Container / Viewfinder)  │
                    └───────────┬──────────────────────┬───────────┘
                                │                      │
                 Swipe Left     │                      │   Swipe Right
                 ───────────────┘                      └───────────────
                                ▼                                      ▼
    ┌────────────────────────────────────────┐     ┌────────────────────────────────────────┐
    │           Camera Viewfinder            │     │           PhotoViewerScreen            │
    │  - PreviewView (3:4 Sensor Ratio)      │     │  - Scoped MediaStore Query             │
    │  - Hardware Saturation Layer           │     │  - 60-Second Sliding Window            │
    │  - Touch AF/AE Metering Point Factory  │     │  - ImageDecoder Downsampling           │
    │  - OrientationEventListener            │     │  - MediaStore Delete Intent / Action   │
    │  - ImageCapture & VideoCapture         │     │  - "No Recent Images" Empty State      │
    └────────────────────────────────────────┘     └────────────────────────────────────────┘
```

---

### Camera & Capture Pipeline

- **Lifecycle-Aware Binding**: Managed through `ProcessCameraProvider`, binding `Preview`, `ImageCapture`, and `VideoCapture<Recorder>` simultaneously to the Compose lifecycle.
- **High-Quality Still Capture**: Configured with `ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY`, engaging camera ISP hardware Multi-Frame Noise Reduction (MFNR) and zero-shutter-lag algorithms for optimal low-light performance.
- **Native 3:4 Aspect Ratio**: Matches the physical aspect ratio of the underlying camera sensor, vertically centered with OLED pure-black letterboxing to eliminate letterbox cropping.
- **Camera Switch Transition**: Seamless toggle between front and rear cameras (`CameraSelector.DEFAULT_BACK_CAMERA` and `DEFAULT_FRONT_CAMERA`) using a zero-overhead transition overlay (`AnimatedVisibility`) to eliminate black flicker or stale-frame flashing during pipeline rebinding.

### Dynamic Orientation & EXIF System

- **Portrait-Locked Window**: `MainActivity` is declared with `android:screenOrientation="portrait"` to prevent Activity destruction, recomposition churn, or viewfinder restarts during device rotation.
- **Hardware Tilt Monitoring**: Tracks the physical device accelerometer in real time using an `OrientationEventListener`, mapping device tilt angles into standard `Surface.ROTATION_*` constants:
  - Natural Portrait ($0^\circ$) $\to$ `Surface.ROTATION_0`
  - Landscape ($90^\circ$ counter-clockwise) $\to$ `Surface.ROTATION_90`
  - Inverted Portrait ($180^\circ$) $\to$ `Surface.ROTATION_180`
  - Reverse Landscape ($90^\circ$ clockwise) $\to$ `Surface.ROTATION_270`
- **Dynamic Target Rotation**: Synchronizes `targetRotation` on `ImageCapture` and `VideoCapture` at initialization, during active tilt changes, and directly at the moment of shutter press.
- **Automatic EXIF Tagging**: CameraX calculates the sensor-to-target rotation offset and writes the correct EXIF orientation tag (`ORIENTATION_NORMAL`, `ORIENTATION_ROTATE_90`, `ORIENTATION_ROTATE_180`, `ORIENTATION_ROTATE_270`) directly into the output JPEG. Landscape photos automatically save horizontally with zero post-capture rotation needed by the user.

### WYSIWYG Color & Processing Pipeline

- **Real-Time Saturation Boost**: Applies a calibrated 15% saturation enhancement (`SATURATION_BOOST = 1.15f`) across both the viewfinder and final JPEG file for consistent WYSIWYG output.
- **Hardware-Accelerated Preview**: The saturation boost is rendered in real time using an Android `ColorMatrixColorFilter` set directly on the `PreviewView` hardware layer (`View.LAYER_TYPE_HARDWARE`), incurring zero CPU or memory allocation overhead.
- **Post-Capture Processing**:
  - Image captured to a temporary cache file via CameraX.
  - Decoded and rendered to an in-memory mutable bitmap with matching `ColorMatrixColorFilter`.
  - Compressed to JPEG at 98% quality into `Pictures/simplah/SNAP_<timestamp>.jpg` using scoped `MediaStore`.
  - **Full EXIF Preservation**: `copyExifAttributes` copies exposure parameters (`TAG_EXPOSURE_TIME`, `TAG_F_NUMBER`, `TAG_ISO_SPEED_RATINGS`, `TAG_FLASH`, `TAG_FOCAL_LENGTH`), GPS coordinates, timestamps, device make/model, and dynamic orientation tags from the raw capture to the final MediaStore file.

### Touch Focus & Exposure Metering

- **Dual AF/AE Metering**: Tapping anywhere on the viewfinder translates Compose coordinates into normalized camera coordinates using `PreviewView.meteringPointFactory.createPoint(offset.x, offset.y)`.
- **Metering Action**: Dispatches a `FocusMeteringAction` with `FLAG_AF or FLAG_AE` to `cameraControl.startFocusAndMetering()`, with an automatic 3-second timeout before resetting to continuous auto-focus.
- **Visual Reticle**: Renders an animated Compose `FocusReticle` with scale contraction ($1.35\times \to 1.0\times$), high-contrast outer shadow and white stroke, a 2.3-second hold duration, and linear fade-out.

### Video Recording Architecture

- **VideoCapture Engine**: Powered by CameraX `Recorder` configured with `QualitySelector.from(Quality.HIGHEST)` and `VideoCapture.withOutput(recorder)`.
- **Audio Integration**: Audio channel captured synchronously via `withAudioEnabled()`.
- **Scoped Video Storage**: Streamed directly into `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` under `Pictures/simplah/SNAP_VID_<timestamp>.mp4` using `MediaStoreOutputOptions`.
- **Auditory & Visual Feedback**: Emits distinct auditory cues via `ToneGenerator` (`TONE_PROP_BEEP`) on recording start and stop, accompanied by haptic feedback and instant shutter button color transitions (blue $\to$ red $\to$ blue).

### Review Stream & MediaStore Query Engine

- **Sliding Window Query**: `queryRecentPhotos` scans `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` for images stored in `Pictures/simplah` where `DATE_ADDED` is within the last 60 seconds (`System.currentTimeMillis() / 1000 - 60`).
- **Memory-Efficient Downsampling**: Employs Android P+ `ImageDecoder.decodeBitmap` with `setTargetSampleSize`, downsampling images to match the physical display resolution while automatically honoring EXIF orientation.
- **Seamless Navigation**: Managed via Compose `HorizontalPager` with `beyondViewportPageCount = 1`, preloading the viewfinder offscreen to prevent latency or black flashes when returning to the camera.
- **Scoped Deletion Protocol**: Integrates with Android 11+ scoped storage requirements using `MediaStore.createDeleteRequest` and `ActivityResultContracts.StartIntentSenderForResult()`, with fallback to direct `ContentResolver.delete()` for legacy Android versions.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin (JVM target 17) |
| **UI Framework** | Jetpack Compose + Material 3 (BOM `2024.10.01`) |
| **Camera Core** | AndroidX CameraX `1.6.2` (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-video`) |
| **Color Processing** | Android `ColorMatrix` / `ColorMatrixColorFilter` on hardware layer |
| **Audio Synthesis** | `SoundPool` (mechanical shutter) · `ToneGenerator` (recording cues) |
| **Build System** | Gradle `8.11.1` (Kotlin DSL) · Android Gradle Plugin (AGP) `8.9.1` |

**Target SDK Specifications**

| Property | Value | Platform |
| :--- | :--- | :--- |
| `minSdk` | **26** | Android 8.0 (Oreo) |
| `targetSdk` | **36** | Android 16 (Baklava) |
| `compileSdk` | **36** | Android 16 (Baklava) |

---

## Permissions & Storage Model

| Permission | Scope | Technical Purpose |
| :--- | :--- | :--- |
| `CAMERA` | Runtime | Viewfinder stream, auto-focus metering, and photo capture |
| `RECORD_AUDIO` | Runtime | Synchronous audio track for video recordings |
| `READ_MEDIA_IMAGES` | Runtime (`minSdk 33`) | Accessing recently captured photos in MediaStore on Android 13+ |
| `READ_EXTERNAL_STORAGE` | Runtime (`maxSdk 32`) | Accessing recently captured photos in MediaStore on Android 12 and below |
| `WRITE_EXTERNAL_STORAGE` | Runtime (`maxSdk 28`) | Legacy disk write fallback on Android 8.0–9.0 |

- **Scoped Storage**: On Android 10+ (API 29+), photos and videos are written directly to `Pictures/simplah/` via `MediaStore` using `RELATIVE_PATH` and `IS_PENDING` flags without requiring broad external storage permissions.

---

## Compatibility

Snappah supports physical Android devices operating on API 26 (Android 8.0) through API 36 (Android 16):

- **Google Pixel**: Pixel 2 through Pixel 10 (Pro, XL, Fold, a-series)
- **Samsung Galaxy**: S-series (S8 through S25), Z Fold / Flip series, Note series, A-series
- **OEM Compatibility**: Motorola, OnePlus, Xiaomi, Nothing Phone, Sony Xperia, Asus, Oppo, Vivo

---

## Project Structure

```text
snappah/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml           # Portrait orientation lock & hardware permissions
│   │   ├── java/com/example/snappah/
│   │   │   ├── MainActivity.kt           # Edge-to-edge setup, permission requests, screen routing
│   │   │   └── ui/
│   │   │       ├── CameraScreen.kt       # Viewfinder, OrientationEventListener, capture pipeline
│   │   │       ├── PhotoViewerScreen.kt  # MediaStore query, ImageDecoder pager, delete handling
│   │   │       └── theme/                # Snappah color palette, typography, and dark theme
│   │   └── res/
│   │       ├── raw/shutter_sound.wav     # Mechanical shutter sound effect
│   │       ├── drawable/                 # Vector assets
│   │       ├── mipmap/                   # Adaptive launcher icons
│   │       └── values/                   # Strings, theme definitions, and colors
│   └── build.gradle.kts                  # Android configuration & dependencies
├── assets/
│   ├── icon.png                          # App icon (PNG raster)
│   └── icon.svg                          # App icon (SVG vector)
├── build.gradle.kts                      # Root build configuration
├── settings.gradle.kts                   # Project module definitions
└── README.md
```

---

## Getting Started

### Prerequisites

- **JDK 17+** (Homebrew: `brew install openjdk@17`)
- **Android SDK** (API 36 build tools and platform)
- **Physical Android device** with USB debugging enabled (camera hardware required)

### Environment Setup

Set your Android SDK location in `local.properties`:

```properties
sdk.dir=/path/to/your/android-sdk
```

### Build & Install

```bash
# Compile debug APK
JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew assembleDebug

# Install directly to connected device
JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew installDebug
```

Alternatively, push an existing build via `adb`:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

This project is licensed under the terms of the project repository.
