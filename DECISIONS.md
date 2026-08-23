# Architectural Decisions (P0 Phase)

This document records architectural decisions, assumptions, and smallest reasonable choices made during the implementation of Phase P0 of **Semblance**, conforming strictly to `ARCHITECTURE.md` and `IMPL-00_UI-SKELETON.md`.

## 1. Naming & Package Root
- **Decision:** Product/App name is **Semblance** (`@string/app_name = "Semblance"`). Package root is `app.semblance`.
- **Rationale:** IMPL-00 and system instructions override Chameleon codename with Semblance.

## 2. MockEngine Thumbnail Synthesis
- **Decision:** In Phase P0, `MockEngine` generates 240x360 placeholder JPEG frames on an in-memory Android `Bitmap`/`Canvas` rendered with profile-specific hue, grid lines, status bar, and active alias/host text, encoded to JPEG `ByteArray` at 1Hz for all live profiles in the worker pool.
- **Rationale:** Master §4/§16 and IMPL-00 §3 specify placeholder JPEGs (solid color + alias text) at 1Hz streaming over `EngineClient.thumbs`.

## 3. Strict Boundary & No Direct Room Access from UI
- **Decision:** All ViewModels (`FleetViewModel`, `MaximizedViewModel`, `WizardViewModel`, `TasksViewModel`, `PulseViewModel`, `SettingsViewModel`) interact solely through `EngineClient` (and `SettingsDataStore` for persistent UI settings).
- **Rationale:** IMPL-00 §1/§2/§5 rule: "every screen binds ONLY to `EngineClient`." This ensures full drop-in readiness for P1+ multi-process AIDL services without modifying any UI screen or composable.

## 4. Wizard Consistency Validator Rules
- **Decision:** `ConsistencyValidator` performs 5 live assertion passes:
  1. TLS Handshake: verifies `tlsId` contains the Chrome major version string (e.g. `HelloChrome_124` for Chrome 124).
  2. Client Hints & UA: verifies User-Agent string contains the device model and `clientHintsModel`.
  3. Screen Geometry & DPR: verifies width ≥ 1080, height ≥ 2000, and density in 2.0..4.0 range.
  4. GPU Renderer: verifies valid chipset manufacturer strings (`Adreno`, `Mali`, `Xclipse`).
  5. Platform OS: verifies `Android` platform token with Android 12..15 API levels.
- **Rationale:** Master §3 Consistency rule ("device fields must agree across all layers") and IMPL-00 §4.

## 5. Wizard Profile Creation State
- **Decision:** Newly created profiles from the 6-step Wizard enter the database with `phase = "WARMUP"`, `warmth = 0`, and `status = "SLEEPING"`.
- **Rationale:** Master §3 warm-up rule ("new profiles start in WARMUP — organic-only sessions until warmth crosses threshold") and IMPL-00 §3.

## 6. Action Schema Verbatim Mapping
- **Decision:** `ActionJson` sealed hierarchy mirrors Master §7 schema: `Tap`, `Swipe`, `TypeText`, `Key`, `Wait`, `Navigate`, `Back`, `Volume`, `Maximize`, `Minimize`.
- **Rationale:** Master §7 and IMPL-00 §2 verbatim contract alignment.

## 7. Execution Phasing & TODO References
- **Decision:** All future phase capabilities (real WebViews, multi-process worker services, AIDL IPC, Go MITM uTLS proxy, live LLM API calls, Motor MotionEvent injection) are marked with `// TODO (Master §X)` and omitted from Phase P0.
- **Rationale:** Scope guard in system work order. Zero INTERNET permission declared in AndroidManifest.xml.
