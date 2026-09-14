# Product Milestones

Статус: **DRAFT roadmap**

Каждый milestone начинается только после принятия предыдущего и явного задания пользователя. Acceptance означает выполнение применимых критериев с evidence; недоказанная функция не переносится в обещание следующего этапа.

## M0 Documentation

- **Scope:** обязательные project rules, product/architecture/draft API/security/privacy/test/device specifications, roadmap, open questions и ADR-0001..0003.
- **Exclusions:** Android/Gradle project, Kotlin, manifest, dependencies, dependency versions, реализация и утверждение CRM contract.
- **Dependencies:** исходное ТЗ; отсутствующий полный внешний `AGENTS.md` отмечен, явно переданные правила включены.
- **Acceptance criteria:** все запрошенные файлы существуют; draft/unknown маркированы; термины и milestones согласованы; contradictions search и diff review выполнены.
- **Automated tests:** проверка файлов, заголовков milestone/обязательных полей, ссылок и конфликтующих терминов текстовым поиском.
- **Emulator tests:** не применимы; emulator layer документирован.
- **Physical-device tests:** не применимы; physical layer и target families документированы.
- **Permissions:** не добавляются; `READ_CONTACTS` отмечен approval-required.
- **Risks:** неполный исходный `AGENTS.md`, неизвестные CRM/phone inputs, ошибочное принятие draft за факт.
- **Rollback conditions:** удалить/исправить только противоречивую документацию до принятия; не переходить к M1 при неполном review или неразрешённом scope.

## M1 Android application skeleton

- **Scope:** отдельный анализ min/target API и версий; минимальный buildable native Android shell; agreed package/modules; CI baseline; empty navigation/theme; test harness.
- **Exclusions:** working call screening, CRM production integration, caller card, lifecycle claims, outcomes, production outbox.
- **Dependencies:** M0 accepted; application ID, naming, repository/CI, signing approach and supported API decision; approvals for every dependency.
- **Acceptance criteria:** reproducible clean build/test, launchable empty internal app, no unnecessary permission, documented dependency/API decisions.
- **Automated tests:** build, lint/static analysis, unit/instrumentation smoke, dependency and secret checks as selected.
- **Emulator tests:** install, launch, rotate/recreate, process restart on agreed API matrix.
- **Physical-device tests:** install/launch smoke on at least one reference, one Realme and one Xiaomi target if devices are available; gaps recorded.
- **Permissions:** none unless separately approved; no implicit call/contact permission.
- **Risks:** premature version choice, over-modularization, signing/CI uncertainty, unavailable target devices.
- **Rollback conditions:** revert dependency/architecture choice if build is irreproducible, unsupported or approval missing; remove superseded skeleton implementation after replacement verification.

## M2 CallScreening proof of concept

- **Scope:** minimal isolated `CallScreeningService` PoC, role/setup discovery, deadline measurement, fail-open response, contact-number behavior investigation.
- **Exclusions:** production CRM lookup, final caller-card UX, claims of accurate answered/ended/duration, blocking/rejecting calls.
- **Dependencies:** M1; official Android API review; test SIM/numbers; supported Phone apps; device access.
- **Acceptance criteria:** service responds inside documented internal safety budget in cold/warm/error/network-delay cases; network is absent from response path; unsupported scenarios and role UX recorded.
- **Automated tests:** response decision/state tests, no-network-on-critical-path guard where feasible, timeout/error tests, sanitized timing telemetry.
- **Emulator tests:** role setup, incoming-call simulation where supported, cold start, permission denial, process death and delayed fake network.
- **Physical-device tests:** real incoming calls on reference, Realme UI and HyperOS; saved/unsaved contact cases; locked/unlocked; OEM evidence recorded.
- **Permissions:** platform role/service declarations reviewed; `READ_CONTACTS` добавлен только после explicit approval и successful physical baseline без permission. Denied, granted и revoked branches проверяются отдельно; иных phone/contact permissions нет.
- **Risks:** OEM/Phone-app variation, missed deadline, system contacts not delivered, role unavailable, emulator mismatch.
- **Rollback conditions:** keep feature disabled/remove PoC if deadline independence or fail-open cannot be proven; do not merge speculative production path.

