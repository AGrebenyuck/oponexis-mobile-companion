# OPONX CRM API Contract

Статус: **M3 lookup + M7 event LOCAL PoC VERIFIED / PRODUCTION DRAFT**

Caller lookup реализован 2026-07-23. Outcome event ingestion реализован и локально проверен 2026-07-24. Контракт остаётся DRAFT и не считается совпадающим с будущими production OPONX CRM models; production identity, authorization, rate limits и token lifecycle не утверждены.

## Общие соглашения (предлагаемые)

- Production HTTPS only; локальный debug PoC допускает явно настроенный HTTP development host. JSON UTF-8.
- Versioned caller-lookup path: `/api/mobile/v1` для текущего PoC.
- Локальный PoC использует отдельный Bearer-токен; production auth и lifecycle токенов не выбраны.
- `X-Correlation-Id` для трассировки без PII.
- `Idempotency-Key` со стабильным UUID для каждой операции записи.
- Сервер возвращает машинный error code, retryability и correlation ID; не возвращает secrets в ошибках.
- Время передаётся в UTC ISO 8601; authoritative clock и допустимый skew открыты.

## Caller lookup

Текущий PoC-запрос:

```http
POST /api/mobile/v1/caller-lookup
Authorization: Bearer <local-poc-token>
Content-Type: application/json
X-Correlation-Id: <uuid>

{
  "phoneNumber": "+48123456789",
  "clientRequestId": "uuid"
}
```

Текущий PoC-ответ:

```json
{
  "result": "matched",
  "match": {
    "customerRef": "opaque-id",
    "displayName": "Example"
  },
  "correlationId": "uuid"
}
```

Реализованы `matched | not_found`; `ambiguous` не реализован, поскольку текущий `Customer.phone` уникален. `displayName` nullable. Lookup нормализует номер существующей CRM-функцией: явный `+` сохраняется, 9 цифр получают `+48`, остальные digits получают `+`. Этот алгоритм проверен как текущее поведение, но ещё не утверждён как международная policy. Persistent cache в M3 PoC отсутствует.

Текущий lookup возвращает: `200` для matched/not-found, `400` для invalid JSON/request ID/phone, `401` для неверного Bearer token, `413` для объявленного body больше 4096 bytes, `500 server_error` и `503 mobile_api_not_configured`. Серверный rate limiting/`429` ещё не реализован; Android уже безопасно маппит будущий `429` в `Throttled`.

## Event ingestion

Текущий M7 PoC endpoint:

```http
POST /api/mobile/v1/call-events
Authorization: Bearer <local-poc-token>
Idempotency-Key: <stable-event-uuid>
Content-Type: application/json
X-Correlation-Id: <uuid>

{
  "eventId": "stable-event-uuid",
  "schemaVersion": 1,
  "eventType": "call_outcome",
  "observedAt": "2026-01-01T00:00:00Z",
  "resolvedAt": "2026-01-01T00:01:00Z",
  "source": "android_post_call",
  "confidence": "user_selected",
  "customerRef": "opaque-id-or-null",
  "callRef": "locally-generated-opaque-id",
  "phoneNumber": "+48123456789",
  "attributes": {
    "disconnectCategory": "remote",
    "durationBucket": "short",
    "outcomeCode": "interested"
  }
}
```

M7 отправляет только выбранный сотрудником outcome вместе с device-scoped disconnect category/coarse duration bucket. Это не утверждает точные `answered`, `ended` или duration. `phoneNumber`/`customerRef` nullable; сервер связывает существующего customer по reference, затем по normalized phone, не создавая клиента автоматически.

Предлагаемый успешный ответ:

```json
{
  "result": "accepted",
  "receiptId": "stable-event-uuid",
  "duplicate": false,
  "correlationId": "stable-event-uuid"
}
```

Повтор с тем же idempotency key и семантически тем же payload должен вернуть тот же результат без повторного side effect. Повтор UUID с другим payload должен возвращать явный non-retryable conflict.

## Outcome ingestion

M7 принимает product-approved codes: `interested`, `follow_up_required`, `not_interested`, `wrong_number`, `other`. Skip не создаёт event; notes/editing отсутствуют. Один `callRef` допускает один outcome event в текущем DRAFT schema.

## Ошибки и retries (предложение)

