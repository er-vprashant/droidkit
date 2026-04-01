# Architecture & Code Design

> This is the technical blueprint. Read this before writing any code.
> 

---

## Layer architecture

Three clean layers, strict dependency direction — UI → Feature → Core. Core never imports Compose.

```
Host app (debugImplementation)
        ↓
DroidKit entry point (ContentProvider auto-init + builder API)
        ↓
DashboardActivity + NavHost
   ↙        ↓        ↘
Storage   DeepLink   Notification   ← Feature layer (Compose screens + ViewModels)
   ↓         ↓           ↓
core:     core:       core:          ← Core layer (pure Kotlin + Android SDK)
storage   intent      notification
```

**Rule:** Everything in `ui/` depends on `core/`. Core never imports Compose. This makes core classes unit-testable without a device.

---

## Package layout

```
droidkit/
├── build.gradle.kts
├── consumer-rules.pro
└── src/
    ├── debug/
    │   ├── AndroidManifest.xml                  ← DroidKitInitializer + DroidKitActivity declared here ONLY
    │   └── kotlin/io/github/er-vprashant/droidkit/
    │       ├── core/init/
    │       │   └── DroidKitInitializer.kt        ← ContentProvider (debug-only class)
    │       └── ui/
    │           └── DroidKitActivity.kt           ← single Activity, hosts NavHost (debug-only class)
    ├── main/
    │   ├── AndroidManifest.xml                   ← empty
    │   └── kotlin/io/github/er-vprashant/droidkit/
    │       ├── DroidKit.kt                       ← public API surface (object)
    │       ├── DroidKitConfig.kt                 ← builder pattern + Module enum
    │       ├── internal/
    │       │   └── DroidKitServiceLocator.kt      ← lightweight manual DI (no Hilt)
    │       ├── core/
    │       │   ├── shake/
    │       │   │   └── ShakeDetector.kt           ← SensorManager-based shake detection
    │       │   ├── launcher/
    │       │   │   └── NotificationLauncher.kt    ← persistent debug notification to open DroidKit
    │       │   ├── storage/
    │       │   │   ├── PrefsReader.kt
    │       │   │   └── DbInspector.kt             ← raw SQLiteDatabase access (no Room dependency)
    │       │   ├── intent/
    │       │   │   ├── IntentFirer.kt
    │       │   │   └── HistoryRepository.kt       ← persists last 10 URIs in internal prefs
    │       │   └── notification/
    │       │       ├── NotificationFirer.kt
    │       │       └── ChannelManager.kt          ← creates/manages notification channels
    │       └── ui/
    │           ├── theme/
    │           │   └── DroidKitTheme.kt           ← Composable theme wrapper + color tokens
    │           ├── dashboard/
    │           │   ├── DashboardScreen.kt
    │           │   └── DashboardViewModel.kt
    │           ├── storage/
    │           │   ├── StorageInspectorScreen.kt
    │           │   ├── StorageViewModel.kt
    │           │   └── EditValueSheet.kt          ← bottom sheet for inline edit
    │           ├── deeplink/
    │           │   ├── DeepLinkTesterScreen.kt
    │           │   └── DeepLinkViewModel.kt
    │           └── notification/
    │               ├── NotificationTesterScreen.kt
    │               └── NotificationViewModel.kt
```

**Note:** Debug-only classes (`DroidKitInitializer`, `DroidKitActivity`) live in `src/debug/kotlin/` so they are completely excluded from release builds — true zero footprint, not relying on R8 tree-shaking.

**Internal prefs namespace:** All DroidKit internal SharedPreferences use the file name `droidkit_internal`. This file is filtered out of the Storage Inspector to avoid self-pollution.

---

## Gradle setup

### droidkit/build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.er-vprashant.droidkit"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // KEY: debug manifest only merges in debug builds
    sourceSets {
        getByName("debug") {
            manifest.srcFile("src/debug/AndroidManifest.xml")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.sqlite:sqlite-framework:2.4.0") // raw DB access, no Room needed
}
```

**Why no Hilt?** Hilt requires the host app to also apply the Hilt Gradle plugin and use `@HiltAndroidApp`. This breaks the "zero-config" promise. Instead, DroidKit uses a lightweight internal service locator (`DroidKitServiceLocator`) for dependency wiring.

**Why no Room?** DroidKit inspects the host app's databases generically via `SQLiteDatabase.openDatabase()`. It doesn't define its own entities — Room would be an unused dependency.

### Host app integration (one line)

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("io.github.er-vprashant:droidkit:1.0.0")
    // Nothing else needed. No Hilt, no plugins, no init code.
}
```

---

## Auto-init via ContentProvider

### src/debug/AndroidManifest.xml

```xml
<manifest>
    <application>
        <provider
            android:name=".core.init.DroidKitInitializer"
            android:authorities="${applicationId}.droidkit-init"
            android:exported="false" />

        <activity
            android:name=".ui.DroidKitActivity"
            android:theme="@style/Theme.DroidKit"
            android:exported="false" />
    </application>
</manifest>
```

### core/init/DroidKitInitializer.kt

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

    override fun query(...) = null
    override fun getType(...) = null
    override fun insert(...) = null
    override fun delete(...) = 0
    override fun update(...) = 0
}
```

**Why ContentProvider?** Runs before `Application.onCreate()`, receives a valid `Context`. Same pattern as LeakCanary — zero setup for the developer. The `autoInit` flag lets teams disable via manifest meta-data.

---

## Public API surface

### DroidKit.kt

```kotlin
object DroidKit {

