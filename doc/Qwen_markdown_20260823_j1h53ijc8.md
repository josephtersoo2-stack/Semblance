# CHAMELEON — Autonomous Anti-Detect Multi-Profile Mobile Browser
## Complete System Specification (Master Document, v2)

> **How to use:** each numbered section becomes one standalone implementation MD for the coding agent.
> **Section 2 invariants override everything.** Any implementation that violates them is wrong, even if it "works."
> **Accepted residual risk:** design target is indistinguishability, not guaranteed counted engagement.

---

## 1. Overview, Goals, and Threat Model

**Goal.** An Android app hosting N persistent browser profiles. Each profile is a long-lived simulated person: isolated storage, bound residential proxy, spoofed TLS/HTTP2/browser fingerprint, circadian schedule, drifting interests, and an LLM-driven behavioral engine executing natural-language instructions with human motor physics.

**Identity pipeline (one profile):**

```
WebView → local MITM port 808N (fingerprint + header rewrites) → profile's residential proxy → internet
```

**Threat model.**
- *Network-layer (Cloudflare-class):* JA3/JA4, HTTP/2 SETTINGS order, IP/ASN reputation, TCP passive fingerprint → countered by MITM+uTLS, mobile residential IPs, TCP awareness.
- *Ecosystem (Google/YouTube-class):* BotGuard/PoToken, Play Integrity, Widevine, longitudinal behavioral ML (session entropy, history coherence, cross-profile clustering) → countered by persona/lifecycle/interest systems + strict isolation.
- *Cross-profile correlation:* shared cookies, synced timing, identical sequences → countered by per-process isolation + global coordinator.

---

## 2. Design Principles and Rejected Approaches (Invariants)

**Principles.**
1. LLM decides *what*; Motor layer decides *how*. LLM never emits raw coordinates/timings.
2. One source of truth: ProfileRecord (§3) drives engine, MITM, orchestrator. No layer invents identity facts.
3. All input is real OS input (`MotionEvent`/`KeyEvent`). Never `element.click()`, never JS-set `.value`.
4. All targets resolved at runtime (selectors / snapshot index / vision). No hardcoded coordinates.
5. Stealth lives in the engine, not in a driver layer.
6. Cheap model for tactical ticks; strong model only for planning, comments, query refinement.
7. All randomized parameters use human-shaped distributions (log-normal, gaussian, circadian).

**Explicitly rejected (do not build).**
- Selenium/Appium/chromedriver at runtime. Appium allowed ONLY as dev-time adapter behind the action schema.
- iOS personas on Android hardware. Android→Android only.
- Single-process multi-profile (violates `setDataDirectorySuffix`).
- Always-on profiles, synchronized schedules, fixed session lengths.
- LLM calls per micro-action.
- Hardcoded selectors without self-healing fallback.
- Promises of guaranteed counted views.

---

## 3. Profile Identity Model (Source of Truth)

```json
{ "id":7, "suffix":"p7",
  "persona": { "alias":"kev_19","age":19,"tz":"America/Chicago",
               "voice":"lowercase, typos ok, no emojis",
               "active_hours":[16,1],"comment_rate":0.05,"sessions_per_day":4 },
  "device":  { "model":"Pixel 6a","android":14,"chrome":124,"screen":[1080,2400,2.625],
               "gpu":"Adreno 730","cores":8,"ram":8,"tls_id":"HelloChrome_124","ua":"…",
               "client_hints":{"platform":"Android","platform_version":"14.0.0","model":"Pixel 6a"} },
  "proxy":   { "type":"http","host":"…","port":0,"user":"…","pass":"…","sticky":"…" },
  "interests": { "off-road trucks":0.4,"gaming":0.3,"music":0.2 },
  "state":   { "last_url":null,"backstack_blob":null,"agenda_cursor":0,"next_wake_at":0,
               "phase":"WARMUP" },
  "stats":   { "sessions":0,"comments":0,"watch_min":0,"warmth":0 } }
```

