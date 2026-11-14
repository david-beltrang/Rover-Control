# Implementation Plan — Rover Control App (Android)

**Project:** Rover Explorador y Monitor Ambiental — Mobile Control App
**File:** `implementation_plan.md`
**Version:** 1.0 — handoff to Opencode for implementation
**Target device:** Samsung Galaxy A57 (One UI 9.x, reports Android 17 → treat as API 34+)
**Deadline:** ~10 calendar days (demo on 2026-11-26)
**Stack owner:** (your name)

---

## 0. TL;DR for the implementer (read this first)

Build a **native Android app** in **Kotlin + Jetpack Compose** that connects over **Bluetooth Classic SPP** to an **HC-05/HC-06** module on an Arduino rover. The app has **two main screens** (Control, Telemetry) plus a **Connection screen**, supports a **dark theme**, allows switching between **joystick** and **D-pad** input modes, and includes a **Demo Mode** that simulates telemetry so the UI is presentable even if hardware fails.

- **Commands out (fire-and-forget):** `F\n`, `B\n`, `L\n`, `R\n`, `S\n`, `V<0-255>\n`, `W1\n`/`W0\n`, `X\n`
- **Telemetry in:** JSON lines every 500 ms: `{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}\n`
- **No login, no cloud, no analytics.** Everything local.
- **Deliverable:** installable debug APK + GitHub repo + README.

---

## 1. Overview

### 1.1 Goal
Build a native Android control app that lets the user:
1. Connect to the rover's Bluetooth module (HC-05/HC-06) and manage the connection.
2. Drive the rover (direction, speed) using either a **virtual joystick** or a **D-pad** (switchable from settings).
3. Turn the **white LED headlights** on/off.
4. Trigger an **emergency stop** that is always visible and reachable.
5. View **live telemetry** (temperature, humidity, light, distance, inclination) on a dedicated screen, refreshing at **2 Hz**.

### 1.2 Non-goals (explicitly out of scope)
- No authentication / login / signup.
- No cloud sync, no analytics, no crash reporting SDKs.
- No offline database, no session history persistence.
- No iOS support.
- No BLE (Bluetooth Low Energy) — only Bluetooth Classic SPP.
- No graphs/charts in telemetry screen (values only).
- No threshold configuration from the app (thresholds are fixed in firmware).
- No auto/autonomous mode toggle from the app (that logic lives in the firmware).
- No account for Arduino Cloud or any third-party service.

### 1.3 Why these decisions
- **Kotlin + Compose:** the user knows Kotlin; Compose makes the dark theme, state handling, and custom joystick straightforward; rapid iteration.
- **Bluetooth Classic SPP:** the user's existing HC-05/HC-06 setup already works with the Arduino app and uses SPP. BLE would require firmware changes.
- **JSON lines:** trivially parseable, human-readable in logs, easy to extend.
- **Fire-and-forget commands:** simplest robust option; no ACK complexity needed at this scale.

---

## 2. Pending items (resolve before/while building)

Flagged as `[PENDING]` — the implementer should treat the rest of the spec as authoritative and only block on these:

| # | Item | Owner | Impact | Default if unresolved |
|---|---|---|---|---|
| P1 | Confirm BT module is **HC-05 or HC-06** (Classic SPP) | User | If ESP32-BLE only, this entire plan needs rework | Assume HC-05/HC-06 Classic |
| P2 | Share current `.ino` firmware `loop()` and Serial/SoftwareSerial pins | User | Needed to confirm command compatibility (`V`, `W`, `X` may need firmware additions) | Assume firmware supports only `F/B/L/R/S` initially; flag `V/W/X` as "firmware update required" in README |
| P3 | Provide **visual reference** for dark theme | User | Cosmetic | Use a sleek dark palette defined in §6.5 |
| P4 | Confirm whether Arduino sends telemetry today | User | If not, firmware must be extended to emit JSON every 500 ms | Assume firmware needs telemetry emission added |

> **These four items do not block starting the app.** The implementer should scaffold the app against the spec, and only firmware-side adjustments depend on P2/P4.

---

## 3. Tech stack