    private var config: DroidKitConfig? = null
    private var appContext: Context? = null

    internal fun initInternal(context: Context, cfg: DroidKitConfig) {
        appContext = context.applicationContext
        config = cfg
        ShakeDetector.start(context, cfg.launchOnShake) { launch(context) }
        if (cfg.showNotification) NotificationLauncher.show(context)
    }

    /** Manual launch — call from any debug button in the host app */
    fun launch(context: Context) {
        context.startActivity(
            Intent(context, DroidKitActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Builder for teams that need explicit control */
    fun builder() = DroidKitConfig.Builder()
}
```

### DroidKitConfig.kt

```kotlin
data class DroidKitConfig(
    val autoInit: Boolean = true,
    val launchOnShake: Boolean = true,
    val showNotification: Boolean = true,
    val enabledModules: Set<Module> = Module.entries.toSet()
) {
    class Builder {
        private var launchOnShake = true
        private var showNotification = true
        private val disabled = mutableSetOf<Module>()

        fun launchOnShake(v: Boolean) = apply { launchOnShake = v }
        fun showNotification(v: Boolean) = apply { showNotification = v }
        fun disable(m: Module) = apply { disabled += m }

        fun init(context: Context) {
            DroidKit.initInternal(
                context,
                DroidKitConfig(
                    launchOnShake = launchOnShake,
                    showNotification = showNotification,
                    enabledModules = Module.entries.toSet() - disabled
                )
            )
        }
    }
}

enum class Module { STORAGE, DEEP_LINK, NOTIFICATIONS }
```

---

## Navigation (single NavHost)

```kotlin
// DroidKitActivity.kt (lives in src/debug/kotlin/)
class DroidKitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidKitTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "dashboard") {
                    composable("dashboard")     { DashboardScreen(navController) }
                    composable("storage")        { StorageInspectorScreen() }
                    composable("deeplink")       { DeepLinkTesterScreen() }
                    composable("notifications")  { NotificationTesterScreen() }
                }
            }
        }
    }
}
```

---

## Core layer skeletons

### core/storage/PrefsReader.kt

```kotlin
internal class PrefsReader(private val context: Context) {

    companion object {
        internal const val INTERNAL_PREFS = "droidkit_internal"
    }

    fun getAllFiles(): List<String> {
        return context.filesDir.parentFile
            ?.resolve("shared_prefs")
            ?.listFiles()
            ?.map { it.nameWithoutExtension }
            ?.filter { it != INTERNAL_PREFS } // hide DroidKit's own prefs
            ?: emptyList()
    }

    fun getAll(fileName: String): Map<String, Any?> {
        return context
            .getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .all
    }

    fun setValue(fileName: String, key: String, value: Any?) {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .edit().apply {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int     -> putInt(key, value)
                    is Long    -> putLong(key, value)
                    is Float   -> putFloat(key, value)
                    is String  -> putString(key, value)
                }
                apply()
            }
    }
}
```

### core/storage/DbInspector.kt

```kotlin
internal class DbInspector(private val context: Context) {

    fun getAllDatabases(): List<String> {
        return context.databaseList()
            .filter { !it.endsWith("-journal") && !it.endsWith("-wal") && !it.endsWith("-shm") }
    }