## M3 CRM caller lookup

Статус: **ACCEPTED 2026-07-23 FOR INTERNAL/NGROK SCOPE**; production identity, rate limiting, permanent deployment и remaining fleet validation остаются release-блокерами.

- **Scope:** approved auth flow, agreed lookup contract, networking boundary, sanitized mapping, timeout/cache policy prototype and contract tests.
- **Exclusions:** network delay of screening response, final caller card, write events/outcomes, unapproved CRM fields.
- **Dependencies:** M2 evidence; CRM owners, environments/test data, finalized auth/lookup contract, security/privacy review, dependency approval.
- **Acceptance criteria:** authorized matched/not-found/ambiguous/error mapping works; auth expiry safe; lookup asynchronous to screening; no PII in logs.
- **Automated tests:** API contract/stub tests, mapping, authorization/error/timeout, redaction and cache-freshness tests.
- **Emulator tests:** online/offline/slow network, token expiry, process restart and cache states.
- **Physical-device tests:** cellular/Wi-Fi switching and lookup behavior on reference/Realme/HyperOS without affecting call allowance.
- **Permissions:** network permission only after approval through project change process; no contacts/call-log permissions by implication.
- **Risks:** CRM mismatch, number normalization errors, enumeration, auth leakage, latency and stale/wrong-tenant data.
- **Rollback conditions:** disable remote lookup and fall back to no-card/safe cache if authorization, contract, privacy or call-path isolation fails.

## M4 Caller card

Статус: **ACCEPTED 2026-07-23 — current Xiaomi/HyperOS scope; remaining fleet deferred to M8**

- **Scope:** approved minimal fields, Compose presentation, loading/not-found/error/stale states, privacy/accessibility behavior; выбранная поверхность — только вкладка Calls внутри приложения.
- **Exclusions:** full CRM editing, default dialer replacement, overlay without approval, outcome flow and unverified lifecycle claims.
- **Dependencies:** M3; product-approved fields/wording; lock-screen/notification policy; UX surface feasibility evidence.
- **Acceptance criteria:** correct authorized card appears in approved context; stale/error states clear; no call interference; accessibility/privacy criteria met.
- **Automated tests:** state reducer/ViewModel, Compose UI/screenshot or semantics tests, redaction and authorization-state tests.
- **Emulator tests:** screen sizes, font scale, dark mode, rotation, locked-state simulation, slow/missing lookup.
- **Physical-device tests:** timing and visibility during real calls on reference/Realme/HyperOS; lock screen, notification/recents, accessibility.
- **Permissions:** `ACCESS_NETWORK_STATE` одобрен для recovery retry; no overlay/notification/additional contacts permission unless separately proposed and approved with rationale.
- **Risks:** Android/OEM UI restrictions, PII exposure, late card, inaccessible UI, misleading match.
- **Rollback conditions:** disable card surface and retain safe internal lookup diagnostics if privacy, correctness or call UX fails; remove replaced UI path after validation.

## M5 Call lifecycle investigation and implementation

