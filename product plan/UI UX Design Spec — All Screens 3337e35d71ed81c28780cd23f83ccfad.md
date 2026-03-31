# UI/UX Design Spec — All Screens

> Covers all 4 MVP screens: Dashboard, Storage Inspector, Deep Link Tester, Push Notification Tester. Use this as the reference while building Compose UI.
> 

---

## Design principles

- **Dark phone shell, light app content** — debug tools feel intentionally separate from the host app
- **Minimal chrome** — every pixel serves a function. No decorative elements.
- **One primary action per screen** — Fire intent. Send notification. No decision paralysis.
- **Live feedback** — notification preview updates as you type, history updates as you fire
- **Returns cleanly** — back press always returns to dashboard or calling app, never orphans the stack
- **Accessible by default** — all interactive elements have `contentDescription`, minimum 48dp touch targets, proper focus ordering
- **Keyboard-aware** — screens with text inputs use `imePadding()` and scroll content above the keyboard

---

## Screen 1 — Dashboard

### Layout

- Top bar: app name + version subtitle + overflow menu (3-dot)
- 2×2 module grid — each card has an icon, title, and subtitle
- Launch method switcher at bottom (Shake / Notification / Code)

### Module cards

| Module | Icon color | Label | Subtitle |
| --- | --- | --- | --- |
| Storage Inspector | Blue | Storage Inspector | SharedPrefs · Room |
| Deep Link Tester | Green | Deep Link Tester | Intents · History |
| Push Notif Tester | Amber | Push Notif Tester | FCM payloads |

### Launch method switcher

- 3 options: Shake / Notification (default active) / Code
- Persisted in `droidkit_internal` SharedPreferences file (avoids polluting host app prefs)

### Launch method details

- `Shake`: SensorManager 3-axis threshold detection
- `Notification`: Persistent foreground notification, tap opens DashboardActivity
- `Code`: `DroidKit.launch(context)` — for developers with their own debug button

### Overflow menu (3-dot)

- **About** — shows DroidKit version, link to GitHub repo
- **Reset all settings** — clears DroidKit internal prefs (launch method, deep link history, presets) with confirmation dialog

### Behaviour

- Back press from dashboard → finish() and return to host app
- No animation between dashboard and modules — instant feel
- Task flag: `Intent.FLAG_ACTIVITY_NEW_TASK` to avoid backstack issues

---

## Screen 2 — Storage Inspector

### Tab structure

- Tab 1: SharedPreferences
- Tab 2: Room DB
- Tabs use pill toggle style, not underline tabs

### SharedPreferences tab

**Search bar**

- Sticky at top of list, below tabs
- Filters keys across all files in real time as user types
- Placeholder: `Search keys...`
- Clear (×) button when text is present

**List view**

- Groups keys by .xml file name (section header)
- Each row: coloured dot (blue) + key name + type badge + value preview
- Type badges: Boolean (green/red for true/false), String (gray), Int (amber), Float (blue), Long (purple)
- Value preview truncated at 20 chars for strings

**Edit sheet (bottom sheet on row tap)**

- Shows full key name and type
- Input field appropriate to type:
    - Boolean → toggle switch
    - Int/Long → number keyboard
    - Float → decimal keyboard
    - String → full text input
- Save button writes back via `prefs.edit().putX(key, value).apply()`
- **Save confirmation:** "Update value for [key]?" dialog before writing
- Cancel discards changes
- Delete button removes the key entirely
- **Delete confirmation:** "Delete [key] from [file]? This cannot be undone." dialog before deleting

### Room DB tab

**Database + table list**

- Lists all Room databases detected in app data directory
- Each database expands to show tables
- Table row shows: name + row count + column count

**Table viewer (on tap)**

- Horizontal scroll for many columns
- Fixed header row with column names
- Max 50 rows shown — pagination with "Load more" button
- Export button (share icon in toolbar) → serialises table to JSON → Android share sheet

**Key implementation note**

