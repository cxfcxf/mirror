# Mirror

An Android TV app that turns your Shield TV (or any Android TV device) into an AirPlay receiver — mirror your iPad or iPhone screen directly to your TV, no Apple TV required.

Built on top of [android-airplay-server](https://github.com/jqssun/android-airplay-server) (UxPlay native engine, GPLv3) with a custom Android TV UI designed for D-pad navigation.

## Features

- **Screen mirroring** from iPhone and iPad (iOS 9+)
- **Audio streaming** — AAC and ALAC
- **Native Android TV UI** — D-pad navigable idle screen, launcher banner, TV-optimised settings
- **PIN authentication** — require a PIN for new connections
- **H.265 / HEVC** — lower bandwidth for same quality
- **Low latency** — hardware MediaCodec decode on Android TV

## Requirements

- Android TV device running Android 8+ (tested on NVIDIA Shield TV)
- iPhone or iPad on the **same Wi-Fi network** as the TV
- Android NDK 27.0.12077973 and CMake 3.22.1 to build

> **Note:** AirPlay mirroring of DRM-protected content (Netflix, Disney+, Apple TV app) is blocked by Apple at the protocol level and cannot be received.

## Build

```bash
# Clone with all native submodules
git clone --recursive https://github.com/<your-username>/mirror
cd mirror

# Create local.properties (adjust paths if your SDK is elsewhere)
cat > local.properties <<EOF
sdk.dir=/Users/$USER/Library/Android/sdk
EOF

# Install NDK and CMake via sdkmanager (one-time setup)
sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"

# Build
./gradlew assembleDebug
```

## Install

```bash
# Connect ADB to your Android TV (enable Developer Options + Network Debugging first)
adb connect <TV-IP>:5555

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.cxfcxf.mirror/.MainActivity
```

## Usage

1. Launch **Mirror** on your Android TV — the idle screen shows the device name and any active PIN
2. On your iPhone or iPad: **Control Center → Screen Mirroring → Mirror**
3. Your screen appears on the TV. Disconnect by tapping Screen Mirroring again and selecting **Stop Mirroring**

## Recommended settings (Shield TV + iPad mini)

| Setting | Value | Why |
|---|---|---|
| Resolution | Auto | iPad negotiates its native 3:2 resolution |
| Max FPS | 60 | Smooth UI animations |
| H.265 | On | Half the bandwidth at same quality |
| Overscanned | Off | Modern TVs don't crop edges |
| ALAC | On | Lossless audio for music AirPlay |

## Settings

Navigate to the **⚙ Settings** button on the idle screen (D-pad to focus, OK to open, Back to close).

| Category | Key settings |
|---|---|
| **Server** | Device name, port, auto-start |
| **Connection** | PIN requirement, allow new connections |
| **Display** | Resolution, FPS, overscan, fullscreen |
| **Codecs** | H.265, ALAC, AAC |
| **Developer** | Audio delay (A/V sync), buffer size, debug overlay |

## Architecture

```
iPhone/iPad  ──Wi-Fi──▶  Android TV (Mirror app)
                              │
                         Kotlin UI shell
                         (Compose for TV)
                              │
                         UxPlay C core (JNI)
                         mDNS · RTSP · FairPlay
                         H.264/H.265 · AAC/ALAC
                              │
                         MediaCodec → SurfaceView
```

The AirPlay engine is the proven UxPlay native C library compiled via Android NDK. The Kotlin layer handles the TV UI, service lifecycle, and surface management. The engine code is untouched.

## License

GPLv3 — inherited from UxPlay. See [LICENSE](LICENSE).

This project uses a third-party GPL library for FairPlay handling whose legal status is unclear. Intended for personal use only.
