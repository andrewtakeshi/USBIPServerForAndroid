# Tech Stack

## Language & Platform
- Java (no Kotlin)
- Android SDK: compileSdk 34, minSdk 21, targetSdk 34
- NDK 27.0.12077973 for native C code (JNI)

## Build System
- Gradle with Android Gradle Plugin 8.5.1
- NDK build via `ndk-build` (Android.mk / Application.mk), not CMake
- CI: AppVeyor (Windows, JDK 17)

## Dependencies
- `androidx.core:core:1.13.1`
- `androidx.activity:activity:1.9.1`
- No third-party networking, serialization, or USB libraries

## Native Code
- Single JNI library: `usblib`
- Performs USB transfers via direct `ioctl()` calls to `/dev/bus/usb` (USBDEVFS_BULK, USBDEVFS_CONTROL)
- Built for all ABIs (`APP_ABI := all`), optimized release, 16KB page support

## Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Full build + lint + connected tests (CI command)
./gradlew build connectedCheck

# Lint only
./gradlew lint
```

## Key Conventions
- No unit test directory exists; testing is done via `connectedCheck` (instrumented tests on device/emulator)
- Lint config suppresses `MissingTranslation` warnings (`app/lint.xml`)
- AndroidX is enabled (`android.useAndroidX=true`)
- Kotlin stdlib is explicitly excluded from the dependency graph
