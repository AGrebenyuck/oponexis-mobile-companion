# Device Compatibility

Статус: **DRAFT test matrix; не архитектурная спецификация**

## Цели

- Android Emulator;
- Realme devices / Realme UI;
- Xiaomi devices / HyperOS;
- другие стандартные/reference Android-устройства.

Первый physical target: Xiaomi 2505DRP06E, HyperOS 2.0, Android 15/API 35, Google Phone и две активные SIM. Остальные модели, регионы, OS builds, SIM-конфигурации и management policies пока не предоставлены. Проект использует min API 29 и target/compile API 36 по результатам M1.

## Правило архитектуры

OEM — измерение capability/test evidence. Домен, API и outbox не зависят от Realme UI или HyperOS. Допустимы изолированные platform workarounds только после воспроизводимого дефекта, documented scope, user approval если изменение крупное/permission-related, automated guard и physical-device regression. После появления проверенной общей замены OEM workaround удаляется.

## Матрица испытаний

| Область | Emulator | Realme UI | HyperOS | Reference Android |
|---|---:|---:|---:|---:|
| Screening role/service setup | Да | Да | Да | Да |
| Deadline/fail-open | Да | Да | Да | Да |
| Системный контакт без permission | Ограниченно | Да | Да | Да |
| `READ_CONTACTS` branch, только если одобрен | Да | Да | Да | Да |
| Реальный входящий звонок | Ограниченно | Да | Да | Да |
| Post-call disconnect/duration bucket, API 30+ | Partial pass | Pending | Pass, current scenarios | Pending |
| Exact answer/end timestamp/duration | Не подтверждено | Не подтверждено | Не подтверждено | Не подтверждено |
| M6 Room outcome + notification | Pass, API 36 | Pending | Pass, API 35 | Pending |
| Lock screen/card privacy | Да | Да | Да | Да |
| Background, idle, reboot, battery policy | Частично | Да | Да | Да |
| M7 offline outbox/network switching | Pending | Pending | Pass, current scope | Pending |
| Update/role retention | Частично | Да | Да | Да |

M8 emulator note: `Medium_Phone_API_36.1` (Generic, 1 vCPU, 2 GB RAM) используется как resource-constrained reference, не как Realme UI substitute.

M8 available-matrix status: Pixel 7 API 36 и Generic low-end API 36.1 automated regression pass; Xiaomi/HyperOS background, secure-lock notification, sync и two-call FIFO pass. Realme UI/reference/API 29 остаются unverified, а не условно пройденными.

Финальная Xiaomi `0.8.0-m8-poc` установка после удаления временного tunnel URL сохранила Call Screening role, `READ_CONTACTS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` и локальные данные.

## Verified facts и assumptions

Официально подтверждено: входящий screening callback требует `respondToCall` в течение 5 секунд; сеть нельзя помещать в критический путь. Без `READ_CONTACTS` service может не получать номера из системных контактов. На API 30+ Android также документирует post-call Activity с disconnect category и coarse duration bucket. На Pixel_7 API 36 Emulator role/service binding, simulated unsaved incoming call и post-call `remote/short` воспроизведены. На Xiaomi/HyperOS воспроизведены real unsaved incoming, contact permission branches, background/screen-off, cold process после reboot и четыре post-call категории. Эти результаты не экстраполируются на Realme UI или другие physical devices. Exact answer/end timestamps и exact duration не подтверждены.

## M2 evidence status

| Конфигурация | Role/service | Incoming | Fail-open timing | Contacts | Cold process | Статус |
|---|---:|---:|---:|---:|---:|---|
| Pixel_7 Emulator, API 36 | Pass; reactivation needed after `adb install -r` | Pass | Pass, 13.212 ms first / 0.667 ms final | Not tested | Not proven | Partial pass |
| Realme UI | Not tested | Not tested | Not tested | Not tested | Not tested | Pending device |
| Xiaomi 2505DRP06E, HyperOS 2.0, API 35 | Pass; retained after reboot and `adb install -r` | Pass, real unsaved call | Pass, 5.001 ms unlocked / 0.757 ms background-screen-off / 0.280 ms PIN-keyguard / 0.532 ms cold-after-reboot / 0.242 ms cold-after-update / 0.585 ms saved-granted | Pass: denied/revoked bypass; granted callback | Pass after reboot and update | Partial pass |
| Reference physical Android | Not tested | Not tested | Not tested | Not tested | Not tested | Pending device |

