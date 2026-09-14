# M3 CRM Caller Lookup

Статус: **ACCEPTED 2026-07-23 FOR INTERNAL/NGROK SCOPE — production blockers remain**

## Реализовано

- OPONX CRM: `POST /api/mobile/v1/caller-lookup` с Bearer-auth, bounded request body, CRM phone normalization и минимальным read-only response.
- Android: Retrofit/OkHttp gateway, typed result mapping, matching normalization и bounded timeouts 2 s connect, 5 s read, 3 s write, 6 s total. Total timeout увеличен с 4 s после воспроизводимого Xiaomi LTE/ngrok cold-start timeout; звонок уже разрешён до начала запроса.
- `INTERNET` добавлен после явного approval; новых зависимостей и DB migrations нет.
- Номер передаётся только в JSON body и не логируется. Серверные ошибки содержат correlation ID и safe error code, но не PII.
- `CallScreeningService` не зависит от M3 gateway и не изменён: звонок разрешается до любой будущей сети.

## Локальная конфигурация

- CRM читает `OPONEXIS_MOBILE_API_TOKEN` из игнорируемого `.env`.
- Android debug читает `OPONEXIS_CRM_BASE_URL` и `OPONEXIS_CRM_API_TOKEN` из игнорируемого `local.properties`.
- Release build всегда получает пустой token и запрещает cleartext. PoC-token в debug APK извлекаем и не пригоден для production.
- Без server token endpoint возвращает `503 mobile_api_not_configured`; без Android config gateway возвращает `NotConfigured` без сети.

## Evidence 2026-07-23

- Targeted ESLint новых CRM-файлов: pass.
- OPONX CRM production build: pass; route зарегистрирован как dynamic `/api/mobile/v1/caller-lookup`.
- Local endpoint с настроенной Neon DB: unauthorized `401`, synthetic no-match `200 not_found`, существующий customer `200 matched`; терминальный вывод был redacted до presence checks.
- Android lint, 10 JVM unit tests, Debug APK и Release APK: pass; release BuildConfig содержит пустой CRM token и запрещает cleartext.
- Pixel_7 API 36 Emulator: 6 instrumentation tests прошли как отдельный secondary emulator layer.
- Xiaomi 2505DRP06E / HyperOS 2 / Android 15: 7 instrumentation tests прошли через USB transport, затем полный набор повторно прошёл по LTE через временный ngrok HTTPS tunnel. Live synthetic no-match прошёл по цепочке Android Retrofit → LTE → ngrok → narrow path-only proxy → локальный Next.js CRM → настроенная Neon DB.
- Перед LTE-тестом подтверждено, что tunnel возвращает `404` для `/admin` и `401 unauthorized` для caller lookup без token. После теста tunnel/proxy/test APK удалены, а локальный base URL возвращён в fail-closed `invalid.local`.
- Wi-Fi во время прогона был выключен. Cellular HTTPS pass подтверждён; Wi-Fi/cellular switching и постоянный deployed HTTPS environment остаются незавершёнными.

## Exclusions и blockers

- Gateway пока не вызывается из screening callback и не показывает caller card; presentation относится к M4.
- Нет persistent cache, outbox, событий записи или background worker.
- Local shared token не даёт employee/tenant/record-level authorization. До production нужны identity/SSO, secure provisioning/revoke, rate limiting/enumeration protection, audit и HTTPS environment.
- Emulator network tests, physical Wi-Fi/cellular switching, Realme UI и reference Android не завершены.
