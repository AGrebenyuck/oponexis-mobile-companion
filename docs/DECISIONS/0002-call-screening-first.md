# ADR-0002: CallScreening-first and fail-open

- Status: Accepted for PoC direction
- Date: 2026-07-22

## Context

Требуется распознать контекст входящего звонка, не заменяя системный Phone. Android предоставляет `CallScreeningService`, но вызывает его в ограниченном критическом окне и применяет условия/roles. OEM и системные контакты могут влиять на доставку вызова сервису.

## Decision

Начать с изолированного `CallScreeningService` PoC. Сервис обязан ответить Android внутри deadline. Он всегда fail-open для данного продукта: сетевой lookup, DNS/TLS, auth refresh, CRM, UI и некритичная долговременная работа никогда не задерживают разрешение звонка.

Локальное наблюдение/кэш допустимы только если измеренно укладываются во внутренний safety budget; при любой ошибке/неуверенности ответ даётся немедленно.

Без `READ_CONTACTS` service может не получать звонки от системных контактов. После physical baseline без permission пользователь явно одобрил `READ_CONTACTS` 2026-07-23 с узким purpose доставки screening callbacks; прямой доступ к контактным полям и `WRITE_CONTACTS` не одобрены. Denied/revoked path остаётся fail-open и требует тестов.

## Verified facts и assumptions

Официальная документация M2 подтверждает 5-секундный deadline для `respondToCall` входящего вызова и ограничение системных контактов без `READ_CONTACTS`. Engineering target PoC — 100 ms. Получение событий `ANSWERED`, `ENDED` и duration не подтверждено и не является частью решения.

## Consequences

- CRM outage/latency не блокирует звонок.
- Caller card может появиться позже или не появиться.
- Нужна instrumentation deadline без PII.
- Saved contacts могут быть вне охвата без дополнительного permission.
- M2 evidence предшествует production implementation.

## Alternatives

Network-first synchronous screening отклонён как нарушающий reliability constraint. Default dialer replacement и overlay не входят в scope и требуют отдельных решений.

## Validation и rollback

Emulator и physical tests отдельно измеряют cold/warm/error cases на reference, Realme UI и HyperOS. Если fail-open deadline не доказан, PoC удаляется/feature остаётся выключенной. Unsupported lifecycle signals не реализуются; замещённый PoC удаляется после проверки production adapter.