- **Status:** accepted 2026-07-23 for API 30+ post-call PoC on Pixel_7 Emulator and current Xiaomi/HyperOS; remaining fleet/call configurations deferred to M8 and do not inherit this evidence.
- **Scope:** evidence-driven, process-local observation of `ACTION_POST_CALL`; categorical disconnect cause and duration bucket; missed/rejected/answered-then-ended scenarios without identity retention.
- **Exclusions:** exact `ANSWERED`, exact `ENDED`, exact duration, durable call history, CRM/outbox emission, call recording, API 29 fallback and unsupported outgoing/SMS features.
- **Dependencies:** M2/M4; official API review and physical test matrix. No new permission dependency was accepted.
- **Acceptance criteria:** supported observations map to recorded device-scoped evidence; unsupported facts are not emitted. Met for current scope with remote/local/missed/rejected on Xiaomi and remote on Emulator.
- **Automated tests:** mapping of documented/unknown values, repeated observation count and absence of identity fields. Durable ordering/deduplication/process-death semantics remain future work only if product events are approved.
- **Emulator tests:** API 36 simulated answered incoming + remote end delivered `remote/short`; rapid, missed/rejected and process-state expansion remain pending.
- **Physical-device tests:** Xiaomi/HyperOS real incoming matrix passed for caller hang-up, local end, missed and rejected. Reference/Realme, outgoing, lock/process states, Bluetooth/call waiting/dual-SIM semantics remain M8 evidence gaps.
- **Permissions:** none added. `READ_PHONE_STATE`, `READ_CALL_LOG`, dialer role and additional permissions were explicitly not selected; existing `READ_CONTACTS` is unrelated to post-call processing.
- **Risks:** callbacks inaccurate/absent, OEM divergence, false duration, duplicate/out-of-order events, regulatory implications.
- **Rollback conditions:** remove/disable any event type that cannot meet evidence threshold; keep only verified observations and update product/API docs.

## M6 Call outcome

- **Status:** accepted 2026-07-24 for current API 30+ Emulator + Xiaomi/HyperOS scope; fleet stabilization remains M8.
- **Scope:** approved optional outcome taxonomy, stable local call UUID, FIFO pending UI, Room persistence, Skip, notification/deep-link and temporary caller identity until resolve.
- **Exclusions:** CRM submission/outbox, edit flow, notes, automatic outcome inference, overlay and API 29 fallback.
- **Dependencies:** M5; approved Room migrations, `POST_NOTIFICATIONS`, outcome dictionary and explicit secure-lock caller-identity visibility decision.
- **Acceptance criteria:** user can identify the pending number, select or skip, persist valid state, resolve multiple drafts in order and avoid call interference. Met for current scope.
- **Automated tests:** UUID/draft mapping, one-time select, optional dismiss, identity cleanup, Room DAO/migrations and Compose choices/count.
- **Emulator tests:** API 36 migrations, DB/UI instrumentation, synthetic number notification, direct navigation and resolve pass. Broader interruption/accessibility matrix remains M8.
- **Physical-device tests:** Xiaomi real calls pass for background/secure-lock notification, matching number in Calls, outcome and Skip. Realme/reference pending.
- **Permissions:** `POST_NOTIFICATIONS` explicitly approved; denial leaves Room/in-app path functional. No overlay/call-log/phone-state permission.
- **Risks:** wrong association, lock-screen PII exposure accepted by product but pending privacy/security sign-off, spoofed post-call intent, best-effort pre-outbox insert and taxonomy drift.
- **Rollback conditions:** disable name/number visibility or notification, retain in-app generic draft, and stop rollout on migration/association/privacy failure.

## M7 Offline queue and reliability

- **Status:** accepted 2026-07-24 for current Xiaomi/HyperOS scope; emulator M7 regression and remaining fleet move to M8.
- **Scope:** durable Room v4 outbox, stable UUID, atomic enqueue, WorkManager delivery/recovery, bounded retry/backoff, server idempotency, permanent-failure visibility and real-data Home/Calls/Diagnostics.
- **Exclusions:** silent infinite retries, edit/notes, manual failure remediation, production auth/deployment, battery exemption, foreground service and M8 fleet stabilization.
- **Dependencies:** approved M3 API/M6 outcomes, Room v4/API changes, `RECEIVE_BOOT_COMPLETED`/`WAKE_LOCK`, server schema/idempotency and retention choice.
- **Acceptance criteria:** Xiaomi confirmed offline pending, online sync and queued reboot recovery; stable UUID duplicate produced one server receipt; permanent failure remains visible by design. Met for current scoped acceptance.
- **Automated tests:** atomic enqueue/repository, Room DAO/migrations, client compilation/classification paths, CRM schema/build and create/duplicate integration. Crash/race/clock/storage stress coverage remains open.
- **Emulator tests:** not rerun for M7 in current session; API 36 remains a separate required layer before acceptance.
- **Physical-device tests:** Xiaomi API 35 Room/UI smoke, real-call offline→online delivery and queued reboot recovery pass. Realme/reference pending M8.
- **Permissions:** `INTERNET`/`ACCESS_NETWORK_STATE` previously approved; `RECEIVE_BOOT_COMPLETED`/`WAKE_LOCK` approved for M7. No broad battery exemption.
- **Risks:** duplicate/lost event, worker suppression, corrupt DB, retry storm, permanent PII retention, unsupported server idempotency.
- **Rollback conditions:** stop delivery safely without deleting queued records; disable event capture if durability/idempotency is unproven; DB rollback only via approved migration/release plan.

