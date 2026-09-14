# M9 Release Preparation

Статус: **INPUT PIPELINE READY; PRODUCTION DECISIONS PENDING**

Этот документ описывает, куда подставить production-данные и какие решения ещё нужны. Он не объявляет версию 1.0 готовой: выбранный device-enrollment flow пока не реализован, ключ подписи не предоставлен, владельцы не назначены. Канал распространения принят: signed APK устанавливается вручную на закреплённый за сотрудником рабочий телефон.

## Что уже подготовлено

- `release.properties.example` — шаблон несекретной production-конфигурации.
- `signing.properties.example` — шаблон локальной ссылки на keystore и credentials.
- `verifyM9ReleaseInputs` — fail-closed проверка обязательных входов.
- `prepareM9Release` — единая команда проверки входов, lint, tests и signed release build.
- `.gitignore` и `scripts/check-git-safety.sh` не допускают случайного commit реальных release/signing properties, keystore и APK/AAB.
- Release APK никогда не получает временный M3 Bearer token из `local.properties`.

## 1. Production CRM

Создать локальный `release.properties` из шаблона и заполнить:

```properties
crm.baseUrl=https://<постоянный-production-host>/
crm.authMode=oidc
```

Для текущего release выбран:

- `device_enrollment` — одноразовый activation code из CRM обменивается на уникальный отзывной per-device credential.

`oidc` зарезервирован как возможный будущий вариант при появлении корпоративного identity provider, но не является выбранным M9 flow.

Статический общий Bearer token внутри APK запрещён: его можно извлечь из приложения, невозможно безопасно связать с сотрудником и сложно точечно отозвать. Выбор значения в properties сам по себе не реализует auth. До реализации выбранного flow `verifyM9ReleaseInputs` намеренно завершается ошибкой.

Production CRM должна отдельно предоставить issuer/enrollment URL, client/device registration, scopes, tenant rules, token TTL/refresh/revoke, endpoint compatibility и server-side authorization. Эти значения добавляются только после утверждения контракта и не должны коммититься, если являются секретами.

## Dev на тестовом планшете

DEV получает configuration только из локального `local.properties`:

```properties
OPONEXIS_CRM_BASE_URL=https://<текущий-ngrok-host>/
OPONEXIS_CRM_API_TOKEN=<только-временный-dev-token>
```

Собирать и устанавливать:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

DEV имеет package `com.oponexis.companion.dev`, label `Oponexis Companion DEV` и отдельные локальные данные. Ngrok URL меняется в `local.properties`, затем APK пересобирается. Реальный token нельзя писать в документацию, commit или команду, которая попадёт в общий лог.

Production получает URL только из игнорируемого `release.properties`, не читает debug token и использует package `com.oponexis.companion`.

### Однократный переход со старого PoC

Ранее debug PoC использовал production package `com.oponexis.companion` и debug signing key. Первый настоящий production APK с другим ключом не сможет обновить его поверх. На тестовом планшете старый PoC можно пока оставить рядом с новым `.dev`, но перед первой установкой production на рабочий телефон нужно убедиться, что pending outbox пуст/синхронизирован, удалить старый debug PoC и выполнить clean install подписанного production APK. После этого все следующие production APK обновляются поверх только тем же production key.

## 2. Подпись

Создать локальный `signing.properties` из шаблона. `storeFile` должен указывать на keystore вне репозитория. Пароли не передавать в чат, Git, `release.properties`, логи или APK metadata.

До генерации production key назначить владельца и определить:

- канал хранения и резервную копию;
- круг лиц/CI, имеющих право подписи;
- процедуру ротации, компрометации и восстановления;
- требуется ли Play App Signing для выбранного канала.

Этот репозиторий не генерирует и не принимает на хранение production key без отдельного решения о custody.

## 3. Распространение — принято

В `release.properties` уже выбран текущий канал:

```properties
distribution.channel=managed_google_play
```

`managed_direct` означает подписанный APK, передаваемый владельцем на корпоративный телефон. Каждое обновление должно иметь тот же production signing key и application ID, больший `versionCode`, зафиксированные SHA-256/version/device/date и пройти upgrade smoke без потери данных. Публичная ссылка не является утверждённым каналом.

Автоматическое обновление через интернет пока не добавляется: безопасный self-update — отдельная подсистема с сервером обновлений, проверкой подписи, защищённым download и rollback policy. При одном устройстве ручное обновление APK проще и надёжнее; решение можно пересмотреть при росте fleet.

## 4. Владельцы

В том же локальном файле заполнить адреса или внутренние identifiers:

```properties
release.owner=<кто разрешает выкладку>
support.contact=<куда сотрудник сообщает о проблеме>
privacy.owner=<кто утверждает обработку данных и lock-screen UI>
security.owner=<кто принимает auth, key custody и incident plan>
```

Эти поля не встраиваются в приложение; gate использует их как checklist. Если identifiers конфиденциальны, их следует хранить во внутренней release system, а не коммитить.

## 5. Команда выпуска

После реализации auth и заполнения локальных файлов:

```bash
./scripts/check-git-safety.sh
./gradlew prepareM9Release
```

Затем обязательны signature verification, чистая установка, upgrade с предыдущей принятой версии, critical call/outbox smoke на согласованной physical matrix и rollback drill. Realme UI, reference Android и API 29 пока не проверены и остаются release blockers до решения владельца о фактической support matrix.

## Входы, которые ещё должен предоставить владелец

1. Постоянный production/staging CRM HTTPS base URL и утверждённый API contract.
2. Тип использования планшетов: персональные или общие/управляемые; на его основе выбирается auth flow.
3. Identity provider или device-enrollment contract и тестовая среда.
4. Release, support, privacy и security owners.
5. Финальные versionCode/versionName 1.0, signing-key custody и rollback artifact. Application IDs уже определены ADR 0005.
6. Финальная physical-device support matrix и формальные privacy/security approvals.

## Stop conditions

Релиз запрещён, если используется временный ngrok URL, shared static token, отсутствует/не реализован production auth, нет корректного signing key, не определён distribution/rollback, провалены проверки или отсутствуют обязательные approvals. При провале распространение останавливается; последняя совместимая подписанная версия остаётся rollback candidate.
