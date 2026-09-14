# Architecture

Статус: **DRAFT; M1–M6 приняты, M7 реализован и проходит physical acceptance**

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
| Call screening adapter | Быстрый fail-open ответ Android и обезличенная локальная метрика | M2 PoC; Emulator + Xiaomi verified, fleet pending |
| Call observation | API 30+ категориальный post-call сигнал без identity/durable history | M5 scoped PoC; Emulator + Xiaomi verified, fleet pending |
| CRM gateway | Auth, caller lookup, event delivery | M3 read-only lookup PoC; production auth открыт |
| Caller cache | Ограниченный локальный контекст и freshness policy | Политика открыта |
| Caller card | Process-local sanitized state во вкладке Calls | M4 accepted for current Xiaomi scope; fleet pending |
| Outcome | Optional FIFO outcome drafts, notification и Room persistence | M6 accepted; M7 delivery implemented |
| Event outbox | Durable enqueue, retry, idempotency, failure visibility | M7 accepted for current Xiaomi scope; remaining fleet pending M8 |
| SMS activity | FCM FID registration, privacy-safe status pushes, Room history and one four-minute verification | Implemented; physical Realme acceptance pending |
| Diagnostics | Privacy-safe structured logs и явный экспорт | Политика draft |

Kotlin, Compose, Material 3, Hilt, Room, Retrofit, OkHttp, Coroutines, StateFlow, WorkManager и DataStore добавлены как M1 foundation; точные версии находятся в `gradle/libs.versions.toml`. M3/M4 lookup с bounded timeouts не входит в critical screening path. M7 использует WorkManager из утверждённого foundation stack для durable outcome delivery; новых dependencies не добавлено.

M5 добавляет отдельную `PostCallActivity` для `TelecomManager.ACTION_POST_CALL` на API 30+. В исходном M5 PoC handle игнорировался. После M6 approval adapter извлекает только bounded `tel:` номер, не логирует его, создаёт Room draft через application-scope dispatcher и немедленно закрывается согласно `Theme.NoDisplay`. Экспортированная Activity необходима для platform delivery, но action потенциально может быть имитирован другим приложением; observation/outcome не является audit-grade CRM evidence без provenance review.

Room schema version 8 содержит `call_outcomes`, `event_outbox` и `sms_activity`. `@Transaction` одновременно фиксирует выбранный outcome и stable-UUID outbox row; Skip не создаёт CRM event. Номер/имя копируются в retention-controlled local history, outbox phone/customer fields очищаются после delivery. UI читает Room/outbox напрямую.

## Критический путь входящего звонка

1. Android вызывает `CallScreeningService` при выполнении платформенных условий.
2. Сервис немедленно отвечает Android с fail-open решением: разрешить, не отклонять и не заглушать звонок.
3. Только после возврата `respondToCall` PoC обновляет process-local счётчик и timing без номера или иных PII.
4. M4 некритический pipeline запускает lookup только после/независимо от безопасного ответа; persistent cache пока отсутствует.

В M4 шаг 4 реализован без изменения fail-open решения: после `respondToCall` dispatcher запускает coroutine на `Dispatchers.IO`, получает M3 gateway через Hilt entry point и публикует sanitized process-local state. UI не блокирует звонок, не использует overlay/notification и не хранит номер. Lookup generation ID не позволяет позднему ответу предыдущего звонка заменить более новую карточку. `NetworkRecoveryWaiter` наблюдает только capability-флаги и не читает SSID, carrier или network identifiers.

Официальный контракт Android требует вызвать `respondToCall` для входящего звонка в течение 5 секунд; после этого framework отвязывает service и игнорирует поздний ответ. M2 использует инженерную цель 100 ms. Ни один DNS, TLS, auth refresh, CRM запрос, disk write или ожидание UI не может задерживать системный ответ. Emulator evidence зафиксирован отдельно; OEM применимость ещё не доказана.

## Потоки данных

### Lookup

`Android call observation -> local normalization -> permitted cache lookup -> async CRM lookup -> sanitized presentation state`

