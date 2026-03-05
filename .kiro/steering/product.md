# Product Overview

USBIPServerForAndroid is an Android application that implements a USB/IP server, allowing USB devices physically connected to an Android device to be shared over a network using the USB/IP protocol (RFC-like standard, Linux kernel module compatible).

A remote machine running a USB/IP client (e.g., Linux `usbip` tools or Windows USB/IP client) can attach to USB devices on the Android device as if they were locally connected.

## Key Capabilities
- Enumerates all USB host-mode devices connected to the Android device
- Serves device listings and handles import/attach requests from USB/IP clients
- Proxies USB Request Blocks (URBs) between the network client and the physical USB device
- Handles control, bulk, and interrupt USB transfers via JNI (direct ioctl calls)
- Runs as a foreground Android service with wake lock and Wi-Fi lock for reliability
- Supports USB speed detection heuristics (low/full/high/super speed)

## User Interface
- Single-activity UI (`UsbIpConfig`) with a start/stop button for the USB/IP service
- Foreground notification showing the number of shared devices
- USB permission prompts when a client attaches to a device