```kotlin
// Generic DB access — no Room dependency, no DAO coupling
val dbPath = context.getDatabasePath(dbName).absolutePath
val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
val cursor = db.rawQuery("SELECT * FROM $tableName LIMIT 50", null)
```

**Empty state:** "No databases found" with a brief explanation ("This app doesn't use SQLite/Room databases, or databases haven't been created yet.")

**Loading state:** Skeleton shimmer rows while scanning databases and loading table data.

**Error state:** "Cannot open database [name]" with reason (locked, corrupt, permission denied). Show retry button.

### SharedPreferences empty state

"No SharedPreferences files found" with explanation ("This app hasn't written any SharedPreferences yet, or files are stored in a non-standard location.")

---

## Screen 3 — Deep Link Tester

### Layout (top to bottom)

1. URI input field (auto-focused on open, monospace font)
2. Extras builder (dynamic key-value rows, + to add, × to remove)
3. Presets row (horizontal scroll chips)
4. Fire Intent button (full width, dark fill)
5. History section (last 10 fired URIs, tap to refill)

**Keyboard handling:** Entire screen wrapped in `verticalScroll` with `imePadding()`. When URI input is focused, history section scrolls up to remain visible above keyboard.

### URI input

- Placeholder: `app://screen/id or https://yourapp.com/path`
- Monospace font for clarity
- Validates before firing — shows inline error for malformed URIs
- Supports both custom schemes and HTTPS app links

### Extras builder

- Each row: key field + value field + remove (×)
- Tap + adds a new empty row
- Empty rows skipped on fire — no null extras added
- Type inference: numbers parsed as Int, "true"/"false" as Boolean, rest as String

### Presets

- Named saves of URI + extras combo
- Tap to load into fields
- "+ Save" chip saves current state with a name dialog
- Stored in `droidkit_internal` SharedPreferences as JSON array

