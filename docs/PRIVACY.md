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
| Call observation | Поддерживаемые платформой события | Точность и набор проверяются PoC |
| Outcome/note | Результат разговора | Справочник и необходимость note открыты |
| Device/account context | Auth, support, audit | Минимизировать; перечень открыт |
| Diagnostics | Устранение неисправностей | Sanitized, явный экспорт, TTL открыт |

Не собирать audio, recording, transcript, SMS или адресную книгу целиком. `READ_CONTACTS` не означает разрешение загружать контакты в CRM; permission требует отдельного approval, purpose и privacy review.

## Экран и уведомления

До решения нельзя показывать PII на lock screen, в notification preview, recent-app snapshot или overlay. Требуются redaction policy, accessibility review и тесты на физическом устройстве.

## Логи и аналитика

Логи структурированы и allowlisted согласно `SECURITY.md`. Запрещены номера, имена, CRM payloads, notes и secrets. Диагностический экспорт создаётся пользователем, показывает состав, не отправляется автоматически и содержит aggregate/state metadata вместо payload.

## Lifecycle данных

Нужно определить collection trigger, local cache TTL, outbox retention после delivery/permanent failure, backup policy, logout wipe, employee departure, remote revoke, legal hold и CRM retention. До утверждения действует принцип минимально необходимого хранения, но численные сроки не выдумываются.

## Права и governance

Нужно определить controller/processor roles, lawful basis, privacy notice, доступ/исправление/удаление, cross-border transfer, DPO/security contacts и incident reporting. Internal-only статус не отменяет эти требования.

## Privacy acceptance до релиза

- утверждён data inventory и purpose для каждого поля;
- утверждены retention/deletion/backup правила;
- проверены lock-screen, notification, screenshot и export сценарии;
- permissions соответствуют минимально необходимым;
- diagnostics проверены на отсутствие PII/secrets;
- согласованы legal basis, notice и support procedure.
