# ADR-0003: Durable offline event outbox

- Status: Accepted for architecture direction; schema/API remain draft
- Date: 2026-07-22

## Context

События звонка и outcomes должны переживать offline, process death, reboot и временные CRM errors. Прямая best-effort отправка создаёт риск потерь и дублей.

## Decision

Каждая требующая доставки операция сначала атомарно сохраняется в durable outbox со стабильным UUID. Все повторы используют тот же UUID как idempotency key. Предлагаемые статусы: `PENDING`, `IN_FLIGHT`, `RETRY_SCHEDULED`, `DELIVERED`, `PERMANENT_FAILURE`.

Server processing обязан быть идемпотентным: повтор того же UUID/payload не повторяет side effect, а тот же UUID с другим payload даёт non-retryable conflict. Временные ошибки получают bounded backoff с jitter; permanent failures не теряются и видимы пользователю/support безопасным образом.

Конкретные Room schema, WorkManager policy, retention, retry values, encryption и CRM endpoint не утверждены. Dependency, API и DB/migration changes требуют пользовательского approval.

## Invariants

- UUID создаётся один раз до первой попытки и не меняется.
- Enqueue и локальное бизнес-состояние согласованы одной transaction/атомарной границей.
- Одну запись одновременно обрабатывает не более одного logical worker; crash lease восстанавливается.
- `DELIVERED` устанавливается только по подтверждению сервера/idempotent replay result.
- Исчерпание retries не означает удаление; запись становится `PERMANENT_FAILURE`.
- Диагностика не включает payload/PII.

## Consequences

- Повышается надёжность и наблюдаемость offline работы.
- Требуются серверная idempotency, storage lifecycle, migrations и recovery tests.
- Возможен delayed delivery; UI должен честно показывать состояние.
- Нельзя бесконечно хранить payload без retention/privacy решения.

## Alternatives

Memory queue и direct-send-only отклонены из-за потери данных. Уникальность только на серверном auto-ID отклонена, потому что клиент не сможет безопасно повторить ambiguous request.

## Validation и rollback

M7 доказывает crash windows, duplicate side effect prevention, reboot/network recovery и permanent-failure visibility на emulator и физических устройствах. При отсутствии server idempotency delivery не выпускается. Rollback останавливает workers без удаления queued data и следует одобренной DB/release процедуре. После проверки нового pipeline старый direct-send pipeline удаляется.
