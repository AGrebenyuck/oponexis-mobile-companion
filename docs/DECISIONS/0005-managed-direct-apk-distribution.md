# ADR 0005: Managed direct APK distribution

- **Status:** Accepted
- **Date:** 2026-07-25
- **Decision owner:** Product owner

## Context

Oponexis Mobile Companion будет внутренним приложением на одном корпоративном телефоне, закреплённом за сотрудником. Managed Google Play и MDM сейчас не используются. Владелец будет передавать и устанавливать APK вручную.

## Decision

- Production распространяется как подписанный release APK через контролируемый внутренний канал.
- Обновление устанавливается поверх существующего приложения и обязано иметь тот же `applicationId`, тот же signing key и больший `versionCode`.
- Dev и production разделены: debug использует `com.oponexis.companion.dev` и label `Oponexis Companion DEV`; production сохраняет `com.oponexis.companion` и label `Oponexis Companion`.
- Production APK не публикуется по общедоступной ссылке. Перед передачей владелец сверяет SHA-256, версию и signer certificate fingerprint.
- Автоматическое self-update/download в текущий scope не входит. Оно потребует отдельного защищённого update service, проверки подписи, UX и security approval.

## Verified behavior

Android build variants могут иметь разные application IDs и устанавливаться параллельно. Android принимает обновление существующего package только при совместимой подписи и версии; фактический upgrade/rollback должен быть проверен подписанными M9 artifacts на физическом устройстве.

## Consequences

- Тестовый планшет может одновременно содержать DEV и production без смешивания БД/DataStore/permissions.
- После перехода существующего несuffixed debug PoC на `.dev` Android увидит DEV как новое приложение: onboarding, Call Screening role и runtime permissions придётся настроить один раз заново.
- Старый debug-signed PoC с package `com.oponexis.companion` нельзя обновить будущим production-signed APK. Перед первой production установкой его удаляют после проверки/синхронизации pending outbox; последующие production updates используют один стабильный key.
- Потеря production signing key лишит возможности выпускать обычные обновления того же package.
- Ручной канал требует реестра версии/устройства, защищённого места выдачи APK и понятной процедуры отката.

## Release checklist

1. Увеличить `versionCode` и назначить утверждённый `versionName`.
2. Выполнить `./gradlew prepareM9Release`.
3. Проверить signer fingerprint и SHA-256 APK.
4. Проверить clean install, затем update поверх предыдущей production-версии без потери Room/DataStore/outbox.
5. Выполнить critical call/notification/offline-sync smoke.
6. Зафиксировать устройство, установленную версию, hash, дату и ответственного.

## Rollback

Обычный downgrade APK может быть заблокирован Android и несовместим с Room schema. Rollback выполняется только заранее подготовленным, совместимым и подписанным artifact/process; иначе распространение останавливается, а server-side mobile capability отключается без удаления pending outbox.
