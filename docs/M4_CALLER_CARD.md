# M4 Caller Card

Статус: **ACCEPTED 2026-07-23 — current Xiaomi/HyperOS scope**

## Scope

- Карточка находится только внутри вкладки Calls.
- После входящего screening callback приложение сначала вызывает `respondToCall`, затем отдельно запускает CRM lookup.
- Состояния: ready, loading, matched, not found, number unavailable, unauthorized и safe unavailable reason.
- Matched UI показывает только nullable `displayName` и общий факт CRM match. Номер и `customerRef` не отображаются.
- Кнопка Clear удаляет process-local state; новый/скрытый звонок заменяет предыдущую карточку.
- При network failure приложение до 5 минут ждёт `VALIDATED` + `NOT_SUSPENDED` network и выполняет один автоматический retry. Это остаётся вне критического пути звонка.

## Exclusions

- Нет overlay, lock-screen surface, notification или default dialer UI.
- Одобрен обычный install-time `ACCESS_NETWORK_STATE`; runtime-диалога нет, содержимое трафика и network identifiers не читаются.
- Нет persistent caller cache, call history replacement, customer editing или outcome flow.
- Mock history под карточкой остаётся явно помеченной как mock foundation и не смешивается с CRM result.

## Critical-path guarantee

`CallScreeningService` сохраняет порядок: fail-open `respondToCall` → privacy-safe timing → extraction permitted `tel` handle → asynchronous dispatcher. Hilt gateway access, normalization, DNS/TLS/HTTP и UI state происходят только после Android response. M2 timing evidence и internal 100 ms budget продолжают действовать.

## Data lifecycle

- Phone number: transient coroutine argument, без persistent storage/logging.
- `customerRef`/`displayName`: только process-local state; ID не показывается в UI.
- Process death или Clear удаляет card state.
- Lookup generation ID отбрасывает stale response предыдущего звонка.

## Acceptance evidence

- JVM: state mapping, stale-result rejection, clear invalidation, coordinator result, missing-number no-network branch и recovery retry branches.
- Android UI: approved display name shown; CRM ID не показан; unavailable-number state не оставляет previous identity.
- Xiaomi/HyperOS: real incoming call по Wi-Fi успешно показал display name из OPONX CRM.
- Xiaomi/HyperOS LTE: системно подтверждено `CELLULAR CONNECTED -> SUSPENDED` на время голосового вызова и восстановление после завершения; recovery retry затем получил HTTP 200 и показал идентифицированного клиента.
- Последний LTE evidence: screening response `0.268 ms`; network `SUSPENDED` до звонка, `CONNECTED` после завершения; retry начался в момент восстановления, завершился HTTP 200 за `1.849 s`.
- Realme UI и reference physical Android остаются M8 validation scope; результаты Xiaomi не экстраполируются на них.
