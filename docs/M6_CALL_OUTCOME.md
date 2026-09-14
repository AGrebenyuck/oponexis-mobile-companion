# M6 Call Outcome

Статус: **ACCEPTED 2026-07-24 — current API 30+ Emulator + Xiaomi/HyperOS scope**

## Scope

- После API 30+ post-call observation создаётся локальный draft со стабильным UUID.
- Пользователь выбирает один из одобренных outcomes: `interested`, `follow_up_required`, `not_interested`, `wrong_number`, `other` либо нажимает Skip.
- Выбор необязателен. Notes и редактирование выбранного результата не реализованы.
- Pending drafts хранятся в Room и показываются во вкладке Calls по очереди, начиная с самого старого.
- Одобренный `POST_NOTIFICATIONS` создаёт reminder после звонка; нажатие ведёт прямо в Calls.
- По отдельному approval полный номер и подтверждённое CRM display name разрешены в notification, включая secure lock screen, и в pending-карточке.

## Data lifecycle

Room schema version 3 хранит pending `callRef`, observed timestamp, coarse disconnect/duration categories, номер и optional CRM display name. Номер извлекается только из `TelecomManager.EXTRA_HANDLE` с `tel:` scheme, ограничивается digits/leading `+` и не логируется.

> Историческая фиксация M6: в M7 schema v4 добавила retention-controlled history и outbox; актуальная модель описана в `M7_OFFLINE_OUTBOX.md`.

После outcome или Skip DAO в той же операции очищает `phone_number` и `display_name`. Локально остаются UUID, coarse observation, выбранный outcome/resolve timestamp либо dismissed timestamp. Retention и перенос в M7 outbox ещё не утверждены.

CRM name применяется только после нового lookup именно post-call номера. Последнее process-local имя не переиспользуется без проверки. При недоступной CRM карточка и notification продолжают работать с номером.

## Exclusions

- Нет CRM event ingestion, outbox, retry или server-side idempotency — это M7.
- Нет notes, outcome editing, автоматического inference или обязательного выбора.
- Нет overlay/default-dialer UI.
- Нет API 29 post-call fallback.
- M6 не заявляет точные `ANSWERED`, `ENDED` или duration.

## Dependencies

- M5 `ACTION_POST_CALL` evidence на API 30+.
- Одобренные outcomes, Room schema/migrations `1→2→3` и `2→3`, `POST_NOTIFICATIONS` и явное product-owner решение показывать caller identity на secure lock screen.
- M3 lookup используется только для optional подтверждённого display name; outcome не зависит от доступности CRM.

## Acceptance criteria и evidence

- Pending outcome переживает закрытие/перезапуск приложения после Room insert.
- Несколько drafts не заменяют друг друга и разбираются по FIFO.
- Outcome/Skip удаляет draft из pending UI и очищает временные identity fields.
- Notification не влияет на fail-open CallScreening critical path.
- Pixel_7 API 36 Emulator: Room migrations, DAO и Compose tests pass; notification routed в Calls, имел `PUBLIC` visibility и показал test number; outcome убрал pending card.
- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: in-place M5→M6→M6.1 updates и Room migrations прошли без crash. Реальные звонки подтвердили notification, deep-link в Calls, outcome, Skip, полный номер на lock screen и тот же номер в карточке.
- Последний physical log: `missed/veryshort`, draft создан через приблизительно `260 ms`; номер отсутствует в structured log.

## Automated tests

- JVM: identity-free UUID draft mapping, one-time selection, optional dismiss и очистка временного номера.
- Android Room: DAO select, migration `1→2→3` с сохранением foundation data и migration `2→3` identity columns.
- Compose: approved outcome click, optional Skip и pending-count UI.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug` и `assembleRelease` должны проходить последовательно.

## Emulator tests

- API 36 migration/DAO/UI instrumentation.
- Синтетический post-call с test `tel:` handle: Room insert, notification content/visibility, navigation и resolved pending UI.

## Physical-device tests

- Current Xiaomi/HyperOS: реальный answered call с outcome, rejected/missed flow со Skip, notification в background и secure lock, number consistency между notification/card, in-place migrations.
- Realme/reference Android, multiple rapid real calls, process kill в узком промежутке до Room insert и CRM-name notification остаются M8/pending evidence.

## Permissions

- `POST_NOTIFICATIONS` явно одобрен 2026-07-24. Runtime denial не мешает Room draft или in-app outcome; notification просто не показывается.
- Других permissions M6 не добавляет. Overlay, call log, phone state и default dialer role не используются.

## Risks

- Product owner явно принял видимость caller identity на secure lock screen; это повышает риск раскрытия номера/имени человеку с физическим доступом. Политика должна быть повторно подтверждена privacy/security owners до M9.
- Экспортированный post-call Activity потенциально spoofable; draft/outcome пока нельзя считать audit-grade CRM evidence.
- Application-scope insert может быть потерян при убийстве процесса сразу после Activity finish; M7 должен заменить этот best-effort участок durable scheduling/outbox решением.
- API/OEM divergence, notification settings и CRM-name latency требуют fleet validation.

## Rollback conditions

- Если identity показывается не для того звонка, отключить display name и оставить номер из текущего `EXTRA_HANDLE` либо generic notification.
- Если lock-screen visibility нарушает утверждённую policy, создать новый private channel и скрыть identity.
- При migration/data-loss/crash остановить rollout и вернуться к последней совместимой подписанной версии только с утверждённым DB rollback plan.