### Fire intent

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    extras.forEach { (k, v) -> putExtra(k, v) }
}
try {
    context.startActivity(intent)
    saveToHistory(uri) // append to history, cap at 10
} catch (e: ActivityNotFoundException) {
    showSnackbar("No app handles this URI")
}
```

### History

- Last 10 URIs, most recent first
- Each row: URI (truncated) + relative time
- Tap → fills URI input field
- Long press → delete from history
- Stored in `droidkit_internal` SharedPreferences via `HistoryRepository`

**Empty state (no history):** "No deep links fired yet" with subtitle "Fire a URI above — it’ll appear here for quick replay."

**Error state (intent fire failed):** Snackbar: "No app handles this URI" with "Dismiss" action.

---

## Screen 4 — Push Notification Tester

### Layout (top to bottom)

0. **Permission banner (Android 13+ only)** — shown if `POST_NOTIFICATIONS` not granted
1. Preset chips row (Default / Success / Promo / OTP / Alert)
2. Title input
3. Body input
4. Deep link input (on tap action)
5. Channel selector (pill toggle)
6. Live preview card (updates in real time)
7. Send notification button (teal fill)

**Keyboard handling:** Screen content in `verticalScroll` with `imePadding()`. Live preview card and Send button remain visible via scroll.

### Notification permission (Android 13+)

- On API 33+, `POST_NOTIFICATIONS` runtime permission is required
- If not granted: show a prominent yellow banner at top of screen:
  - Text: "Notification permission required"
  - Subtitle: "DroidKit needs permission to show test notifications"
  - Button: "Grant permission" → triggers `ActivityResultContracts.RequestPermission`
- If permanently denied: banner shows "Permission denied" with "Open Settings" button
- On API < 33: banner hidden, notifications work without permission

### Preset definitions

| Preset | Title | Body | Deep link | Channel |
| --- | --- | --- | --- | --- |
| Default | Test notification | Hello from DroidKit | *(blank)* | Default |
| Success | Payment successful | Your payment of ₹499 was received | app://orders/latest | Default |
| Promo | Limited time offer | Get 30% off — today only! | app://promo/SALE30 | Default |
| OTP | Your OTP is 847291 | Valid for 10 minutes. Do not share. | *(blank)* | Heads-up |
| Alert | Action required | Please verify your email address | app://settings/email | Heads-up |

### Live preview card

- Mimics how the notification will appear in the shade
- Shows: app icon + app name + timestamp + title + body
- Updates on every keystroke — no submit needed
- Visually distinct from the inputs (white card with subtle border)

### Channel selector

- Default: standard notification
- Silent: `NotificationCompat.PRIORITY_LOW`, no sound/vibration
- Heads-up: `NotificationCompat.PRIORITY_HIGH`, shows as banner
- Channels created lazily on first use per type

### Notification firing

```kotlin
val pendingIntent = if (deepLink.isNotBlank()) {
    PendingIntent.getActivity(
        context, 0,
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
} else null

val notification = NotificationCompat.Builder(context, channelId)
    .setSmallIcon(R.drawable.ic_droidkit)
    .setContentTitle(title)
    .setContentText(body)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .build()

NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
```

**Error state (permission denied):** Send button disabled with tooltip "Grant notification permission to send test notifications."

**Error state (notification channel blocked):** Snackbar: "Channel [name] is blocked in system settings" with "Open Settings" action.

### The killer demo flow

1. Select "Success" preset (auto-fills all fields)
2. Change deep link to a real screen in your app
3. Tap "Send notification"
4. Notification drops down as heads-up banner
5. Tap it → navigates to the correct screen
6. Record this as a GIF → LinkedIn post content ready

---

## Compose implementation notes

### Navigation

```kotlin
// Single NavHost for the whole toolkit
NavHost(navController, startDestination = "dashboard") {
    composable("dashboard") { DashboardScreen(navController) }
    composable("storage") { StorageInspectorScreen() }
    composable("deeplink") { DeepLinkTesterScreen() }
    composable("notifications") { NotificationTesterScreen() }
}
```

### Color tokens + Theme (Compose)

```kotlin
// Color tokens — referenced by individual screens
object DroidKitColors {
    val StorageBlue = Color(0xFF185FA5)
    val LinkGreen = Color(0xFF3B6D11)
    val NotifAmber = Color(0xFFBA7517)
    val FireButton = Color(0xFF1D9E75)
    val DarkSurface = Color(0xFF1A1A1A)
}

// Composable theme wrapper — used in DroidKitActivity.setContent { DroidKitTheme { ... } }
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

### Shared ViewModel pattern

Each screen gets its own ViewModel. No shared state between modules — they're intentionally independent:

```kotlin
// StorageViewModel — reads SharedPrefs + Room
// DeepLinkViewModel — manages URI, extras, history, presets
// NotificationViewModel — manages payload, channel, preview state
```

---

## Design decisions log

| Decision | Rationale |
| --- | --- |
| Dark phone shell in mockups | Makes toolkit feel deliberately separate from host app UI |
| Pill tabs not underline tabs | More touch-friendly on mobile, visually distinct |
| Live notification preview | Reduces fire-and-check iteration loop — saves time |
| Auto-focus URI input | Fastest path to firing — no extra taps |
| History capped at 10 | Enough for session context, doesn’t clutter |
| Presets saved to `droidkit_internal` prefs | Persist across app restarts, isolated from host app prefs |
| Back press returns to host | Never traps the developer inside the toolkit |
| Generic DB access via raw SQLiteDatabase | No coupling to specific app’s DAO/Room classes — works in any project |
| Confirmation dialogs on destructive actions | Prevents accidental SharedPrefs edits/deletes that could break host app state |
| Search bar on SharedPrefs | Essential for apps with 100+ pref keys — unusable without filter |
| POST_NOTIFICATIONS permission banner | Android 13+ requirement — silent failure without it would confuse users |
| Removed Phase 2 placeholder card | Signalled incompleteness — ship only what’s ready |
| `DroidKitTheme` as Composable, not object | Wraps MaterialTheme correctly, consistent with Compose conventions |
| `imePadding()` on all input screens | Prevents keyboard from covering content — standard Compose keyboard handling |