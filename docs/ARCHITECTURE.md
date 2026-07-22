# Architecture

Статус: **DRAFT; foundation реализован в M1, CallScreening adapter реализован как M2 PoC**

## Принципы

- Native Android, single application.
- Fail-open screening: системный ответ отделён от CRM lookup.
- Offline-first event delivery через outbox.
- Privacy by design и минимальные привилегии.
- Platform abstractions описывают возможности Android, а не конкретные OEM.
- Подтверждённые факты отделены от PoC-гипотез.

## Предлагаемые логические области

| Область | Ответственность | Статус |
|---|---|---|
| Call screening adapter | Быстрый fail-open ответ Android и обезличенная локальная метрика | M2 PoC; emulator verified, physical pending |
| Call observation | Унифицированные наблюдения жизненного цикла с источником/уверенностью | Требует M5 PoC |
| CRM gateway | Auth, caller lookup, event delivery | Draft contract |
| Caller cache | Ограниченный локальный контекст и freshness policy | Политика открыта |
| Caller card | Presentation state без OEM-зависимостей | UX открыт |
| Outcome | Выбор/валидация результата разговора | Справочник открыт |
| Event outbox | Durable enqueue, retry, idempotency, failure visibility | Решение ADR-0003 |
| Diagnostics | Privacy-safe structured logs и явный экспорт | Политика draft |

Kotlin, Compose, Material 3, Hilt, Room, Retrofit, OkHttp, Coroutines, StateFlow, WorkManager и DataStore добавлены как M1 foundation; точные версии находятся в `gradle/libs.versions.toml`. M2 добавляет только platform `CallScreeningService`, role controller и process-local diagnostics. CRM endpoints, workers, production Room entities и lifecycle observation пока не реализованы.

## Критический путь входящего звонка

1. Android вызывает `CallScreeningService` при выполнении платформенных условий.
2. Сервис немедленно отвечает Android с fail-open решением: разрешить, не отклонять и не заглушать звонок.
3. Только после возврата `respondToCall` PoC обновляет process-local счётчик и timing без номера или иных PII.
4. Будущий некритический pipeline сможет читать кэш и запускать lookup только после/независимо от безопасного ответа.

Официальный контракт Android требует вызвать `respondToCall` для входящего звонка в течение 5 секунд; после этого framework отвязывает service и игнорирует поздний ответ. M2 использует инженерную цель 100 ms. Ни один DNS, TLS, auth refresh, CRM запрос, disk write или ожидание UI не может задерживать системный ответ. Emulator evidence зафиксирован отдельно; OEM применимость ещё не доказана.

## Потоки данных

### Lookup

`Android call observation -> local normalization -> permitted cache lookup -> async CRM lookup -> sanitized presentation state`

### Event delivery

`supported observation/outcome -> local transaction -> outbox UUID -> background attempt -> idempotent CRM endpoint -> delivered or retry/permanent failure`

Outbox row предполагает: UUID, event type, schema version, payload (минимизированный), created/observed timestamps, attempt count, next attempt, status, last safe error category, correlation ID и optional server receipt. Схема БД и миграции требуют отдельного одобрения.

## Состояния outbox

- `PENDING`: сохранено локально, отправка ещё не подтверждена.
- `IN_FLIGHT`: взято одним worker с lease/эквивалентной защитой.
- `RETRY_SCHEDULED`: временная ошибка; рассчитана следующая попытка.
- `DELIVERED`: сервер подтвердил применение либо повтор того же idempotency key.
- `PERMANENT_FAILURE`: автоматические retries прекращены; ошибка видима и не теряется.

Переход `IN_FLIGHT -> RETRY_SCHEDULED/PENDING` после crash должен быть восстанавливаемым. Retry использует exponential backoff с jitter и ограничениями, которые ещё не определены. HTTP-коды и CRM business errors должны быть классифицированы контрактом.

## Данные и границы хранения

- Credentials — только через Android-supported secure storage после threat analysis.
- Room зарезервирован для будущего кэша/outbox, DataStore используется для малых настроек; production schema не создана.
- PII не попадает в обычные логи.
- Retention и logout/wipe semantics открыты.

## Permission boundary

Permissions добавляются только после одобрения пользователя, отдельного обоснования и тест-плана. `READ_CONTACTS` особенно чувствителен: без него системные контакты могут не поступать в screening; это ограничение должно быть принято продуктом либо permission отдельно одобрен. Не подразумеваются call log, phone state, overlay или default dialer permissions/roles.

## Verified versus unverified

Здесь слово «verified» означает подтверждение официальной документацией для выбранного API и/или воспроизводимым тестом с записанными условиями. На Pixel_7 API 36 Emulator подтверждены role binding, simulated incoming callback, allow response и timing; это не доказывает physical/OEM behavior. Точность answer/end/duration, contact filtering на целевых телефонах, OEM background behavior и UI timing остаются unverified.

## Изменения архитектуры

Малые безопасные рефакторинги разрешены по `AGENTS.md`. Permissions, зависимости, API, DB migrations и крупные архитектурные изменения требуют пользовательского одобрения. После проверки replacement прежняя реализация удаляется; параллельный legacy pipeline не сохраняется.
