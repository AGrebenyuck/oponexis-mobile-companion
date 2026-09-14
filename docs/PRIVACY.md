# Privacy

Статус: **DRAFT; требует владельца privacy/legal**

## Принципы

Purpose limitation, data minimization, least privilege, short retention, user-visible diagnostics и отсутствие PII в telemetry по умолчанию.

## Потенциальные категории данных

| Категория | Назначение | Решение/retention |
|---|---|---|
| Номер телефона | CRM lookup и связь события | Формат, законное основание и retention открыты |
| CRM customer reference | Связать карточку/событие | Должен быть opaque; retention открыт |
| Краткая карточка | Контекст сотруднику | Поля, lock-screen policy и cache TTL открыты |
| Call observation | API 30+ disconnect category и coarse duration bucket | M5 process-local PoC; без identity, retention только до смерти процесса |
| Outcome/note | Optional outcome; notes отсутствуют | История 30 дней default или forever по выбору; CRM retention открыт |
| Device/account context | Auth, support, audit | Минимизировать; перечень открыт |
| Diagnostics | Устранение неисправностей | Sanitized, явный экспорт, TTL открыт |

Не собирать audio, recording, transcript, SMS или адресную книгу целиком. Пользователь одобрил `READ_CONTACTS` 2026-07-23 с узкой целью: позволить Android передавать CallScreeningService звонки от сохранённых номеров. M2 не читает Contact Provider и не загружает контакты в CRM. Любое прямое чтение полей контакта, синхронизация или `WRITE_CONTACTS` требует отдельного purpose, approval и privacy review.

M3 PoC передаёт нормализованный номер только в body авторизованного lookup-запроса, не помещает его в URL или логи и не сохраняет persistent cache. Ответ ограничен opaque `customerRef` и nullable `displayName`; обычные логи не содержат ни одного из этих значений.

M4 хранит последнюю карточку только в памяти процесса. UI показывает nullable `displayName` и общий статус match; номер и `customerRef` не отображаются. Состояние исчезает после process death или явного Clear. Карточка не показывается через lock screen, overlay, notification или recent-app export policy.

M5 игнорирует переданный Android call handle и хранит только process-local счётчик, disconnect category и coarse duration bucket. Эти данные не связываются с CRM-клиентом, не помещаются в Room/DataStore/outbox и не экспортируются. Категории не должны интерпретироваться как точное время ответа/завершения или длительность.

После M7 outcome полный номер и optional подтверждённое CRM display name сохраняются в локальной истории на 30 дней по умолчанию либо бессрочно по явному выбору в Settings. При delivery outbox очищает phone/customer payload, но история сохраняется по выбранной policy. Cleanup не удаляет unresolved, undelivered или permanent-failure records. Номер/имя не попадают в structured logs.

## Экран и уведомления

Product owner 2026-07-24 явно одобрил показ полного caller number и подтверждённого CRM display name в M6 notification, включая secure lock screen. Это осознанное исключение из прежнего conservative baseline; оно подтверждено на текущем Xiaomi, но требует formal privacy/security approval до M9. Overlay по-прежнему отсутствует. Recents/screenshot policy и fleet notification settings остаются открытыми.

## Логи и аналитика

Логи структурированы и allowlisted согласно `SECURITY.md`. Запрещены номера, имена, CRM payloads, notes и secrets. Диагностический экспорт создаётся пользователем, показывает состав, не отправляется автоматически и содержит aggregate/state metadata вместо payload.

## Lifecycle данных

Локальная history policy для M7: 30 дней default или forever. Всё ещё нужно определить outbox metadata retention после delivery, permanent-failure remediation, backup, logout/wipe, employee departure, legal hold и CRM retention. Forever является product setting, но требует formal privacy/legal acceptance до M9.

## Права и governance

Нужно определить controller/processor roles, lawful basis, privacy notice, доступ/исправление/удаление, cross-border transfer, DPO/security contacts и incident reporting. Internal-only статус не отменяет эти требования.

## Privacy acceptance до релиза

- утверждён data inventory и purpose для каждого поля;
- утверждены retention/deletion/backup правила;
- проверены lock-screen, notification, screenshot и export сценарии;
- permissions соответствуют минимально необходимым;
- diagnostics проверены на отсутствие PII/secrets;
- согласованы legal basis, notice и support procedure.
