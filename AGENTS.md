# AGENTS.md

## Назначение

Этот репозиторий содержит Oponexis Mobile Companion — внутреннее нативное Android-приложение для сотрудников Oponexis. Настоящий файл обязателен для всех людей и coding agents, работающих в репозитории.

> Примечание: в исходном запросе вместо полного внешнего текста `AGENTS.md` был оставлен плейсхолдер. Поэтому этот файл фиксирует только явно переданные требования и не пытается восстановить отсутствующие правила.

## Текущий этап

- Завершён этап **M1 Android application skeleton**.
- Этап **M2 CallScreening proof of concept** принят 2026-07-23 для текущего scope: Android Emulator и Xiaomi/HyperOS. Realme UI и reference physical Android явно отложены пользователем до появления устройств; совместимость с ними не заявляется и остаётся обязательной для M8.
- **M3 CRM caller lookup** принят 2026-07-23 для internal/ngrok PoC scope после Android/CRM contract tests и Xiaomi/HyperOS cellular HTTPS evidence. Production identity/SSO, rate limiting, permanent deployment и остальная fleet validation остаются release-блокерами.
- **M4 Caller card** принят 2026-07-23 для текущего Xiaomi/HyperOS scope: реальный Wi-Fi match и LTE recovery retry подтверждены. Одобренная поверхность — только in-app UI после fail-open ответа; overlay, lock-screen UI и notification не входят в scope. Обычный `ACCESS_NETWORK_STATE` отдельно одобрен для ожидания восстановления сети.
- **M5 Call lifecycle investigation and implementation** принят 2026-07-23 для API 30+ post-call PoC на Emulator и текущем Xiaomi/HyperOS: категории remote/local/missed/rejected воспроизведены. Это не подтверждает точные `ANSWERED`, `ENDED` или duration; API 29 fallback и остальная fleet matrix остаются открытыми.
- **M6 Call outcome** принят 2026-07-24 для текущего API 30+ Emulator + Xiaomi/HyperOS scope: optional outcomes, Skip, Room migrations, notification/deep-link и номер в pending UI/secure lock screen проверены. Notes, editing и CRM submission исключены.
- **M7 Offline queue and reliability** принят 2026-07-24 для текущего Xiaomi/HyperOS scope после approvals для Room v4, CRM write API, `RECEIVE_BOOT_COMPLETED` и `WAKE_LOCK`: offline pending, online sync и queued reboot recovery подтверждены. CRM idempotency proof и automated checks пройдены. M7 emulator regression, Realme/reference devices, production auth/deployment и расширенная OEM stabilization остаются M8/M9 scope.
- **M8 Multi-device stabilization** принят 2026-07-24 для согласованной доступной матрицы: Pixel 7 API 36 emulator, Generic low-end API 36.1 (1 vCPU/2 GB) и Xiaomi/HyperOS API 35. На Xiaomi подтверждены background/sync, secure lock-screen notification/sync и FIFO двух быстрых звонков. Realme UI нельзя достоверно эмулировать стандартным AVD; physical Realme/reference devices и API 29 emulator пока не предоставлены и остаются явно неподтверждёнными.
- **M9 Signed internal 1.0 release** находится в preparation: release/signing input templates и fail-closed gate готовы, managed-direct distribution и device-enrollment direction приняты, но auth API/schema/implementation, permanent CRM deployment, signing custody и formal approvals не готовы. Не объявлять 1.0 готовой до прохождения всех M9 gates.
- Созданы одобренные Android/Gradle foundation и зависимости M1; новые permissions, зависимости, API/DB changes по-прежнему требуют процесса одобрения ниже.
- Реальные `release.properties`, `signing.properties`, keystore и release artifacts не коммитятся. Shared static Bearer token запрещён в release APK; выбранный production auth должен быть реализован и проверен до M9.
- Для M9 принят `managed_direct`: production — `com.oponexis.companion`, DEV — `com.oponexis.companion.dev`; подписанный APK вручную устанавливается/обновляется владельцем на одном закреплённом рабочем телефоне. Автоматический self-update не одобрен.

## Границы продукта

- Приложение идентифицирует звонящего через OPONX CRM, показывает краткую карточку клиента, фиксирует только технически подтверждённые события звонка, позволяет указать результат разговора и надёжно синхронизирует события с CRM.
- Это не полноценная CRM, не замена системному приложению Phone и не SMS-приложение.
- SMS Gateway for Android остаётся отдельным продуктом.
- Целевая минимальная версия предполагается API 29, но должна быть подтверждена отдельным анализом до создания проекта.
- Android Emulator, Realme / Realme UI, Xiaomi / HyperOS и стандартные Android-устройства — цели совместимости, а не архитектурные зависимости.

