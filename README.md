# Oponexis Mobile Companion

Внутреннее нативное Android-приложение для сотрудников Oponexis. Планируемая ценность: определить звонящего через OPONX CRM, показать краткую карточку клиента, зафиксировать поддерживаемые события звонка, дать сотруднику выбрать результат разговора и надёжно доставить события в CRM.

## Статус

Завершены **M1–M8** для доступной матрицы: Pixel 7 API 36, Generic low-end API 36.1 и Xiaomi/HyperOS API 35. Стандартный AVD не эмулирует Realme UI, поэтому Realme/reference physical и API 29 остаются неподтверждёнными. **M9 preparation начат:** подготовлены локальные input templates и fail-closed release gate; production auth/deployment, signing/distribution и formal privacy/security acceptance остаются release-блокерами.

Foundation собран для Android API 29–36. Приложение всегда разрешает звонок до любой сети или записи на диск. На API 30+ outcome атомарно создаёт stable-UUID outbox event, WorkManager доставляет его идемпотентному CRM endpoint. История хранится 30 дней по умолчанию или бессрочно по выбору в Settings; pending/permanent-failure записи сроком не удаляются. Одобрены runtime `READ_CONTACTS`/API 33+ `POST_NOTIFICATIONS` и install-time `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`.

## Сборка

Требуются JDK 17 и Android SDK 36.

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Debug APK создаётся в `app/build/outputs/apk/debug/app-debug.apk`.

Для M9 сначала заполните локальные копии `release.properties.example` и `signing.properties.example`. Полная инструкция для DEV/ngrok, production CRM, подписи и ручной установки: [M9 release preparation](docs/M9_RELEASE_PREPARATION.md). Production release не собирается из временного ngrok URL или общего токена.

Debug устанавливается как отдельное `com.oponexis.companion.dev` с названием **Oponexis Companion DEV**. Production сохраняет `com.oponexis.companion`; подписанные обновления устанавливаются поверх него тем же ключом и с увеличенным `versionCode`.

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
- [M3 CRM caller lookup evidence](docs/M3_CRM_CALLER_LOOKUP.md)
- [M4 Caller card evidence](docs/M4_CALLER_CARD.md)
- [M5 call lifecycle evidence](docs/M5_CALL_LIFECYCLE_POC.md)
- [M6 call outcome evidence](docs/M6_CALL_OUTCOME.md)
- [M7 offline outbox evidence](docs/M7_OFFLINE_OUTBOX.md)
- [M8 stabilization evidence](docs/M8_STABILIZATION.md)
- [M9 release preparation](docs/M9_RELEASE_PREPARATION.md)
- [Open questions](docs/OPEN_QUESTIONS.md)
- [Architecture decisions](docs/DECISIONS/README.md)

## Правила участия

Обязательные правила находятся в [AGENTS.md](AGENTS.md). Переход к следующему milestone требует отдельного запроса пользователя.
