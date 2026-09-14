# Architecture Decision Records

ADR фиксируют значимые решения, контекст и последствия. Они не заменяют approval process из `AGENTS.md`: permissions, dependencies, API changes, DB migrations и крупные архитектурные изменения требуют одобрения пользователя.

## Статусы

- Proposed — предложение, не разрешает реализацию.
- Accepted — принято уполномоченным владельцем.
- Superseded — заменено указанным ADR; после проверки replacement устаревшая реализация удаляется.
- Rejected — не используется.

## Реестр

| ADR | Решение | Статус |
|---|---|---|
| [0001](0001-native-android.md) | Native Android application | Accepted; foundation implemented in M1 |
| [0002](0002-call-screening-first.md) | CallScreening-first, fail-open | Accepted for PoC direction |
| [0003](0003-offline-outbox.md) | Durable offline outbox | Accepted for architecture direction; schema/API draft |
| [0004](0004-production-auth.md) | One-time device enrollment for production authentication | Accepted for implementation; API/schema contract pending |
| [0005](0005-managed-direct-apk-distribution.md) | Signed APK installed directly on managed work phone | Accepted |
| [0007](0007-fcm-sms-activity.md) | Event-driven SMS activity through Firebase Installation IDs | Accepted and implemented |

Новый ADR использует следующий номер, описывает context, decision, verified facts, assumptions, consequences, alternatives, validation и rollback/supersession.
