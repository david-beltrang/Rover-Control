# Rover Control

Native Android app (Kotlin + Jetpack Compose) to drive an Arduino rover over
**Bluetooth Classic SPP** and monitor its environmental telemetry. The Control
screen is a landscape, immersive HUD; Connection and Telemetry stay portrait.
The design targets the Samsung Galaxy A57.

The app talks to an **HC-05/HC-06** module. It lists only devices already paired
in Android settings; pairing itself happens in Android Bluetooth settings.

## Features

- **Connection screen** with bonded-device list, connection status, automatic
  retry (once), manual reconnect and Demo Mode.
- **Control HUD** in immersive landscape with PAD/STICK steering, a vertical
  speed slider (0–100 %), engine arm, turbo, brake, headlights and **E-STOP**.
- **Telemetry HUD** in portrait with temperature/humidity cards, light,
  distance and inclination panels, a live freshness indicator and a stale
  state that dims the readings.
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

The Control screen rotates to `sensorLandscape` and hides the system bars.
Connection and Telemetry return to portrait. Control and Telemetry each own a
floating HUD bottom pill that switches between **CONTROLS** and **TELEMETRY**;
the connection pill in either top bar opens the Connection screen.

- **PAD / STICK**: segmented steering-mode switch. PAD uses five touch targets
  with a central stop; STICK is a drag joystick with a 15 % deadzone. Commands
  are sent only when the direction changes, and `S` is sent when released or
  centered. The choice is persisted across launches.
- **Speed**: vertical 0–100 % slider mapped to the firmware's 0–255 (`V`
  command). The default is 60 % (`V153`). Changes are debounced by ~200 ms and
  the value is restored on the next launch.
- **ENGINE**: app-level arm/disarm state. It is ON after launch so the controls
  work immediately. Turning it off sends `S` and disables drive controls;
  turning it on re-sends the selected speed. The firmware has no engine command.
- **LIGHTS**: switches the white LED on/off (`W1` / `W0`). The state is re-sent
  after connecting.
- **TURBO**: temporarily switches to 100 % (`V255`); toggling it off restores
  the previous speed. It is disabled while stopped or with the engine off.
- **BRAKE**: immediately sets speed to 0 (`V0`) and sends `S`. It does not latch
  the E-STOP.
- **E-STOP** (red pill in the Control header and in the Telemetry top bar):
  sends `S`, `X` and `W0`, dims/locks movement and speed/light input, and shows
  a **STOPPED** banner. **Rearm** sends `S` and restores the speed from before
  the stop.

### Telemetry screen

Telemetry is read as JSON lines and refreshed at 2 Hz. A strip under the top
bar shows `Updated X.X s ago`. If no valid line arrives for more than **3
seconds**, the readings dim and a **STALE** badge appears. Missing fields keep
their last known values instead of blanking the UI.

| Panel | Unit | Display / thresholds |
|---|---|---|
| Temperature | °C | **OPTIMAL** 20–35 °C; **LOW** below 20 °C; **HIGH** above 35 °C |
| Humidity | % | **NORMAL** 30–70 %; **DRY** below 30 %; **HUMID** above 70 % |
| Light | raw ADC | **DARK** < 200, **DIM** 200–699, **BRIGHT** ≥ 700; raw value and % of the 0–1023 range |
| Distance | cm | **DANGER** < 20 cm, **CAUTION** < 30 cm, **ACTIVE** ≥ 30 cm; `0` or missing means no data |
| Inclination | ° | **STABLE** ±15°, **CAUTION** ±15–25°, **TILTED** beyond ±25°; radar bubble, pitch/roll and derived slope |

The rover protocol exposes one inclination value. The app displays it as
**PITCH**, keeps **ROLL** at 0° until firmware reports a second axis, and
derives **SLOPE** from the angle. There are no graphs or history; the MVP is
live values only.

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

HUD-only controls map onto the existing protocol: engine off sends `S`, engine
on re-sends `V`; turbo sends `V255` and restores the previous `V`; brake sends
`V0` followed by `S`. No new firmware commands are required.

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

## Companion Arduino firmware

The repository includes a ready-to-upload sketch at
[`app/codigo_Arduino.ino`](app/codigo_Arduino.ino) for the original four-motor
rover with an **Adafruit Motor Shield v1** (`AFMotor`) and an **HC-05/HC-06**
module on `SoftwareSerial(9, 10)`.

Upload it with the Arduino IDE, pair the HC-05 in Android Bluetooth settings,
connect from the app, and the Control screen works without further firmware
changes:

- **Movements:** `F`, `B`, `L`, `R` move/hold until the next movement command;
  releasing the d-pad or joystick sends `S`.
- **Speed:** `V<0-255>` updates the speed immediately, including while moving.
- **Turbo:** the app sends `V255` and restores the previous speed afterwards.
- **Brake:** the app sends `V0` followed by `S`.
- **E-STOP / rearm:** `X` stops and latches the rover; only `S` clears the latch
  and rearms. The app sends `S` before `X` and again on **Rearm**.