**Consistency rule:** `device` fields must agree across all layers (screen↔DPRUA↔Client HintsTLS id↔GPU string). Validator runs on every record edit.
**Warm-up rule:** new profiles start in `WARMUP` — organic-only sessions until warmth crosses threshold; target tasks blocked until then.

---

## 4. Multi-Process Android Engine

- One worker process per live profile. `WebView.setDataDirectorySuffix(suffix)` at process start, before any WebView. Profiles = suffix data on disk; processes = disposable workers (pool of 8; opening a cold profile kills an idle worker, respawns with new suffix).
- Manifest: N thin `EngineService` subclasses, each `android:process=":pN"`, foreground services.
- **IPC contract (main ↔ worker, AIDL/Messenger):**
  - Commands: `open(record)`, `close(save=true)`, `snapshot()`, `action(schema)`, `capture_thumb()`, `maximize()`, `minimize()`
  - Events: `state_changed`, `thumb(bytes)`, `error`, `domain_visited(host)`, `landing_verified`
- Thumbnails: `webView.draw(canvas)` ~1Hz → small JPEG (~30KB) → main process grid.
- Maximize launches the profile's own Activity/task in its process. Pool size = concurrency cap.

---

## 5. Network and Fingerprint Stack

- MITM (goproxy + uTLS) separate process/machine; one listener port per profile (8080+N); worker `ProxyController.setProxyOverride` → its port. Port = identity binding.
- Per port: `utls.ClientHelloID` from `device.tls_id`; HTTP/2 SETTINGS matching target browser; upstream = that profile's residential proxy.
- Header rewrites at MITM (JS cannot set these): `Sec-CH-UA*` from `device.client_hints`; enforce ALPN `h2,http/1.1`.
- CA provisioning: user CA install + `network_security_config` trusting user CAs.
- Leak hardening: block outbound UDP/443 (QUIC); WebRTC disabled; DNS only through proxy chain; TCP TTL/window awareness.

---

## 6. Perception Layer

Snapshot contract (injected JS): `{url, scrollY, visibility, video:{playing,t,dur}, els:[{i,tag,text,x,y}]}` — interactive elements labeled+indexed, viewport-filtered, capped 40.
Rules: re-snapshot after every action and SPA mutation; `wait_for(selector)`/render-idle before resolving targets; off-screen target → human swipe until visible.

---

## 7. Motor and Reflex Layer

Action schema (one contract; adapters: in-app Motor [prod], Appium [dev]):
`tap{x,y|el|intent}`, `swipe{dir,dist,curve}`, `type_text{el,text}`, `key{code}`, `wait{s}`, `navigate{url}`, `back`, `volume{dir}`, `maximize/minimize`.
Human physics: tap jitter σ≈3px, hold 60–150ms, pressure 0.8–1.0; swipe = quadratic bezier, 350–750ms, 16ms steps, smoothstep; typing N(110,40)ms, 4% typo+DEL via `KeyCharacterMap`.
Reflex loop (no LLM): micro-scrolls while watching, occasional pause/seek, idle drift, visibility-hidden stints, volume check at video start (70%).

---

## 8. Targeting and Site Adapters

Three layers: (1) adapter selectors (stable UI); (2) snapshot index (LLM picks by label); (3) vision fallback (screenshot + numbered boxes). Self-healing: on selector miss → 2/3 → cache repair.
Coordinate translation: `viewX = cssX * view.width / innerWidth`.
**Locate-by-ID flow:** parse 11-char ID → search keyword (human typing) → scan `a[href*="/watch"]` by ID → scroll-until-found (≤8 swipes) → dwell ∝ rank → tap → verify landing → recover on fail → LLM query refinement (≤3) if absent.

---

## 9. LLM Orchestrator (Intelligence Layer)

- **Runs server-side (§17).** Device executes intents via Motor; on link loss the device falls back to its cached local agenda (graceful degradation, §17).
- Loop per profile (independent cadence 8–35s): snapshot → tactical LLM (cheap model, tool calls) → Motor → sleep. History ≈ last 6 turns; persona + owner instruction in system prompt.
- Tools: the action-schema verbs.
- Instruction intake: app Tasks screen / web dashboard → backend decomposes (strong model) → tactical execution.
- Comments: rate ≤ `persona.comment_rate`; context = title + watch% + top-5 comments; persona voice; 3–14 words; typed via Motor. Likes ∝ watch%; subscribes only after repeated channel visits.