| Layer | Choice | Notes |
|---|---|---|
| Language | **Kotlin 2.x** | Matches user's skillset |
| UI framework | **Jetpack Compose** | Declarative, dark-theme friendly, custom joystick via Canvas |
| Min SDK | **26** (Android 8.0) | Covers ~99% of devices; simpler permission model than 23 |
| Compile / Target SDK | **35** | Latest stable at build time; adjust if Play/Studio requires |
| Architecture | **MVVM** with `ViewModel` + `StateFlow` + Compose `collectAsStateWithLifecycle()` | No Hilt; manual DI via `Application` |
| Async | **Kotlin Coroutines + Flow** | Bluetooth I/O on `Dispatchers.IO` |
| Bluetooth | **Android framework `BluetoothAdapter` + `BluetoothSocket`** (SPP, UUID `00001101-0000-1000-8000-00805F9B34FB`) | No third-party BT lib needed; framework support is complete for Classic SPP |
| JSON parsing | **`kotlinx.serialization`** or `org.json` | `kotlinx.serialization` preferred for type safety |
| Navigation | **Navigation Compose** (`androidx.navigation:navigation-compose`) | Single-activity architecture |
| Dependency injection | **Manual** via `Application` singleton | Avoid Hilt for scope |
| Build | **Gradle (Kotlin DSL, `build.gradle.kts`)** | Standard Android Studio template |
| Testing | **JUnit 4 + Compose UI Test** (light) | Focus on manual test checklist per §11 |

---

## 4. Architecture

### 4.1 Layer diagram


### 4.2 State management rules
- **Single source of truth:** `RoverRepository` holds `ConnectionState` and the latest `Telemetry`. ViewModels derive UI state from these flows.
- **Commands:** ViewModels call `repository.sendCommand(...)` — never touch the socket directly.
- **Connection lifecycle:** owns the socket; opens on connect, closes on disconnect; reads and writes on coroutines scoped to a `SupervisorJob` inside the repository.
- **Recomposition safety:** Compose state is immutable data classes; mutations happen only in ViewModels.

### 4.3 Threading rules
- All Bluetooth I/O on `Dispatchers.IO`.
- Compose rendering on `Main`.
- Inbound telemetry parsed on `Dispatchers.Default`, then emitted to `MutableSharedFlow<Telemetry>`.
- Outgoing commands queued in a `Channel<RoverCommand>` (capacity = `Channel.UNLIMITED`) consumed by a single writer coroutine — guarantees ordered writes and prevents interleaving.

### 4.4 Reconnection strategy (minimum viable)
- If the socket throws `IOException` during read/write → transition to `DISCONNECTED`, expose `lastError` in `ConnectionState`.
- Auto-retry **once** with 1 s delay; if that fails, surface error to UI.
- Manual "Reconnect" button on Connection screen.

---
## 5. Bluetooth protocol