- **Headlights:** `W0`/`W1` are accepted but do nothing by default. Uncomment
  and set `PIN_LUCES` in the sketch if a relay, MOSFET or LED is wired.
- **Newline handling:** commands are parsed line-by-line, so the app's `\n`
  terminator is tolerated and several commands can arrive together.

The sketch does **not** emit telemetry because no temperature, humidity, light,
distance or inclination sensors are defined in the original hardware. The
Telemetry screen will therefore show **No telemetry received yet / STALE**;
controls still work normally. To enable telemetry, add the sensors and emit one
newline-terminated JSON object every 500 ms from `loop()` without using
`delay()`:

```json
{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}
```

The parser tolerates unknown fields and missing fields keep their last known
values. If you use different firmware, it must implement the same command and
telemetry contract as the app; the essentials are newline-terminated commands,
`X` latching the stopped state and `S` rearming it.

## Demo Mode

Enable **Demo Mode (no hardware)** on the Connection screen. The app:

- does not touch Bluetooth;
- force-sets the connection state to connected with `isDemo = true`;
- emits synthetic telemetry every 500 ms;
- accepts every control input, logging commands to Logcat instead of sending
  them;
- shows a **DEMO** / **DEMO MODE** label so the state is never confused with a
  real link.

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

- `assembleDebug`, `testDebugUnitTest` and `lintDebug` pass locally (JVM unit
  tests cover command encoding and telemetry parsing, including
  malformed/missing fields).
- Emulator smoke test passed for the new HUDs: landscape/immersive Control,
  portrait Telemetry, HUD navigation, PAD/STICK switching, vertical speed
  slider, engine arm/disarm, headlights, turbo, brake, E-STOP/rearm, stale
  state and telemetry scrolling.
- The companion firmware command parser was host-tested with stubbed motors for
  `F/B/L/R/S/V/X`, multiple commands per read, speed clamping, movement while
  moving, brake and E-STOP/rearm. It still needs an upload test on the physical
  Arduino and Motor Shield.
- The full manual checklist (T01–T24: permissions, pairing, driving, speed
  persistence, headlights, E-STOP/rearm, stale telemetry, Demo Mode, orientation,
  5-minute soak, reconnect) requires the physical rover and HC-05. It has **not**
  been executed in this build environment because no hardware is available.
  The complete checklist is in [`Implementation_plan.md`](Implementation_plan.md)
  §11.1. Orientation is intentionally route-based: Control is landscape,
  Connection and Telemetry are portrait.
- Telemetry emission is still a firmware-side optional feature; the companion
  sketch has no sensor definitions.

## Troubleshooting

| Symptom | Check |
|---|---|
| "No paired devices" | Pair the HC-05 in Android Bluetooth settings, then tap **Refresh**. |
| "Bluetooth permission denied" | Grant **Nearby devices** permission, or open app settings. |
| "Connection failed" | Rover powered? HC-05 in range and not connected to another phone? Try **Reconnect**; the app already tried secure, insecure and channel-1 sockets. |
| Connected but rover does not move | Confirm the sketch is uploaded and the HC-05 is wired to `SoftwareSerial(9, 10)`. If the rover was E-STOPped earlier, press **Rearm** (or power-cycle the Arduino) to clear the `X` latch. |
| Speed/turbo/brake do nothing | Use the companion sketch or firmware that implements `V<0-255>`; turbo is `V255` and brake is `V0` + `S`. |
| Headlights do nothing | The companion sketch accepts `W0`/`W1` but has no light pin by default. Set `PIN_LUCES` in the sketch and wire the light. |
| Telemetry stays at "No telemetry received yet" | The companion movement sketch does not define sensors. Add sensors and emit newline-terminated JSON at 2 Hz, or check the module's TX/RX wiring. |
| Readings turn **STALE** | No valid telemetry line for over 3 seconds: link lost, firmware stopped emitting, or lines are malformed. |
| E-STOP does not stop the rover | Firmware must implement `X` (and/or `S`). On legacy firmware, cut rover power; then upload the companion sketch. |

## Project structure

```text
app/
├── codigo_Arduino.ino      # companion firmware for the four-motor rover
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/rover_control/
    │   ├── MainActivity.kt / RoverApplication.kt
    │   ├── data/
    │   │   ├── bluetooth/      # adapter, bonded devices, socket fallback
    │   │   ├── protocol/       # commands, encoder, telemetry parser
    │   │   ├── repository/     # RoverRepository (socket, demo, E-STOP)
    │   │   └── settings/       # DataStore preferences
    │   ├── ui/
    │   │   ├── components/     # control HUD, telemetry HUD, navigation pill
    │   │   ├── navigation/     # NavHost + per-route orientation/immersive mode
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
