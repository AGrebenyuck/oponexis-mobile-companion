# Device Compatibility

Статус: **DRAFT test matrix; не архитектурная спецификация**

## Цели

- Android Emulator;
- Realme devices / Realme UI;
- Xiaomi devices / HyperOS;
- другие стандартные/reference Android-устройства.

Конкретные physical-device модели, регионы, OS builds, SIM-конфигурации и management policies пока не предоставлены. Проект использует min API 29 и target/compile API 36 по результатам M1.

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
| Answer/end/duration evidence | Исследование | Исследование | Исследование | Исследование |
| Lock screen/card privacy | Да | Да | Да | Да |
| Background, idle, reboot, battery policy | Частично | Да | Да | Да |
| Offline outbox/network switching | Да | Да | Да | Да |
| Update/role retention | Частично | Да | Да | Да |

## Verified facts и assumptions

Официально подтверждено: входящий screening callback требует `respondToCall` в течение 5 секунд; сеть нельзя помещать в критический путь. Без `READ_CONTACTS` service может не получать номера из системных контактов. На Pixel_7 API 36 Emulator role/service binding и simulated unsaved incoming call воспроизведены, allow response занял 13.212 ms в первом записанном прогоне. Это не экстраполируется на physical devices. Точные callbacks жизненного цикла, contact filtering на целевых телефонах, UI timing, фоновые ограничения и настройки автозапуска остаются hypotheses.

## M2 evidence status

| Конфигурация | Role/service | Simulated incoming | Fail-open timing | Contacts | Cold process | Статус |
|---|---:|---:|---:|---:|---:|---|
| Pixel_7 Emulator, API 36 | Pass; reactivation needed after `adb install -r` | Pass | Pass, 13.212 ms first / 0.667 ms final | Not tested | Not proven | Partial pass |
| Realme UI | Not tested | Not tested | Not tested | Not tested | Not tested | Pending device |
| HyperOS | Not tested | Not tested | Not tested | Not tested | Not tested | Pending device |
| Reference physical Android | Not tested | Not tested | Not tested | Not tested | Not tested | Pending device |

## Запись evidence

Для каждого результата: производитель/модель, регион, OS/OEM build, API, security patch, app build, Phone app, permissions/roles, SIM/eSIM, contact state, lock/battery state, steps, actual/expected, timestamps и sanitized artifacts. Матрица версий поддерживается по фактическому парку, а не по предположениям.

## Открытые входные данные

Нужны inventory целевых телефонов, доли моделей/версий, dual-SIM/eSIM, work profile/MDM, default Phone apps, возможность выдачи screening role, политики батареи/автозапуска, upgrade cadence и доступные тестовые устройства/номера. Полный список — в `OPEN_QUESTIONS.md`.
