<p align="center">
  <h1 align="center">DroidKit</h1>
  <p align="center">
    The on-device debug toolkit for Android.<br/>
    No laptop. No USB. No Flipper. Just shake.
  </p>
</p>

<p align="center">
  <a href="#installation">Installation</a> •
  <a href="#features">Features</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#license">License</a>
</p>

---

DroidKit is a **debug-only** Android library that gives you an on-device toolkit for inspecting storage, firing deep links, and testing push notifications — all without leaving your phone.

Add one line to your `build.gradle.kts` and you're done. DroidKit auto-initializes, adds zero bytes to your release APK, and never touches your production code.

## Installation

```kotlin
// build.gradle.kts (app module)
dependencies {
    debugImplementation("io.github.er-vprashant:droidkit:1.0.0")
}
```

That's it. No `Application` subclass, no `ContentProvider` registration, no init calls. DroidKit uses a `ContentProvider` under the hood to auto-initialize on app start.

## Features

### Storage Inspector
Browse and edit your app's **SharedPreferences** and **SQLite databases** in real time.

- List all SharedPreferences files (DroidKit's own internal prefs are filtered out)
- View, edit, and delete individual key-value pairs
- Type-aware badges (Boolean, Int, Long, Float, String)
- Search/filter keys across files
- Browse SQLite/Room databases: list tables, view schemas, paginate rows
- NULL-aware cell rendering

### Deep Link Tester
Fire any URI from the device — no `adb` required.

- Enter any `app://` or `https://` URI and fire it instantly
- Attach string extras as key-value pairs
- URI validation with clear error messages
- **History** — recently fired URIs are saved for quick replay (long-press to delete)
- **Presets** — save frequently used deep links with their extras for one-tap replay

### Push Notification Tester
Compose and send local notifications with a live preview.

- Title, body, and deep-link-on-tap fields
- Channel selector: **Default**, **Silent** (low importance), **Heads-up** (high importance)
- Live notification preview card that updates as you type
- Handles `POST_NOTIFICATIONS` permission on Android 13+ with grant/settings flow
- Preset quick-fill chips for common notification shapes

### Network Inspector
Inspect and mock HTTP/HTTPS traffic from your OkHttp client.

- View all network requests and responses in real time
- Filter by HTTP method (GET, POST, PUT, DELETE)
- Inspect headers, request/response bodies (up to 100KB)
- **JSON beautification** — toggle between raw and formatted JSON
- **Mock responses** — return local data instead of hitting real APIs
- Copy requests as cURL for Postman/terminal
- Test error scenarios and edge cases offline
- Performance tracking (request duration)

**Setup (one-time):**
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(DroidKit.networkInterceptor())
    .build()

// If using Retrofit:
val retrofit = Retrofit.Builder()
    .client(client)
    .baseUrl("https://api.example.com")
    .build()
```

**Note:** Only captures OkHttp traffic. WebView and other network stacks are not supported.

## How to Launch

DroidKit provides three ways to open the toolkit:

| Method | Details |
|---|---|
| **Shake** | Shake the device to open DroidKit (enabled by default) |
| **Notification** | Tap the persistent "DroidKit" notification in the shade |
| **Programmatic** | Call `DroidKit.launch(context)` from any debug button |

## Configuration

DroidKit works with zero configuration, but you can customize it:

```kotlin
// In your Application.onCreate() or any debug entry point
DroidKit.builder()
    .launchOnShake(true)           // default: true
    .showNotification(true)        // default: true
    .disable(Module.NOTIFICATIONS) // hide specific modules
    .init(context)
```

To disable auto-initialization entirely, add this to your `AndroidManifest.xml`:

```xml
<meta-data
    android:name="droidkit_auto_init"
    android:value="false" />
```

Then call `DroidKit.builder().init(context)` manually when you're ready.

### Available Modules

| Module | Description |
|---|---|
| `Module.STORAGE` | SharedPreferences + SQLite inspector |
| `Module.DEEP_LINK` | Deep link tester with history & presets |
| `Module.NOTIFICATIONS` | Push notification composer & tester |
| `Module.NETWORK` | HTTP inspector with mocking support |

## Architecture

```
droidkit/
├── src/main/kotlin/com/prashant/droidkit/
│   ├── DroidKit.kt                  # Public API singleton
│   ├── DroidKitConfig.kt            # Configuration + Builder
│   ├── Module.kt                    # Feature module enum
│   ├── core/
│   │   ├── intent/                  # IntentFirer, HistoryRepository
│   │   ├── launcher/                # NotificationLauncher
│   │   ├── network/                 # NetworkInterceptor, MockRepository
│   │   ├── notification/            # ChannelManager, NotificationFirer
│   │   ├── shake/                   # ShakeDetector (accelerometer)
│   │   └── storage/                 # PrefsReader, DbInspector
│   ├── internal/
│   │   └── DroidKitServiceLocator   # Manual DI (no Hilt)
│   └── ui/
│       ├── dashboard/               # Main dashboard screen
│       ├── deeplink/                # Deep link tester UI + VM
│       ├── network/                 # Network inspector UI + mock editor
│       ├── notification/            # Notification tester UI + VM
│       ├── storage/                 # Storage inspector UI + VM
│       └── theme/                   # DroidKitTheme, DroidKitColors
├── src/debug/
│   ├── AndroidManifest.xml          # DroidKitActivity + Initializer
│   └── kotlin/.../
│       ├── DroidKitActivity.kt      # Compose NavHost (debug only)
│       └── DroidKitInitializer.kt   # ContentProvider auto-init
└── src/test/                        # Unit tests (Robolectric)
```

### Key Design Decisions

- **`debugImplementation` only** — DroidKit is added as a debug dependency. Zero classes ship in release builds.
- **No Hilt / No DI framework** — Uses an internal `DroidKitServiceLocator` so the host app is never forced to adopt a DI framework.
- **No Room dependency** — Inspects host databases via raw `SQLiteDatabase.openDatabase()`.
- **No `BuildConfig.DEBUG` checks** — The library is debug-only by Gradle scope, making runtime checks redundant.
- **Debug source set** — `DroidKitActivity` and `DroidKitInitializer` live in `src/debug/` for true zero release footprint.
- **Internal prefs isolation** — All DroidKit state is stored in a dedicated `droidkit_internal` SharedPreferences file, never polluting the host app's prefs and filtered from the Storage Inspector.
- **Single instance** — `singleTop` launch mode + `FLAG_ACTIVITY_SINGLE_TOP` ensures only one DroidKit instance is ever open.

## Requirements

| Requirement | Version |
|---|---|
| Min SDK | 24 (Android 7.0) |
| Compile SDK | 34 |
| Kotlin | 1.9.22+ |
| Jetpack Compose BOM | 2024.02.00 |
| AGP | 8.2.2+ |

## Sample App

The `sample/` module demonstrates DroidKit integration with seeded SharedPreferences data. Run it on an emulator or device:

```bash
./gradlew :sample:installDebug
```

Then shake the device or tap the notification to open DroidKit.

## License

```
Copyright 2026 Prashant Verma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
