# Requirements Document

## Introduction

Update USBIPServerForAndroid to compile against and target Android 16 (API level 36), while maintaining backward compatibility down to Android 5.0 (API 21). The app currently targets API 34 with AGP 8.5.1 and Gradle 8.7. Android versions 15 and 16 introduce mandatory behavioral changes around edge-to-edge display, foreground service lifecycle, deprecated API removal, and build toolchain requirements that must be addressed.

## Glossary

- **App**: The USBIPServerForAndroid Android application
- **Build_System**: The Gradle-based build configuration including Android Gradle Plugin, Gradle wrapper, and NDK build
- **Foreground_Service**: The `UsbIpService` Android foreground service that runs the USB/IP server
- **Activity**: The `UsbIpConfig` single-activity UI for starting and stopping the service
- **Native_Library**: The `usblib` JNI shared library built via ndk-build that performs USB ioctl calls
- **CI_Pipeline**: The AppVeyor continuous integration configuration that builds and validates the project
- **Notification_Channel**: The Android `NotificationChannel` used by the Foreground_Service for its persistent notification

## Requirements

### Requirement 1: Compile SDK and Target SDK Update

**User Story:** As a developer, I want the app to compile against and target API 36, so that it can be distributed on the Google Play Store and run correctly on Android 16 devices.

#### Acceptance Criteria

1. THE Build_System SHALL set `compileSdk` to 36
2. THE Build_System SHALL set `targetSdk` to 36
3. THE Build_System SHALL maintain `minSdk` at 21
4. WHEN the App is built, THE Build_System SHALL produce a valid APK targeting API 36 without compilation errors

### Requirement 2: Android Gradle Plugin and Gradle Upgrade

**User Story:** As a developer, I want the build toolchain upgraded to support API 36 compilation, so that the project uses a supported AGP and Gradle version.

#### Acceptance Criteria

1. THE Build_System SHALL use Android Gradle Plugin version 8.9.x or later (minimum required for compileSdk 36)
2. THE Build_System SHALL use a Gradle wrapper version compatible with the selected AGP version
3. WHEN the App is built with the upgraded toolchain, THE Build_System SHALL complete compilation, linting, and packaging without errors
4. THE Build_System SHALL continue to exclude the Kotlin standard library from the dependency graph

### Requirement 3: AndroidX Dependency Updates

**User Story:** As a developer, I want AndroidX dependencies updated to versions that support API 36 features, so that the app uses compatible library versions.

#### Acceptance Criteria

1. THE Build_System SHALL use `androidx.core:core` version 1.16.0 or later
2. THE Build_System SHALL use `androidx.activity:activity` version 1.10.1 or later
3. WHEN the App is built with updated dependencies, THE Build_System SHALL resolve all dependencies without conflicts

### Requirement 4: Edge-to-Edge Display Enforcement

**User Story:** As a developer, I want the activity UI to handle edge-to-edge display correctly, so that the app renders properly on Android 15+ devices where edge-to-edge is enforced.

Note: The existing `values-v29/styles.xml` already sets transparent status and navigation bars (`@android:color/transparent`), which on Android 15+ causes content to draw behind system bars with no padding adjustment. The layout (`activity_usbip_config.xml`) uses fixed padding from `dimens.xml` and does not handle `WindowInsets` at all, making the overlap problem worse under enforced edge-to-edge.

#### Acceptance Criteria

1. WHEN the Activity is displayed on Android 15 (API 35) or later, THE Activity SHALL render content without overlap behind system bars (status bar, navigation bar)
2. THE Activity SHALL apply `WindowInsets` to its root layout padding so that interactive content remains within the safe area, replacing the current fixed-padding approach on API 35+
3. WHILE running on Android 14 (API 34) or earlier, THE Activity SHALL continue to render with the existing fixed-padding layout behavior

### Requirement 5: Foreground Service startForeground Timing Compliance

**User Story:** As a developer, I want the foreground service to call `startForeground()` promptly after creation, so that the service is not killed by the system on Android 14+ which enforces a strict timeout.

Note: Currently `UsbIpService.onCreate()` calls `startForeground()` (via `updateNotification()`) at the END of `onCreate()`, after `server.start()`, lock acquisition, and notification channel creation. The fix must move the `startForeground()` call to BEFORE long-running initialization (before server start, before lock acquisition). The manifest already declares `foregroundServiceType="specialUse"` and the `FOREGROUND_SERVICE_SPECIAL_USE` permission.

#### Acceptance Criteria

1. WHEN the Foreground_Service is created, THE Foreground_Service SHALL call `startForeground()` as the first substantive operation in `onCreate()`, before starting the TCP server, acquiring wake locks, and acquiring Wi-Fi locks
2. THE Foreground_Service SHALL pass `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` in the `startForeground()` call programmatically on Android 10 (API 29) or later, which the current `updateNotification()` method supports but must be invoked earlier in the lifecycle
3. IF the Foreground_Service fails to call `startForeground()` within the system-imposed timeout, THEN THE Foreground_Service SHALL handle the resulting exception gracefully

### Requirement 6: Service Launch via startForegroundService

**User Story:** As a developer, I want the activity to launch the service using `startForegroundService()` on Android 8.0+, so that the system correctly expects a foreground service and does not terminate it.

#### Acceptance Criteria

1. WHEN the Activity starts the Foreground_Service on Android 8.0 (API 26) or later, THE Activity SHALL use `startForegroundService()` instead of `startService()`
2. WHILE running on Android versions below 8.0, THE Activity SHALL continue to use `startService()`

### Requirement 7: Deprecated API Migration

**User Story:** As a developer, I want deprecated Android APIs replaced with their modern equivalents, so that the app avoids runtime warnings and potential removal in future API levels.