    fun getTables(dbName: String): List<String> {
        val db = openDatabase(dbName) ?: return emptyList()
        return db.use {
            val cursor = it.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                null
            )
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
                cursor.close()
            }
        }
    }

    fun query(dbName: String, table: String, limit: Int = 50): List<Map<String, String?>> {
        val db = openDatabase(dbName) ?: return emptyList()
        return db.use {
            val cursor = it.rawQuery("SELECT * FROM $table LIMIT $limit", null)
            val columns = cursor.columnNames.toList()
            buildList {
                while (cursor.moveToNext()) {
                    add(columns.associateWith { col -> cursor.getString(cursor.getColumnIndexOrThrow(col)) })
                }
                cursor.close()
            }
        }
    }

    private fun openDatabase(name: String): SQLiteDatabase? {
        val path = context.getDatabasePath(name)
        if (!path.exists()) return null
        return SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
}
```

**Why raw SQLiteDatabase?** DroidKit doesn't know the host app's Room `@Database` class. Using `SQLiteDatabase.openDatabase()` works with any app — no DAO coupling, no Room dependency.

### core/shake/ShakeDetector.kt

```kotlin
internal object ShakeDetector : SensorEventListener {

    private const val THRESHOLD = 12f // m/s², tuned for intentional shake
    private const val COOLDOWN_MS = 1000L
    private var lastShakeTime = 0L
    private var onShake: (() -> Unit)? = null
    private var sensorManager: SensorManager? = null

    fun start(context: Context, enabled: Boolean, callback: () -> Unit) {
        if (!enabled) return
        onShake = callback
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        onShake = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (acceleration > THRESHOLD && now - lastShakeTime > COOLDOWN_MS) {
            lastShakeTime = now
            onShake?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
```

### core/launcher/NotificationLauncher.kt

```kotlin
internal object NotificationLauncher {

    private const val CHANNEL_ID = "droidkit_launcher"
    private const val NOTIF_ID = 7390

    fun show(context: Context) {
        val nm = NotificationManagerCompat.from(context)

        // Create channel (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DroidKit Launcher",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, DroidKitActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_droidkit_notif)
            .setContentTitle("DroidKit")
            .setContentText("Tap to open debug toolkit")
            .setContentIntent(pi)
            .setOngoing(true) // persistent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }
}
```

### core/notification/ChannelManager.kt

```kotlin
internal class ChannelManager(private val context: Context) {

    fun ensureChannel(channel: NotifChannel) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channel.id) != null) return

        val importance = when (channel) {
            NotifChannel.SILENT   -> NotificationManager.IMPORTANCE_LOW
            NotifChannel.HEADS_UP -> NotificationManager.IMPORTANCE_HIGH
            else                  -> NotificationManager.IMPORTANCE_DEFAULT
        }
        val nc = NotificationChannel(channel.id, channel.displayName, importance)
        nm.createNotificationChannel(nc)
    }
}
```

### core/intent/HistoryRepository.kt

```kotlin
internal class HistoryRepository(private val context: Context) {

    private val prefs get() = context.getSharedPreferences(
        PrefsReader.INTERNAL_PREFS, Context.MODE_PRIVATE
    )

    fun getHistory(): List<String> {
        val json = prefs.getString("deeplink_history", "[]") ?: "[]"
        return JSONArray(json).let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
    }

    fun addEntry(uri: String) {
        val history = getHistory().toMutableList()
        history.remove(uri) // dedup
        history.add(0, uri)
        val capped = history.take(MAX_HISTORY)
        prefs.edit().putString("deeplink_history", JSONArray(capped).toString()).apply()
    }

    fun removeEntry(uri: String) {
        val history = getHistory().toMutableList()
        history.remove(uri)
        prefs.edit().putString("deeplink_history", JSONArray(history).toString()).apply()
    }

    companion object { private const val MAX_HISTORY = 10 }
}
```

### core/intent/IntentFirer.kt

```kotlin
internal class IntentFirer(private val context: Context) {

    fun fire(uri: String, extras: Map<String, String> = emptyMap()) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw NoHandlerException(uri)
        }
    }

    class NoHandlerException(val uri: String) : Exception("No activity handles: $uri")
}
```

### core/notification/NotificationFirer.kt

```kotlin
internal class NotificationFirer(private val context: Context) {

