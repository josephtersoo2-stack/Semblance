# Phase P1c Completion: Viewport, Readiness & URL Normalization

## Overview
Phase P1c resolves the offscreen/detached worker WebView rendering failure, IPC command readiness race conditions, URL normalization, and state machine honesty.

---

### Fix 1: Explicit Worker Viewport (`BaseEngineWorker.kt`)
- Added `applyViewport(w, h)` calling `measure()` and `layout()` on the worker's headless `WebView` using the profile's screen dimensions.
- Invoked `applyViewport()` immediately upon `WebView` creation in `openProfile()`.
- Updated `requestThumbnail()` to use the real measured viewport dimensions and maintain 300px-wide JPEG scaling.

### Fix 2: Screen Dimensions in IPC Bundle (`RealEngine.kt`)
- Added `screen_width` and `screen_height` to the `openProfile` IPC Bundle, sourced from `ProfileEntity.screenWidth` / `ProfileEntity.screenHeight` (defaulting to 1080x2400).

### Fix 3: Worker Readiness (`RealEngine.kt`)
- Introduced per-profile `ConcurrentHashMap<Int, CompletableDeferred<Unit>>` readiness tracking.
- Initialized in `open()` before binding to worker service.
- Completed in `onServiceConnected()` after `openProfile()` and `registerCallback()`.
- Protected `action()`, `maximize()`, `minimize()`, and `simulateAppSwitch()` with a 5-second readiness timeout, emitting a visible `"Worker not ready"` error event on timeout instead of dropping commands silently.

### Fix 4: URL Normalization (`UrlUtils.kt`)
- Implemented `UrlUtils.normalizeUrl(raw: String): String?` to trim whitespace, prepend `https://` to scheme-less inputs, reject non-network schemes (e.g. `javascript:`, `ftp:`, `file:`), and return `null` on invalid/empty URLs.
- Applied across all navigation paths (`RealEngine.action`, `RealEngine.wakeNow`, `BaseEngineWorker.loadUrl`, `BaseEngineWorker.openProfile`).

### Fix 5: Honest State Machine (`BaseEngineWorker.kt`)
- Removed fake immediate `broadcastState("IDLE", lastUrl)` after `loadUrl()`.
- State transitions are now driven strictly by `WebViewClient` callbacks:
  - `onPageStarted` -> `BROWSING`
  - `onPageFinished` -> `IDLE`
  - `onReceivedError` -> `ERROR` with failure details and error code.

---

## Verification & Unit Testing
- **Unit Tests**: Added `UrlUtilsTest` (scheme prepend, whitespace trim, scheme preservation, empty/blank rejection, non-network scheme rejection) + `RealEngineTest` (seeds, wizard lifecycle, state events). All 20 unit tests pass.
- **Build Status**:
  - `testDebugUnitTest`: **PASSED** (0 failures)
  - `assembleDebug`: **BUILD SUCCESSFUL** -> `app/build/outputs/apk/debug/app-debug.apk` (56.3 MB)
  - `assembleRelease`: **BUILD SUCCESSFUL** -> `app/build/outputs/apk/release/app-release.apk` (41.0 MB)
