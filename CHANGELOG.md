# Changelog

## Unreleased

- Added Firebase Installation ID-based push updates for manual and automatic SMS events, a compact local SMS activity panel, and a one-time four-minute stalled-message alert without heartbeat polling.
- Replaced the legacy five-second SMS delivery polling worker with event-driven FCM updates plus one protected server refresh at the four-minute deadline.
- Added one-tap `Skip all` for a queue of unresolved calls.
- Grouped consecutive calls from the same phone number in call history while keeping separated calls as distinct entries.
- Added an authenticated live SMS Gateway preflight to the SMS composer; sending stays disabled until the Gateway confirms readiness.
- Gateway readiness now requires a recently active configured device, exposes the work SIM phone number, and reports an offline Gateway or Cloud Server instead of a false ready state.
- SMS delivery status now continues refreshing in the background, updates call history from `QUEUED` to the final Gateway state, and posts a success or failure notification.
- Added a persistent, bounded local diagnostic journal for Gateway, SMS, caller lookup, and call-event sync failures without secrets or customer payloads.
- Saved SMS Gateway delivery state on each call card: queued, sent, delivered, failed, or cancelled. The Companion refreshes it after sending and when opening a call from history.
- Added a complete Polish/English/Ukrainian interface with Polish as the saved default.
- Added instant local Companion SMS templates with on-device creation and editing, known-customer previews and form-link placeholders. Telegram and other server workflows continue to use CRM-backed templates.
- Added truthful SMS delivery notifications and visible delivery state in call history.
- Removed reservation confirmation from incoming `TAK/YES` replies; submitted forms now complete the flow.
- Added synchronized call outcome analytics support for the CRM Performance dashboard.

## 0.8.1-m8-sms-poc — 2026-07-25

- Добавлен dev-only CRM-mediated quick action `Send form SMS` в post-call уведомление и карточку Companion.
- SMS отправляет CRM через отдельный SMS Gateway; новое Android-разрешение `SEND_SMS` не добавлялось.
- Call reference используется как стабильный idempotency key, а неуспешная отправка остаётся доступной для повтора.
- Добавлены защищённый mobile API-контракт, unit/UI tests и ADR 0006; production auth/deployment по-прежнему не готовы.

Значимые изменения проекта документируются в этом файле. Формат основан на Keep a Changelog; до первого релиза используется секция `Unreleased`.

## [Unreleased]

### Changed

- Calls: запись истории теперь открывает нижний лист быстрых действий: звонок, отправка SMS и выбор доступного исхода.
- SMS composer показывает редактируемый шаблон с безопасными placeholders для даты, времени и ссылки на форму; новое Android-разрешение не добавлялось.

### M9 release preparation

- Добавлены игнорируемые `release.properties`/`signing.properties` с безопасными tracked examples и точными местами для production CRM, auth mode, distribution, owners и signing inputs.
- Release variant больше не получает M3 debug Bearer token; временные/ngrok URL запрещены M9 gate.
- Добавлены `verifyM9ReleaseInputs` и `prepareM9Release`; gate fail-closed до реализации выбранной production auth, корректного keystore и обязательных release inputs.
- Добавлены M9 runbook и Proposed ADR 0004; production auth, signing custody и support/privacy/security decisions пока не приняты.
- Git safety check теперь также запрещает tracked `release.properties` и `signing.properties`.
- Принят managed-direct APK channel: debug отделён как `com.oponexis.companion.dev`/`Oponexis Companion DEV`, production сохраняет стабильный `com.oponexis.companion` для подписанных ручных обновлений.
- Добавлен ADR 0005 и DEV/ngrok → production release runbook; автоматический self-update явно оставлен за пределами текущего scope.
- Принято направление production auth: одноразовый CRM activation code обменивается на уникальный отзывной credential устройства; реализация отложена до общего CRM/site/Android workspace и отдельного API/schema approval.

### Added

