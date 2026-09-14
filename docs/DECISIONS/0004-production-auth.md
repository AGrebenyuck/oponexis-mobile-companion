# ADR 0004: Production mobile authentication

- **Status:** Accepted for implementation; contract pending
- **Date:** 2026-07-24
- **Decision owner:** Open

## Context

M3 использовал отзывной shared Bearer token только для локального PoC. Он встраивается в debug APK и считается извлекаемым. Для M9 нужна identity, которую можно связать с сотрудником или конкретным управляемым устройством, ограничить scopes/tenant и точечно отозвать.

2026-07-25 владелец подтвердил текущую модель: один корпоративный телефон закрепляется за сотрудником, приложение устанавливается вручную. При отсутствии готового корпоративного IdP минимальным вариантом является уникальная регистрация этого устройства; окончательное принятие требует CRM enrollment/revoke contract.

## Decision

Для текущей модели одного корпоративного телефона, закреплённого за сотрудником, использовать device-enrollment flow:

1. Администратор создаёт в CRM одноразовый ограниченный по времени activation code.
2. Код вводится на телефоне один раз.
3. CRM обменивает его на уникальный отзывной credential конкретного устройства.
4. Приложение использует credential без постоянного повторного входа сотрудника.
5. CRM позволяет отозвать только это устройство при потере, замене или увольнении сотрудника.

OIDC/OAuth 2 Authorization Code + PKCE остаётся возможной будущей заменой, если появится корпоративный identity provider и потребность в интерактивном входе каждого сотрудника.

Shared static token, API key или secret внутри release APK не допускаются. Release build остаётся fail-closed до реализации и проверки выбранного flow.

## Assumptions requiring validation

- существует совместимый identity provider либо CRM может предоставить enrollment service;
- определены employee/device identity, tenant, roles, scopes, refresh/revoke/logout и device replacement;
- выбранный flow совместим с Managed Google Play/MDM и actual fleet policies.

## Consequences

Потребуются утверждённые API changes, security review, runtime credential storage и auth/logout/revoke tests. Выбор может потребовать новую dependency; она добавляется только после отдельного approval и анализа вариантов.

## Alternatives rejected for production

- общий Bearer token в APK — извлекаем и не обеспечивает точечный revoke/audit;
- постоянный URL с credential в query — утечка через логи/прокси и отсутствие нормальной identity;
- ngrok — временный development transport, не production deployment.

## Acceptance before production-ready status

До реализации владелец отдельно утверждает CRM API/schema changes, срок действия activation code и credential, scopes/tenant, rotation/revoke, lost-device response и audit. Contract/security tests доказывают одноразовость/expiry кода, отсутствие shared secret в APK, fail-closed при invalid/revoked credential и корректную изоляцию tenant/roles.

## Rollback

До принятия ADR production release блокируется. После rollout сервер может отозвать конкретную identity/устройство и остановить mobile capability без изменения call fail-open behavior.