| Класс | Пример | Клиентское действие |
|---|---|---|
| Auth | expired token | один контролируемый refresh; затем user-visible auth state |
| Validation/schema | unsupported code | `PERMANENT_FAILURE` |
| Conflict | UUID reused with different payload | `PERMANENT_FAILURE`, security/diagnostic signal |
| Throttle | 429 + Retry-After | retry не ранее указанного времени |
| Transient server | 5xx | bounded backoff + jitter |
| Network | timeout/offline | retry при подходящей сети |

M7 client: `200/201` delivered; `400/401/403/409/413/422` permanent; `429/5xx/network` retryable; максимум 10 attempts. Production policy и `Retry-After` support ещё требуют согласования. Lookup/delivery failure не блокирует звонок.

## Совместимость и эволюция

- Сервер игнорирует только явно разрешённые неизвестные additive fields.
- Breaking changes требуют новой версии и пользовательского одобрения.
- Клиент отправляет schema version; сервер публикует период поддержки.
- Mobile release compatibility window и kill switch не определены.

## Необходимые подтверждения

Все endpoints, auth, tenant model, phone normalization, fields, authorization, rate limits, environments, idempotency storage window, error taxonomy, retention и audit requirements перечислены в `OPEN_QUESTIONS.md`.
## POST `/api/mobile/v1/sms-actions` — dev/internal

Требует тот же временный Bearer dev-token, что caller lookup и call events.

- `send_booking_form`: стабильный `requestId=callRef`, телефон, дата, время и редактируемый `messageOverride`. CRM всё равно повторно определяет нового/постоянного клиента, создаёт form token и подставляет безопасные placeholders.
- `send_custom_message`: новый UUID на каждую отправку, телефон и редактируемый текст до 1000 символов.
- `GET /api/mobile/v1/sms-actions`: возвращает фактический статус Gateway для `receiptId` (`QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `CANCELLED`).

CRM передаёт сообщение отдельному SMS Gateway; Companion не требует `SEND_SMS`. Входящее `TAK/YES` не подтверждает резервацию: единственным подтверждением является отправка публичной формы. После успешной отправки формы CRM отправляет отдельное SMS-подтверждение.

## GET `/api/mobile/v1/sms-gateway-health` — dev/internal

Защищён тем же mobile Bearer-token. Сервер проверяет SMS Gateway и настроенное устройство с timeout 4,5 секунды. `ready` возвращается только если `lastSeen` устройства не старше 20 минут; ответ содержит профиль, использование device ID, номер SIM, номер рабочей SIM, имя устройства, `deviceLastSeen`, время проверки и correlation ID. Логины, пароли, Gateway URL и токены клиенту не передаются.

Android блокирует кнопку отправки до полноценного ответа `status=ready` с `deviceLastSeen` и позволяет повторить проверку. Ошибки включают `sms_gateway_device_offline`, `sms_gateway_device_not_found`, `sms_gateway_device_status_unknown`, `sms_gateway_not_configured`, `sms_gateway_timeout`, `sms_gateway_unavailable`, `mobile_api_not_configured` или `unauthorized` без секретов и provider payload.

## Companion SMS templates — local

Companion хранит системные и пользовательские шаблоны локально на рабочем телефоне и не обращается к этому API при открытии, создании или изменении шаблона. Telegram и остальные серверные сценарии продолжают получать шаблоны из CRM; их поведение не изменено. Старые mobile endpoints сохраняются для обратной совместимости, но текущий Companion их не использует.

## Firebase SMS activity — internal

- `POST /api/mobile/v1/push-registration` принимает Firebase Installation ID и точный Android application ID. Контракт защищён текущим mobile Bearer credential; FID не логируется и хранится с `lastSeenAt`.
- `DELETE /api/mobile/v1/push-registration` отключает FID при явной отмене регистрации.
- `GET /api/mobile/v1/sms-activity` возвращает до 100 последних исходящих SMS-событий без текста сообщения: event ID, provider message ID, статус, источник, телефон, failure detail и время.
- FCM data payload не содержит номера телефона, имени или текста SMS: только event ID, статус, источник и время. Companion получает подробности через защищённый API.
- Сервер публикует `QUEUED`, `SENT`, `DELIVERED`, `FAILED` и `CANCELLED` для Companion, формуляжей, напоминаний, подтверждений и кампаний.
- После `QUEUED` Android создаёт один локальный WorkManager deadline на четыре минуты. В deadline выполняется одна серверная синхронизация; если финального статуса нет, Companion показывает локальное уведомление.
