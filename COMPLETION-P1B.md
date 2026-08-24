# Phase P1b: WebView Hardening, Fullscreen Support & Minimize/Audio Semantics

## 1. Overview & Verification Summary
- **Target**: Phase P1b (WebView Hardening, Fullscreen Video, Mute & Visibility Semantics).
- **Status**: **COMPLETE & VERIFIED**.
- **Unit Tests**: Passed (`testDebugUnitTest` executed 32 tasks successfully).
- **Compilation & Packaging**:
  - `compileDebugAidl` & `compileReleaseAidl` succeeded.
  - `assembleDebug` & `assembleRelease` succeeded.
  - Signed Release APK generated at `app/build/outputs/apk/release/app-release.apk` (42.9 MB).

---

## 2. Hardened WebView Settings (`BaseEngineWorker.kt`)
In `BaseEngineWorker.kt`, each isolated worker process configures its `WebView` instance with strict mobile fidelity, anti-fingerprinting realism, and session persistence:

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    useWideViewPort = true               // CRITICAL for authentic mobile viewport rendering
    loadWithOverviewMode = true
    setSupportZoom(false)
    builtInZoomControls = false
    mediaPlaybackRequiresUserGesture = false // Unlocks automated media playback for simulated dwell
    cacheMode = WebSettings.LOAD_DEFAULT // Real caching across process restarts
}

val userAgent = profileData.getString("user_agent")
if (!userAgent.isNullOrBlank()) {
    settings.userAgentString = userAgent
}

CookieManager.getInstance().apply {
    setAcceptCookie(true)
    setAcceptThirdPartyCookies(currentWv, true) // Required for Google Auth, YouTube, OAuth
}
WebView.setWebContentsDebuggingEnabled(false)    // Stealth: Disables DevTools inspector hooks
```

---

## 3. Fullscreen HTML5 Video Support (`WebChromeClient`)
To support full-fidelity video rendering (e.g. YouTube HTML5 player requesting fullscreen), a `WebChromeClient` was implemented with custom view interception:

- Intercepts `onShowCustomView(view, callback)` and `onHideCustomView()`.
- Dispatches AIDL callback `onCustomViewChanged(boolean isShowing)` across Binder to the main UI process.
- The UI layer (`MaximizedViewModel` and `MaximizedScreen`) tracks this state to display fullscreen playback indicators and seamless overlay rendering.

---

## 4. Operator Minimize vs. Simulated App-Switch Semantics

| Mode | Trigger | Semantics & Behavior |
| :--- | :--- | :--- |
| **Operator Minimize** | Navigating back to Fleet Grid / Card UI | **Audio Muted Only**. The WebView stays running in the background worker process. JS evaluates `document.querySelectorAll('video, audio').forEach(el => el.muted = true)` so up to 8 running profiles do not emit cacophony to the operator. |
| **Operator Maximize** | Opening a profile card into Maximized View | **Audio Unmuted**. JS evaluates `document.querySelectorAll('video, audio').forEach(el => el.muted = false)`. |
| **Simulated App-Switch** | Agent / LLM Action (`ActionJson.SimulateAppSwitch`) | **Native Visibility Change & Auto-Pause**. Invokes `webView.onPause()`, causing Chromium to trigger internal `visibilitychange = hidden`. YouTube/media players naturally auto-pause. After `durationMs`, invokes `webView.onResume()`, restoring `visibilitychange = visible` while remaining paused until human-like motor interaction resumes playback. |

---

## 5. IPC AIDL Updates

### `IEngineCallback.aidl`
```aidl
package app.semblance.engine.ipc;

interface IEngineCallback {
    void onStateChanged(int profileId, String status, String currentUrl);
    void onDomainVisited(int profileId, String host);
    void onError(int profileId, String message);
    void onThumbnailReady(int profileId, in byte[] jpegData);
    void onCustomViewChanged(boolean isShowing);
}
```

### `IEngineWorker.aidl`
```aidl
package app.semblance.engine.ipc;

import app.semblance.engine.ipc.IEngineCallback;

interface IEngineWorker {
    void openProfile(in Bundle profileData);
    void closeProfile(boolean saveState);
    void loadUrl(String url);
    void requestThumbnail();
    void executeAction(String actionJson);
    void maximize();
    void minimize();
    void simulateAppSwitch(long durationMs);
    void registerCallback(IEngineCallback cb);
    void unregisterCallback(IEngineCallback cb);
}
```

---

## 6. Verification
- **Automated Tests**: Added tests in `RealEngineTest.kt` verifying `maximize`, `minimize`, and `simulateAppSwitch` event dispatch and repository recording.
- **Gradle Build**: Ran and verified `./gradlew.bat testDebugUnitTest assembleRelease assembleDebug`.
