# M2 CallScreening Proof of Concept

Статус: **реализован и частично проверен; milestone не принят до physical-device evidence**

## Scope и exclusions

Реализованы изолированный Android `CallScreeningService`, запрос системной роли, немедленный fail-open ответ и privacy-safe process-local метрики. CRM, сеть, caller card, call blocking/rejection/silencing, durable storage и lifecycle events `ANSWERED`/`ENDED`/duration не реализованы.

## Platform contract

По официальной Android API documentation входящий callback должен вызвать `respondToCall` в течение 5 секунд; затем framework отвязывает service и игнорирует ответ. До ответа нельзя выполнять CRM/network/disk work. M2 engineering target — 100 ms.

Без `READ_CONTACTS` вызовы от номеров в системных контактах могут не передаваться service. Этот permission не запрашивается и требует отдельного пользовательского approval. `android.permission.BIND_SCREENING_SERVICE` защищает системное связывание объявленного service и не является запрашиваемым runtime permission.

## Реализация

- `OponexisCallScreeningService` явно разрешает входящие вызовы и не блокирует, не отклоняет и не заглушает их.
- Timing начинается на входе в callback; счётчик обновляется только после возврата `respondToCall`.
- Critical path не обращается к Hilt, Room, DataStore, Retrofit, OkHttp, WorkManager или CRM.
- Structured log содержит только event, direction, response time и budget result. Номер, handle, имя, contact state и payload не принимаются diagnostics API.
- Diagnostics показывает role state, callback counts и timing; данные исчезают вместе с процессом и не экспортируются.

## Acceptance evidence

- **Automated tests:** counters, incoming/outgoing separation, maximum timing и visibility превышения 100 ms проходят.
- **Lint/build:** `lintDebug`, `testDebugUnitTest`, `assembleDebug` проходят.
- **Merged manifest:** service declaration присутствует; `<uses-permission>` отсутствуют.
- **Emulator:** Pixel_7, API 36, `com.oponexis.companion` назначен `ROLE_CALL_SCREENING`; `adb emu gsm call` вызвал service. Telecom evidence: screening bound and completed as allow/logged/notified. Первый записанный response — 13.212 ms; финальный callback собранного APK — 0.667 ms.
- **Role UX:** активное состояние и timing видимы в Diagnostics. Интерактивный system-consent dialog требует отдельной ручной проверки после снятия роли.
- **Physical devices:** не выполнялись.

## Permissions

Новые `<uses-permission>` отсутствуют. `READ_CONTACTS`, phone state, call log, overlay, notification и default-dialer permissions/roles не добавлены. Пользователь явно выбирает только системную Call Screening role.

## Remaining tests and risks

- Cold process, process death, CPU pressure and local-error timing ещё не доказаны воспроизводимым тестом.
- После `adb install -r` role holder всё ещё отображался системной командой, но simulated callbacks возобновились только после remove/add role. Нужно определить, является ли это emulator/ADB artifact или проблемой update/role retention.
- Saved/unsaved/private number behavior нужно проверить без `READ_CONTACTS` на реальном reference Android, Realme UI и HyperOS.
- Нужны real incoming calls в locked/unlocked states и проверка role retention.
- Emulator не доказывает OEM Phone-app behavior.
- `ANSWERED`, `ENDED` и duration остаются неподтверждёнными и не реализованы.

## Rollback conditions

Если любой supported physical target не обеспечивает независимый fail-open ответ в agreed budget, PoC остаётся выключенным или удаляется; нельзя компенсировать дефект сетевым ожиданием, блокировкой звонка или недоказанным OEM-specific pipeline.
