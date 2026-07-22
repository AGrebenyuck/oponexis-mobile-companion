# ADR-0001: Native Android application

- Status: Accepted; foundation implemented in M1
- Date: 2026-07-22

## Context

Продукту нужна тесная интеграция с Android telephony/call-screening lifecycle, background reliability, local storage и системными roles/permissions. Он внутренний, Android-only и не должен заменять Phone или включать SMS.

## Decision

Строить Oponexis Mobile Companion как нативное Android-приложение. Планируемый UI — Jetpack Compose/Material 3; Kotlin и перечисленный в документации Android stack анализируются и версионируются отдельно в M1. Решение не разрешает сейчас создавать проект или добавлять зависимости.

Realme UI, HyperOS и reference Android являются targets совместимости и тестов, а не отдельными архитектурами.

## Verified facts и assumptions

Факт проекта: целевая платформа Android и требуется call screening investigation. Assumption: предполагаемый min API 29 достаточен; это проверяется до M1. Возможности конкретных OEM, lifecycle events и UI surface не считаются доказанными.

## Consequences

- Можно использовать Android-native roles/services, lifecycle и offline scheduling.
- Нужны отдельные emulator и physical-device layers.
- Permission/API fragmentation и OEM behavior становятся ключевыми рисками.
- iOS/web/cross-platform клиенты не входят в решение.

## Alternatives

Cross-platform shell и web/PWA отклонены для текущего направления из-за системной интеграции и отсутствия требования multi-platform; пересмотр требует нового ADR и approval.

## Validation и rollback

M1 подтверждает API/stack/build, M2 — screening feasibility. Если нативный Android не обеспечивает обязательный сценарий в поддерживаемых рамках, остановить feature/project direction и создать superseding ADR; не поддерживать параллельный экспериментальный stack после решения.
