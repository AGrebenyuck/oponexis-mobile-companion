# Changelog

Значимые изменения проекта документируются в этом файле. Формат основан на Keep a Changelog; до первого релиза используется секция `Unreleased`.

## [Unreleased]

### Added

- Документационный baseline для milestone M0.
- Product, architecture, draft API, security, privacy, testing, compatibility и milestone specifications.
- ADR для native Android, CallScreening-first и offline outbox.
- Реестр открытых вопросов для OpenX CRM и целевых устройств.
- Android application skeleton для milestone M1 на Kotlin и Jetpack Compose.
- Material 3 light/dark design system, Oponexis branding и adaptive launcher icon.
- Splash, onboarding, dashboard, calls, diagnostics и settings screens с нижней навигацией.
- Hilt foundation, mock repositories, DataStore preferences, Room/Retrofit/WorkManager integration points и test harness.
- M2 CallScreening proof of concept с системной role/setup UX и fail-open `CallScreeningService`.
- Privacy-safe process-local counters и timing для screening callbacks без номера телефона или contact/CRM данных.
- JVM-тесты метрик response budget и emulator evidence для simulated incoming call.

### Changed

- Зафиксированы совместимые M1 build/dependency versions для API 29–36 и JDK 17.
- В интерфейсе и launcher icon применён предоставленный фирменный логотип Oponexis.
- В Diagnostics добавлены состояние Call Screening role и результаты M2 PoC; версия build обновлена до `0.2.0-m2`.
