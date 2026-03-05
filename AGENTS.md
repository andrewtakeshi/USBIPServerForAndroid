# Agent Guidelines

## Language & Style
- All code is Java. Do not introduce Kotlin files or Kotlin dependencies.
- Follow existing code style: tabs for indentation, opening brace on same line, `System.out.printf`/`System.err.println` for logging (no Android `Log` class used in this codebase).
- No third-party libraries unless absolutely necessary. The project deliberately avoids external dependencies.

## Architecture Rules
- `UsbRequestHandler` is the interface between the server and the Android service layer. New device operations must be added to this interface and implemented in `UsbIpService`.
- Protocol wire format classes live in `server/protocol/`. Client-phase messages go in `cli/`, device-phase messages go in `dev/`. All wire serialization uses `ByteBuffer` with `BIG_ENDIAN` byte order (network byte order) except USB descriptors which are `LITTLE_ENDIAN`.
- USB transfers must go through `XferUtils` → `UsbLib` (JNI). Do not use Android's `UsbDeviceConnection.bulkTransfer()` or `controlTransfer()` directly — the JNI path exists for correctness (proper errno handling, timeout behavior).

## Native Code
- The JNI layer (`usblib_jni.c`) is intentionally minimal. It wraps Linux USB ioctl calls. Changes here should be rare and carefully reviewed.
- Native build uses `ndk-build` (Android.mk), not CMake. Do not add CMakeLists.txt.

## Android Service
- `UsbIpService` runs as a foreground service. It holds wake locks and Wi-Fi locks. Any changes to service lifecycle must preserve these locks.
- USB permissions are requested at attach time, not at service start. Do not change this flow.
- The `AttachedDeviceContext` holds per-device state including a `ThreadPoolExecutor` sized to the number of endpoints. This is intentional for concurrent URB processing.

## Protocol
- The USB/IP server listens on TCP port 3240 (standard USB/IP port). Do not change this.
- The protocol has two phases: client phase (device listing, import) and device phase (URB submit/unlink). The server transitions from client to device phase after a successful import.

## Testing
- There are no unit tests. Testing requires a physical Android device with USB host support and a USB/IP client on another machine.
- Use `./gradlew build connectedCheck` for CI validation.
- For manual testing: install the APK, start the service, then use `usbip list -r <android-ip>` and `usbip attach -r <android-ip> -b <busid>` from a Linux client.

## Common Pitfalls
- Sign extension: Java's signed bytes/shorts require masking (`& 0xFF`, `& 0xFFFF`) when converting to protocol fields. Existing code handles this — follow the same pattern.
- Thread safety: URB replies are written to the socket under `synchronized (s)` to prevent interleaving. Maintain this pattern for any new socket writes.
- Timeout loops: Transfer methods retry on `-110` (ETIMEDOUT) and check for cancellation between retries. New transfer code must follow this pattern.
