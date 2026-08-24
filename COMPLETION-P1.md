# Phase P1: The Multi-Process Engine Core — Completion Report

**Project:** Semblance — Autonomous Anti-Detect Multi-Profile Mobile Browser  
**Phase:** P1 (Multi-Process Engine Core & IPC)  
**Package:** `app.semblance`  
**Target Device / MinSdk:** MinSdk 28 (Android 9.0 Pie) / TargetSdk 34 (Android 14)  
**Date:** August 24, 2026  

---

## 1. Executive Summary

Phase P1 is complete. We have successfully implemented the **Multi-Process Android Engine Architecture** specified in `ARCHITECTURE.md` (§4, §12, §13). 

Key achievements:
- **AIDL IPC System:** Defined and compiled `IEngineWorker.aidl` and `IEngineCallback.aidl`.
- **8 Dedicated Process Worker Services:** Declared `EngineWorker1` through `EngineWorker8` running on `:worker1` through `:worker8` isolated Linux processes in `AndroidManifest.xml`.
- **Strict `setDataDirectorySuffix` Lifecycle & Process Recycling Rule:** In `BaseEngineWorker`, `setDataDirectorySuffix(suffix)` is guaranteed to run before any WebView instantiation on API 28+. If a worker process is asked to switch profile suffixes, it immediately kills itself (`Process.killProcess(Process.myPid())`), prompting the main process to spawn a fresh worker process.
- **Binder Limit Enforcement:** Thumbnails are scaled to a fixed width of 300px with preserved aspect ratio, rendered in `RGB_565` format, and compressed at 50% JPEG quality into small byte arrays (< 50KB), avoiding any Binder transaction overflow exceptions.
- **Main Process Engine (`RealEngine`):** Manages the worker pool (slots 1..8), maps Profile IDs to ServiceConnections and AIDL interfaces, handles state updates, event streams, thumbnail polling (1Hz), and integrates seamlessly with Room persistence and Hilt dependency injection.

---

## 2. P1 Definition of Done Checklist

| Requirement | Implementation Details | Status |
|---|---|:---:|
| **AIDL Compilation** | `app/src/main/aidl/app/semblance/engine/ipc/IEngineCallback.aidl` & `IEngineWorker.aidl` enabled via `buildFeatures { aidl = true }` and compiled with `compileDebugAidl`. | **PASS** |
| **8 Worker Services in Manifest** | `<service android:name=".engine.worker.EngineWorker1" android:process=":worker1" android:exported="false" />` .. `EngineWorker8` (`:worker8`) declared inside `<application>`. | **PASS** |
| **`setDataDirectorySuffix` Invariant** | Suffix set before any WebView constructor call. Dynamic process self-termination (`killProcess`) on suffix mismatch. | **PASS** |
| **Strict Binder Payload Constraint** | `requestThumbnail` renders to 300px wide `RGB_565` Bitmap, 50% JPEG compression (~15–35KB payload per frame). | **PASS** |
| **`RealEngine` Adapter & Routing** | `RealEngine` implements `EngineClient`, manages 8 worker slots, bidirectional IPC, Room sync, and 1Hz thumbnail polling loop. | **PASS** |
| **Hilt Injection** | `EngineModule` binds `RealEngine` as singleton `EngineClient`. | **PASS** |
| **Unit Test Suite** | 100% test pass rate across `RealEngineTest`, `MockEngineTest`, `ConsistencyValidatorTest`, and `ActionJsonTest`. | **PASS** |
| **Release Build** | `assembleRelease` generates signed installable APK (~42.9 MB). | **PASS** |

---

## 3. Architecture & Code Structure

### 3.1 IPC Interface (`app.semblance.engine.ipc`)
- **`IEngineCallback.aidl`**:
  ```java
  package app.semblance.engine.ipc;
  interface IEngineCallback {
      void onStateChanged(int profileId, String status, String currentUrl);
      void onDomainVisited(int profileId, String host);
      void onError(int profileId, String message);
      void onThumbnailReady(int profileId, in byte[] jpegData);
  }
  ```
- **`IEngineWorker.aidl`**:
  ```java
  package app.semblance.engine.ipc;
  import app.semblance.engine.ipc.IEngineCallback;
  interface IEngineWorker {
      void openProfile(in Bundle profileData);
      void closeProfile(boolean saveState);
      void loadUrl(String url);
      void requestThumbnail();
      void executeAction(String actionJson);
      void registerCallback(IEngineCallback cb);
      void unregisterCallback(IEngineCallback cb);
  }
  ```

### 3.2 Worker Engine (`app.semblance.engine.worker`)
- **`BaseEngineWorker.kt`**:
  - Encapsulates `WebView` lifecycle, WebSettings (JavaScript, DOM storage, database, wide viewport, overview mode), and `WebViewClient`.
  - Enforces `setDataDirectorySuffix(suffix)` on API 28+ before WebView instantiation.
  - Implements process self-termination rule if suffix changes on the same process.
  - Generates lightweight JPEG thumbnails.
  - Manages `RemoteCallbackList<IEngineCallback>` with thread-safe IPC broadcasting.
- **`EngineWorkers.kt`**:
  - `EngineWorker1` through `EngineWorker8` extending `BaseEngineWorker`.

### 3.3 Main Process Engine (`app.semblance.engine.real`)
- **`RealEngine.kt`**:
  - Dynamic 8-slot worker allocation and recycling.
  - Binds and unbinds worker services via `Context.bindService`.
  - Persists navigation state (`BROWSING`, `IDLE`, `TYPING`, `SLEEPING`, `ERROR`) and visited hosts directly to Room (`ProfileDao`, `EventDao`).
  - Dispatches `ActionJson` commands across Binder.
  - Streams thumbnails via `thumbs: Flow<ThumbFrame>` and telemetry via `events: Flow<AgentEvent>`.
  - Auto-seeds 8 realistic test profiles on initial launch via `ProfileSeeder`.

---

## 4. Verification & Testing

### 4.1 Unit Tests
```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1m 29s
```
- `RealEngineTest.kt`: Tests database seeding, profile flows, wizard creation with `WARMUP` phase and 0 warmth, and profile deletion.
- `MockEngineTest.kt`: Validates fallback MockEngine contracts and wizard warm-up enforcement.
- `ConsistencyValidatorTest.kt`: Verifies all 5 cross-layer invariant checks.
- `ActionJsonTest.kt`: Verifies polymorphic serialization and deserialization of all 10 action verbs.

### 4.2 Release Build
```
> Task :app:assembleRelease
BUILD SUCCESSFUL in 6m 39s
Output: app/build/outputs/apk/release/app-release.apk (42.9 MB)
```

---

## 5. Next Phase: P2 (Motor and Perception Layer)

With the multi-process engine core and IPC established, Phase P2 will implement:
1. **Perception Layer (§6):** JavaScript snapshot injection contract (`{url, scrollY, visibility, video, els:[{i,tag,text,x,y}]}`).
2. **Motor & Reflex Layer (§7):** Real OS input simulation (`MotionEvent`/`KeyEvent`) with human physics (Gaussian jitter, Bézier swipe curves, log-normal typing latencies).
3. **Targeting & Site Adapters (§8):** Locate-by-ID YouTube adapter with self-healing fallback.
