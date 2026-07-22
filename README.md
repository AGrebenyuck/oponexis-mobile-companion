# Oponexis Mobile Companion

Внутреннее нативное Android-приложение для сотрудников Oponexis. Планируемая ценность: определить звонящего через OpenX CRM, показать краткую карточку клиента, зафиксировать поддерживаемые события звонка, дать сотруднику выбрать результат разговора и надёжно доставить события в CRM.

## Статус

Завершён **M1 Android application skeleton**. Реализован emulator-validated срез **M2 CallScreening proof of concept**: системная role/setup UX, изолированный fail-open service и privacy-safe локальные метрики времени ответа.

Foundation собран для Android API 29–36. PoC всегда разрешает звонок и не выполняет сеть или запись на диск до ответа Android. CRM, caller card, lifecycle events, блокировка звонков и runtime permissions отсутствуют. M2 остаётся непринятым до physical-device испытаний; evidence приведён в [M2 CallScreening PoC](docs/M2_CALL_SCREENING_POC.md).

## Сборка

Требуются JDK 17 и Android SDK 36.

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Debug APK создаётся в `app/build/outputs/apk/debug/app-debug.apk`.

## Проверка перед Git commit

Локальные SDK paths, IDE/build caches, APK/AAB, signing keys и типовые secret-файлы исключены через `.gitignore`. Перед commit запустите:

```bash
./scripts/check-git-safety.sh
git status --short
```

Проверка завершается ошибкой, если чувствительный файл был принудительно добавлен в Git или в tracked-файлах найден типичный private-key/token pattern. Не используйте `git add -f` для игнорируемых файлов.

## Не входит в продукт

- полноценная CRM;
- замена системному приложению Phone;
- SMS-функциональность;
- SMS Gateway for Android, который остаётся отдельным приложением.

## Документация

- [Product specification](docs/PRODUCT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Draft API contract](docs/API_CONTRACT.md)
- [Security](docs/SECURITY.md)
- [Privacy](docs/PRIVACY.md)
- [Test strategy](docs/TEST_STRATEGY.md)
- [Milestones](docs/MILESTONES.md)
- [Device compatibility](docs/DEVICE_COMPATIBILITY.md)
- [M2 CallScreening PoC evidence](docs/M2_CALL_SCREENING_POC.md)
- [Open questions](docs/OPEN_QUESTIONS.md)
- [Architecture decisions](docs/DECISIONS/README.md)

## Правила участия

Обязательные правила находятся в [AGENTS.md](AGENTS.md). Переход к следующему milestone требует отдельного запроса пользователя.