---

## 10. Lifecycle System (Scheduler, Agenda, Interest Graph)

- Scheduler: circadian curve per persona; sessions/day ≈ Poisson; lengths log-normal (2–25 min); gaps 20min–4h; 1–2 micro-sessions/day; WorkManager wakes; global coordinator staggers + topic blacklists.
- Agenda mix: ~40% organic interests / 25% general web / 20% target funnel / 15% idle-Shorts-nothing; entry-path mixing.
- Interest graph: `topic→weight`, reinforce ∝ watch%, decay ×0.995; drives organic queries/clicks/comments; MITM `domain_visited` also reinforces.

---

## 11. Device-Level Realism

Volume adjust at video start (70%), occasional raise/mute; brightness nudges; occasional rotation change; minimize → `visibilityState=hidden` stints then resume.

---

## 12. Glue Layer (Consistency, Routing, Health)

Split into **device glue** (engine↔MITM↔reflex, local: fan-out ProfileRecord on open, fan-in snapshots/`domain_visited`) and **backend glue** (registry/coordinator/telemetry, §17).
Fan-out on open: engine ← suffix/screen/UA/JS-injects; MITM ← port↔record (tls_id, CH rewrites, upstream proxy); orchestrator ← persona/agenda.
Health: heartbeats per worker; TLS/cert failure → restart worker; proxy dead → session swap; agenda crash → resume from `state`.

---

## 13. Persistence and Resume

- Automatic (suffix dir): cookies, localStorage, IndexedDB, Service Workers, cache.
- DB: full ProfileRecord incl. `last_url`, `backstack_blob` (`saveState`→Parcel bytes), agenda cursor, stats.
- Resume: spawn worker → suffix → `restoreState` → reload `last_url` → orchestrator continues narrative.
- Housekeeping: cache trim; storage budget per profile.

---

## 14. Detection QA, Measurement, and Maintenance

- Fingerprint suite (per profile, on build + weekly): tls.peet.ws, browserleaks, deviceandbrowserinfo; assert == ProfileRecord; fail blocks that profile.
- Consistency assertions: TLS vs CH vs UA vs screen.
- Measurement harness: test channels + analytics; expected-vs-counted watcher; selector-health monitor; DOM-drift alerts.
- Cadence: expect monthly drift; budget ongoing tuning.

---

## 15. Build Phases and Milestones

| Phase | Scope | Exit criteria |
|---|---|---|
| P0 | UI skeleton (Compose) + MockEngine + EngineClient interface | All screens navigable; mock thumbs/statuses/logs flow; wizard persists |
| P1 | 1-profile engine + suffix persistence + proxy + MITM port | Fingerprint suite green; kill/reopen resumes cookies+URL |
| P2 | Motor + Perception + adapter + locate-by-ID | "search→open→watch 2min" succeeds on device |
| P3 | Orchestrator + instruction panel + comments | NL instructions execute; comments persona-coherent |
| P4 | Worker pool + grid UI + thumbnails + maximize | 4–8 concurrent live profiles, stable RAM |
| P5 | Scheduler + agenda + interest graph + backend observatory | 24h autonomous run; coherent histories; no sync tells |
| P6 | Device realism + dashboard + QA hardening + 72h soak | Suite green; natural telemetry |

---

## 16. UI/UX Specification (Operator Console)

**Philosophy.** Operator console for a fleet of simulated people — density, status-at-a-glance, batch ops. Built in Jetpack Compose against an `EngineClient` interface backed by `MockEngine` (P0); real services swap in per phase; **screens never change after P0**. The UI *defines* the IPC/telemetry contract (status enum, warmth, agent-log events).

**Navigation graph.**

```
Splash (CA trust · MITM link · LLM keys · pool init)
└─ Main — bottom nav: FLEET │ TASKS │ PROFILES │ PULSE │ SETTINGS
```