### 5.1 Connection parameters
- **Transport:** Bluetooth Classic, RFCOMM, SPP
- **Service UUID:** `00001101-0000-1000-8000-00805F9B34FB`
- **Baud (paired device side):** 9600 (typical HC-05), app side doesn't set baud — SPP abstracts it
- **Fallback socket:** if `createRfcommSocketToServiceRecord` fails on HC-05 clones, retry with `createInsecureRfcommSocketToServiceRecord` (and as last resort, reflection on `createRfcommSocket` with channel 1 — most HC-05 clones need this). Implement all three attempts in order.
- **Pairing:** assume the device is **already paired** in Android settings. The app lists **bonded devices only** — no in-app pairing flow (keeps scope tight and matches user's existing setup).

### 5.2 Outgoing commands (app → rover)

All commands are ASCII and newline-terminated (`\n`). Fire-and-forget.

| Command | Bytes | Meaning |
|---|---|---|
| Forward | `F\n` | Move forward at current speed |
| Backward | `B\n` | Move backward at current speed |
| Left | `L\n` | Turn left in place |
| Right | `R\n` | Turn right in place |
| Stop | `S\n` | Stop all motors |
| Speed | `V<0-255>\n` | Set speed. Sent **once** when slider changes (debounced ~200 ms) |
| White LED on | `W1\n` | Headlights on |
| White LED off | `W0\n` | Headlights off |
| Emergency stop | `X\n` | Hard stop; equivalent to `S` + disable motors |

**Rules:**
- Movement commands (`F/B/L/R/S`) are only sent on **button-press edges** or **joystick state changes**, never in a periodic loop (avoids flooding HC-05).
- The **current speed** is re-sent whenever it changes and also once after connect as a sync (`V<current>\n`).
- **Debounce** speed slider to ~200 ms.
- **Do not** send anything while `ConnectionState != CONNECTED`.

> `V`, `W`, `X` are **additions** to the current firmware. If firmware (P2) can't be updated in time, the app should still work with `F/B/L/R/S` and the README must state that speed and light control need firmware support.

### 5.3 Telemetry (rover → app)

JSON object per line, terminated by `\n`, emitted at **2 Hz (every 500 ms)**:

```json
{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}
```

# Rover Control – Android Application Specification

## 5.3 Telemetry Protocol

### Telemetry fields

|Field|Type|Unit|Description|
|---|---|---|---|
|`t`|float|°C|Ambient temperature|
|`h`|float|%|Humidity|
|`l`|int|raw ADC (0-1023)|Ambient light level|
|`d`|int|cm|Distance to obstacle (0 = no obstacle/invalid)|
|`i`|float|degrees|Inclination from IMU|

### Parser tolerance rules

- Ignore lines that don't start with `{` and end with `}`.
- Ignore unknown fields (forward-compatible).
- Ignore lines that fail JSON parsing (log to a debug counter, don't crash).
- Missing fields → treat the last known value (do not blank the UI).
- Values are considered stale if no telemetry line arrives for more than 3 seconds → UI shows a `"No data"` badge on each card.

### Optional future fields

Ignore for MVP:

- `a`: Array of alert codes, e.g. `["HOT","DRY"]`.
- `ts`: Millisecond timestamp.

Reserve these keys, but don't rely on them.

## 5.4 Encoding/Decoding Implementation Notes

**Writer:**

kotlin

```
BufferedWriter(
    OutputStreamWriter(socket.outputStream, Charsets.US_ASCII)
)
```

Call `flush()` after each command.

**Reader:**

kotlin

```
BufferedReader(
    InputStreamReader(socket.inputStream, Charsets.US_ASCII)
)
```

Loop `readLine()` in a dedicated coroutine.

**Error handling:**

All exceptions in reader/writer:

- Emit to `ConnectionState.error`.
- Close socket.
- Stop loop.

---

# 6. Screens & UX

## 6.1 Navigation Graph

text

```
Start (launch)
   │
   ▼
ConnectionScreen ──[connected]──► ControlScreen ◄──► TelemetryScreen
   ▲                                    │
   │                                    └─ top app bar shows connection status
   └──────────────[disconnect]──────────┘
```

- Single activity (`MainActivity`).
- Compose `NavHost` with routes:
    - `connection`
    - `control`
    - `telemetry`

### Navigation behavior

- Bottom navigation bar with two items: Control and Telemetry.
- Bottom navigation items disabled until connected.
- A persistent connection status pill in the top app bar:
    - Green = connected.
    - Amber = connecting.
    - Red = disconnected.

### Emergency Stop

Emergency Stop (E-STOP) button:

- Available as a floating action button (FAB).
- Located at the bottom-right of both Control and Telemetry screens.
- Always visible.
- High-contrast red.

## 6.2 Connection Screen

### Layout (top → bottom)

1. **App title:** `"Rover Control"` + small subtitle `"Monitor Ambiental"`.
2. **Status card:** Current connection state:
    - Disconnected
    - Connecting…
    - Connected to HC-05
3. **Paired devices list:**
    
    - `LazyColumn` of bonded Bluetooth devices.
    - Each row shows:
        - Device name.
        - MAC address.
        - A `"Connect"` button (or the row itself is tappable).
    
    Empty state:
    
    `"No paired devices — pair your HC-05 in Android settings first."`
    

### Actions

- Refresh button (re-reads bonded devices).
- Disconnect button (visible only when connected).
- Demo Mode toggle/button (see §12).

### Behavior

- Request runtime permissions on first open (§9).
- If Bluetooth is disabled:
    - Show banner `"Bluetooth is off"`.
    - Show button `"Enable Bluetooth"` (launches `BluetoothAdapter.ACTION_REQUEST_ENABLE` intent).
- If no paired devices:
    - Show empty state.
- Tapping a device:
    - Attempts connection.
    - Transitions to Control on success.
    - Shows Snackbar on failure.

### Demo Mode

No Bluetooth required. Enables Control and Telemetry screens with simulated data.

### Bottom hint

`"Tip: pair HC-05 in Android Bluetooth settings, then return here."`

## 6.3 Control Screen

### Layout (top → bottom)

**Top app bar:**

- Title `"Control"`.
- Connection status pill on the right.
- Gear icon (opens input-mode toggle sheet — see below).

**Main control area (flexible):**

If Joystick mode is active:

- A large circular virtual joystick centered on screen (~60% of width).
- Above it, small labels `"Fwd / Back / Left / Right"` fade in when displaced.

If D-pad mode is active:

- Four large directional buttons arranged in a D-pad.
- Center Stop button.

**Speed slider row:**

- Label `"Speed [value]%"`.
- Slider with range `0..100`, step `5`.
- Internally mapped to `0..255` when sent.
- Persisted across sessions via DataStore (see §6.6).

**Bottom action row:**

- `"Headlights"` toggle (white LED): Switch or ToggleButton.
- `"Reset Speed"` button (sets slider to default 60%).

**FAB:**

- E-STOP (red, `X` command).
- Also forces slider to 0 visually.
- Disables movement until user re-enables via a small `"Rearm"` chip (see below).

### Joystick Behavior

Custom Canvas-based joystick.

On drag:

1. Determine direction using dominant axis and sign.
2. Direction mapping:
    - Forward: `F`
    - Backward: `B`
    - Left: `L`
    - Right: `R`
3. Diagonal drags pick the dominant axis. Only cardinal directions are sent.

Do NOT send continuous streams.

- Send the command on direction change (edge-triggered).
- When the joystick returns to center, send `S\n`.
- On release, send `S\n`.

**Deadzone:** Inner 15% radius → treated as center → `S`.

### D-pad Behavior

- Press-and-hold on a direction button:
    - Send command on press-down.
    - Send `S\n` on press-up.
- If user drags to another button:
    - Cancel and send the new command.
- Center Stop button sends `S\n`.

### E-STOP / Rearm

Pressing E-STOP:

1. Sends `X\n`.
2. Sends `W0\n` (turn off headlights for visibility).
3. Sets a `"STOPPED"` banner across the top of the screen.
4. Disables all movement input.

A `"Rearm"` button appears in the banner.

Pressing it:

- Clears the banner.
- Re-enables input.

**Arduino-side requirement:** Firmware must reset motion state on `X`. See firmware notes in §16.

### Input Mode Toggle

- Gear icon in top bar opens a `ModalBottomSheet`.
- Contains a `SegmentedButton` for:
    - Joystick
    - D-pad
- Choice persists via DataStore.

## 6.4 Telemetry Screen

### Layout (top → bottom)

**Top app bar:**

- Title `"Telemetry"`.
- Connection status pill.

**Last update indicator:**

- `"Updated 0.3 s ago"` text.
- Recomputes every 500 ms.
- Turns gray if more than 3 seconds since last line (`"Stale"`).

### Telemetry cards

Grid of five cards:

- Two columns.
- Last card spans full width.

|Card|Value|Unit|Icon|Color / Hint|
|---|---|---|---|---|
|Temperature|Big number|°C|Thermometer|Red if > 40 °C|
|Humidity|Big number|%|Water drop|Blue if > 80%|
|Light|Raw ADC|—|Sun|Dark / Dim / Bright based on thresholds|
|Distance|Big number|cm|Radar|Danger (red) if < 20 cm; Near (amber) if < 50 cm|
|Inclination|Big number|°|Tilt|Tilt! (red) if > 25°|

### FAB

E-STOP (same as Control screen).

### Graphs and history

No graphs, no history — MVP is value cards only, per user decision.

### Staleness handling

If:

kotlin

```
now - lastTelemetryMillis > 3000
```

Then:

- Overlay a subtle `"No data"` chip on each card.
- Dim the values.

## 6.5 Shared Components

|Component|Responsibility|
|---|---|
|`ConnectionStatusPill(state: ConnectionState)`|Colored pill in top bars|
|`SpeedSlider(value, onChange)`|Labeled slider|
|`Joystick(state, onDirectionChange, onRelease)`|Custom Canvas|
|`DirectionalPad(onDirectionDown, onDirectionUp, onStop)`|Directional controls|
|`TelemetryCard(icon, title, value, unit, hintColor)`|Telemetry display|
|`EmergencyStopFab(onClick)`|Emergency Stop button|
|`StaleBadge(visible: Boolean)`|Stale-data indicator|

## 6.6 Theme (Dark)

Compose `MaterialTheme` with a custom `darkColorScheme`:

|Token|Hex (suggested)|Usage|
|---|---|---|
|background|`#0B0F14`|Full-screen background|
|surface|`#141A21`|Cards, sheets|
|surfaceVariant|`#1E2630`|Elevated surfaces|
|primary|`#00E5FF`|Accents, active slider track, connected pill|
|secondary|`#7C4DFF`|Joystick knob|
|error|`#FF3D57`|E-STOP, danger states|
|onBackground|`#E6EDF3`|Primary text|
|onSurfaceVariant|`#9AA6B2`|Secondary text|
|warning|`#FFC048`|Amber status pill|

### Typography

Material 3 defaults.

Override the `"display"` style for telemetry big numbers:

kotlin

```
fontWeight = FontWeight.Bold
fontSize = 40.sp
```

### System bars

- No light theme in v1 (always dark).
- Status bar and navigation bar set to `#0B0F14`.
- Light icons.

### Shape

Rounded corners everywhere:

- Cards: 16 dp.
- FAB: 24 dp.

### Visual reference

User will provide a visual reference (P3).

Implementer should tune `Color.kt` once P3 arrives. The palette above is the safe default.

## 6.7 String Resources

File: `res/values/strings.xml`

All UI text in English (user decision).

No hardcoded strings in Compose. Everything must go through:

kotlin

```
stringResource(R.string.example_string)
```

This keeps future internationalization (i18n) trivial.

## 6.8 Settings Persistence (DataStore Preferences)

Persist the following values:

|Key|Type|Default|Description|
|---|---|---|---|
|`input_mode`|String|`"joystick"`|`"joystick"` or `"dpad"`|
|`last_speed`|Int (0..255)|`153`|Default 60%|
|`headlights_on`|Boolean|`false`|Headlights state|
|`last_device_mac`|String?|`null`|Auto-select on Connection screen if present|

Use:

kotlin

```
androidx.datastore:datastore-preferences
```

No SharedPreferences.

---

# 7. Project Structure

text

```
app/src/main/
├── AndroidManifest.xml
├── java/com/rovercontrol/app/
│   ├── MainActivity.kt
│   ├── RoverApplication.kt
│   ├── data/
│   │   ├── bluetooth/
│   │   │   ├── BluetoothManager.kt
│   │   │   ├── BluetoothConstants.kt
│   │   │   ├── ConnectionState.kt
│   │   │   └── PairedDevice.kt
│   │   ├── protocol/
│   │   │   ├── RoverCommand.kt
│   │   │   ├── CommandEncoder.kt
│   │   │   ├── Telemetry.kt
│   │   │   └── TelemetryParser.kt
│   │   ├── repository/
│   │   │   └── RoverRepository.kt
│   │   └── settings/
│   │       └── SettingsRepository.kt   // DataStore
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt
│   │   │   ├── Theme.kt
│   │   │   └── Type.kt
│   │   ├── navigation/
│   │   │   └── RoverNavHost.kt
│   │   ├── screens/
│   │   │   ├── connection/
│   │   │   │   ├── ConnectionScreen.kt
│   │   │   │   └── ConnectionViewModel.kt
│   │   │   ├── control/
│   │   │   │   ├── ControlScreen.kt
│   │   │   │   └── ControlViewModel.kt
│   │   │   └── telemetry/
│   │   │       ├── TelemetryScreen.kt
│   │   │       └── TelemetryViewModel.kt
│   │   └── components/
│   │       ├── ConnectionStatusPill.kt
│   │       ├── SpeedSlider.kt
│   │       ├── Joystick.kt
│   │       ├── DirectionalPad.kt
│   │       ├── TelemetryCard.kt
│   │       ├── EmergencyStopFab.kt
│   │       ├── StaleBadge.kt
│   │       └── InputModeSheet.kt
│   └── util/
│       ├── Permissions.kt
│       └── Extensions.kt
└── res/
    ├── values/
    │   ├── strings.xml
    │   ├── colors.xml
    │   └── themes.xml
    ├── mipmap-*/ (launcher icons)
    └── xml/ (backup rules, data extraction rules)
```

---

# 8. Dependencies

File: `app/build.gradle.kts`

kotlin

```
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.09.03"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.activity:activity-compose:1.9.2")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.2")

// DataStore (settings)
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// JSON parsing
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
// OR fallback: android org.json (built-in, no dependency)

// Testing (optional, light)
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-tooling")
```

Versions are indicative — implementer should let Android Studio suggest current stable versions.

---

# 9. Permissions & Manifest

## 9.1 AndroidManifest.xml

xml

`BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` are runtime permissions on API 31+.

The `neverForLocation` flag is safe because we only list already-bonded devices.

## 9.2 Runtime Permission Flow

On Connection screen first composition:

1. Check `BLUETOOTH_CONNECT`.
2. If not granted:
    - Show rationale dialog.
    - Request via `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`.

If permission denied:

- Show blocking banner with `"Open settings"` button.

If Bluetooth adapter is off:

- Show `"Enable Bluetooth"` banner.
- Launch the appropriate enable intent.

## 9.3 Manifest Bits

xml

```
android:theme="@style/Theme.RoverControl"
android:usesCleartextTraffic="false"
android:screenOrientation="portrait"
```

Requirements:

- Dark theme, no light variant.
- No network at all.
- Portrait orientation to keep the layout predictable for the demo.

---

# 10. Implementation Phases

Each phase should be independently demoable — if we run out of time, cut from the bottom.

## Phase 0 — Project Setup (0.5 day)

- Create Android Studio project:
    - Empty Compose Activity.
    - Kotlin DSL.
    - minSdk 26.
    - Package `com.rovercontrol.app`.
- Add Gradle dependencies (§8).
- Set up theme skeleton (§6.6) with placeholder colors.
- Create `AndroidManifest.xml` permissions.
- Initialize Git repository.
- Add Android `.gitignore`.
- Make first commit.

**Exit criterion:** App launches on the Samsung A57 showing a dark empty screen.

## Phase 1 — Bluetooth & Protocol Layer (1.5 days)

- `BluetoothConstants.kt`: UUID, service names.
- `BluetoothManager.kt`:
    - `bondedDevices()`.
    - `openSocket(device)` with 3-attempt fallback.
- `ConnectionState.kt`: sealed class.
- `RoverRepository.kt`:
    - `connect(device)` / `disconnect()`.
    - Read loop → of raw lines.
    - Write queue (`Channel`) → socket writer coroutine.
    - `sendCommand(RoverCommand)`.
- `TelemetryParser.kt`:
    - JSON line → `Telemetry`.
    - Tolerance rules of §5.3.
- `CommandEncoder.kt`:
    - `RoverCommand` → `ByteArray` (per §5.2).
- `RoverCommand.kt` sealed class:
    - `Forward`
    - `Backward`
    - `Left`
    - `Right`
    - `Stop`
    - `SetSpeed(Int)`
    - `Headlights(Boolean)`
    - `EmergencyStop`

**Exit criterion:**

- Unit-testable by logging commands to Logcat.
- Manual test:
    - Connect to HC-05.
    - See raw telemetry lines in Logcat.
    - Send `F\n` from a debug button.
    - Confirm motors move.

## Phase 2 — Connection Screen (0.5 day)

- `ConnectionViewModel.kt`:
    - .
    - .
- `ConnectionScreen.kt` with layout from §6.2.
- Permission flow (§9.2).
- Auto-select last device from `SettingsRepository`.

**Exit criterion:** User can pair in Android settings, open app, see device, tap it, connect, and be navigated to Control.

## Phase 3 — Control Screen (2 days)

- `ControlViewModel.kt`:
    - Speed state.
    - Input mode state.
    - Headlights state.
    - E-STOP/rearm state.
    - Debounced speed send.
- `Joystick.kt`:
    - Custom Canvas with drag.
    - Deadzone.
    - Edge-triggered direction changes.
- `DirectionalPad.kt`:
    - Four direction buttons + center stop.
- `SpeedSlider.kt`.
- `InputModeSheet.kt`:
    - Bottom sheet.
    - SegmentedButton.
- `EmergencyStopFab.kt` + rearm banner.
- Wire into `ControlScreen.kt`.
- Persist settings via `SettingsRepository` (§6.8).

**Exit criterion:**

- Joystick drives the car.
- Slider changes speed.
- Headlights toggle works.
- E-STOP and rearm work.
- Input mode switch works and persists.

## Phase 4 — Telemetry Screen (0.5 day)

- `TelemetryViewModel.kt`:
    - Collect .
    - Track `lastUpdateMillis` for staleness.
- `TelemetryCard.kt`.
- `TelemetryScreen.kt` per §6.4.

**Exit criterion:** Five live cards update at 2 Hz; stale badge appears after 3 seconds without data.

## Phase 5 — Navigation & Chrome (0.5 day)

- `RoverNavHost.kt`.
- Bottom navigation with Control / Telemetry (disabled until connected).
- Connection status pill in both screens' top bars.
- E-STOP FAB present on both screens.

**Exit criterion:** All navigation transitions are smooth; app back stack behaves correctly (back from Telemetry → Control → Connection is exit).

## Phase 6 — Demo Mode (0.5 day)

- `DemoTelemetryGenerator.kt`:
    - Coroutine emitting randomized `Telemetry` every 500 ms.
    - Temperature: 18–35.
    - Humidity: 30–90.
    - Light: 100–900.
    - Distance: 5–200.
    - Inclination: 0–30.
- Add `demoMode` state in `RoverRepository`.
- When on:
    - Skip Bluetooth entirely.
    - Commands are no-ops logged to Logcat.
- `"Demo Mode"` toggle on Connection screen.

**Exit criterion:** With no HC-05 paired, toggling Demo Mode gives a fully functional Control + Telemetry UI.

## Phase 7 — Polish, Build, README, Rehearsal (1.5 days)

- Tune colors per user's visual reference (P3).
- Add app launcher icons:
    - Adaptive icon.
    - Simple white rover glyph on dark background.
- Set:
    - `versionName = "1.0.0"`.
    - `applicationId = "com.rovercontrol.app"`.
- Generate APK:
    - Build → Build APK(s).
    - Debug APK is automatically signed.
- Write `README.md` at repository root covering:
    - Install instructions.
    - Permission prerequisites.
    - How to pair HC-05.
    - Commands supported.
    - Telemetry format.
    - Firmware compatibility note (P2/P4).
    - Demo Mode.
- Rehearse the demo with the real rover 3× end-to-end.

**Exit criterion:** APK installs on Samsung A57; full demo runs clean without crashes; README exists.

## Phase 8 — Buffer (1 day)

Reserve for:

- Firmware changes if P2/P4 requires them.
- Unexpected bugs.
- A second rehearsal.

---

# 11. Testing Plan

## 11.1 Manual Test Checklist

Execute before demo day.

|ID|Test|Steps|Pass criterion|
|---|---|---|---|
|T01|App installs|Install APK on Samsung A57|Opens without crash|
|T02|Permissions|First launch on Connection screen|`BLUETOOTH_CONNECT` requested; works after grant|
|T03|Bluetooth off|Disable BT on device, open app|Banner with "Enable Bluetooth" shown; tapping enables it|
|T04|Bonded list|Pair HC-05 in settings, open app|Device appears in list|
|T05|Connect|Tap device|Status pill turns green within 5 s|
|T06|Connect failure|Remove HC-05, tap any device|Snackbar "Connection failed", remains on screen|
|T07|Forward|Tap F / press up on D-pad|Rover moves forward at current speed|
|T08|Reverse|Tap B|Rover moves backward|
|T09|Left/Right|Tap L / R|Rover turns in place|
|T10|Stop|Release joystick / center|Rover stops (`S\n` sent)|
|T11|Speed slider|Drag to 30%, tap F|Rover moves slower; slider % matches|
|T12|Speed persist|Change speed, restart app|Slider restores to previous value|
|T13|Headlights|Toggle switch|White LED turns on/off on rover|
|T14|E-STOP|Tap FAB|Rover stops instantly, banner "STOPPED" shown, all input disabled|
|T15|Rearm|Tap "Rearm"|Banner clears, controls work again|
|T16|Input mode|Open sheet, pick D-pad|Control screen shows D-pad; persists after restart|
|T17|Telemetry refresh|Open Telemetry screen, observe 10 s|Values change; "Updated X.Xs ago" stays < 0.6 s|
|T18|Telemetry stale|Disconnect rover (power off)|All cards dim + "No data" badge appears within 3 s|
|T19|Telemetry field sanity|Compare with Arduino Serial monitor|`t`, `h`, `l`, `d`, `i` match|
|T20|Demo Mode|On Connection screen, enable Demo Mode with BT off|Control screen fully usable; Telemetry shows simulated values|
|T21|Back navigation|From Telemetry press back|Goes to Control (not exit)|
|T22|Orientation lock|Rotate device|App stays in portrait|
|T23|5 min soak|Leave app connected 5 min on Control screen, then check|No ANR, no disconnect, telemetry still flowing|
|T24|Reconnect|Connect → disassemble rover power → reconnect|"Reconnect" button works|

## 11.2 Non-functional Acceptance Criteria

| Metric            | Requirement                                                   |
| ----------------- | ------------------------------------------------------------- |
| Startup time      | Cold start ≤ 3 s on Samsung A57                               |
| Telemetry latency | From Serial write on Arduino to Compose render ≤ 1 s          |
| Memory            | ≤ 200 MB RSS after 5 min of use                               |
| Stability         | No ANRs or crashes in a 30-minute continuous session          |
| Battery           | Report observed drain during 10-min demo (informational only) |

## 11.3 Unit Tests (Light, Optional)
- `TelemetryParserTest`:
    - Valid JSON.
    - Missing fields.
    - Malformed JSON.
    - Extra fields.
    - Empty line.
- `CommandEncoderTest`:
    - Each `RoverCommand` encodes to the expected bytes.
Skip Compose UI tests — manual is faster here.

---
# 12. Demo Mode (Plan B for the Live Demo)
## Why
The Arduino rover can fail on demo day (battery, wiring, loose HC-05).
Demo Mode guarantees the presenter can still show the full UI. It also lets the audience see the app UI independent of physical constraints.
## Behavior
Toggle on Connection screen:
- A Switch labeled `"Demo Mode (no hardware)"`.
When ON:
`RoverRepository` does not touch Bluetooth.

Instead:
1. A coroutine emits synthetic `Telemetry` every 500 ms.
2. `sendCommand(...)` is a no-op that logs to Logcat.
3. Connection state is force-set to `CONNECTED` with a special flag `isDemo = true`.

### Visual indicator
The connection status pill shows:
- `"DEMO"` in a distinct amber color.
- This ensures nobody is confused about whether the rover is connected.

### Persistence
Demo Mode persists via `SettingsRepository` but is disabled on fresh install and whenever the app is force-stopped.

---

# 13. Risks & Mitigations

|Risk|Likelihood|Impact|Mitigation|
|---|---|---|---|
|HC-05 clone rejects standard SPP socket|Medium|Blocking|3-attempt socket fallback (§5.1)|
|Android 12+ permission flow breaks|Low|Blocking|Follow §9.2 exactly; test on device early in Phase 2|
|Firmware doesn't support V/W/X (P2)|Medium|Degraded demo|App degrades gracefully (commands logged, no crash); README notes firmware requirement|
|Bluetooth disconnect during demo|Medium|Visible failure|Demo Mode + Reconnect button + auto-retry once|
|User visual reference (P3) arrives late|Low|Cosmetic|Ship with §6.6 palette; easy to swap Color.kt|
|Speed slider floods HC-05|Low|UI lag|Debounce 200 ms|
|Joystick jitter causes command spam|Medium|UI/rover jitter|Deadzone + edge-triggered commands|
|Battery drain on phone|Low|Minor|No foreground work; app is only active when user is interacting|
|Scope creep|Medium|Missing deadline|§1.2 non-goals is the contract; do not add features|

---
# 14. Timeline (10 Days, ~3–4 h/day)

| Day | Focus                   | Output                                        |
| --- | ----------------------- | --------------------------------------------- |
| 1   | Phase 0 + start Phase 1 | Project builds; BluetoothManager skeleton     |
| 2   | Phase 1                 | Manual connect works; raw telemetry in Logcat |
| 3   | Phase 1 + Phase 2       | Connection screen works end-to-end            |
| 4   | Phase 3                 | Joystick drives car                           |
| 5   | Phase 3                 | D-pad + slider + headlights + E-STOP          |
| 6   | Phase 4 + Phase 5       | Telemetry screen + navigation complete        |
| 7   | Phase 6                 | Demo Mode + polish                            |
| 8   | Phase 7                 | Signed APK + README + first rehearsal         |
| 9   | Phase 8                 | Buffer + second rehearsal                     |
| 10  | Demo day prep           | Third rehearsal, backup APK on second phone   |

---
# 15. Definition of Done (Acceptance Criteria)
The app is done when all of the following are true:
- [ ] APK installs on the Samsung A57 and launches without crash.
- [ ] User can pair HC-05 in Android settings, then connect from the app in ≤ 3 taps.
- [ ] Joystick and D-pad both drive the rover; switching modes is persisted.
- [ ] Speed slider changes rover speed and persists across launches.
- [ ] Headlights toggle controls the white LED.
- [ ] E-STOP stops the rover immediately and requires explicit rearm.
- [ ] Telemetry screen shows all 5 values updating at ~2 Hz.
- [ ] Stale-data indication appears if rover stops sending for > 3 s.
- [ ] Demo Mode produces a fully interactive UI with no Bluetooth hardware.
- [ ] All T01–T24 tests in §11.1 pass.
- [ ] README at repository root covers install + pairing + commands + firmware note + Demo Mode.
- [ ] Repo is public (or shared link) with the final commit tagged `v1.0.0`.

---
# 16. Firmware-Side Notes
Informational, not part of the app build.
These are the firmware changes required for the app to be fully functional. Flag them in the README even if not done in time.

## Telemetry emission
Arduino sends: JSON

```
{"t":..,"h":..,"l":..,"d":..,"i":..}
```
Followed by `\n` every 500 ms via Serial/SoftwareSerial.
Must not block the control loop — use a `millis()` non-blocking schedule.

## New commands
The following commands must be recognized in the firmware `loop()`:
- `W1\n`
- `W0\n`
- `X\n`

## X (E-STOP)
Must set motor PWM to 0 on both channels immediately and enter a `"stopped"` state that ignores movement commands until a `S` or `X` re-arm signal is received.
Design decision: `X` again toggles rearm.
Document the chosen behavior in the README.

## Newline handling
Firmware should use `trim()` on incoming commands to tolerate the `\n` terminator (which the current firmware may not expect).

## Fallback if firmware cannot be updated
If firmware cannot be updated before demo day:
- F/B/L/R/S only.
- Speed fixed.
- Headlights off.
Demo still passes minimum requirements of the course project.

---
# 17. Open Questions (To Confirm with User Before Phase 0)
### OQ1 (P2): Firmware command support
Share the current `.ino` file (or its command parser) so the implementer can verify V/W/X support and avoid protocol drift.

### OQ2 (P4): Telemetry emission
Does firmware already emit telemetry?
If not, coordinate who writes the emission code.
Recommendation: firmware change is ~15 lines and belongs in a separate `firmware/` folder of the same repository, tagged as a companion change.

### OQ3 (P3): Visual reference
Visual reference for dark theme.
Until received, use §6.6 palette.
### OQ4: HC-05 pairing
Confirm HC-05 is paired and visible in the phone's Bluetooth settings before Phase 2 testing (needed to test the bonded-device list).

---

# 18. Handoff Checklist for the Implementer
Read in this order:
1. §0 TL;DR → §1 non-goals → §17 open questions.
2. §3, §4, §5 (the technical contract).
3. §6 (UI, includes wireframe descriptions and palette).
4. §7, §8, §9 (project skeleton).
5. §10 phases (build order).
6. §11 testing plan (definition of "working").
7. §12 Demo Mode (do NOT skip — this is the plan B).
8. §15 DoD.