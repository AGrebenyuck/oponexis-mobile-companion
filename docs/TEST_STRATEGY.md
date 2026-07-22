# Test Strategy

Статус: **DRAFT; M2 emulator evidence recorded, physical layers pending**

## Принципы

- Тест доказывает только наблюдаемое поведение в записанной конфигурации.
- Официальная Android-документация задаёт ожидаемый контракт; PoC подтверждает применимость на выбранных API/OEM.
- Emulator и physical devices — отдельные слои, ни один не заменяет другой.
- `ANSWERED`, `ENDED` и duration не считаются доступными/точными до доказательств M2/M5.
- CRM/network failure никогда не должен задерживать или блокировать звонок.

## Слои

### Automated host/JVM tests

Текущий JVM baseline проверяет mock repositories/navigation и privacy-safe screening counters/timing. Phone normalization, API mapping, outbox transitions, retry classification/backoff и idempotency будут добавляться только в соответствующих milestone.

### Android instrumentation/component tests

Room transactions/recovery, WorkManager constraints, DataStore, service lifecycle adapters, permission branches, process death и UI accessibility. Реальные звонки и OEM behavior этими тестами полностью не доказываются.

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

## Outbox reliability

Проверить atomic enqueue, stable UUID, concurrent workers, crash до/после HTTP response, duplicate response, retry after reboot, bounded retry, auth failure, permanent validation failure, visibility/remediation, cleanup и migration после отдельного approval. Серверный тест подтверждает отсутствие повторного side effect.

## Security/privacy tests

Static secret scan, dependency review, TLS failure, auth revoke, cross-user/tenant denial, logs/exports grep на seeded PII/secrets, screenshot/recents/notification review, backup/logout behavior и malicious deep-link/input tests согласно реализованной поверхности.

## Evidence и defect policy

Каждый device result содержит model, OS/OEM build, API, app build, permissions/roles, SIM/network setup и steps. Неуспешное OEM-наблюдение не становится общим Android-фактом. Release blockers и численные quality thresholds должны быть определены до M8/M9.
