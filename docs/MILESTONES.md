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
- **Permissions:** only platform role/service declarations after review; `READ_CONTACTS` is not added without explicit approval and must be tested as absent first.
- **Risks:** OEM/Phone-app variation, missed deadline, system contacts not delivered, role unavailable, emulator mismatch.
- **Rollback conditions:** keep feature disabled/remove PoC if deadline independence or fail-open cannot be proven; do not merge speculative production path.

## M3 CRM caller lookup

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

- **Scope:** approved minimal fields, Compose presentation, loading/not-found/error/stale states, privacy/accessibility behavior and chosen Android surface.
- **Exclusions:** full CRM editing, default dialer replacement, overlay without approval, outcome flow and unverified lifecycle claims.
- **Dependencies:** M3; product-approved fields/wording; lock-screen/notification policy; UX surface feasibility evidence.
- **Acceptance criteria:** correct authorized card appears in approved context; stale/error states clear; no call interference; accessibility/privacy criteria met.
- **Automated tests:** state reducer/ViewModel, Compose UI/screenshot or semantics tests, redaction and authorization-state tests.
- **Emulator tests:** screen sizes, font scale, dark mode, rotation, locked-state simulation, slow/missing lookup.
- **Physical-device tests:** timing and visibility during real calls on reference/Realme/HyperOS; lock screen, notification/recents, accessibility.
- **Permissions:** no overlay/notification/contacts permission unless separately proposed and approved with rationale.
- **Risks:** Android/OEM UI restrictions, PII exposure, late card, inaccessible UI, misleading match.
- **Rollback conditions:** disable card surface and retain safe internal lookup diagnostics if privacy, correctness or call UX fails; remove replaced UI path after validation.

## M5 Call lifecycle investigation and implementation

- **Scope:** evidence-driven investigation and implementation only for reproducible call observations; source/confidence semantics; missed/rejected/answered/ended/duration scenarios.
- **Exclusions:** assuming exact `ANSWERED`, `ENDED` or duration; call recording; unsupported outgoing/SMS features.
- **Dependencies:** M2/M4; official API review; physical test matrix; product decision on acceptable confidence; permission approvals if any.
- **Acceptance criteria:** each emitted event maps to recorded evidence across supported matrix or is explicitly device-scoped/confidence-qualified; unsupported facts are not emitted.
- **Automated tests:** observation state machine, ordering/deduplication, timestamp semantics, ambiguous/missing callback and process-death tests.
- **Emulator tests:** reproducible supported scenarios, rapid calls, missed/rejected, process states; emulator limitations recorded.
- **Physical-device tests:** full evidence protocol on reference/Realme/HyperOS, contacts, lock states, caller hang-up, answer/end, Bluetooth/dual-SIM when applicable.
- **Permissions:** no phone/call-log/contact permission without explicit approval; test approved and denied branches separately.
- **Risks:** callbacks inaccurate/absent, OEM divergence, false duration, duplicate/out-of-order events, regulatory implications.
- **Rollback conditions:** remove/disable any event type that cannot meet evidence threshold; keep only verified observations and update product/API docs.

## M6 Call outcome

- **Scope:** approved outcome taxonomy, UI association with supported call reference, validation, local save/edit policy and accessible error states.
- **Exclusions:** arbitrary CRM editing, unapproved free-text notes, automatic outcome inference, reliance on unsupported lifecycle event.
- **Dependencies:** M5; business-owned outcome dictionary, edit/time-window rules, CRM association and privacy decisions.
- **Acceptance criteria:** user can select and locally persist valid outcome for correct call context; duplicate/missing context handled; wording and authorization approved.
- **Automated tests:** taxonomy/validation, state restoration, association/deduplication, optional-note redaction and UI tests.
- **Emulator tests:** interruption, rotation/process death, multiple calls, offline entry, accessibility/input behavior.
- **Physical-device tests:** real-call follow-up flow on reference/Realme/HyperOS, notification/task switching and user acceptance.
- **Permissions:** no new permission expected; any notification/surface permission requires approval.
- **Risks:** wrong call association, sensitive notes, confusing mandatory flow, taxonomy drift.
- **Rollback conditions:** disable submission UI and retain no incomplete outcome if taxonomy/association/privacy is unsafe; migrate/delete drafts only by approved policy.

## M7 Offline queue and reliability

- **Scope:** durable outbox, stable UUID, atomic enqueue, WorkManager-style delivery after dependency approval, retry/backoff, server idempotency, permanent-failure visibility/remediation.
- **Exclusions:** silent infinite retries, best-effort memory queue, hidden data loss, unapproved DB migration or API change.
- **Dependencies:** M3 API write contract and M6 outcomes; server idempotency proof; approved DB schema/dependencies/migration; retry/retention policy.
- **Acceptance criteria:** events survive offline, process death and reboot; retries preserve UUID; duplicates cause one server side effect; permanent failures visible and actionable.
- **Automated tests:** state transitions, transactions, leases/concurrency, crash windows, retry classification/backoff/jitter, auth, duplicate/conflict, cleanup and approved migrations.
- **Emulator tests:** airplane mode, network constraints, reboot/process kill, clock change, storage pressure and long offline interval.
- **Physical-device tests:** background/idle/reboot/network switching on reference/Realme/HyperOS; OEM battery restrictions and remediation evidence.
- **Permissions:** network and boot/background declarations only as approved/required; no broad battery exemption without explicit approval.
- **Risks:** duplicate/lost event, worker suppression, corrupt DB, retry storm, permanent PII retention, unsupported server idempotency.
- **Rollback conditions:** stop delivery safely without deleting queued records; disable event capture if durability/idempotency is unproven; DB rollback only via approved migration/release plan.

## M8 Multi-device stabilization

- **Scope:** agreed device/OS matrix regression, OEM issue isolation, performance/battery/accessibility/privacy stabilization, upgrade tests and operational diagnostics.
- **Exclusions:** permanent OEM forks without evidence, expansion to unsupported devices/features, release signing/distribution completion.
- **Dependencies:** M7; actual fleet inventory, physical devices, support workflow, acceptance SLOs and prioritized compatibility policy.
- **Acceptance criteria:** agreed matrix passes release-critical scenarios or has explicitly accepted documented limitations; no PII diagnostics; stable deadline/outbox metrics.
- **Automated tests:** full CI regression, compatibility guards, performance baselines, log/export PII scans and upgrade tests.
- **Emulator tests:** supported API matrix regression, form factors/configuration changes, stress/failure injection.
- **Physical-device tests:** complete reference/Realme UI/HyperOS matrix including real calls, idle/reboot, lock screen, contacts, network changes, upgrade and long-run tests.
- **Permissions:** audit actual manifest/runtime requests against approved register; remove unused permissions.
- **Risks:** fragmented OEM behavior, unavailable models, battery regressions, workaround accumulation, unrepresentative test fleet.
- **Rollback conditions:** narrow documented support matrix or disable affected capability/device path; remove failed workaround and restore last verified common behavior.

## M9 Signed internal 1.0 release

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