### Event delivery

`supported observation/outcome -> local transaction -> outbox UUID -> background attempt -> idempotent CRM endpoint -> delivered or retry/permanent failure`

Pipeline реализован в M7. Выбор outcome и enqueue атомарны; WorkManager запускает unique network-constrained work, использует lease и максимум 10 попыток. Application startup восстанавливает потерянное scheduling для pending/retry/expired in-flight rows. CRM хранит UUID и payload hash: идентичный replay возвращает прежний receipt, другой payload с тем же UUID даёт permanent conflict.

### SMS activity

`CRM/SMSGate event -> privacy-safe FCM data push -> protected activity refresh -> Room status -> UI/notification`

FCM использует Firebase Installation ID и не передаёт PII. Номер и failure detail возвращаются только защищённым mobile API. `QUEUED` создаёт один локальный deadline; через четыре минуты Companion выполняет одну сверку и уведомляет, если финальный webhook отсутствует. Частый heartbeat и прежний пятисекундный polling worker удалены.

## Состояния outbox

- `PENDING`: сохранено локально, отправка ещё не подтверждена.
- `IN_FLIGHT`: взято одним worker с lease/эквивалентной защитой.
- `RETRY_SCHEDULED`: временная ошибка; рассчитана следующая попытка.
- `DELIVERED`: сервер подтвердил применение либо повтор того же idempotency key.
- `PERMANENT_FAILURE`: автоматические retries прекращены; ошибка видима и не теряется.

Переход после crash восстанавливается lease и повтором WorkManager. Network/429/5xx retryable; validation/auth/conflict permanent; после 10 transient attempts событие становится `PERMANENT_FAILURE`. Raw response/error payload не логируется.

## Данные и границы хранения

- Credentials — только через Android-supported secure storage после threat analysis.
- Room хранит outcome history/outbox; DataStore хранит выбор 30 дней (default) или forever. Cleanup не удаляет pending/outbox/permanent failures.
- PII не попадает в обычные логи.
- Retention и logout/wipe semantics открыты.

## Permission boundary

Permissions добавляются только после одобрения пользователя, отдельного обоснования и тест-плана. `READ_CONTACTS` одобрен 2026-07-23 после baseline без permission; его единственная M2-функция — platform delivery сохранённых звонящих в screening. Архитектура не читает Contact Provider. Не подразумеваются `WRITE_CONTACTS`, call log, phone state, overlay или default dialer permissions/roles.

Обычный install-time permission `INTERNET` одобрен 2026-07-23 для M3. `ACCESS_NETWORK_STATE` отдельно одобрен 2026-07-23 для M4 recovery retry; runtime prompt отсутствует. Debug может разрешать cleartext только когда локальный `OPONEXIS_CRM_BASE_URL` явно использует `http://`; release всегда запрещает cleartext и не встраивает PoC credentials.

`POST_NOTIFICATIONS` одобрен 2026-07-24 для M6 reminders. `RECEIVE_BOOT_COMPLETED` и `WAKE_LOCK` одобрены 2026-07-24 для M7 WorkManager recovery/delivery. Battery optimization exemption, foreground service и broad OEM-autostart permission не добавлялись.

## Verified versus unverified

Здесь слово «verified» означает подтверждение официальной документацией для выбранного API и/или воспроизводимым тестом с записанными условиями. На Pixel_7 API 36 Emulator подтверждены role binding, simulated incoming callback, allow response, timing и post-call `remote/short`. На Xiaomi/HyperOS API 35 подтверждены post-call категории remote/local/missed/rejected для четырёх реальных входящих звонков. Это не доказывает точные answer/end timestamps, exact duration, API 29 behavior, исходящие/параллельные звонки или применимость к Realme/reference devices.

## Изменения архитектуры

Малые безопасные рефакторинги разрешены по `AGENTS.md`. Permissions, зависимости, API, DB migrations и крупные архитектурные изменения требуют пользовательского одобрения. После проверки replacement прежняя реализация удаляется; параллельный legacy pipeline не сохраняется.
