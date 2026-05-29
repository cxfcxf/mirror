# Android TV AirPlay Receiver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android TV app that receives AirPlay mirroring from an iPad, with a D-pad-navigable TV idle screen and launcher integration, by forking the proven `jqssun/android-airplay-server` (UxPlay JNI engine, GPLv3).

**Architecture:** Fork `jqssun/android-airplay-server` — Kotlin shell + UxPlay native C engine via NDK/JNI. Keep native engine untouched. Swap in a Compose-for-TV idle screen, add leanback launcher + banner, and wire Compose-for-TV into MainActivity. Service/renderer/bridge layers stay as-is.

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.tv:tv-material:1.0.0`, Hilt, UxPlay (native C via NDK 27.0.12077973 + CMake), mDNS, MediaCodec, AudioTrack, ADB for TV deploy.

---

## Prerequisites Map

Before any code task: your machine needs NDK and CMake; your TV needs adb pairing.

```
~/Library/Android/sdk/
  ndk/27.0.12077973/          ← must exist (Task 0a)
  cmake/3.22.1/               ← must exist (Task 0a)

~/Vibecoding/mirror/
  android-airplay-server/     ← cloned here (Task 0b)
    app/src/main/
      AndroidManifest.xml
      cpp/                    ← UxPlay native source
      kotlin/io/github/jqssun/airplay/
        AirPlayApp.kt
        BootReceiver.kt
        MainActivity.kt       ← modified in Task 3
        Prefs.kt
        ui/                   ← modified in Task 4
        service/AirPlayService.kt
        bridge/NativeBridge.kt
        ...
```

## Files to Create

| Path | Purpose |
|------|---------|
| `app/src/main/res/drawable/tv_banner.png` | 320×180 TV home screen banner |
| `app/src/main/kotlin/.../ui/TvIdleScreen.kt` | New Compose-for-TV idle/receiver screen |

## Files to Modify

| Path | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | Verify/add `LEANBACK_LAUNCHER`, add `android:banner`, confirm feature flags |
| `app/build.gradle.kts` | Add `androidx.tv:tv-material:1.0.0` dependency |
| `app/src/main/kotlin/.../MainActivity.kt` | Detect TV, show `TvIdleScreen` instead of phone `MainScreen` |

---

## Task 0a: Install NDK and CMake

**Files:** none — SDK manager only

- [ ] **Step 1: Accept SDK licenses**

```bash
yes | ~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses
```

Expected: a series of license prompts, all answered "yes". No errors.

- [ ] **Step 2: Install NDK 27.0.12077973**

```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "ndk;27.0.12077973"
```

Expected: progress output, final line `done`.

- [ ] **Step 3: Verify NDK installed**

```bash
ls ~/Library/Android/sdk/ndk/
```

Expected: `27.0.12077973` directory listed.

- [ ] **Step 4: Install CMake 3.22.1**

```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "cmake;3.22.1"
```

Expected: progress output, final line `done`.

- [ ] **Step 5: Verify CMake installed**

```bash
ls ~/Library/Android/sdk/cmake/
```

Expected: `3.22.1` directory listed.

---

## Task 0b: Clone the upstream repo with all submodules

**Files:** creates `~/Vibecoding/mirror/android-airplay-server/`

- [ ] **Step 1: Clone with submodules in one command**

```bash
cd ~/Vibecoding/mirror && \
  git clone --recursive https://github.com/jqssun/android-airplay-server
```

Expected: clones main repo then fetches UxPlay, libplist, openssl-cmake, alac submodules. Final line: `Submodule path 'app/src/main/cpp/third_party/alac': checked out '...'`.

If the clone already ran without `--recursive`, run instead:

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  git submodule update --init --recursive
```

- [ ] **Step 2: Confirm all four submodules have content**

```bash
ls ~/Vibecoding/mirror/android-airplay-server/app/src/main/cpp/third_party/
```

Expected: `UxPlay  alac  libplist  openssl-cmake` (all four directories, non-empty).

```bash
ls ~/Vibecoding/mirror/android-airplay-server/app/src/main/cpp/third_party/UxPlay/lib/
```

