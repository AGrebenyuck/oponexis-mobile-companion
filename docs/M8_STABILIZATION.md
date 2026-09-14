# M8 Multi-device Stabilization

Статус: **ACCEPTED FOR AVAILABLE MATRIX** (2026-07-24)

## Agreed current matrix

| Layer | Configuration | Meaning |
|---|---|---|
| Reference emulator | Pixel 7, API 36, Google Play image | Standard Android regression |
| Low-end reference emulator | Generic Medium Phone, API 36.1, 1 vCPU, 2 GB RAM | Resource-constrained standard Android regression |
| Physical OEM | Xiaomi 2505DRP06E, HyperOS 2.0, API 35 | Current real-call/OEM evidence |
| Realme UI | No device/image available | Explicitly unverified |

Android Emulator does not reproduce Realme UI merely by changing a hardware profile. The Generic low-end AVD approximates resource constraints only; it cannot prove Realme background, telephony, permission or notification behavior.

## Scope

- regression of M2–M7 capabilities on the agreed current matrix;
- clean/update launch, role and approved-permission audit;
- Room/outbox, navigation, theme, larger text, orientation, background/doze and reboot checks;
- performance baseline and privacy-safe crash/ANR/log review;
- real-call HyperOS regression in one batched physical protocol.

## Exclusions

- claims about Realme UI without a physical device;
- exact `ANSWERED`, `ENDED` or duration claims;
- new permissions, dependencies, API/DB changes, OEM forks or battery exemptions;
- release signing/distribution and production identity, which remain M9 scope.

## Evidence to date

- Pixel 7 API 36: full instrumentation suite 14/14 pass in 15.643 s.
- Generic low-end API 36.1: full instrumentation suite 14/14 pass in 21.677 s; после M8 fixes landscape/130% suite 14/14 pass in 11.552 s.
- Generic low-end repeated cold starts: 4.352 s, 4.033 s, 3.704 s; no observed ANR, fatal exception or skipped-frame warning in the reviewed app log slice.
- API 29 system image is not installed locally and remains a separate required compatibility check; API 29 also lacks the API 30+ post-call surface by platform design.
- Landscape + 130% font scale выявил clipping onboarding CTA. После добавления scroll экран показывает third feature и `Enter companion`; test harness outcome card также исправлен для scroll-aware конфигурации.
- Generic low-end force-idle достиг `IDLE`, затем unforce/reboot/cold launch прошли без app-specific fatal exception/ANR. Во время экстремальной 1-vCPU landscape нагрузки один раз завис emulator `System UI`; приложение не было источником ANR и восстановилось после `Wait`.
- Xiaomi in-place `0.7.0→0.8.0-m8-poc` update сохранил Room history, Call Screening role и approved permissions; cold launch 1.567 s.
- Xiaomi real-call batch: background call outcome синхронизирован; secure PIN lock-screen notification/outcome синхронизирован; missed + rejected calls создали два FIFO pending drafts, оба последовательно синхронизированы, итоговый `Pending = 0`.
- Final host verification: lint, Debug/Release unit tests and Debug/unsigned Release APK builds pass; 42 executed JVM test instances (Debug + Release), 0 failures.
- Debug/Release permission audit содержит только approved `READ_CONTACTS`, `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`. Git safety и diff whitespace checks pass.
- Временный ngrok/CRM dev server остановлен. Финальная установленная Xiaomi debug-сборка возвращена к `https://invalid.local/`, сохранив history, role и permissions; production endpoint не подразумевается.

## Remaining validation / release blockers

- API 29 emulator image/regression и physical Realme/reference validation when devices become available;
- production CRM identity/deployment, signing/distribution and formal privacy/security acceptance for M9.
