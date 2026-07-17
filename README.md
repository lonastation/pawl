# Thought you were hidden? Caught you.

**PawL** is an on-device Android app that finds duplicate and visually similar photos and videos, then helps you review and remove them to reclaim storage. Everything runs locally — no accounts, no cloud upload, and no remote ML service.

It targets Android users with large media libraries who want a private, offline cleaner. Requires **Android 15+** (`minSdk` 35).

---

## Features

### Video scanner
- Scan device videos for duplicates and near-duplicates
- Group candidates by duration/size, then confirm with multi-frame perceptual hashing (dHash) and MD5
- Review groups with stats (total / groups / duplicates), ignore false positives, or move selected items to trash
- Preview clips with ExoPlayer in a detail screen

### Image scanner
- Find similar images using perceptual hashes (dHash + pHash)
- GIFs only match other GIFs; static images only match static images
- Same review flow: select, ignore, trash, and open details

### Recycle bin
- Soft-delete: media is staged in an app recycle bin before MediaStore removal
- Restore or permanently delete items; filter by All / Images / Videos
- Items auto-purge after **3 days**

### Settings
- Regenerate video or image fingerprints (clear cache and rescan)
- Clear ignored duplicate groups
- Optional **All files access** for restoring media to original folders (without it, restore falls back to app-managed Pictures/Movies paths)

---

## How detection works

| Media | Approach |
|-------|----------|
| **Video** | Duration (±500ms) and size (±10KB) candidates → MD5 + multi-frame dHash (samples at 10% / 25% / 50% / 75% / 90%) |
| **Image** | Cached dHash and pHash; both must be within Hamming distance 6 |

Fingerprints are cached in Room so unchanged files can be skipped on later scans.

---

## Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | Single-activity Compose, ViewModels, repositories |
| DI | Hilt |
| Database | Room |
| Video playback | Media3 ExoPlayer |
| Build | Android Gradle Plugin, Gradle Wrapper, version catalog |

Package / application ID: `com.linn.pawl`

---

## Requirements

- [Android Studio](https://developer.android.com/studio) with recent AGP / Kotlin support
- JDK 17+ (project compiles to JVM 17)
- Android SDK with `compileSdk` 37
- Device or emulator running **API 35+** (Android 15)

---

## Build

Clone the repository, then from the project root:

```bat
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

Release build:

```bat
.\gradlew.bat assembleRelease
```

Or open the project in Android Studio and run the `app` configuration.

Create a `local.properties` file (gitignored) pointing at your Android SDK if it is not already configured.

---

## Permissions

- `READ_MEDIA_VIDEO` / `READ_MEDIA_IMAGES` — scan and display media
- `MANAGE_EXTERNAL_STORAGE` (optional) — restore deleted files to their original locations

---

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2025 lonastation