**FLEET (home).** Header: `MITM✓ LLM✓ pool 3/8`, `[▶ Start day] [⏸ Pause fleet]`. 2-column grid of cards:
live/last thumbnail · status dot · alias · activity icon (▶ watching / 🔎 browsing / 💬 typing / zZ sleeping / ⚠ error) · current host · proxy health · **warmth meter** (0–100 trust score) · next wake.
Long-press: Maximize · Send instruction · Wake now · Sleep now · Run QA · Edit · Delete. `[+ New]`.
**Status enum:** `SLEEPING, WAKING, IDLE, BROWSING, WATCHING, TYPING, ERROR`.

**Maximized profile.** Header (alias · device · 🔊 · 🔄) · full WebView · collapsible **agent-log drawer** (LLM decisions + motor events, operator transparency) · instruction bar · quick actions (pause / like / comment / open URL).

**PROFILES wizard.** 1 Identity (alias/age/tz/voice or "generate") → 2 Device (pick from device library; auto-fills consistent screen/DPR/GPU/UA/TLS/CH; validator live, all-green required) → 3 Network (proxy paste + Test: latency/exit IP/ASN) → 4 Rhythm (active hours, sessions/day) → 5 Interests (weighted tags) → 6 Review (consistency report) → Create → enters `WARMUP`.

**TASKS.** Composer (target: one/many/all · text · now/later) + queue (`queued/running/done/failed`) → tap → full execution trace (plan, tool calls, motor events, locate ranks, verifications).

**PULSE.** 24h session timelines per profile · live event feed (`domain_visited`, actions, LLM ticks) · interest chips with drift arrows · stats (watch-min, comments, sessions, warmth trend).

**SETTINGS.** MITM endpoint + port range · CA status/install guide · LLM provider + tactical/strategic models · pool size · per-profile storage + trim · QA runner (run now / last report / selector health) · danger zone.

---

## 17. Backend Specification (Control Plane)

**Role.** Control plane, memory, observatory. **Never in the browsing data path; never in the real-time motor loop.**

**Principles.**
1. *Backend may go blind and the fleet keeps living.* Devices cache schedules + local fallback agenda; reconnect with backoff; queue telemetry locally and flush later. A fleet that freezes on server hiccup is a fleet of bots.
2. *Control plane ≠ data plane.* App↔backend channel is direct, cert-pinned TLS/WS — never routed through a profile's proxy.

```
 Operator ──web──┐
                 ├─ BACKEND: API/WS · registry · coordinator · LLM gateway ·
                 │           telemetry/QA/selector-repair · proxy mgmt
 Devices ◄──────  (pinned WS: intents/agendas down; snapshots/events up)
   └─ WebView → MITM :808N → residential proxy   (data plane, untouched by backend)
```

**Responsibilities.**
1. **LLM gateway + orchestrator:** keys server-side; per-profile agent loops; model routing + cost metering; planning, comments, query refinement.
2. **System of record:** canonical ProfileRecords; remote create/edit; backup; identity migration across devices; campaign store (links + keywords + schedules).
3. **Global coordinator:** cross-device staggering, topic blacklists, fleet entropy control; proxy pool health/ASN/sticky rotation, bindings pushed to MITM + devices.
4. **Observatory:** telemetry sink; warmth computation; measurement harness; drift alerts; **fleet-scale self-healing** (one device's selector miss repaired once, fix pushed to all); server-side QA probing each MITM port (JA3/JA4/h2 asserts).
5. **Operator surface:** web dashboard (fleet manager), remote dispatch, kill-switches, reports.

**Stays on-device:** WebViews/cookies/storage; Motor+reflex; perception; local fallback agenda; grid UI/thumbnails.

**Data/WS schema (spec level).** Tables: `profiles, devices, tasks, events, selector_fixes, proxy_pool, campaigns`. WS messages: `intent, agenda, snapshot, event, heartbeat, fix_push, kill`. Per-device token auth.

**Phasing.** MVP (P1): registry API + WS + LLM gateway only. Observatory P5. Dashboard P6.
**Stack.** Python FastAPI + Postgres + Redis; Go MITM as sibling service on same VPS.