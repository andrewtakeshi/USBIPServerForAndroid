# Project Structure

Single-module Android app. All source lives under `app/src/main/`.

```
app/src/main/
├── java/org/cgutman/usbip/
│   ├── config/          # Activity UI
│   │   └── UsbIpConfig.java        — Main activity (start/stop service)
│   ├── jni/             # JNI bridge
│   │   └── UsbLib.java              — Native method declarations for USB transfers
│   ├── server/          # USB/IP server core
│   │   ├── UsbIpServer.java         — TCP server (port 3240), client connection handling
│   │   ├── UsbRequestHandler.java   — Interface for device operations (implemented by UsbIpService)
│   │   ├── UsbDeviceInfo.java       — Device + interfaces wire-format container
│   │   └── protocol/               # USB/IP wire protocol
│   │       ├── ProtoDefs.java        — Protocol constants (opcodes, status codes)
│   │       ├── UsbIpDevice.java      — Device descriptor wire format
│   │       ├── UsbIpInterface.java   — Interface descriptor wire format
│   │       ├── cli/                 # Client-phase protocol messages (device list, import)
│   │       └── dev/                 # Device-phase protocol messages (URB submit, unlink)
│   ├── service/         # Android service layer
│   │   ├── UsbIpService.java        — Foreground service, UsbRequestHandler impl, USB device management
│   │   └── AttachedDeviceContext.java — Per-device state (connection, endpoints, thread pool)
│   ├── usb/             # USB helpers
│   │   ├── UsbControlHelper.java    — Control transfer handling (SET_CONFIGURATION, SET_INTERFACE, descriptors)
│   │   ├── UsbDeviceDescriptor.java — USB device descriptor parser
│   │   └── XferUtils.java           — Transfer wrappers around JNI calls
│   └── utils/
│       └── StreamUtils.java         — InputStream read helpers
├── jni/                 # Native C code
│   ├── Android.mk
│   ├── Application.mk
│   └── usblib/
│       ├── Android.mk
│       └── usblib_jni.c             — ioctl-based USB bulk and control transfers
└── res/                 # Android resources (layouts, drawables, values)
```

## Architecture Layers
1. **UI** (`config/`) — Single activity, starts/stops the service
2. **Service** (`service/`) — Android foreground service, manages USB device connections and permissions
3. **Server** (`server/`) — TCP socket server implementing USB/IP protocol state machine
4. **Protocol** (`server/protocol/`) — Wire format serialization/deserialization for USB/IP messages
5. **USB** (`usb/`) — USB transfer execution and control request handling
6. **JNI** (`jni/` + native `usblib`) — Direct kernel USB ioctl calls for performance

## Data Flow
Client TCP connection → `UsbIpServer` → `UsbRequestHandler` (interface) → `UsbIpService` (impl) → `XferUtils` → `UsbLib` (JNI) → kernel ioctl