Expected: at least `raop/` and `playfair/` directories present.

- [ ] **Step 3: Commit a tracking note (no code change)**

```bash
cd ~/Vibecoding/mirror && git init && git add android-airplay-server/ && \
  git commit -m "chore: add upstream android-airplay-server fork as working base"
```

> **Note:** We initialize a git repo at `~/Vibecoding/mirror/` to track our changes on top of the upstream fork.

---

## Task 0c: First build — prove it compiles

**Files:** `android-airplay-server/app/build.gradle.kts` (read only, fix only if build errors)

- [ ] **Step 1: Set ANDROID_HOME if needed and run the debug build**

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  ANDROID_HOME=~/Library/Android/sdk \
  ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/27.0.12077973 \
  ./gradlew assembleDebug --info 2>&1 | tail -40
```

Expected: `BUILD SUCCESSFUL` at the end. The native `airplay_native.so` will be compiled for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

> Common failure: `CMake 3.x.x or higher is required`. Fix: `app/build.gradle.kts` → `externalNativeBuild { cmake { version "3.22.1" } }`.

> Common failure: NDK not found. Fix: add `ndkVersion = "27.0.12077973"` inside `android {}` in `build.gradle.kts` if missing, and verify `ANDROID_NDK_HOME` points to the right path.

- [ ] **Step 2: Confirm the APK was created**

```bash
ls ~/Vibecoding/mirror/android-airplay-server/app/build/outputs/apk/debug/*.apk
```

Expected: `app-debug.apk` present.

---

## Task 0d: Pair TV with adb and install upstream APK (Phase 0 proof)

**Files:** none — device-side only

- [ ] **Step 1: Enable Developer Options on your Android TV**

On the TV:
1. Settings → About → scroll to **Build number** → press OK 7 times → "You are now a developer"
2. Settings → Device Preferences → Developer Options → **Network debugging** (or USB debugging) → **Enable**
3. Note the TV's IP address from Settings → Network → About (or from your router).

- [ ] **Step 2: Connect adb to the TV**

```bash
~/Library/Android/sdk/platform-tools/adb connect <TV-IP>:5555
```

Replace `<TV-IP>` with your TV's local IP address, e.g. `192.168.1.42`.

Expected: `connected to 192.168.1.42:5555` or `already connected`.

Accept the "Allow USB debugging?" dialog on the TV screen.

- [ ] **Step 3: Verify the TV is listed as a connected device**

```bash
~/Library/Android/sdk/platform-tools/adb devices -l
```

Expected: your TV IP appears with `device` status (not `offline`).

- [ ] **Step 4: Install the debug APK to the TV**

```bash
~/Library/Android/sdk/platform-tools/adb install -r \
  ~/Vibecoding/mirror/android-airplay-server/app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Performing Streamed Install` → `Success`.

- [ ] **Step 5: Launch the app on the TV**

```bash
~/Library/Android/sdk/platform-tools/adb shell am start \
  -n io.github.jqssun.airplay/.MainActivity
```

The app should open on the TV screen.

- [ ] **Step 6: Test real AirPlay mirroring from your iPad** ⬅ KEY CHECKPOINT

On the iPad:
1. Make sure iPad and TV are on the **same Wi-Fi network**.
2. Swipe down from top-right → **Screen Mirroring** → your device should appear (named `airplay` or whatever the app shows).
3. Tap it — your iPad screen should appear on the TV within a few seconds.

Expected: iPad screen mirrors to the TV. Latency should be < 300ms for still content.

> **If the device doesn't appear in iPad's Screen Mirroring list:** The mDNS advertisement may not be working. Check that the TV's AirPlay service started (look for a notification or log). Both devices must be on the same LAN subnet (not guest/IoT network segregation).

> **If this step fails**, do NOT proceed to Phase 1 UI work until the engine is debugged — the entire plan depends on the engine working.

---

## Task 1: Add TV banner asset

**Files:**
- Create: `app/src/main/res/drawable-xhdpi/tv_banner.png`

Android TV requires a 320×180 px banner for the home launcher.

- [ ] **Step 1: Generate a minimal TV banner**

```bash
cd ~/Vibecoding/mirror/android-airplay-server

# Create a Python script to generate a 320x180 banner
python3 - <<'EOF'
from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("app/src/main/res/drawable-xhdpi", exist_ok=True)

img = Image.new("RGB", (320, 180), color=(10, 10, 30))
draw = ImageDraw.Draw(img)
# Draw border
draw.rectangle([2, 2, 317, 177], outline=(100, 100, 255), width=2)
# Draw text
draw.text((20, 60), "AirPlay", fill=(255, 255, 255))
draw.text((20, 100), "Receiver", fill=(150, 150, 255))
img.save("app/src/main/res/drawable-xhdpi/tv_banner.png")
print("Banner saved.")
EOF
```

Expected: `Banner saved.` and file created at `app/src/main/res/drawable-xhdpi/tv_banner.png`.

> If `PIL` is not installed: `pip3 install Pillow` first.

> Alternative (no Python): use any image editor to create a 320×180 PNG with a dark background and white text "AirPlay Receiver", save to `app/src/main/res/drawable-xhdpi/tv_banner.png`.

- [ ] **Step 2: Verify the file exists**

```bash
ls -lh app/src/main/res/drawable-xhdpi/tv_banner.png
```

Expected: file present, size > 0.

- [ ] **Step 3: Commit**

```bash
cd ~/Vibecoding/mirror && \
  git add android-airplay-server/app/src/main/res/drawable-xhdpi/tv_banner.png && \
  git commit -m "feat: add Android TV home screen banner asset (320x180)"
```

---

## Task 2: Update AndroidManifest.xml for TV launcher + banner

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

The manifest already has `LEANBACK_LAUNCHER` and feature declarations per our research. We confirm and add the `android:banner` attribute.

- [ ] **Step 1: Read the current manifest**

```bash
cat ~/Vibecoding/mirror/android-airplay-server/app/src/main/AndroidManifest.xml
```

Check for:
1. `<category android:name="android.intent.category.LEANBACK_LAUNCHER"/>` inside MainActivity's intent-filter — if missing, add it
2. `<uses-feature android:name="android.software.leanback" android:required="false"/>` — if missing, add it
3. `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>` — if missing, add it

- [ ] **Step 2: Add `android:banner` to the `<application>` element**

Open `app/src/main/AndroidManifest.xml`. Find the `<application` opening tag and add the banner attribute:

```xml
<application
    android:name=".AirPlayApp"
    android:banner="@drawable/tv_banner"
    android:allowBackup="true"
    ...>
```

(Add `android:banner="@drawable/tv_banner"` — the existing attributes stay unchanged.)

- [ ] **Step 3: Ensure the LEANBACK_LAUNCHER intent-filter is present in MainActivity**

The `<activity android:name=".MainActivity">` should have (or gain) a second `<intent-filter>` block alongside the standard launcher:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
</intent-filter>
```

If it's already there, no change needed.

- [ ] **Step 4: Rebuild and verify**

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  ANDROID_HOME=~/Library/Android/sdk ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Install and verify app appears in TV launcher**

```bash
~/Library/Android/sdk/platform-tools/adb install -r \
  app/build/outputs/apk/debug/app-debug.apk
```

On the TV: navigate to the app launcher row. Expected: your app appears with the banner image.

- [ ] **Step 6: Commit**

```bash
cd ~/Vibecoding/mirror && \
  git add android-airplay-server/app/src/main/AndroidManifest.xml && \
  git commit -m "feat: add TV banner and leanback launcher category to manifest"
```

---

## Task 3: Add Compose-for-TV dependency

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Check current Compose BOM version**

```bash
grep -n "compose.bom\|compose-bom" \
  ~/Vibecoding/mirror/android-airplay-server/app/build.gradle.kts
```

Note the BOM version. Compose-for-TV is compatible with any recent Compose BOM.

- [ ] **Step 2: Add the TV material dependency**

Open `app/build.gradle.kts`. In the `dependencies { }` block, add after the existing Compose dependencies:

```kotlin
// Compose for TV
implementation("androidx.tv:tv-material:1.0.0")
```

- [ ] **Step 3: Sync and build**

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  ANDROID_HOME=~/Library/Android/sdk ./gradlew assembleDebug 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL` with `tv-material` downloaded.

- [ ] **Step 4: Commit**

```bash
cd ~/Vibecoding/mirror && \
  git add android-airplay-server/app/build.gradle.kts && \
  git commit -m "feat: add androidx.tv:tv-material dependency for Compose-for-TV UI"
```

---

## Task 4: Create the TV idle screen Composable

**Files:**
- Create: `app/src/main/kotlin/io/github/jqssun/airplay/ui/TvIdleScreen.kt`

This is the full-screen "waiting for connection" view shown on the TV. It must be D-pad-navigable (no touch required), show the device name and PIN, and have a Settings button.

- [ ] **Step 1: Write the composable (no test needed — UI-only, verified visually)**

Create `app/src/main/kotlin/io/github/jqssun/airplay/ui/TvIdleScreen.kt`:

```kotlin
package io.github.jqssun.airplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText

/**
 * Full-screen AirPlay idle/ready screen designed for D-pad navigation on Android TV.
 *
 * @param deviceName   Advertised AirPlay receiver name (shown on screen + in iPad's list).
 * @param pin          Current 4-digit PIN, or null/empty if PIN auth is disabled.
 * @param isRunning    True when the AirPlay server is actively listening.
 * @param onSettings   Called when the user navigates to and activates the Settings button.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvIdleScreen(
    deviceName: String,
    pin: String?,
    isRunning: Boolean,
    onSettings: () -> Unit,
) {
    val settingsFocusRequester = remember { FocusRequester() }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A1E)),
        ) {
            // ── Centre column: status + instructions ──────────────────────
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TvText(
                    text = if (isRunning) "Ready to Mirror" else "Starting…",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(Modifier.height(8.dp))

                TvText(
                    text = "Device name: $deviceName",
                    fontSize = 24.sp,
                    color = Color(0xFFAAAAAA),
                )

                if (!pin.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Divider(
                        modifier = Modifier.width(200.dp),
                        color = Color(0xFF444466),
                    )
                    Spacer(Modifier.height(8.dp))
                    TvText(
                        text = "PIN: $pin",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF88AAFF),
                    )
                    TvText(
                        text = "Enter this PIN on your iPad when prompted",
                        fontSize = 18.sp,
                        color = Color(0xFF777777),
                    )
                }

                Spacer(Modifier.height(8.dp))

                TvText(
                    text = "On your iPad: Control Center → Screen Mirroring → $deviceName",
                    fontSize = 18.sp,
                    color = Color(0xFF666666),
                )
            }

            // ── Top-right: Settings button ─────────────────────────────────
            Button(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .focusRequester(settingsFocusRequester),
            ) {
                TvText("⚙  Settings")
            }
        }
    }

    // Give D-pad focus to the Settings button on first composition so
    // the remote's OK key can reach it immediately.
    LaunchedEffect(Unit) {
        settingsFocusRequester.requestFocus()
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  ANDROID_HOME=~/Library/Android/sdk ./gradlew compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no errors referencing `TvIdleScreen.kt`.

> If you see `Unresolved reference: tv.material3.Button` — verify the `androidx.tv:tv-material:1.0.0` dependency was added in Task 3.

> If you see `Unresolved reference: Divider` — swap for `HorizontalDivider` (newer Material3 alias): replace `Divider(` with `HorizontalDivider(`.

- [ ] **Step 3: Commit**

```bash
cd ~/Vibecoding/mirror && \
  git add android-airplay-server/app/src/main/kotlin/io/github/jqssun/airplay/ui/TvIdleScreen.kt && \
  git commit -m "feat: add TvIdleScreen composable — D-pad-navigable TV idle screen"
```

---

## Task 5: Wire TvIdleScreen into MainActivity

**Files:**
- Modify: `app/src/main/kotlin/io/github/jqssun/airplay/MainActivity.kt`

We need MainActivity to detect whether it's running on an Android TV and show `TvIdleScreen` instead of the existing phone `MainScreen`. The service binding and engine startup logic stays unchanged.

- [ ] **Step 1: Read current MainActivity.kt**

```bash
cat ~/Vibecoding/mirror/android-airplay-server/app/src/main/kotlin/io/github/jqssun/airplay/MainActivity.kt
```

Identify:
1. Where `MainScreen(...)` is called inside `setContent { }`.
2. What state variables are available (device name, PIN, isRunning) from the ViewModel or direct service binding.
3. Any existing function that navigates to settings.

- [ ] **Step 2: Read the ViewModel to find exact state field names**

```bash
find ~/Vibecoding/mirror/android-airplay-server \
  -name "*.kt" -path "*/viewmodel/*" | xargs grep -n "var\|val\|StateFlow\|LiveData" | head -40
```

Look for fields corresponding to: the advertised receiver name, the current PIN, and the server running/stopped state. Note their exact Kotlin names — you'll use them in Step 3.

Example fields you might find (names will differ in reality):
- `val serverName: StateFlow<String>` — the advertised AirPlay device name
- `val displayPin: StateFlow<String?>` — current PIN or null
- `val isRunning: StateFlow<Boolean>` — true when server is listening

- [ ] **Step 3: Add a TV-detection helper near the top of the file (after imports)**

```kotlin
import android.content.pm.PackageManager

/** Returns true when the app is running on an Android TV / leanback device. */
private fun Context.isAndroidTv(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
```

- [ ] **Step 4: Replace the `MainScreen(...)` call in `setContent` with a conditional**

Using the exact ViewModel field names you discovered in Step 2, find the existing phone-UI call inside `setContent { AirPlayTheme { ... } }` and wrap it:

```kotlin
if (isAndroidTv()) {
    TvIdleScreen(
        // Replace <field> with the ACTUAL names from Step 2:
        deviceName = viewModel.<serverNameField>.collectAsState().value,
        pin        = viewModel.<pinField>.collectAsState().value,
        isRunning  = viewModel.<isRunningField>.collectAsState().value,
        onSettings = {
            // Use whichever navigation pattern the existing code uses:
            //   navController.navigate("settings")   ← if Compose nav
            //   startActivity(Intent(this@MainActivity, SettingsActivity::class.java))  ← if Activity
        },
    )
} else {
    MainScreen(/* leave original arguments exactly as they were */)
}
```

`collectAsState()` needs `import androidx.compose.runtime.collectAsState`.

- [ ] **Step 5: Add the import for TvIdleScreen**

At the top of `MainActivity.kt`, add:
```kotlin
import io.github.jqssun.airplay.ui.TvIdleScreen
import androidx.compose.runtime.collectAsState
```

- [ ] **Step 6: Compile**

```bash
cd ~/Vibecoding/mirror/android-airplay-server && \
  ANDROID_HOME=~/Library/Android/sdk ./gradlew compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Build full APK**

```bash
ANDROID_HOME=~/Library/Android/sdk ./gradlew assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
cd ~/Vibecoding/mirror && \
  git add android-airplay-server/app/src/main/kotlin/io/github/jqssun/airplay/MainActivity.kt && \
  git commit -m "feat: show TvIdleScreen on Android TV, phone MainScreen on non-TV devices"
```

---

## Task 6: Install to TV and verify end-to-end

**Files:** none — device testing

- [ ] **Step 1: Install updated APK to TV**

```bash
~/Library/Android/sdk/platform-tools/adb install -r \
  ~/Vibecoding/mirror/android-airplay-server/app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success`.

- [ ] **Step 2: Launch and inspect the TV idle screen**

```bash
~/Library/Android/sdk/platform-tools/adb shell am start \
  -n io.github.jqssun.airplay/.MainActivity
```

Expected on TV:
- Large "Ready to Mirror" text centred on screen
- Device name shown
- PIN shown (if PIN mode enabled in settings)
- "⚙ Settings" button in top-right, auto-focused (highlighted by D-pad)
- No touch required to interact — D-pad navigates to the Settings button

- [ ] **Step 3: Verify app tile in TV launcher**

Press the Home button on the TV remote. Navigate to the app row. Expected: the app appears with the `tv_banner.png` banner (dark background, "AirPlay Receiver" text).

- [ ] **Step 4: Test AirPlay mirroring end-to-end** ⬅ FINAL PROOF

From your iPad (same Wi-Fi network as the TV):
1. Control Center → **Screen Mirroring**
2. Your TV's device name appears in the list
3. Tap it → accept PIN if prompted
4. iPad screen mirrors to the TV at full resolution

Expected: iPad content visible on TV, latency acceptable (< 300 ms for video, < 1s for static content).

- [ ] **Step 5: Test that phone/emulator still shows original phone UI**

```bash
~/Library/Android/sdk/platform-tools/adb -e install -r \
  app/build/outputs/apk/debug/app-debug.apk 2>/dev/null || \
  echo "No emulator attached — skip if no emulator running"
```

If the Android TV emulator (`Android_TV_API_34`) is running:
```bash
~/Library/Android/sdk/emulator/emulator -avd Android_TV_API_34 &
# wait ~30s for boot, then:
~/Library/Android/sdk/platform-tools/adb -e install -r app/build/outputs/apk/debug/app-debug.apk
```

The emulator should also show `TvIdleScreen` (it reports `FEATURE_LEANBACK`). The mDNS advertisement won't reach your iPad through the Mac's NAT, so mirroring won't work from the emulator — that's expected.

- [ ] **Step 6: Final commit**

```bash
cd ~/Vibecoding/mirror && \
  git tag v0.1.0 -m "Phase 1 complete: Android TV idle screen + working AirPlay mirroring"
```

---

## Verification Summary

| Checkpoint | How to verify |
|---|---|
| NDK + CMake installed | `ls ~/Library/Android/sdk/ndk/27.0.12077973` exits 0 |
| Submodules populated | `ls .../cpp/third_party/UxPlay/lib/` shows raop/ and playfair/ |
| Build succeeds | `./gradlew assembleDebug` → `BUILD SUCCESSFUL` |
| TV adb paired | `adb devices` lists TV IP with `device` status |
| **Mirroring works (Phase 0)** | iPad Screen Mirroring → TV device visible → screen mirrors |
| Banner in launcher | TV home row shows dark banner with "AirPlay Receiver" |
| TV idle screen | D-pad navigable screen with device name, PIN, instructions |
| **Mirroring still works (Phase 1)** | Repeat AirPlay test with the new build |

---

## Phase 2 Notes (not in scope yet)

After Phase 1 is proven:

- **Video-URL "remote" mode:** Handle `/play`, `/scrub`, `/rate`, `/stop` AirPlay HTTP endpoints. The native bridge currently focuses on mirroring (RTSP). This needs a new HTTP handler in the JNI layer + ExoPlayer/Media3 for URL playback + position reporting back to the iPad.
- This is its own spec → plan → implementation cycle once Phase 1 is in daily use.

---

## Troubleshooting

**`iPad doesn't see the TV in Screen Mirroring`**
- Confirm both devices on same Wi-Fi SSID and same LAN subnet (not guest VLAN)
- Check TV firewall: `adb shell iptables -L` — should allow incoming TCP on the AirPlay port (default 7000)
- Check app notifications: the AirPlay service should show a "Server running" persistent notification
- Multicast must be allowed: the `CHANGE_WIFI_MULTICAST_STATE` permission is declared, but some TVs block multicast at the firmware level

**`BUILD FAILED: NDK not found`**
- `export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/27.0.12077973` before gradle, or add to `local.properties`: `ndk.dir=/Users/xuefengchen/Library/Android/sdk/ndk/27.0.12077973`

**`CMake Error: Could not find cmake`**
- In `app/build.gradle.kts`, inside `externalNativeBuild { cmake { } }` add: `version = "3.22.1"`

**`Unresolved reference: androidx.tv`**
- Confirm `implementation("androidx.tv:tv-material:1.0.0")` is in `app/build.gradle.kts` dependencies and Gradle sync succeeded

**`TvIdleScreen shows but mirroring stopped working`**
- The TvIdleScreen path must still eventually call the same service-start code the phone path calls; check that `AirPlayService` is being started in the TV branch of MainActivity
