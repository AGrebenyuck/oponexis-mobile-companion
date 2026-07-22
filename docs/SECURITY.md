# Security

Статус: **DRAFT threat baseline для M0**

## Цели

- не блокировать входящий звонок из-за отказа приложения или CRM;
- защищать credentials, номера, CRM-идентификаторы и карточки клиентов;
- не допускать подмены/дублирования событий;
- ограничить доступ минимальными permissions и данными;
- обеспечить расследуемость без попадания PII в логи.

## Границы доверия и угрозы

Границы: Android OS и системная телефония, приложение/локальное хранилище, сеть, identity provider, OpenX CRM, диагностический экспорт и пользователь устройства.

Основные угрозы: потерянное/скомпрометированное устройство, вредоносное приложение, перехват сети, утечка токена/PII через лог или backup, replay/duplicate event, tampered payload, чрезмерная карточка на lock screen, enumeration номеров, неправильный tenant/user scope и OEM-background failure.

Threat model должен быть уточнён до M3; penetration/security review — до M9.

## Authentication и authorization

Механизм входа, MFA/SSO, token storage/refresh, device enrollment, logout и remote revoke не определены. Клиент не должен считать наличие токена доказательством доступа к конкретной записи: CRM фильтрует результаты по текущему пользователю/tenant. Credentials не попадают в логи, exports, analytics или crash reports.

## Транспорт и API

- HTTPS и стандартная проверка сертификата обязательны; pinning не вводится без отдельного анализа operational risk.
- Timeouts bounded; сеть никогда не задерживает ответ `CallScreeningService`.
- Каждая запись использует stable UUID/idempotency key.
- Server-side idempotency, authorization и validation обязательны; мобильный клиент не является trust boundary.
- Rate limiting и защита от enumeration нужны для lookup; конкретные параметры открыты.

## Локальные данные

- Хранить только необходимое, с утверждённым retention.
- Credentials — в Android-supported secure storage; конкретный механизм выбирается после анализа API/identity.
- Нужна оценка database/file encryption, backup exclusion, screenshots/recents, lock-screen redaction и rooted-device policy.
- Logout/disable должен очищать данные по утверждённой политике, не уничтожая юридически обязательные записи без решения владельца.

## Permissions

Permission запрашивается только при доказанной необходимости, с явным пользовательским одобрением изменения репозитория и понятным runtime rationale. `READ_CONTACTS` approval-required: без него screening может не получать звонки для системных контактов. Нельзя добавлять `READ_CALL_LOG`, `READ_PHONE_STATE`, overlay, default dialer role или иные привилегии как подразумеваемые решения.

## Structured logging

Разрешённые поля: allowlisted event name, timestamp, severity, app/build version, OS/API, coarse device model, correlation ID, outbox state, attempt number, duration bucket и safe error category.

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
- M7: outbox replay, tamper и failure tests.
- M8: device-policy and lock-screen checks.
- M9: release signing, secret scan, dependency audit и security acceptance.
