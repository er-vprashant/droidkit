# Product Strategy — Positioning, Scope & Integration

> This document defines the product foundation before any code is written. Revisit this before adding any new feature.
> 

---

## Part 1 — Positioning

### One-liner

> "The only Android debug toolkit that lives inside your debug build — no laptop, no USB, no Flipper. Inspect storage, fire deep links, and test push payloads directly on-device."
> 

### Target audience

| Tier | Who | Why they care |
| --- | --- | --- |
| Primary | Solo Android devs | Building without a QA team. Need fast on-device debugging without desktop tooling. |
| Secondary | Small dev teams / startups | Shared toolkit dropped into the project. Whole team benefits. |
| Stretch | QA engineers | Need to trigger edge cases (bad network, deep links) without dev help. |

### Competitive landscape

| Tool | Needs desktop | Needs USB/ADB | Open source | Still maintained |
| --- | --- | --- | --- | --- |
| ------ | :---: | :---: | :---: | :---: |
| Flipper (Meta) | Yes | Yes | Yes | No — archived 2024 |
| Stetho (Facebook) | Yes (Chrome) | Yes | Yes | No — deprecated |
| Chuck / OkHttp logger | No | No | Yes | Yes (HTTP logging only — different problem domain) |
| Play Store dev tools | No | No | No | Yes |
| **This toolkit** | **No** | **No** | **Yes** | **Yes — active** |

### Positioning statements

**For README:**

> "Flipper is archived. ADB is a cable away. DroidKit is a zero-setup Android debug toolkit that lives in your debug build — inspect app storage, test deep links, and simulate push payloads without leaving your phone."
> 

**For LinkedIn:**

> "Built the toolkit I always wished existed: debug your Android app's storage, deep links, and push notifications — directly on-device, no laptop, no USB. Post-Flipper era needs a new tool."
> 

### Name decision

- **DroidKit** ← Recommended. Short, searchable, Android-native feel. Check GitHub + Maven Central availability.
- DevDeck ← More neutral, platform-agnostic if you expand later.
- DebugMate ← Friendly and approachable, slightly generic.

---

## Part 2 — Scope

### Scope philosophy

> Each module is standalone and works independently. Users drop in only what they need. No feature should require another to function. This is how you get GitHub stars from people who only need one thing.
> 

### MVP (3 weeks realistic)

#### Module 1 — App Storage Inspector

- **In scope:** SharedPreferences viewer + editor, Room/SQLite table browser, row search, JSON export
- **Out of scope (Phase 2):** DataStore/Proto viewer, file system browser, encryption key handling
- **Definition of done:** Can view and edit any SharedPref key and browse any Room table without ADB

#### Module 2 — Deep Link & Intent Tester

- **In scope:** URI input, custom intent extras (key/value/type), fire intent, history of last 10, preset save/load
- **Out of scope (Phase 2):** Intent interception/monitoring, activity backstack visualiser
- **Definition of done:** Can fire any deep link with any extras and replay from history in one tap

#### Module 3 — Push Notification Tester

- **In scope:** JSON payload editor (title/body/deep_link/data fields), fire local notification, presets, channel selector, tap → deep link navigation
- **Out of scope (Phase 2):** Real FCM sending, server-side simulation
- **Definition of done:** Can simulate the full push → tap → deep link flow end-to-end without a backend

### Phase 2 (post-MVP, priority order)

| Feature | Priority | Reason |
| --- | --- | --- |
| Performance overlay | High | WindowManager + Choreographer = highest interview signal. Build it right in Week 4. |
| Network simulation | High | After researching real implementation (VPN service or OkHttp interceptor). No thread-sleep faking. |
| Permission manager | Medium | Fast to build, solid interview topic (runtime permissions). |
| DataStore / Proto viewer | Medium | Natural Phase 2 extension of Module 1. High demand as DataStore replaces SharedPrefs. |
| Network call log | Later | OkHttp interceptor-based. Chuck does this already — position as integrated companion, not standalone. |
| Locale & timezone override | Later | Useful for i18n testing. Simple to build once other modules are stable. |

### Cut permanently

| Feature | Reason |
| --- | --- |
| Sensor simulation | Requires root or SensorManager mock injection. Not worth complexity for the audience. |
| Battery simulation | Faking battery level requires root. Broadcast intents only. Low value. |
| API mock server | Separate project entirely. MockWebServer and WireMock do this better. Don't compete. |

---

## Part 3 — Integration Design

### Integration philosophy

> Drop in, zero config, zero production impact. A developer should go from "never heard of this" to "it's running in my app" in under 5 minutes. This is the bar that gets GitHub stars.
> 

### Integration model

**Recommended: Debug-only AAR via Gradle**

One line in `build.gradle`. Zero release APK footprint. Toolkit auto-initialises in debug builds via `ContentProvider` (same pattern as LeakCanary). No `Application` class changes needed.

```kotlin
dependencies {
    debugImplementation 'io.github.pverma:droidkit:1.0.0'
}
```

**Alternative: Manual initialisation**

Developer calls `DroidKit.init(this)` in `Application.onCreate()`. More explicit, less magical.