- Документационный baseline для milestone M0.
- Product, architecture, draft API, security, privacy, testing, compatibility и milestone specifications.
- ADR для native Android, CallScreening-first и offline outbox.
- Реестр открытых вопросов для OPONX CRM и целевых устройств.
- Android application skeleton для milestone M1 на Kotlin и Jetpack Compose.
- Material 3 light/dark design system, Oponexis branding и adaptive launcher icon.
- Splash, onboarding, dashboard, calls, diagnostics и settings screens с нижней навигацией.
- Hilt foundation, mock repositories, DataStore preferences, Room/Retrofit/WorkManager integration points и test harness.
- M2 CallScreening proof of concept с системной role/setup UX и fail-open `CallScreeningService`.
- Privacy-safe process-local counters и timing для screening callbacks без номера телефона или contact/CRM данных.
- JVM-тесты метрик response budget и emulator evidence для simulated incoming call.
- Одобрено и добавлено разрешение `INTERNET` для M3 CRM caller lookup.
- Реализованы Retrofit caller-lookup gateway, CRM-compatible phone normalization, bounded timeouts и typed safe error mapping без новых зависимостей.
- В OPONX CRM добавлен защищённый `POST /api/mobile/v1/caller-lookup` с минимальным read-only ответом и без PII-логов.
- Добавлены JVM normalization tests и Android contract/mapping instrumentation tests.
- Реализован M4 process-local caller-card state, защищённый от stale lookup result и скрытых/отсутствующих номеров.
- Во вкладке Calls добавлена in-app карточка со state UI: ready, loading, matched, not found, number unavailable, unauthorized и safe error.
- `CallScreeningService` запускает lookup только после `respondToCall`; номер не сохраняется и не логируется.
- Добавлены unit tests M4 state/coordinator и Compose UI tests approved-field/redaction states.
- После явного approval добавлен обычный `ACCESS_NETWORK_STATE`; M4 ждёт восстановления приостановленной LTE-сети и делает один автоматический retry lookup.
- Добавлен permission-free M5 post-call PoC для API 30+: process-local категории disconnect/duration bucket без чтения или логирования call handle.
- В Diagnostics добавлены счётчик и последняя категориальная post-call информация с явным предупреждением об отсутствии точных lifecycle timestamps/duration.
- Добавлены JVM-тесты mapping/unknown/repeated post-call observations и отдельный M5 evidence report.
- Добавлен M6 optional outcome flow: `Interested`, `Follow-up required`, `Not interested`, `Wrong number`, `Other` и Skip.
- Добавлены Room outcome drafts, последовательные migrations `1→2→3`/`2→3`, FIFO pending UI и очистка временных identity fields после resolve.
- После явного approval добавлен `POST_NOTIFICATIONS`, public lock-screen reminder, полный номер в notification/Calls и optional имя только после lookup того же номера.
- Добавлены JVM repository tests, Room migration/DAO instrumentation и Compose outcome-card tests.
- Добавлен M7 durable outbox в Room v4: stable UUID, atomic outcome+enqueue, lease, bounded retries и permanent-failure visibility.
- Добавлена WorkManager-доставка с network constraint, exponential backoff и startup/reboot recovery; явно одобрены `RECEIVE_BOOT_COMPLETED` и `WAKE_LOCK`.
- В OPONX CRM добавлен DRAFT `POST /api/mobile/v1/call-events` и `MobileCallEvent` с server-side idempotency/payload-conflict protection.
- Home, Calls и Diagnostics переведены с mock repository на реальные Room/outbox данные; legacy mock implementation удалена после проверки.
- В Settings добавлен выбор хранения истории: 30 дней по умолчанию или бессрочно; недоставленные и permanent-failure события не очищаются сроком.

### Changed

- Зафиксированы совместимые M1 build/dependency versions для API 29–36 и JDK 17.
- В интерфейсе и launcher icon применён предоставленный фирменный логотип Oponexis.
- В Diagnostics добавлены состояние Call Screening role и результаты M2 PoC; версия build обновлена до `0.2.0-m2`.
- Зафиксирован physical M2 evidence на Xiaomi/HyperOS: real unsaved call, saved-contact bypass без `READ_CONTACTS`, background/screen-off, cold process после reboot и role/callback retention после package update.
- После явного approval добавлен optional `READ_CONTACTS` с runtime rationale для сохранённых звонящих; `WRITE_CONTACTS` и прямое чтение адресной книги не добавлены.
- На Xiaomi/HyperOS подтверждены denied, granted и revoked saved-contact branches: bypass без доступа и fail-open callback после grant.
- M2 принят для текущего Emulator + Xiaomi/HyperOS scope; Realme UI и reference physical Android явно отложены до multi-device validation.
- Версия приложения обновлена до `0.3.0-m3`; release-конфигурация не содержит CRM URL или PoC-токен.
- M3 принят для internal/ngrok scope; версия приложения обновлена до `0.4.0-m4`.
- На Xiaomi/HyperOS подтверждена приостановка LTE-data на время голосового вызова; версия обновлена до `0.4.1-m4`, а UI-бренд исправлен на `OPONX CRM`.
- M4 принят для текущего Xiaomi/HyperOS scope после реального Wi-Fi match и успешного LTE recovery retry; Realme/reference fleet остаётся M8 scope.
- Версия приложения обновлена до `0.5.0-m5-poc`; M5 принят для API 30+ Emulator + текущего Xiaomi/HyperOS scope без новых permissions.
- На Xiaomi/HyperOS четыре реальных сценария воспроизвели категории `remote`, `local`, `missed`, `rejected`; Pixel_7 API 36 Emulator подтвердил доставку `remote`. Эти данные не объявляются точными `ANSWERED`, `ENDED` или duration.
- Версия обновлена до `0.6.1-m6-poc`; M6 принят для текущего API 30+ Emulator + Xiaomi/HyperOS scope.
- На Xiaomi подтверждены in-place Room migrations, real-call notification/deep-link, outcome/Skip и одинаковый полный номер на secure lock screen и в pending-карточке.
- Версия обновлена до `0.7.0-m7-poc`; CRM production build, schema validation/push и duplicate replay proof пройдены.
- M7 принят для текущего Xiaomi/HyperOS scope: real-call offline pending, online sync и queued reboot recovery подтверждены.
- Начат M8 для Pixel 7 API 36, Generic low-end API 36.1 (1 vCPU/2 GB) и Xiaomi/HyperOS API 35; версия обновлена до `0.8.0-m8-poc`.
- Исправлена недоступная нижняя часть onboarding при landscape/130% font scale: экран теперь прокручивается.
- Compose outcome test harness сделан configuration-safe через реальный scroll-container; large-text landscape suite снова проходит 14/14.
- M8 принят для доступной матрицы: Pixel 7 API 36, Generic low-end API 36.1 и Xiaomi/HyperOS API 35.
- На Xiaomi подтверждены M7→M8 update/role/permission retention, background outcome sync, secure-lock notification sync и FIFO двух быстрых звонков с итоговым `Pending = 0`.