## M3 network evidence status

- Pixel_7 API 36 Emulator: client contract/mapping instrumentation pass; это отдельный emulator layer.
- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: 7 instrumentation tests pass через USB ADB reverse и повторный полный pass по LTE через временный path-restricted ngrok HTTPS tunnel; live synthetic no-match достиг локального CRM и настроенной Neon DB.
- Narrow tunnel scope проверен: `/admin` → `404`, caller lookup без token → `401`. Tunnel удалён после теста.
- Xiaomi Wi-Fi был выключен; Wi-Fi/cellular switching и постоянный deployed HTTPS endpoint пока не проверены.
- Realme UI и reference physical Android для M3 не проверены.

## M4 caller-card evidence status

- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: реальный известный клиент успешно идентифицирован через Wi-Fi/ngrok; UI показывает только разрешённое имя и факт match.
- На том же устройстве без Wi-Fi HyperOS переводит default LTE network в `SUSPENDED` на всё время голосового вызова и возвращает `CONNECTED` после завершения. Немедленный online lookup в этот период технически недоступен.
- После явного approval добавлен `ACCESS_NETWORK_STATE`: M4 сохраняет loading state, ждёт validated/non-suspended network до 5 минут и делает один автоматический retry.
- LTE recovery подтверждён реальным звонком: screening response `0.268 ms`, cellular data восстановилась после завершения, retry достиг CRM с HTTP 200 за `1.849 s`, карточка показала безопасный matched state.

## M5 post-call evidence status

- Pixel_7 API 36 Emulator: answered simulated incoming + remote end дал `remote/short`; screening response `4.208 ms`.
- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: четыре реальных входящих сценария дали по порядку `remote/short`, `local/short`, `missed/very short`, `rejected/very short`; Diagnostics count `4`.
- M5 не добавляет permission. Сигнал доступен только API 30+, process-local и не содержит exact timestamps/duration; номер/call handle намеренно игнорируется.
- API 29 fallback, outgoing, rapid/parallel/call waiting, conference, Bluetooth, dual-SIM semantics, Realme UI и reference physical Android не проверены.

## M6 outcome evidence status

- Pixel_7 API 36 Emulator: Room migrations `1→2→3`/`2→3`, DAO и Compose tests pass; notification с test number имел `PUBLIC` visibility, открыл Calls и outcome удалил pending card.
- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: обновления поверх существующей БД прошли без crash; real-call notification, deep-link, outcome, Skip и полный номер на secure lock screen/в Calls подтверждены.
- `POST_NOTIFICATIONS` на текущем Xiaomi granted by call-screening role; denied/manual-request branches должны отдельно проверяться на fleet, где role grant отсутствует.
- CRM display name в notification не был физически подтверждён с permanent endpoint; при недоступной CRM номер остаётся доступен.
- Realme/reference Android, rapid multiple calls, process kill до insert и notification OEM policies остаются pending/M8.

## M7 outbox evidence status

- Xiaomi 2505DRP06E, HyperOS 2.0, API 35: `0.7.0-m7-poc` cold launch, real-data Home и retention Settings smoke pass; targeted Room tests 3/3 pass.
- CRM server create + identical UUID replay подтвердили один receipt и `duplicate: true`; это server evidence, не OEM evidence.
- Xiaomi real-call outcome подтвердил offline `Pending` → online `Synced`; второй queued outcome пережил reboot и синхронизировался после возврата сети.
- Emulator M7 regression, Realme UI и reference physical остаются отдельными pending M8 layers.

## Запись evidence

Для каждого результата: производитель/модель, регион, OS/OEM build, API, security patch, app build, Phone app, permissions/roles, SIM/eSIM, contact state, lock/battery state, steps, actual/expected, timestamps и sanitized artifacts. Матрица версий поддерживается по фактическому парку, а не по предположениям.

## Открытые входные данные

Нужны inventory целевых телефонов, доли моделей/версий, dual-SIM/eSIM, work profile/MDM, default Phone apps, возможность выдачи screening role, политики батареи/автозапуска, upgrade cadence и доступные тестовые устройства/номера. Полный список — в `OPEN_QUESTIONS.md`.
