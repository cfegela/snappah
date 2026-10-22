# <img src="assets/icon.png" width="40" height="40" alt="Snappah Icon" valign="middle"> Snappah

A minimal, distraction-free point-and-shoot camera app for Android built with **Jetpack Compose** and **CameraX**, featuring an integrated media gallery and pro photo editor powered by **Google ML Kit** and **Media3 ExoPlayer**.

## Quickstart

* Install the app using the instructions below.
* Open the app.
* Take pictures by pressing the big blue button.
* Take selfies by hitting the "change camera" icon in the upper-right corner and press the big blue button.
* Take video by long pressing the big blue button to start recording. Press the big red button to stop recording.
* Swipe left to review photos taken in the last minute. Delete them if they suck.
* View the camera roll by pressing the "photo gallery" icon in the upper-left corner.
* View a picture by pressing on it.
* Scroll through pictures by swiping.
* Share a photo by pressing the "share" icon.
* Edit a photo by pressing the "edit" icon. Edits are destructive once saved. Drive fast. Take chances. Or save as a copy.
* Delete a photo by long-pressing the "delete" icon.

## Installation

Pre-built APKs are published to GitHub Releases at https://github.com/cfegela/snappah/releases. Follow the step-by-step instructions below to install release **26.10.22**.

1. **Download the APK**: Navigate to [GitHub Releases](https://github.com/cfegela/snappah/releases) (or the [v26.10.22 Release](https://github.com/cfegela/snappah/releases/tag/v26.10.22)) on your device browser and download [`Snappah-v26.10.22.apk`](https://github.com/cfegela/snappah/releases/download/v26.10.22/Snappah-v26.10.22.apk).
2. **Allow Installation from Unknown Sources**: Tap the downloaded APK in your notification shade or Downloads folder. If prompted with a security dialog, tap **Settings** and enable **Allow from this source** (or navigate to **Settings** → **Apps** → **Special app access** → **Install unknown apps**).
3. **Install the App**: Return to the installer prompt and tap **Install**.
4. **Grant Permissions**: Once installed, tap **Open** and approve the required permissions (Camera, Microphone, and Photos/Storage) when prompted.

## Compatibility

Snappah supports physical Android devices operating on API 26 (Android 8.0) through API 36 (Android 16):

- **Google Pixel**: Pixel 2 through Pixel 10 (Pro, XL, Fold, a-series)
- **Samsung Galaxy**: S-series (S8 through S25), Z Fold / Flip series, Note series, A-series
- **OEM Compatibility**: Motorola, OnePlus, Xiaomi, Nothing Phone, Sony Xperia, Asus, Oppo, Vivo


## Project Structure

```text
snappah/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml                  # Hardware permissions, camera features, and queries
│   │   │   ├── java/
│   │   │   │   ├── com/example/snappah/
│   │   │   │   │   ├── MainActivity.kt              # Edge-to-edge layout, permissions, and app entry
│   │   │   │   │   ├── SnappahApp.kt                # Application class & custom Coil ImageLoader
│   │   │   │   │   ├── qr/                          # ML Kit QR code analyzer, parser, actions, & overlay UI
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── CameraScreen.kt          # CameraX pipeline, OIS, shutter, and letterboxing
│   │   │   │   │       ├── PhotoViewerScreen.kt     # 1-minute recent review stream and deletion
│   │   │   │   │       └── theme/                   # Snappah color palette, typography, and dark theme
│   │   │   │   └── com/saturatah/gallery/
│   │   │   │       ├── data/
│   │   │   │       │   └── PhotoRepository.kt       # MediaStore queries, EXIF preservation, and file saves
│   │   │   │       ├── model/
│   │   │   │       │   ├── Photo.kt                 # Media item model (images and videos)
│   │   │   │       │   └── PhotoFilter.kt           # Film presets (Warm, Cool, Noir, Fade)
│   │   │   │       ├── ui/
│   │   │   │       │   ├── EditPhotoScreen.kt       # Color, light, crop, rotate, and bokeh editor
│   │   │   │       │   ├── GalleryApp.kt            # Navigation routing between grid, viewer, and editor
│   │   │   │       │   ├── GalleryScreen.kt         # 3-column photo roll with video duration badges
│   │   │   │       │   ├── GalleryViewModel.kt      # Gallery UI state, deletion, and editing orchestration
│   │   │   │       │   ├── PhotoDetailScreen.kt     # Fullscreen viewer & Media3 ExoPlayer video playback
│   │   │   │       │   └── theme/                   # Gallery design tokens and typography
│   │   │   │       └── util/
│   │   │   │           ├── ColorAdjustmentHelper.kt # Skin-tone reduction and saturation matrices
│   │   │   │           ├── MediaStoreThumbnailFetcher.kt # Fast OS-level thumbnail loader for Coil
│   │   │   │           ├── PortraitBlurEngine.kt    # ML Kit Subject Segmentation & bokeh synthesis
│   │   │   │           └── StackBlur.kt             # High-speed optical stack blur algorithm
│   │   │   └── res/
│   │   │       ├── raw/shutter_sound.wav            # Mechanical shutter sound effect
│   │   │       ├── drawable/                        # Vector assets
│   │   │       ├── mipmap/                          # Adaptive launcher icons
│   │   │       └── values/                          # App strings, themes, and colors
│   │   └── test/
│   │       └── java/com/saturatah/gallery/
│   │           └── PhotoModelTest.kt                # Unit tests for models, sorting, filters, and blur
│   └── build.gradle.kts                             # App module build configuration and dependencies
├── assets/
│   ├── icon.png                                     # App icon (PNG raster)
│   └── icon.svg                                     # App icon (SVG vector)
├── build.gradle.kts                                 # Root build configuration
├── settings.gradle.kts                              # Project module definitions
└── README.md
```