### Target integration experience (5 minutes start to finish)

**Step 1 — Add dependency (30 seconds)**

```kotlin
// In app/build.gradle
dependencies {
    debugImplementation 'io.github.pverma:droidkit:1.0.0'
}
```

That's it. No other setup needed for auto-init builds.

**Step 2 — Launch the toolkit (choose one)**

- Shake the device to open (recommended default)
- Persistent debug notification with tap-to-open
- Call `DroidKit.launch(context)` from a button in your own debug UI

**Step 3 — Use it (no docs needed)**

Dashboard shows all modules as cards. Tap any card to open that tool. Self-explanatory.

### Release APK safety (non-negotiable)

| Concern | Solution |
| --- | --- |
| Code in release APK | `debugImplementation` only — Gradle excludes from release entirely |
| Manifest entries | All activities/services declared in library's own manifest under debug variant. Merged only in debug. |
| Auto-init side effects | ContentProvider-based init. Can be disabled: `droidkit_auto_init = false` in manifest meta-data |
| Storage access | Reads SharedPrefs + DB via host app's own context. No elevated permissions required. |

### Module opt-in (for enterprise teams)

All MVP modules enabled by default. Teams can selectively disable:

```kotlin
DroidKit.builder()
    .disable(Module.STORAGE)
    .disable(Module.NOTIFICATIONS)
    .init(this)
```

### Auto-init via ContentProvider (the LeakCanary pattern)

```kotlin
internal class DroidKitInitializer : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val config = DroidKitConfig.readFromManifest(ctx)
        if (config.autoInit) {
            DroidKit.initInternal(ctx, config)
        }
        return true
    }
    // ... stub out other ContentProvider methods
}
```

**Note:** No `BuildConfig.DEBUG` check needed — this class lives in `src/debug/kotlin/` and is only included in debug builds via `debugImplementation`. The library's `BuildConfig` would refer to the library's own build type, not the host app's, making the check both incorrect and unnecessary.

Declare in the library's **debug** `AndroidManifest.xml`:

```xml
<provider
    android:name=".DroidKitInitializer"
    android:authorities="${applicationId}.droidkit-init"
    android:exported="false" />
```

This runs before `Application.onCreate()` and gives access to `Context` — zero setup for the developer.

---

## Interview talking point (memorise this)

> "I modelled the integration after LeakCanary — ContentProvider-based auto-init so developers get zero-config setup, with a builder pattern override for teams that need explicit control. The key constraint was zero release APK footprint, which I solved by scoping everything to `debugImplementation` and keeping all manifest entries inside the library's debug variant. The whole architecture is modular — each tool is a standalone feature that can be disabled independently, which matters for enterprise teams who need audit control over debug tooling."
> 

---

## CI/CD & Release Plan

### GitHub Actions pipeline

```yaml
# .github/workflows/ci.yml
Trigger: push to main + PRs
Steps:
  1. Build library (:droidkit:assembleDebug)
  2. Lint (:droidkit:lintDebug)
  3. Unit tests (:droidkit:testDebugUnitTest)
  4. Build sample app (:sample:assembleDebug)
```

### Sample app (`:sample` module)

- Minimal Android app with fake SharedPreferences, a Room database, and registered deep links
- Used for manual testing, integration verification, and GIF recording for README/LinkedIn
- `debugImplementation(project(":droidkit"))` — dogfoods the real integration path

### Publishing (Maven Central via Sonatype)

- Group ID: `io.github.pverma`
- Artifact ID: `droidkit`
- Use `maven-publish` + `signing` Gradle plugins
- Publish via GitHub Actions on tag push (`v1.0.0`)
- GPG key stored as GitHub secret

### Versioning strategy

- Semantic versioning: `MAJOR.MINOR.PATCH`
- `MAJOR` — breaking API changes (e.g., removing a Module)
- `MINOR` — new features (e.g., new module, new screen)
- `PATCH` — bug fixes, UI polish
- Maintain `CHANGELOG.md` in repo root

---

## Decision log

| Date | Decision | Rationale |
| --- | --- | --- |
| Day 1 | Named the project DroidKit | Short, searchable, Android-native |
| Day 1 | Chose ContentProvider auto-init | Zero setup = lower barrier to adoption |
| Day 1 | Cut Network Simulation from MVP | Thread-sleep faking doesn't survive interview scrutiny |
| Day 1 | Storage Inspector as MVP anchor | Fastest build, highest interview signal, most relatable pain point |
| Day 1 | Cut Sensor + Battery simulation permanently | Root required for real implementation — not worth it |
| Day 1 | Removed Hilt dependency | Library forcing host app to adopt Hilt breaks zero-config promise. Manual service locator instead. |
| Day 1 | Removed Room dependency | DroidKit inspects DBs generically via raw SQLiteDatabase — Room would be unused overhead |
| Day 1 | Adjusted MVP timeline to 3 weeks | Original 2-week estimate was aggressive for solo dev with 3 modules + infra |
| Day 1 | Added CI/CD + sample app to plan | Essential for quality, demo capability, and publishing readiness |