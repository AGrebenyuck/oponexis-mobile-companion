# Security

Статус: **DRAFT threat baseline; M9 production controls pending**

## Цели

- не блокировать входящий звонок из-за отказа приложения или CRM;
- защищать credentials, номера, CRM-идентификаторы и карточки клиентов;
- не допускать подмены/дублирования событий;
- ограничить доступ минимальными permissions и данными;
- обеспечить расследуемость без попадания PII в логи.

## Границы доверия и угрозы

Границы: Android OS и системная телефония, приложение/локальное хранилище, сеть, identity provider, OPONX CRM, диагностический экспорт и пользователь устройства.

Основные угрозы: потерянное/скомпрометированное устройство, вредоносное приложение, перехват сети, утечка токена/PII через лог или backup, replay/duplicate event, tampered payload, чрезмерная карточка на lock screen, enumeration номеров, неправильный tenant/user scope и OEM-background failure.

Threat model должен быть уточнён до M3; penetration/security review — до M9.

## Authentication и authorization

Для локального M3 PoC используется отдельный отзывной Bearer-токен из CRM `.env` и Android `local.properties`; оба файла игнорируются Git. Токен встраивается только в debug APK, поэтому считается извлекаемым и не является production identity. Release build получает пустой токен и fail-closed состояние `NotConfigured`. MFA/SSO, employee identity, secure runtime provisioning/storage, token TTL/refresh, device enrollment, tenant/record authorization, logout и remote revoke остаются production-блокерами. Credentials не попадают в логи, exports, analytics или crash reports.

Для M9 принят уникальный device-enrollment credential: одноразовый CRM activation code обменивается на credential конкретного корпоративного телефона. [ADR 0004](DECISIONS/0004-production-auth.md) принят как направление, но API/schema/TTL/revoke contract и реализация ещё требуют отдельного approval. Заполнение `crm.authMode` не заменяет реализацию: `verifyM9ReleaseInputs` блокирует production release, пока flow не реализован и не проверен. Общий статический token/API key в release APK запрещён.

## Release signing и secrets

- Production keystore хранится вне репозитория; реальные `signing.properties`, `release.properties`, keystore, APK/AAB игнорируются и проверяются Git safety script.
- Владелец ключа, backup, CI access, rotation/compromise recovery и Play App Signing определяются до генерации/использования ключа.
- `release.properties` содержит только утверждённую environment metadata; секретные client/device credentials provisioned at runtime через выбранный auth flow.
- M9 release выполняется через fail-closed `prepareM9Release`; обычный unsigned release artifact не является distributable 1.0.
- Firebase Admin private key хранится только в Vercel Sensitive Environment Variables и никогда не помещается в APK, Git или диагностический экспорт. Android `google-services.json` содержит только Firebase project/app identifiers и остаётся локальной build-конфигурацией.

## Транспорт и API

- HTTPS и стандартная проверка сертификата обязательны для production; локальный debug PoC может явно использовать cleartext для development host, release — нет. Pinning не вводится без отдельного анализа operational risk.
- Timeouts bounded; сеть никогда не задерживает ответ `CallScreeningService`.
- Каждая запись использует stable UUID/idempotency key.
- Server-side idempotency, authorization и validation обязательны; мобильный клиент не является trust boundary.
- Rate limiting и защита от enumeration нужны для lookup; конкретные параметры открыты.

## Локальные данные

- Хранить только необходимое, с утверждённым retention.
- Credentials — в Android-supported secure storage; конкретный механизм выбирается после анализа API/identity.
- Нужна оценка database/file encryption, backup exclusion, screenshots/recents, lock-screen redaction и rooted-device policy.
- Logout/disable должен очищать данные по утверждённой политике, не уничтожая юридически обязательные записи без решения владельца.
- M4 caller card process-local: persistent DB/DataStore/cache не используются; номер существует только как transient lookup input, а `customerRef`/`displayName` не логируются.
- M5 post-call diagnostics process-local: call handle игнорируется; разрешены только disconnect category, duration bucket и aggregate count. Экспортированная Activity нужна для platform delivery, но action может быть имитирован другим приложением, поэтому PoC нельзя использовать как audit/CRM evidence без provenance controls и threat review.
- M7 сохраняет номер/optional CRM имя в retention-controlled history (30 дней default или forever). Outbox PII очищается после confirmed delivery; unresolved/permanent failures сохраняются для предотвращения скрытой потери. Notification public visibility остаётся явным product decision.
- SMS activity хранит на рабочем телефоне provider event ID, статус, источник, номер и ограниченный failure detail. FCM payload содержит только event ID/status/source/time; FID и Firebase credentials не логируются.

## Permissions

Permission запрашивается только при доказанной необходимости, с явным пользовательским одобрением изменения репозитория и понятным runtime rationale. `READ_CONTACTS` одобрен пользователем 2026-07-23 после physical baseline: purpose ограничен доставкой Android screening callbacks для сохранённых номеров, denied/revoked path остаётся fail-open. Нельзя добавлять `WRITE_CONTACTS`, `READ_CALL_LOG`, `READ_PHONE_STATE`, overlay, default dialer role или иные привилегии как подразумеваемые решения.

`INTERNET` одобрен 2026-07-23 для M3 lookup. Это install-time permission без runtime prompt; он не означает одобрение других network/phone/contact capabilities.

`RECEIVE_BOOT_COMPLETED` и `WAKE_LOCK` одобрены 2026-07-24 для M7 WorkManager recovery и короткой durable delivery. Foreground service и battery-optimization exemption не добавлены.

## Structured logging

Разрешённые поля: allowlisted event name, timestamp, severity, app/build version, OS/API, coarse device model, correlation ID, outbox state, attempt number, duration bucket и safe error category. M6 log фиксирует только факт draft creation/failure, не его номер, имя или outcome payload.

Запрещены: полный/частичный номер, имя, организация, CRM payload/ID без отдельного обоснования, note, contacts, token, auth header, cookie, URL query с данными, stack-local values с PII. Для корреляции номера возможен только краткоживущий keyed digest после security/privacy approval; ключ не экспортируется и ротируется.

Ошибки сервера маппятся в allowlisted категории. Не логировать сырой response body.

## Диагностический экспорт

- Только явное действие пользователя или управляемый admin flow, который ещё не определён.
- Перед созданием показывать состав, период и предупреждение; дать отмену.
- Экспорт содержит sanitized structured logs, build/device context и outbox counts/states без payload.
- Локальный файл имеет ограниченный TTL, безопасный share flow и удаляется по политике.
- Автоматическая отправка запрещена; destination, encryption и support workflow требуют решения.

## Incident readiness

Нужны владельцы incident response, credential revoke, feature kill switch, minimum supported version и CRM-side idempotency/audit. Эти вопросы открыты. Permanent failures должны быть видимы без раскрытия payload.

## Security gates

- M1: dependency/permission review.
- M3: auth/API threat review.
- M5/M6: post-call intent provenance и безопасная call/outcome association до любой CRM emission.
- M7: outbox replay, tamper и failure tests.
- M8: device-policy and lock-screen checks.
- M9: release signing, secret scan, dependency audit и security acceptance.
