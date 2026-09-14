# Test Strategy

Статус: **DRAFT; M2–M8 available-matrix evidence recorded, remaining Realme/reference/API 29 pending**

## Принципы

- Тест доказывает только наблюдаемое поведение в записанной конфигурации.
- Официальная Android-документация задаёт ожидаемый контракт; PoC подтверждает применимость на выбранных API/OEM.
- Emulator и physical devices — отдельные слои, ни один не заменяет другой.
- M5 подтвердил только post-call disconnect category и coarse duration bucket на текущих API 30+ targets. Точные `ANSWERED`, `ENDED` timestamps и duration по-прежнему не считаются доступными.
- CRM/network failure никогда не должен задерживать или блокировать звонок.

## Слои

### Automated host/JVM tests

Текущий JVM baseline проверяет Room outcome→outbox enqueue, navigation, normalization, caller state и privacy-safe screening/lifecycle metrics. Legacy mock repository удалён после перехода UI на Room.

### Android instrumentation/component tests

Room transactions/recovery, WorkManager constraints, DataStore, service lifecycle adapters, permission branches, process death и UI accessibility. Реальные звонки и OEM behavior этими тестами полностью не доказываются.

M7 baseline включает Room v4/migrations, atomic outcome+outbox, Compose outcomes/Skip и notification routing. На Xiaomi 3 targeted Room DAO/migration tests прошли; полный runner завершил 10 tests + 1 skip без failures, затем HyperOS/UTP transport завис до последних UI cases, поэтому это не объявляется полным pass.

### Contract/integration tests

CRM sandbox/stub, auth expiry, idempotent duplicate, UUID conflict, schema mismatch, 429/Retry-After, 5xx, timeout, offline/online, ambiguous lookup и authorization filtering. Нужны test environment и test data от CRM.

### Android Emulator

Отдельная матрица выбранных API levels: role/service setup, входящий эмулированный звонок где возможно, deadline instrumentation, cached/uncached lookup, permission denial, process death, airplane mode, reconnect, clock change, UI/accessibility. Ограничения emulator telephony фиксируются как ограничения, а не экстраполируются на OEM.

### Physical devices

Минимум: согласованные Realme/Realme UI, Xiaomi/HyperOS и reference/standard Android устройства. Проверяются реальные входящие звонки, системные контакты без/с одобренным `READ_CONTACTS`, lock screen, background/idle/reboot, battery restrictions, autostart, dual-SIM/eSIM если применимо, network switching, role retention, notifications, upgrade и uninstall/reinstall.

Realme UI и HyperOS — compatibility targets, не специальные архитектурные implementations.

## CallScreening deadline test

Инструментировать monotonic время от service entry до возврата `respondToCall`, не записывая номер. Платформенный deadline — 5 секунд, внутренний M2 target — 100 ms. Тестировать cold/warm process, CPU pressure и локальные ошибки. M2 critical path конструктивно не содержит сети или disk I/O; будущая сеть должна искусственно задерживаться/отключаться без влияния на fail-open ответ.

## Call lifecycle evidence protocol

Для каждой комбинации API/device/role/permission/lock state записать: входящий/исходящий сценарий, фактические callbacks и monotonic timestamps, эталонное ручное действие, process state и результат. Отдельно исследовать rejected, missed, answered then ended, caller hangs up, second call, Bluetooth и dual-SIM при наличии. Только воспроизводимые сигналы получают продуктовые event names; приблизительные сигналы маркируются confidence/source.

M5 baseline: API 30+ `ACTION_POST_CALL` тестируется отдельно от `CallScreeningService`; в M5 handle не читался, проверялись только disconnect category и duration bucket. M6 после отдельного approval добавил bounded `tel:` handle для temporary pending identity. Pixel_7 API 36 покрывает simulated answered/remote end. Xiaomi/HyperOS API 35 покрывает remote end, local end, missed и rejected реальными входящими звонками. Это device-scoped evidence, не точный lifecycle event и не замена оставшейся fleet matrix.

## Outbox reliability

M7 реализует atomic enqueue, stable UUID, lease, network constraint, exponential WorkManager backoff, startup/reboot recovery, максимум 10 attempts и permanent visibility. CRM integration 2026-07-24 подтвердила create и identical duplicate replay с одним receipt. Xiaomi real-call test подтвердил offline pending, online sync и queued reboot recovery. Emulator M7 regression и остальные OEM остаются M8 scope.

## Security/privacy tests

Static secret scan, dependency review, TLS failure, auth revoke, cross-user/tenant denial, logs/exports grep на seeded PII/secrets, screenshot/recents/notification review, backup/logout behavior и malicious deep-link/input tests согласно реализованной поверхности.

## Evidence и defect policy

Каждый device result содержит model, OS/OEM build, API, app build, permissions/roles, SIM/network setup и steps. Неуспешное OEM-наблюдение не становится общим Android-фактом. Release blockers и численные quality thresholds должны быть определены до M8/M9.
