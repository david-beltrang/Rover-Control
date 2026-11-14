# Rover Control

Native Android app (Kotlin + Jetpack Compose) to drive an Arduino rover over
**Bluetooth Classic SPP** and monitor its environmental telemetry. The UI is a
portrait-only dark theme, designed for the Samsung Galaxy A57.

The app talks to an **HC-05/HC-06** module. It lists only devices already paired
in Android settings; pairing itself happens in Android Bluetooth settings.

## Features

- **Connection screen** with bonded-device list, connection status, automatic
  retry (once), manual reconnect and Demo Mode.
- **Control screen** with a joystick or D-pad, speed slider (0–100 %), headlight
  switch, input-mode bottom sheet and a global **E-STOP**.
- **Telemetry screen** with five live cards (temperature, humidity, light,
  distance, inclination), freshness indicator and per-card stale badge.
- **Demo Mode** that simulates telemetry and accepts input with no Bluetooth
  hardware, so the app can be presented even if the rover fails.
- All UI text is in English and lives in `strings.xml`.

## Requirements

| Item | Requirement |
|---|---|
| Phone | Android 8.0 (API 26) or newer; target Samsung Galaxy A57 |
| Radio | Bluetooth Classic (not BLE) |
| Rover | Arduino + HC-05/HC-06 SPP module, firmware compatible with §Protocol |
| Pairing | HC-05 already paired in Android Bluetooth settings |
| App ID | `com.example.rover_control` |
| Version | `1.0.0` (versionCode 1) |

## Install the APK

A debug-signed APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it over USB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone, allow "Install unknown apps" for the file manager,
and open it.

On first launch:

- **Android 12+**: allow **Nearby devices** (`BLUETOOTH_CONNECT`). This is
  required to list paired devices and open the SPP socket.
- **Android 11 and lower**: no runtime Bluetooth permission is requested.
- If permission was denied, use **Grant permission** or **Open settings** in the
  red banner on the Connection screen.

The app uses the standard SPP UUID `00001101-0000-1000-8000-00805F9B34FB`. The
baud rate is configured on the HC-05 side (typically 9600); the phone does not
set it.

## Pair the HC-05

1. Power the rover and make sure the HC-05 LED is blinking (not already
   connected to another phone).
2. Open **Android Settings → Bluetooth**, scan and pair the module. The usual
   PIN is `1234` or `0000`.
3. Open Rover Control and tap **Refresh** on the Connection screen.
4. Tap **Connect** next to the HC-05.

The status pill turns **green** when connected. While connecting it is amber; if
the link fails or drops it turns red. If connection fails, the app retries once
automatically. Use **Reconnect** to retry manually.

> The app cannot pair devices for you, and it only shows bonded devices. If the
> list is empty, pair the module in Android settings first and refresh.

## Using the app

### Connection screen

- **Paired devices**: name, address and last-used marker.
- **Connect / Disconnect / Reconnect**.
- **Continue** opens the Control screen when connected.
- **Demo Mode (no hardware)**: starts a simulated connection. The status pill
  shows an amber **DEMO** label. Demo Mode is not persisted: it is off again on
  the next app launch or after a force-stop.

### Control screen

- **Input mode**: gear icon → bottom sheet → Joystick or D-pad. The choice is
  persisted across launches.
- **Joystick**: drag to move; a 15 % deadzone prevents drift. Commands are sent
  only when the direction changes, and `S` is sent when released or centered.
- **D-pad**: press a direction to move; release or press the center button to
  stop.
- **Speed**: 0–100 % in the UI, mapped to the firmware's 0–255 (`V` command).
  The default is 60 % (`V153`). Slider changes are debounced by ~200 ms and the
  value is restored on the next launch.
- **Headlights**: switches the white LED on/off (`W1` / `W0`). The state is
  re-sent after connecting.