## Доказательность Android-поведения

- В документации и коде отделять подтверждённое официальной Android-документацией поведение от гипотез, результатов PoC и OEM-наблюдений.
- `CallScreeningService` обязан ответить в установленный Android срок. Сетевой CRM lookup никогда не должен задерживать разрешение звонка.
- Без `READ_CONTACTS` `CallScreeningService` может не получать вызовы для номеров из системных контактов. `READ_CONTACTS` — permission, требующий явного одобрения пользователя до добавления.
- Не считать `ANSWERED`, `ENDED` или duration точными/доступными, пока это не доказано PoC на эмуляторе и физических устройствах.
- Тестирование на Android Emulator и физических устройствах — отдельные обязательные слои.

## Утверждённый foundation stack

В M1 добавлены Kotlin, Jetpack Compose, Material 3, Gradle Kotlin DSL, Hilt, Room, Retrofit, OkHttp, Coroutines, StateFlow, WorkManager и DataStore. Зафиксированные версии находятся в version catalog. Любая новая зависимость или существенное обновление существующей требует одобрения пользователя.

## Процесс изменений и одобрений

Agent может без отдельного согласования:

- делать небольшие безопасные рефакторинги без изменения поведения или публичных контрактов;
- переименовывать локальные сущности ради ясности;
- удалять подтверждённо неиспользуемый код и обновлять тесты/документацию в рамках одобренной задачи;
- исправлять форматирование, опечатки и очевидные дефекты в рамках текущего scope.

До изменения agent обязан запросить одобрение пользователя для:

- добавления или расширения Android permissions;
- добавления, удаления или существенного обновления зависимостей;
- изменения CRM/API-контрактов;
- изменения схемы БД или любой миграции;
- крупного архитектурного изменения, нового subsystem или смены фундаментального подхода;
- изменения минимальной/целевой Android API, подписи, распространения или security policy.

Если граница неясна, изменение считается требующим одобрения. Решение фиксируется ADR или в соответствующей документации.

## Удаление и рефакторинг

- После проверки замены удалить устаревшую реализацию, её тестовые doubles, флаги и документацию.
- Не накапливать параллельные legacy-реализации «на всякий случай».
- До удаления подтвердить эквивалентность или сознательное изменение поведения тестами и acceptance criteria.
- Не удалять пользовательские изменения и не выполнять разрушительные Git-операции без явного запроса.

## Данные, логирование и диагностика

- Использовать privacy-safe structured logging: фиксированный event name, severity, timestamp, build/device context и correlation IDs.
- Не логировать номера телефонов, имена, CRM payloads, токены, содержимое заметок и иные персональные/секретные данные. Если для корреляции нужен номер, использовать краткоживущий необратимый keyed digest; схема требует security review.
- Диагностический экспорт инициируется пользователем, показывает состав перед отправкой, исключает PII/secrets по умолчанию, имеет ограниченный срок хранения и не отправляется автоматически.
- Все CRM-события, требующие доставки, проходят через durable outbox со стабильным UUID, статусами retry, идемпотентной обработкой сервером и видимостью permanent failure.

## Качество и завершение работы

- Соблюдать scope текущего milestone и его exclusions.
- Для каждого milestone документировать scope, exclusions, dependencies, acceptance criteria, automated tests, emulator tests, physical-device tests, permissions, risks и rollback conditions.
- Перед завершением сверять термины, ссылки, milestones и API-модели во всех документах; искать конфликтующие утверждения по всему репозиторию.
- Не выдавать предположение за решение и не придумывать ответы на открытые бизнес- или технические вопросы.
- Обновлять `CHANGELOG.md` при значимых изменениях.

## Общие принципы работы agent

Эти behavioral guidelines дополняют project-specific инструкции выше и направлены на снижение типичных ошибок coding agents. Они отдают приоритет осторожности перед скоростью; для тривиальных задач следует использовать здравое суждение. При конфликте действует более строгое project-specific правило.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass".
- "Fix the bug" → "Write a test that reproduces it, then make it pass".
- "Refactor X" → "Ensure tests pass before and after".

For multi-step tasks, state a brief plan:

```text
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let the agent loop independently. Weak criteria ("make it work") require constant clarification.

Эти guidelines работают, если diffs содержат меньше необязательных изменений, реже возникают переписывания из-за переусложнения, а уточняющие вопросы задаются до реализации, а не после ошибок.