## M8 Multi-device stabilization

- **Status:** accepted 2026-07-24 for available Pixel 7 API 36, Generic low-end API 36.1 and Xiaomi/HyperOS API 35 matrix; Realme/reference/API 29 remain explicitly unverified.
- **Scope:** agreed device/OS matrix regression, OEM issue isolation, performance/battery/accessibility/privacy stabilization, upgrade tests and operational diagnostics.
- **Exclusions:** permanent OEM forks without evidence, expansion to unsupported devices/features, release signing/distribution completion.
- **Dependencies:** M7; actual fleet inventory, physical devices, support workflow, acceptance SLOs and prioritized compatibility policy.
- **Acceptance criteria:** available matrix passes automated/UI/background/lock/FIFO sync scenarios with documented API 29/Realme/reference limitations. Met for scoped acceptance.
- **Automated tests:** full CI regression, compatibility guards, performance baselines, log/export PII scans and upgrade tests.
- **Emulator tests:** Pixel 7 14/14; Generic low-end 14/14 plus landscape/130% font, force-idle and reboot. API 29 pending because image is not installed.
- **Physical-device tests:** Xiaomi update/role/permission retention, background, secure lock, notification, FIFO rapid calls and sync pass. Realme/reference pending devices.
- **Permissions:** audit actual manifest/runtime requests against approved register; remove unused permissions.
- **Risks:** fragmented OEM behavior, unavailable models, battery regressions, workaround accumulation, unrepresentative test fleet.
- **Rollback conditions:** narrow documented support matrix or disable affected capability/device path; remove failed workaround and restore last verified common behavior.

## M9 Signed internal 1.0 release

- **Status:** preparation in progress; local configuration/signing templates and fail-closed Gradle gate added 2026-07-24. Managed direct APK distribution, separate DEV/production identities and device-enrollment auth direction accepted 2026-07-25. Auth API/schema/runtime implementation is pending; no signed 1.0 exists.
- **Scope:** signed reproducible internal 1.0, approved distribution/update/rollback, release notes, support/incident/privacy procedures and final acceptance.
- **Exclusions:** public Play release unless separately approved, external customers, SMS/full CRM/default Phone features.
- **Dependencies:** M8 accepted; signing custody, application ID/versioning, distribution/MDM, legal/privacy/security approvals, CRM production readiness, support owners and rollback artifact.
- **Acceptance criteria:** signed artifact verified; production auth/API and compatibility gates pass; monitoring/privacy-safe diagnostics/support runbook ready; owners approve release.
- **Automated tests:** release build/lint/tests, signature/provenance, secret/dependency/vulnerability checks, API compatibility and upgrade regression.
- **Emulator tests:** clean install and upgrade from supported predecessor on API matrix; release configuration smoke.
- **Physical-device tests:** signed build install/upgrade/rollback drill and critical real-call/outbox scenarios on agreed reference/Realme/HyperOS fleet.
- **Permissions:** final approved permission inventory and user rationale; no undeclared/unreviewed permission.
- **Risks:** key loss, production config leak, incompatible CRM, failed update, unsupported device, missing incident/rollback path.
- **Rollback conditions:** halt distribution; revoke/disable server capability where designed; redeploy last signed compatible build through approved channel; preserve/reconcile outbox data per runbook.