- **E-STOP** (red FAB, present on Control and Telemetry): sends `S`, `X` and
  `W0`, dims/locks movement and speed/light input, and shows a **STOPPED**
  banner. **Rearm** sends `S` and restores the speed from before the stop.

### Telemetry screen

Telemetry is read as JSON lines and refreshed at 2 Hz. The header shows
`Updated X.X s ago`; if no valid line arrives for more than **3 seconds**, the
cards dim and show a **No data** badge. Missing fields keep their last known
values instead of blanking the UI.

| Card | Unit | Alert |
|---|---|---|
| Temperature | °C | **Hot** (red) above 40 °C |
| Humidity | % | **Humid** (blue) above 80 % |
| Light | raw ADC | **Dark** < 200, **Dim** 200–699, **Bright** ≥ 700 |
| Distance | cm | **Danger** (red) < 20 cm, **Near** (amber) < 50 cm; `0` means no obstacle and is shown as `—` |
| Inclination | ° | **Tilt!** (red) above 25° |

There are no graphs or history; the MVP is value cards only.

## Protocol

### App → rover (ASCII, newline-terminated)

Commands are fire-and-forget and only sent while connected.

| Command | Bytes | Meaning |
|---|---|---|
| Forward | `F\n` | Move forward at the current speed |
| Backward | `B\n` | Move backward |
| Left | `L\n` | Turn left in place |
| Right | `R\n` | Turn right in place |
| Stop | `S\n` | Stop all motors; also used for rearm |
| Speed | `V<0-255>\n` | Set speed; sent after a slider change (debounced ~200 ms) and once after connecting |
| Headlights on | `W1\n` | White LED on |
| Headlights off | `W0\n` | White LED off |
| Emergency stop | `X\n` | Hard stop / latch stopped state |

The app sends the current speed and headlight state immediately after a
successful connection (`V…`, then `W0`/`W1`). Movement is edge-triggered: it is
never sent in a periodic loop.

### Rover → app (JSON lines, 2 Hz)

```json
{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}
```

| Field | Type | Unit | Description |
|---|---|---|---|
| `t` | float | °C | Ambient temperature |
| `h` | float | % | Humidity |
| `l` | int | raw ADC (0–1023) | Ambient light |
| `d` | int | cm | Distance to obstacle; `0` = no obstacle/invalid |
| `i` | float | degrees | Inclination |

The parser tolerates unknown fields and ignores malformed lines. Lines should
be newline-terminated; telemetry is expected every 500 ms.

## Firmware compatibility

> **The current rover firmware may only support `F`, `B`, `L`, `R` and `S`.**
> The app still builds and runs, but speed (`V`), headlights (`W`) and the hard
> E-STOP (`X`) require a firmware update. If they are unavailable, movement
> still works at the firmware's fixed speed and headlights remain off.

The firmware should:

1. Read newline-terminated commands and `trim()` each line, so the `\n`
   terminator is tolerated.
2. Recognize `F/B/L/R/S` plus `V<0-255>`, `W0/W1` and `X`.
3. On `X`: immediately set both motor PWM channels to 0 and enter a latched
   `stopped` state that ignores movement commands.
4. On `S`: clear the latched E-STOP state (rearm). The app always sends `S` when
   **Rearm** is pressed, then re-sends the previous speed with `V`.
   The app also sends `S` immediately before `X` on E-STOP, so firmware that
   only understands `S` still stops.
5. Emit telemetry as one JSON object per line every 500 ms, without blocking the
   control loop (use a `millis()` schedule, not `delay()` in the parser).

Recommended E-STOP semantics: **`X` latches stopped; `S` rearms.** Do not
implement rearm only as "X toggles", because the app never sends X for rearm and
would stay stopped.

## Demo Mode

Enable **Demo Mode (no hardware)** on the Connection screen. The app:

- does not touch Bluetooth;
- force-sets the connection state to connected with `isDemo = true`;
- emits synthetic telemetry every 500 ms;
- accepts every control input, logging commands to Logcat instead of sending
  them;
