# IMPL-00 — App Skeleton, Navigation, MockEngine (Phase P0)

**Implements:** master §16 (all screens), §4 contract surface (as interface), §15-P0 exit criteria.
**Rule:** every screen binds ONLY to `EngineClient`. No real WebView, no network, no IPC yet.

## 1. Stack & Structure

Kotlin · Jetpack Compose + Material3 · Navigation-Compose · Room · kotlinx-serialization · Hilt.

```
app/src/main/java/app/chameleon/
├─ App.kt, MainActivity.kt
├─ ui/
│  ├─ nav/AppNav.kt                  // routes: fleet, tasks, profiles, pulse, settings,
│  │                                 //        maximized/{id}, wizard/{step}, trace/{taskId}
│  ├─ splash/SplashScreen.kt         // mock checks → Main
│  ├─ fleet/FleetScreen.kt, ProfileCard.kt, FleetViewModel.kt
│  ├─ maximized/MaximizedScreen.kt, AgentLogDrawer.kt, InstructionBar.kt
│  ├─ wizard/WizardScreens.kt (steps 1–6), DeviceLibrary.kt, ConsistencyValidator.kt
│  ├─ tasks/TasksScreen.kt, TaskTraceScreen.kt, Composer.kt
│  ├─ pulse/PulseScreen.kt
│  ├─ settings/SettingsScreen.kt
│  └─ components/StatusDot.kt, WarmthMeter.kt, ThumbImage.kt
├─ engine/
│  ├─ EngineClient.kt                // THE contract (below)
│  ├─ model/ (ProfileUiState, ProfileStatus, AgentEvent, ThumbFrame, TaskUi, ActionJson)
│  └─ mock/MockEngine.kt
└─ data/ (AppDb.kt, ProfileEntity, TaskEntity, EventEntity, repos/)
```

## 2. The Contract (UI ↔ engine boundary)

```kotlin
enum class ProfileStatus { SLEEPING, WAKING, IDLE, BROWSING, WATCHING, TYPING, ERROR }

data class ProfileUiState(
    val id: Int, val alias: String, val deviceLabel: String,
    val status: ProfileStatus, val currentHost: String?,
    val proxyOk: Boolean, val warmth: Int, val nextWakeAt: Long?,
    val isLive: Boolean)

data class ThumbFrame(val profileId: Int, val jpeg: ByteArray, val ts: Long)
data class AgentEvent(val profileId: Int, val ts: Long,
    val kind: String,   // "llm" | "motor" | "nav" | "mitm" | "sys"
    val text: String)

interface EngineClient {
    val profiles: Flow<List<ProfileUiState>>
    fun profileFlow(id: Int): Flow<ProfileUiState>
    val thumbs: Flow<ThumbFrame>
    val events: Flow<AgentEvent>
    suspend fun open(id: Int)
    suspend fun close(id: Int, save: Boolean = true)
    suspend fun wakeNow(id: Int); suspend fun sleepNow(id: Int)
    suspend fun snapshot(id: Int): String
    suspend fun action(id: Int, action: ActionJson)
    suspend fun sendInstruction(targets: List<Int>, text: String, runAt: Long?)
    suspend fun runQa(id: Int)
}
```

`ActionJson` mirrors master §7 schema verbatim (`tap/swipe/type_text/key/wait/navigate/back/volume/maximize/minimize`).

## 3. MockEngine Behavior

- Seeds 8 profiles (varied aliases/devices/warmth); persists to Room.
- `thumbs`: emits placeholder JPEGs (solid color + alias text) at 1Hz for live profiles.
- Status machine per live profile: `IDLE→BROWSING→WATCHING→TYPING→IDLE` on log-normal-ish timers (20–90s); occasional `SLEEPING`.
- `events`: canned lines, e.g. `llm: "3rd result matches, dwelling 2.1s"`, `motor: tap(512,830) σ=2.8`, `mitm: domain_visited reddit.com`.
- `sendInstruction`: creates TaskEntity; transitions `queued→running→done` over 10–30s; trace screen shows generated mock trace lines.
- Wizard `Create` → new ProfileEntity with `phase=WARMUP`, warmth=0, status SLEEPING.

## 4. Screen Requirements (acceptance per screen)

- **Splash:** 4 mock checks with spinners → auto-nav.
- **Fleet:** header shows `MITM✓ LLM✓ n/8`; grid 2-col; cards show all anatomy fields from §16; long-press menu all 7 items wired to MockEngine; `[+ New]` → wizard.
- **Maximized:** placeholder "WebView" surface (colored box + current URL text), agent-log drawer streaming `events` for that id, instruction bar → `sendInstruction([id], …)`, quick-action buttons emit mock events.
- **Wizard:** 6 steps; step 2 uses `DeviceLibrary` (≥6 real device presets with consistent fields); `ConsistencyValidator` shows green/red per field (mock logic: preset = green, manual edit mismatch = red); Review blocks Create until all green.
- **Tasks:** composer + queue list + trace view.
- **Pulse:** timelines (bars per profile over 24h), event feed, interest chips, stats row.
- **Settings:** all toggles persist to DataStore; QA "Run now" emits mock report.

## 5. Exit Criteria (P0 done when)

- [ ] App fully navigable end-to-end with zero crashes.
- [ ] Grid animates live mock thumbs at ~1Hz; statuses transition visibly.
- [ ] Wizard creates a persisted profile that appears in Fleet as SLEEPING/WARMUP.
- [ ] Instruction → task → trace flow works.
- [ ] **No screen references anything outside `EngineClient`** (swap-ready for P1).