Note: The codebase currently acquires BOTH a `WIFI_MODE_FULL_HIGH_PERF` lock AND a `WIFI_MODE_FULL_LOW_LATENCY` lock on API 29+. Since `WIFI_MODE_FULL_LOW_LATENCY` supersedes `WIFI_MODE_FULL_HIGH_PERF`, the deprecated lock should be dropped entirely on API 29+ and only retained on API < 29.

#### Acceptance Criteria

1. THE Activity SHALL replace `ActivityManager.getRunningServices()` with an alternative mechanism for detecting whether the Foreground_Service is running
2. THE Foreground_Service SHALL use the type-safe `Intent.getParcelableExtra(String, Class)` method on Android 13 (API 33) or later instead of the deprecated `Intent.getParcelableExtra(String)` method
3. WHEN running on Android 10 (API 29) or later, THE Foreground_Service SHALL acquire only the `WifiManager.WIFI_MODE_FULL_LOW_LATENCY` Wi-Fi lock and SHALL NOT acquire the deprecated `WifiManager.WIFI_MODE_FULL_HIGH_PERF` lock
4. WHILE running on Android versions below API 29, THE Foreground_Service SHALL continue to acquire the `WifiManager.WIFI_MODE_FULL_HIGH_PERF` Wi-Fi lock
5. WHILE running on Android versions below the threshold for each replacement, THE App SHALL continue to use the older API via version-gated code paths

### Requirement 8: NDK and Native Library Compatibility

**User Story:** As a developer, I want the native JNI library to remain compatible with API 36, so that USB ioctl calls continue to function on Android 16 devices.

#### Acceptance Criteria

1. THE Native_Library SHALL continue to build with `APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true` for 16KB page alignment
2. THE Build_System SHALL use an NDK version compatible with the selected AGP version
3. WHEN the Native_Library is loaded on an Android 16 device, THE Native_Library SHALL execute USB ioctl calls without page-alignment-related crashes
4. THE Native_Library SHALL continue to build for all ABIs via `APP_ABI := all`

### Requirement 9: CI Pipeline Update

**User Story:** As a developer, I want the CI pipeline updated to build with the new toolchain, so that automated builds continue to pass.

#### Acceptance Criteria

1. THE CI_Pipeline SHALL use a JDK version compatible with the upgraded AGP (JDK 17 or later)
2. WHEN the CI_Pipeline runs, THE CI_Pipeline SHALL execute `gradlew build connectedCheck` successfully with the updated SDK, AGP, and dependencies
3. THE CI_Pipeline SHALL have access to Android SDK platform 36 build tools

### Requirement 10: Broadcast Receiver Registration Cleanup

**User Story:** As a developer, I want the broadcast receiver registration code to use `ContextCompat` for version-safe flag handling and remove the lint suppression, so that the code is clean and correct across all API levels.

Note: The current code already correctly gates on `Build.VERSION_CODES.TIRAMISU` (API 33) for `RECEIVER_NOT_EXPORTED`. The `RECEIVER_NOT_EXPORTED` flag was introduced in API 33, not API 31. The existing implementation is functionally correct. This requirement focuses on migrating to `ContextCompat.registerReceiver()` which handles the version gating internally, and removing the `@SuppressLint("UnspecifiedRegisterReceiverFlag")` annotation that is currently suppressing the lint warning.

#### Acceptance Criteria

1. THE Foreground_Service SHALL use `ContextCompat.registerReceiver()` from AndroidX to register dynamic broadcast receivers, which internally handles the `RECEIVER_NOT_EXPORTED` flag on API 33+ and omits it on earlier versions
2. WHEN `ContextCompat.registerReceiver()` is used, THE Foreground_Service SHALL remove the `@SuppressLint("UnspecifiedRegisterReceiverFlag")` annotation from `onCreate()`
3. THE Foreground_Service SHALL pass `ContextCompat.RECEIVER_NOT_EXPORTED` for all internal broadcast receivers (such as the USB permission receiver) that do not need to receive broadcasts from external apps

### Requirement 11: Notification Channel Importance Level

**User Story:** As a developer, I want the notification channel to use an appropriate importance level, so that the foreground service notification does not produce unwanted sound or vibration on newer Android versions.

Note: The current notification channel uses `IMPORTANCE_DEFAULT` which can produce sound. Although the notification builder sets `setSilent(true)`, on newer Android versions the channel importance takes precedence. Using `IMPORTANCE_LOW` ensures no sound or vibration while still displaying the notification.

#### Acceptance Criteria

1. THE Foreground_Service SHALL create the Notification_Channel with `NotificationManager.IMPORTANCE_LOW` instead of `NotificationManager.IMPORTANCE_DEFAULT`
2. WHEN the Notification_Channel is created with `IMPORTANCE_LOW`, THE Foreground_Service SHALL continue to display the foreground service notification visibly in the notification shade

### Requirement 12: Dead Resource Cleanup (Optional)

**User Story:** As a developer, I want unused resource qualifier directories removed, so that the project does not contain dead code that could confuse future maintainers.

Note: `values-v11/styles.xml` and `values-v14/styles.xml` define `AppBaseTheme` for API 11+ and API 14+ respectively. Since `minSdk` is 21, these are never used — the `values-v21/` or higher qualifier always takes precedence. These are dead code.

#### Acceptance Criteria

1. THE Build_System SHALL remove the `res/values-v11/` directory and its contents, as `minSdk` 21 makes API 11-specific resource qualifiers unreachable
2. THE Build_System SHALL remove the `res/values-v14/` directory and its contents, as `minSdk` 21 makes API 14-specific resource qualifiers unreachable
3. WHEN the dead resource directories are removed, THE App SHALL continue to resolve all style and theme references correctly using the remaining resource qualifiers