- shows an amber **DEMO** pill so the state is never confused with a real link.

Demo Mode is in-memory only, so it resets to off when the app process is
restarted or force-stopped. It is ideal for UI rehearsal at the demo table.

## Build from source

Prerequisites:

- JDK 21 (the Gradle wrapper is configured for Java 21).
- Android SDK Platform 37 (only `compileSdk = 37` is required).
- Windows: `gradlew.bat`; macOS/Linux: `./gradlew`.

```bash
# Debug APK
gradlew.bat :app:assembleDebug

# Unit tests (protocol encoder + telemetry parser)
gradlew.bat :app:testDebugUnitTest
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. No release
signing configuration is included; the debug APK is signed with the Android
debug key and is suitable for installing on test phones, not for store
distribution.

## Verification status

- `assembleDebug` and `testDebugUnitTest` pass locally (JVM unit tests cover
  command encoding and telemetry parsing, including malformed/missing fields).
- The full manual checklist (T01–T24: permissions, pairing, driving, speed
  persistence, headlights, E-STOP/rearm, stale telemetry, Demo Mode, orientation,
  5-minute soak, reconnect) requires the physical rover and HC-05. It has **not**
  been executed in this build environment because no hardware is available.
  The complete checklist is in [`Implementation_plan.md`](Implementation_plan.md)
  §11.1.
- There is no firmware source in this repository; the required firmware changes
  are listed above.

## Troubleshooting

| Symptom | Check |
|---|---|
| "No paired devices" | Pair the HC-05 in Android Bluetooth settings, then tap **Refresh**. |
| "Bluetooth permission denied" | Grant **Nearby devices** permission, or open app settings. |
| "Connection failed" | Rover powered? HC-05 in range and not connected to another phone? Try **Reconnect**; the app already tried secure, insecure and channel-1 sockets. |
| Connected but rover does not move | Confirm the firmware recognizes `F/B/L/R` and that the HC-05 is wired to the Arduino's serial pins. Check Logcat tag `RoverRepository`. |
| Speed/headlights do nothing | Firmware needs `V`/`W` support. Legacy `F/B/L/R/S` firmware keeps a fixed speed and headlights off. |
| Telemetry stays at "No telemetry received yet" | Firmware must emit newline-terminated JSON at 2 Hz. Check the Arduino Serial monitor and the module's TX/RX wiring. |
| All cards show **No data** | No valid telemetry line for over 3 seconds: link lost, firmware stopped emitting, or lines are malformed. |
| E-STOP does not stop the rover | Firmware must implement `X` (and/or `S`). On legacy firmware, cut rover power; then update the firmware. |

## Project structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/rover_control/
│   ├── MainActivity.kt / RoverApplication.kt
│   ├── data/
│   │   ├── bluetooth/      # adapter, bonded devices, socket fallback
│   │   ├── protocol/       # commands, encoder, telemetry parser
│   │   ├── repository/     # RoverRepository (socket, demo, E-STOP)
│   │   └── settings/       # DataStore preferences
│   ├── ui/
│   │   ├── components/     # joystick, D-pad, cards, status pill, FAB
│   │   ├── navigation/     # bottom-nav NavHost
│   │   ├── screens/        # connection, control, telemetry + ViewModels
│   │   └── theme/          # dark palette per spec
│   └── util/
└── res/values/             # English strings, colors, theme
```

## Scope

This is the MVP defined by `Implementation_plan.md`. Out of scope: login,
cloud sync, analytics, BLE, graphs/history, light theme, in-app pairing and a
web dashboard. There is no telemetry history; the latest values are held in
memory.

## Version

**v1.0.0** — package `com.example.rover_control`, min SDK 26, target/compile SDK
37. Built with Kotlin 2.2, Jetpack Compose (Material 3), Navigation Compose,
DataStore and `org.json`.
