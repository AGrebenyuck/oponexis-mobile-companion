# OpenX CRM API Contract

Статус: **DRAFT / PROPOSAL — НЕ ПОДТВЕРЖДЁН ТЕКУЩЕЙ OPENX CRM**

Этот документ описывает потребности мобильного клиента и не утверждает существование endpoints, полей или моделей. Любое изменение/утверждение API требует согласования с владельцем OpenX CRM и пользователем.

## Общие соглашения (предлагаемые)

- HTTPS only; JSON UTF-8.
- Versioned base path, например `/mobile/v1`, если это соответствует правилам CRM.
- Авторизация и lifecycle токенов не выбраны.
- `X-Correlation-Id` для трассировки без PII.
- `Idempotency-Key` со стабильным UUID для каждой операции записи.
- Сервер возвращает машинный error code, retryability и correlation ID; не возвращает secrets в ошибках.
- Время передаётся в UTC ISO 8601; authoritative clock и допустимый skew открыты.

## Caller lookup

Предлагаемый запрос:

```http
POST /mobile/v1/caller-lookup
Authorization: <TBD>
Content-Type: application/json
X-Correlation-Id: <uuid>

{
  "phoneNumber": "+48123456789",
  "clientRequestId": "uuid"
}
```

Предлагаемый ответ:

```json
{
  "result": "matched",
  "match": {
    "customerRef": "opaque-id",
    "displayName": "Example",
    "organization": "Example Org",
    "ownerDisplayName": "Example Owner",
    "statusLabel": "Example Status"
  },
  "cache": { "maxAgeSeconds": 300 },
  "serverTime": "2026-01-01T00:00:00Z",
  "correlationId": "uuid"
}
```

`result` предлагается как `matched | not_found | ambiguous`. Возврат нескольких кандидатов, правила выбора, нормализация, поля карточки и authorization filtering не определены. Номер в примере вымышленный и не задаёт региональную политику.

## Event ingestion

Предлагаемый endpoint:

```http
POST /mobile/v1/call-events
Authorization: <TBD>
Idempotency-Key: <stable-event-uuid>
Content-Type: application/json
X-Correlation-Id: <uuid>

{
  "eventId": "stable-event-uuid",
  "schemaVersion": 1,
  "eventType": "call_observed",
  "observedAt": "2026-01-01T00:00:00Z",
  "source": "android_call_screening",
  "confidence": "observed",
  "customerRef": "opaque-id-or-null",
  "callRef": "locally-generated-opaque-id",
  "attributes": {}
}
```

До доказательства нельзя отправлять `answered`, `ended` или duration как точные факты. Если PoC даст только приближённые сигналы, event type/`confidence` должны прямо отражать это.

Предлагаемый успешный ответ:

```json
{
  "eventId": "stable-event-uuid",
  "status": "accepted",
  "receiptId": "opaque-server-id",
  "correlationId": "uuid"
}
```

Повтор с тем же idempotency key и семантически тем же payload должен вернуть тот же результат без повторного side effect. Повтор UUID с другим payload должен возвращать явный non-retryable conflict.

## Outcome ingestion

Outcome может быть отдельным событием через тот же ingestion endpoint либо отдельным endpoint — решение открыто. Предлагаемые данные: stable event UUID, call reference, opaque outcome code, optional note только при подтверждённой необходимости, observedAt и schemaVersion.

Справочник outcome, локализация, обязательность заметки и возможность изменения не определены.

## Ошибки и retries (предложение)

| Класс | Пример | Клиентское действие |
|---|---|---|
| Auth | expired token | один контролируемый refresh; затем user-visible auth state |
| Validation/schema | unsupported code | `PERMANENT_FAILURE` |
| Conflict | UUID reused with different payload | `PERMANENT_FAILURE`, security/diagnostic signal |
| Throttle | 429 + Retry-After | retry не ранее указанного времени |
| Transient server | 5xx | bounded backoff + jitter |
| Network | timeout/offline | retry при подходящей сети |

Точные status codes, retry limits, timeouts и response schemas должны быть согласованы с CRM. Lookup failure не блокирует звонок.

## Совместимость и эволюция

- Сервер игнорирует только явно разрешённые неизвестные additive fields.
- Breaking changes требуют новой версии и пользовательского одобрения.
- Клиент отправляет schema version; сервер публикует период поддержки.
- Mobile release compatibility window и kill switch не определены.

## Необходимые подтверждения

Все endpoints, auth, tenant model, phone normalization, fields, authorization, rate limits, environments, idempotency storage window, error taxonomy, retention и audit requirements перечислены в `OPEN_QUESTIONS.md`.