    fun fire(payload: NotificationPayload) {
        ChannelManager(context).ensureChannel(payload.channel)

        val pendingIntent = payload.deepLink?.let {
            PendingIntent.getActivity(
                context, 0,
                Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, payload.channel.id)
            .setSmallIcon(R.drawable.ic_droidkit_notif)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object { private const val NOTIF_ID = 7391 }
}

data class NotificationPayload(
    val title: String,
    val body: String,
    val deepLink: String? = null,
    val channel: NotifChannel = NotifChannel.DEFAULT
)

enum class NotifChannel(val id: String, val displayName: String) {
    DEFAULT("droidkit_default", "Default"),
    SILENT("droidkit_silent", "Silent"),
    HEADS_UP("droidkit_headsup", "Heads-up")
}
```

### internal/DroidKitServiceLocator.kt

```kotlin
internal object DroidKitServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val prefsReader: PrefsReader by lazy { PrefsReader(appContext) }
    val dbInspector: DbInspector by lazy { DbInspector(appContext) }
    val intentFirer: IntentFirer by lazy { IntentFirer(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(appContext) }
    val notificationFirer: NotificationFirer by lazy { NotificationFirer(appContext) }
    val channelManager: ChannelManager by lazy { ChannelManager(appContext) }
}
```

**Why a service locator instead of Hilt?** Keeps the library self-contained. ViewModels call `DroidKitServiceLocator.prefsReader` etc. No annotation processing, no host app requirements.

### ui/theme/DroidKitTheme.kt

```kotlin
object DroidKitColors {
    val StorageBlue = Color(0xFF185FA5)
    val LinkGreen = Color(0xFF3B6D11)
    val NotifAmber = Color(0xFFBA7517)
    val FireButton = Color(0xFF1D9E75)
    val DarkSurface = Color(0xFF1A1A1A)
}

@Composable
fun DroidKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DroidKitColors.StorageBlue,
            surface = DroidKitColors.DarkSurface,
            onSurface = Color.White
        ),
        content = content
    )
}
```

---

## Interview talking points (architecture specific)

> "I structured DroidKit in three layers: a public entry point, a feature layer with Compose screens and ViewModels, and a core layer with pure Kotlin business logic that has zero Compose imports. This means the core is unit-testable without a device or robolectric."
> 

> "I used the ContentProvider pattern for auto-init — same approach as LeakCanary — so developers get zero-config setup. The ContentProvider runs before Application.onCreate() and has access to Context, which is exactly what we need to set up the shake detector and the debug notification."
> 

> "The entire library is scoped to a debug sourceSet in Gradle. The debug manifest declares the ContentProvider and the Activity — in release builds, those manifest entries simply don't exist. The host app's release APK has zero DroidKit footprint."
> 

> "The builder pattern gives enterprise teams explicit control — they can disable specific modules or turn off the shake detector if their app already uses it for something else. The default experience is zero-config, but it's fully configurable."
> 

> "I deliberately avoided Hilt — a library that forces the host app to adopt a DI framework isn't truly zero-config. Instead I used a lightweight internal service locator with lazy initialisation. Core classes are plain Kotlin with constructor-injected Context, which makes them trivially unit-testable."
> 

---

## Testing strategy

### Core layer (unit tests — no device needed)

- `PrefsReader` — mock `Context` and `SharedPreferences`, verify read/write/delete
- `DbInspector` — use Robolectric or in-memory SQLite to verify table listing and queries
- `IntentFirer` — verify intent construction, verify `NoHandlerException` thrown correctly
- `HistoryRepository` — verify FIFO capping at 10, dedup, persistence
- `ShakeDetector` — verify threshold logic with synthetic `SensorEvent` values
- `ChannelManager` — verify channel creation idempotency (API 26+)

### UI layer (instrumentation tests)

- Navigation: verify all routes reachable from dashboard
- StorageInspectorScreen: verify prefs list renders, edit sheet opens
- DeepLinkTesterScreen: verify extras builder add/remove, history tap-to-fill
- NotificationTesterScreen: verify preset fill, live preview updates

### Test directory structure

```
src/
├── test/kotlin/io/github/er-vprashant/droidkit/     ← unit tests
│   ├── core/storage/PrefsReaderTest.kt
│   ├── core/storage/DbInspectorTest.kt
│   ├── core/intent/IntentFirerTest.kt
│   ├── core/intent/HistoryRepositoryTest.kt
│   └── core/shake/ShakeDetectorTest.kt
└── androidTest/kotlin/io/github/er-vprashant/droidkit/  ← instrumentation tests
    └── ui/NavigationTest.kt
```

---

## Build order (what to implement first)

1. Gradle project setup + `DroidKitServiceLocator` + `DroidKitTheme` — foundation
2. `DroidKitInitializer` + `DroidKitActivity` + `NavHost` — get the shell launching
3. `DashboardScreen` — static cards, no logic needed yet
4. `PrefsReader` + `StorageViewModel` + `StorageInspectorScreen` — Module 1 core
5. `EditValueSheet` — inline edit bottom sheet with confirmation dialog
6. `DbInspector` + Room/SQLite tab in StorageInspectorScreen
7. `IntentFirer` + `HistoryRepository` + `DeepLinkTesterScreen` — Module 2
8. `NotificationFirer` + `ChannelManager` + `NotificationTesterScreen` — Module 3
9. `ShakeDetector` + `NotificationLauncher` — launch methods
10. Empty states, loading states, error handling across all screens
11. Unit tests for core layer
12. Sample app (`:sample` module) for integration testing and demos