# Implementation Plan: Android 16 Compatibility

## Overview

Incrementally update USBIPServerForAndroid to target API 36 (Android 16). The work is organized as: build toolchain upgrade first, then service-layer changes, then activity-layer changes, then resource/CI cleanup. Each step builds on the previous and ends with a build verification checkpoint.

## Tasks

- [x] 1. Upgrade build toolchain and dependencies
  - [x] 1.1 Update Gradle wrapper to 8.11.1
    - In `gradle/wrapper/gradle-wrapper.properties`, change `distributionUrl` to use `gradle-8.11.1-bin.zip`
    - _Requirements: 2.2_

  - [x] 1.2 Update Android Gradle Plugin to 8.9.1
    - In root `build.gradle`, change `com.android.tools.build:gradle` version from `8.5.1` to `8.9.1`
    - _Requirements: 2.1_

  - [x] 1.3 Update compileSdk, targetSdk, and AndroidX dependencies
    - In `app/build.gradle`, set `compileSdk 36` and `targetSdk 36`, keep `minSdk 21`
    - Update `androidx.core:core` to `1.16.0` and `androidx.activity:activity` to `1.10.1`
    - Verify Kotlin stdlib exclusion is still present and effective
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.4, 3.1, 3.2, 3.3_

- [x] 2. Checkpoint - Verify build toolchain upgrade
  - Run `./gradlew assembleDebug` to confirm the project compiles with the new toolchain
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Reorder UsbIpService.onCreate() and add foreground service error handling
  - [x] 3.1 Reorder onCreate() to call startForeground() early
    - In `UsbIpService.java`, restructure `onCreate()` to: init fields → create notification channel → call `startForeground()` via `updateNotification()` → register receiver → acquire locks → start server
    - Move `createNotificationChannel()` and `updateNotification()` calls to before receiver registration, lock acquisition, and `server.start()`
    - _Requirements: 5.1, 5.2_

  - [x] 3.2 Add try-catch for foreground service start exceptions
    - Wrap the `startForeground()` call path in try-catch for `ForegroundServiceStartNotAllowedException` (API 31+) and `InvalidForegroundServiceTypeException` (API 34+)
    - On catch, log via `System.err.println` and call `stopSelf()`
    - _Requirements: 5.3_

  - [x] 3.3 Add static isRunning flag to UsbIpService
    - Add `public static volatile boolean isRunning = false;` field
    - Set to `true` at start of `onCreate()`, set to `false` at start of `onDestroy()`
    - _Requirements: 7.1_

- [x] 4. Migrate deprecated APIs in UsbIpService
  - [x] 4.1 Replace manual receiver registration with ContextCompat.registerReceiver()
    - Replace the version-gated `registerReceiver()` block with a single `ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)` call
    - Remove `"UnspecifiedRegisterReceiverFlag"` from the `@SuppressLint` annotation (keep `"UseSparseArrays"`)
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 4.2 Version-gate getParcelableExtra for API 33+
    - In the `usbReceiver.onReceive()` method, use `intent.getParcelableExtra(key, UsbDevice.class)` on API 33+ and the deprecated overload on older versions
    - _Requirements: 7.2, 7.5_

  - [x] 4.3 Fix Wi-Fi lock to be exclusive by API level
    - On API 29+: acquire only `WIFI_MODE_FULL_LOW_LATENCY`, do NOT acquire `WIFI_MODE_FULL_HIGH_PERF`
    - On API < 29: acquire only `WIFI_MODE_FULL_HIGH_PERF`
    - Update `onDestroy()` to null-check each lock before releasing, since only one will be non-null
    - _Requirements: 7.3, 7.4, 7.5_

  - [x] 4.4 Change notification channel importance to IMPORTANCE_LOW
    - In the `createNotificationChannel()` call, replace `NotificationManager.IMPORTANCE_DEFAULT` with `NotificationManager.IMPORTANCE_LOW`
    - _Requirements: 11.1, 11.2_

- [x] 5. Checkpoint - Verify service changes compile
  - Run `./gradlew assembleDebug` to confirm UsbIpService changes compile cleanly
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Update UsbIpConfig activity
  - [x] 6.1 Add launchService() helper with startForegroundService on API 26+
    - Add a `private void launchService()` method that uses `startForegroundService()` on API 26+ and `startService()` below
    - Replace both existing `startService(new Intent(...))` calls (in `onClick` and `requestPermissionLauncher` callback) with `launchService()`
    - _Requirements: 6.1, 6.2_

  - [x] 6.2 Replace isMyServiceRunning() to use UsbIpService.isRunning
    - Change the `isMyServiceRunning()` method body to simply return `UsbIpService.isRunning`
    - Remove the `ActivityManager.getRunningServices()` call and related code
    - _Requirements: 7.1_

  - [x] 6.3 Add edge-to-edge WindowInsets handling for API 35+
    - In `activity_usbip_config.xml`, add `android:id="@+id/rootLayout"` to the root `RelativeLayout`
    - In `UsbIpConfig.onCreate()`, after `setContentView()`, add a version-gated `ViewCompat.setOnApplyWindowInsetsListener()` block for API 35+ that sets root layout padding from system bar insets
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 6.4 Create values-v35/styles.xml for edge-to-edge theme
    - Create `app/src/main/res/values-v35/styles.xml` with `AppBaseTheme` inheriting `android:Theme.Material` and setting transparent navigation/status bar colors
    - _Requirements: 4.1_

- [x] 7. Checkpoint - Verify activity changes compile
  - Run `./gradlew assembleDebug` to confirm UsbIpConfig changes compile cleanly
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Update CI pipeline and clean up resources
  - [x] 8.1 Add SDK platform 36 install step to appveyor.yml
    - Add an `sdkmanager` command to install `platforms;android-36` before the build step
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 8.2 Delete dead resource directories
    - Delete `app/src/main/res/values-v11/` directory and contents
    - Delete `app/src/main/res/values-v14/` directory and contents
    - _Requirements: 12.1, 12.2, 12.3_

  - [x] 8.3 Verify NDK configuration is unchanged
    - Confirm `APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true` and `APP_ABI := all` are still set in `Application.mk`
    - Confirm NDK version 27.0.12077973 is still configured
    - No code changes needed — verification only
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 9. Final checkpoint - Full build verification
  - Run `./gradlew assembleDebug` and `./gradlew lint` to confirm the complete set of changes compiles and lints cleanly
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- No property-based test tasks are included because the project has no unit test infrastructure and deliberately avoids third-party libraries. The three correctness properties (WindowInsets-to-padding, service running flag, Wi-Fi lock exclusivity) are verified through code review and manual testing.
- All code is Java with tabs for indentation and same-line braces per project conventions.
- Each task references specific requirement clauses for traceability.
- Checkpoints are placed after each logical group of changes to catch issues incrementally.
