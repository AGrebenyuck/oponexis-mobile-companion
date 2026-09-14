# M2 CallScreening Proof of Concept

Статус: **Accepted 2026-07-23 для текущего scope: Emulator и Xiaomi/HyperOS; Realme UI/reference physical Android deferred до появления устройств**

## Scope и exclusions

Реализованы изолированный Android `CallScreeningService`, запрос системной роли, немедленный fail-open ответ и privacy-safe process-local метрики. CRM, сеть, caller card, call blocking/rejection/silencing, durable storage и lifecycle events `ANSWERED`/`ENDED`/duration не реализованы.

## Platform contract

По официальной Android API documentation входящий callback должен вызвать `respondToCall` в течение 5 секунд; затем framework отвязывает service и игнорирует ответ. До ответа нельзя выполнять CRM/network/disk work. M2 engineering target — 100 ms.

Без `READ_CONTACTS` вызовы от номеров в системных контактах могут не передаваться service. Baseline без permission подтверждён на Xiaomi/HyperOS; пользователь явно одобрил `READ_CONTACTS` 2026-07-23 только для доставки таких callbacks. `android.permission.BIND_SCREENING_SERVICE` защищает системное связывание объявленного service и не является запрашиваемым runtime permission.

## Реализация

- `OponexisCallScreeningService` явно разрешает входящие вызовы и не блокирует, не отклоняет и не заглушает их.
- Timing начинается на входе в callback; счётчик обновляется только после возврата `respondToCall`.
- Critical path не обращается к Hilt, Room, DataStore, Retrofit, OkHttp, WorkManager или CRM.
- Structured log содержит только event, direction, response time и budget result. Номер, handle, имя, contact state и payload не принимаются diagnostics API.
- Diagnostics показывает role state, callback counts и timing; данные исчезают вместе с процессом и не экспортируются.

## Acceptance evidence

- **Automated tests:** counters, incoming/outgoing separation, maximum timing и visibility превышения 100 ms проходят.
- **Lint/build:** `lintDebug`, `testDebugUnitTest`, `assembleDebug` проходят.
- **Merged manifest:** service declaration присутствует; до approval `<uses-permission>` отсутствовали. В build `0.2.1-m2` допускается ровно один runtime permission: `READ_CONTACTS`.
- **Emulator:** Pixel_7, API 36, `com.oponexis.companion` назначен `ROLE_CALL_SCREENING`; `adb emu gsm call` вызвал service. Telecom evidence: screening bound and completed as allow/logged/notified. Первый записанный response — 13.212 ms; финальный callback собранного APK — 0.667 ms.
- **Role UX:** system-consent dialog, выбор Oponexis и активное состояние в Diagnostics вручную подтверждены на Xiaomi/HyperOS.
- **Physical Xiaomi/HyperOS:** Xiaomi 2505DRP06E, HyperOS 2.0, Android 15/API 35, Google Phone. Несохранённый входящий вызов в разблокированном состоянии получил fail-open response за 5.001 ms. Для сохранённого контакта без `READ_CONTACTS` Telecom записал `contact exists`, service callback отсутствовал. В build `0.2.1-m2` denied и revoked saved-contact branches повторили безопасный bypass; при granted permission тот же saved contact был передан service, response занял 0.585 ms. При приложении в фоне и выключенном экране response занял 0.757 ms. С активным PIN-keyguard до и после звонка response занял 0.280 ms. После reboot роль сохранилась; до звонка процесса приложения не было, Telecom самостоятельно создал процесс и получил response за 0.532 ms. После `adb install -r` роль также сохранилась; без ручного запуска приложения или повторного назначения роли следующий звонок создал процесс и получил response за 0.242 ms. Во всех полученных callbacks решение было allow/logged/notified.

## Permissions

Пользователь явно одобрил `READ_CONTACTS` 2026-07-23 после successful saved-contact baseline без permission. Purpose ограничен тем, чтобы Android передавал сохранённых звонящих CallScreeningService. M2 не обращается к Contact Provider, не сохраняет и не синхронизирует адресную книгу. `WRITE_CONTACTS`, phone state, call log, overlay, notification и default-dialer permissions/roles не добавлены. Call Screening role выбирается пользователем отдельно.

## Remaining tests and risks

- Cold process после reboot доказан на одном Xiaomi/HyperOS; отдельные process-death, CPU-pressure и local-error timing сценарии ещё не проверены.
- После `adb install -r` role holder всё ещё отображался системной командой, но simulated callbacks возобновились только после remove/add role. Нужно определить, является ли это emulator/ADB artifact или проблемой update/role retention.
- Saved/unsaved behavior без `READ_CONTACTS` доказано на одном Xiaomi/HyperOS; private numbers, Realme UI и reference Android ещё не проверены.
- Xiaomi подтвердил role retention после reboot и package update, вызов при приложении в фоне/выключенном экране и active PIN-keyguard. Остальные physical targets ещё не проверены.
- Saved-contact ветки granted, denied и revoked `READ_CONTACTS` подтверждены на Xiaomi/HyperOS; их ещё нельзя экстраполировать на остальные physical targets.
- Emulator не доказывает OEM Phone-app behavior.
- `ANSWERED`, `ENDED` и duration остаются неподтверждёнными и не реализованы.

Realme UI и reference physical Android отложены пользователем 2026-07-23 до появления устройств. Этот acceptance не заявляет их совместимость; полный multi-device gate остаётся обязательным в M8. Private-number, CPU-pressure и отдельный crash/process-death сценарии также остаются recorded gaps и не превращаются в подтверждённое поведение.

## Rollback conditions

Если любой supported physical target не обеспечивает независимый fail-open ответ в agreed budget, PoC остаётся выключенным или удаляется; нельзя компенсировать дефект сетевым ожиданием, блокировкой звонка или недоказанным OEM-specific pipeline.
