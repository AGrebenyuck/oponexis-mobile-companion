# M7 Offline Queue and Reliability

Статус: **ACCEPTED FOR CURRENT XIAOMI/HYPEROS SCOPE** (2026-07-24)

## Scope

- Room schema v4 с `event_outbox` и migration `3→4`;
- атомарный выбор outcome + enqueue stable UUID;
- WorkManager unique work, network constraint, lease, exponential backoff и максимум 10 attempts;
- состояния `PENDING`, `IN_FLIGHT`, `RETRY_SCHEDULED`, `DELIVERED`, `PERMANENT_FAILURE`;
- DRAFT OPONX CRM `POST /api/mobile/v1/call-events` с bearer auth, validation, payload hash и idempotent replay;
- реальные Home/Calls/Diagnostics данные вместо mocks;
- history retention: 30 дней default или forever; pending/failure records не очищаются.

## Exclusions

Production SSO/device enrollment, editing/notes, automatic outcome, exact lifecycle timestamps/duration, manual permanent-failure retry UI, battery exemption, foreground service, API 29 post-call fallback и M8 fleet stabilization.

## Evidence

- Android unit tests, Android-test compilation, lint и Debug APK — pass.
- Xiaomi/HyperOS API 35 targeted Room DAO/migrations — 3/3 pass; M7 cold launch and real-data/retention UI smoke — pass.
- CRM Prisma schema validation, database push и production Next build — pass.
- Обезличенный server integration: первый stable UUID accepted/created; идентичный replay вернул `duplicate: true` и тот же receipt без второго side effect.
- Полный Xiaomi instrumentation run завершил 10 tests + 1 expected skip без failures, затем UTP transport завис на оставшихся Compose cases; run остановлен и не считается полным pass.
- Xiaomi real-call acceptance: без сети событие осталось `Pending`; после восстановления сети стало `Synced`; queued событие пережило reboot и синхронизировалось после возврата сети.

## Permissions

`RECEIVE_BOOT_COMPLETED` и `WAKE_LOCK` явно одобрены для M7 WorkManager. Runtime prompt отсутствует. `INTERNET`/`ACCESS_NETWORK_STATE` ранее одобрены. Foreground service, overlay и battery exemption отсутствуют.

## Risks and rollback

Открытые риски: OEM worker suppression, post-call intent spoofing, permanent PII retention, production auth и server policy. При проблеме delivery capture можно остановить, не удаляя queued rows; откат Room требует отдельной forward migration, downgrade/destructive migration запрещены.

## Remaining validation

M7 emulator regression и физические Realme/reference layers остаются отдельными M8 requirements. Production auth/deployment и formal privacy/security acceptance остаются M9 blockers.
