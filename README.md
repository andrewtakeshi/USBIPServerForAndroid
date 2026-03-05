# USB/IP Server for Android

Share USB devices connected to your Android device over the network using the [USB/IP protocol](https://www.kernel.org/doc/html/latest/usb/usbip_protocol.html). Remote machines can attach to these devices as if they were plugged in locally.

## Requirements

- Android device with USB host mode support (USB OTG)
- Android 5.0 (API 21) or later
- A USB/IP client on the remote machine (e.g., Linux `usbip` tools, Windows USB/IP driver)

## Building

### Prerequisites

- JDK 17
- Android SDK (API 34)
- Android NDK 27.0.12077973

Android Studio will typically handle SDK/NDK installation for you. If building from the command line, set `ANDROID_HOME` to your SDK path.

### Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Full build with lint checks
./gradlew build
```

The output APK is at `app/build/outputs/apk/debug/app-debug.apk` (or `release/`).

### Native code

The JNI library (`usblib`) is built automatically by Gradle via `ndk-build`. No separate native build step is needed. The native build config lives in `app/src/main/jni/` using Android.mk (not CMake).

## Installing

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Open the app on your Android device
2. Tap "Start Service" to start the USB/IP server (listens on TCP port 3240)
3. Connect a USB device to your Android device via OTG
4. From a remote Linux machine:

```bash
# List available devices
usbip list -r <android-device-ip>

# Attach a device
sudo usbip attach -r <android-device-ip> -b <bus-id>
```

5. The Android device will prompt for USB permission on first attach
6. The device now appears as a local USB device on the remote machine

To stop sharing, tap "Stop Service" in the app, or detach from the client side:

```bash
sudo usbip detach -p <port>
```

## Testing

There are no unit tests. The project relies on integration testing with real hardware.

### CI

```bash
./gradlew build connectedCheck
```

This runs lint, builds the APK, and executes any connected (instrumented) tests on an attached device or emulator. CI runs on AppVeyor with JDK 17.

### Manual testing

1. Build and install the debug APK on an Android device with USB host support
2. Connect a USB device via OTG adapter
3. Start the service from the app
4. From a Linux client, list and attach devices as shown above
5. Verify the device works on the client (e.g., `lsusb`, device-specific tools)

## Debugging

### Logcat

The app uses `System.out` / `System.err` for logging. Filter with:

```bash
adb logcat -s System.out System.err
```

Or more broadly:

```bash
adb logcat | grep -i usbip
```

### Debug build

Set `DEBUG = true` in `UsbIpService.java` to enable verbose transfer logging (bulk/interrupt byte counts per URB).

### Common issues

- **"No devices listed"** — Make sure the Android device supports USB host mode and a device is physically connected
- **Permission denied on attach** — The user must grant USB permission via the Android dialog. Check that the app is in the foreground when the first attach happens
- **Transfers failing** — Check `adb logcat` for `Xfer failed` messages with errno values. `-110` is a normal timeout during polling; other negative values indicate real errors

## Supported Android versions

| Version | API Level | Status |
|---------|-----------|--------|
| Android 5.0 – 9.0 | 21 – 28 | Supported |
| Android 10 – 14 | 29 – 34 | Supported (targetSdk) |
| Android 15+ | 35+ | Should work, not yet targeted |

## Project structure

```
app/src/main/
├── java/org/cgutman/usbip/
│   ├── config/       — UI activity (start/stop service)
│   ├── jni/          — JNI native method declarations
│   ├── server/       — TCP server + USB/IP protocol implementation
│   ├── service/      — Android foreground service, USB device management
│   ├── usb/          — USB transfer helpers and control request handling
│   └── utils/        — Stream utilities
├── jni/              — Native C code (ioctl-based USB transfers)
└── res/              — Android resources
```

## License

See [LICENSE](LICENSE).
