# M5 Call Lifecycle Investigation

Статус: **ACCEPTED 2026-07-23 — API 30+ post-call PoC, Emulator + current Xiaomi/HyperOS scope**

## Решение

M5 использовал permission-free `TelecomManager.ACTION_POST_CALL` как узкий post-call сигнал на Android 11/API 30 и выше. В M5 scope экспортированная `PostCallActivity` игнорировала `EXTRA_HANDLE`, сохраняла только process-local счётчик и категориальные значения `EXTRA_DISCONNECT_CAUSE`/`EXTRA_CALL_DURATION`, записывала privacy-safe structured log и немедленно закрывалась. M6 позднее добавил отдельно одобренное temporary handle usage для outcome identity; M5 evidence от этого не меняется.

Это не полноценная модель жизненного цикла звонка. Реализация не создаёт CRM/outbox event, не хранит call history и не заявляет точное время `ANSWERED`/`ENDED` или точную длительность.

Официальные Android-контракты:

- [`TelecomManager.ACTION_POST_CALL`](https://developer.android.com/reference/android/telecom/TelecomManager#ACTION_POST_CALL) запускает Activity call-screening приложения после завершённого исходящего или разрешённого входящего звонка на API 30+;
- [`EXTRA_DISCONNECT_CAUSE`](https://developer.android.com/reference/android/telecom/TelecomManager#EXTRA_DISCONNECT_CAUSE) предоставляет категорию причины завершения;
- [`EXTRA_CALL_DURATION`](https://developer.android.com/reference/android/telecom/TelecomManager#EXTRA_CALL_DURATION) предоставляет только coarse bucket, а не секунды.

## Почему не выбраны другие API

- `TelephonyCallback.CallStateListener` требует нового approval для `READ_PHONE_STATE`, даёт только coarse `IDLE/RINGING/OFFHOOK` и сам по себе не доказывает точное событие ответа.
- `CallLog` потребовал бы sensitive `READ_CALL_LOG` и чтение истории звонков.
- `InCallService` потребовал бы роль полноценного default dialer и реализацию dial/in-call UI, что противоречит границам продукта.

Ни одно из этих permissions/roles не добавлено.

## Acceptance evidence

### Automated

- JVM mapping tests покрывают известные disconnect/duration значения, неизвестные значения и повторные наблюдения без идентификаторов.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug` и `assembleRelease` проходят.

### Android Emulator

- Pixel_7, API 36: simulated incoming call был принят и завершён удалённой стороной.
- CallScreening fail-open response: `4.208 ms`.
- Post-call observation: `remote`, duration bucket `short`.

### Physical device

Xiaomi 2505DRP06E, HyperOS 2.0, Android 15/API 35, четыре последовательных реальных входящих звонка:

| Сценарий | Ручное действие | Наблюдение Android |
|---|---|---|
| A | ответ на планшете, caller завершил | `remote`, `short` |
| B | ответ на планшете, планшет завершил | `local`, `short` |
| C | без ответа, caller завершил | `missed`, `very short` |
| D | отклонение на планшете | `rejected`, `very short` |

Diagnostics показал четыре события. Screening responses составили `0.295`, `0.508`, `0.307` и `0.277 ms`, поэтому post-call наблюдение не изменило M2 fail-open critical path.

## Ограничения и риски

- API 29 не предоставляет `ACTION_POST_CALL`; UI явно показывает отсутствие capability.
- Coarse duration buckets: `<3 s`, `3–59 s`, `60–119 s`, `>=120 s`. Они не являются exact duration и не доказывают момент ответа.
- Получение Activity происходит после звонка и не даёт точного `ENDED` timestamp; время запуска — только время наблюдения приложением.
- Process-local diagnostics исчезают после смерти процесса и не являются durable call history.
- Intent-filter Activity экспортирована для platform delivery. Другие приложения потенциально могут имитировать action, поэтому PoC-данные нельзя считать security-grade audit evidence или отправлять в CRM без отдельной provenance/threat review.
- Исходящие звонки, API 29 fallback, rapid/parallel calls, call waiting, conference, Bluetooth, dual-SIM semantics, reboot/process-death delivery, Realme UI и reference physical Android не проверены.

## Rollback conditions

Удалить или отключить post-call surface, если он мешает системному Phone UI, раскрывает идентификаторы, не доставляется воспроизводимо либо даёт вводящие в заблуждение категории. `CallScreeningService` и fail-open ответ остаются независимыми.

## Следующий вход для M6

До реализации outcome нужны утверждённые outcome codes, обязательность выбора, edit window и допустимый способ связать outcome с конкретным звонком без номера в логах или ошибочной корреляции.
