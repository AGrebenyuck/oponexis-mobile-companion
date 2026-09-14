# Open Questions

Статус: **BLOCKING INPUT REGISTER**

Не придумывать ответы. Владелец и deadline для каждого вопроса назначаются до milestone, которому он нужен.

## Product и users

- Кто product owner и кто принимает каждый milestone?
- Какие группы сотрудников используют приложение, в каких странах/языках и на личных или корпоративных телефонах?
- Входящие и/или исходящие звонки входят в scope? Какие типы линий: SIM, eSIM, VoIP, work profile?
- Каков точный definition of caller match и что делать при нескольких клиентах на один номер?
- Какие поля обязательны в краткой карточке, кто имеет право их видеть и допустим ли показ на lock screen?
- Когда и где должна появляться карточка, сколько она живёт, что показывать при late lookup/stale cache/error?
- Каков утверждённый справочник outcomes, локализация, обязательность, edit window и допустима ли заметка?
- Какие call events действительно нужны бизнесу и какая точность/полнота приемлема?
- Нужны ли исходящие звонки, missed/rejected/blocked calls или только входящие?
- Какие SLO/KPI и release blockers: deadline budget, card latency, delivery latency, error/duplicate rate, crash/ANR-free?

## Current OPONX CRM

Проверено 2026-07-23 по локальному репозиторию `/Users/mac/oponexis devide/oponx-crm`: Next.js/Prisma, `Customer.phone @unique`, текущая нормализация номера и отсутствие прежнего mobile lookup endpoint. Для M3 PoC добавлен `/api/mobile/v1/caller-lookup`; это не отвечает на production-вопросы ниже.

- Кто владелец CRM API и где должна поддерживаться его актуальная документация?
- Какие production/staging URL, sandbox и тестовые tenants/accounts использовать для mobile endpoint?
- Каков base URL/environment discovery и versioning/deprecation policy?
- Какой auth: OIDC/OAuth2/SSO, PKCE, MFA, client registration, scopes, token/refresh TTL, revoke/logout и device enrollment?
- Как устроены tenant, employee identity, roles и record-level authorization?
- Текущая CRM нормализует 9 цифр как `+48` и хранит `Customer.phone` уникально; нужно ли утвердить это как production policy и как обрабатывать extensions, withheld/private, malformed и international numbers?
- Разрешён ли lookup по полному номеру; есть ли защита от enumeration, audit и rate limits?
- Какие существующие модели customer/contact/lead/company и стабильные opaque IDs доступны?
- Какие поля карточки разрешены мобильному клиенту и какие могут быть null/stale/localized?
- Как CRM представляет no match, multiple match, merged/deleted/blocked/restricted record?
- Какие call/event/outcome models уже существуют и как они связаны с customer/user/device/call?
- Какие event types и timestamp semantics принимает CRM? Нужен ли source/confidence?
- Есть ли server-side idempotency; какой key, storage window, duplicate response и UUID-conflict behavior?
- Можно ли принимать batch; каковы payload/rate limits, timeouts и ordering expectations?
- Каковы error schemas/status codes, transient/permanent classification и `Retry-After` semantics?
- Есть ли справочник outcomes endpoint; как версионируются/локализуются/деактивируются codes?
- Допустимы ли notes; ограничения длины/формата, retention, access и audit?
- Каковы environments, certificates, network/VPN requirements, DNS/proxy and certificate pinning policy?
- Каковы audit, retention, deletion, legal hold, residency and incident obligations?
- Требуются ли device/app attestation, MDM certificate или minimum app version/kill switch?
- Какие correlation/tracing fields разрешены и как support находит server receipt без PII?
- Как обеспечивается backward compatibility во время mobile rollout и CRM deploy?
- Кто утверждает DRAFT `API_CONTRACT.md` и как выполняются contract tests?

## Android platform и fleet

- Подтверждаем ли min API 29? Каковы target API policy и список поддерживаемых Android versions?
- Какие точные manufacturer/model/region/OS/OEM build/security patch/Phone app составляют fleet и их доли?
- Какие конкретные Realme UI и HyperOS версии обязательны?
- Есть ли reference Android/Pixel/Samsung/другие устройства и минимальная тестовая матрица?
- Single SIM, dual SIM, eSIM, Wi-Fi Calling, VoLTE, Bluetooth/headset и call waiting — что поддерживается?
- Какое системное Phone app/default dialer используется; может ли employee назначить call screening role?
- Есть ли work profile, fully managed device, MDM, kiosk, VPN, private DNS, proxy или ограничения установки?
- Разрешены ли sideload/internal store/Managed Google Play и какой update cadence?
- Какие OEM battery/autostart/background настройки применяются и может ли MDM их задавать?
- Должно ли приложение работать после reboot до unlock?
- Как тестировать входящие звонки: SIMs, номера, тарифы, лаборатория и согласие владельцев?
- Есть ли системные контакты у сотрудников и допустимо ли отсутствие screening для них?
- Нужен ли когда-либо прямой доступ к полям Contact Provider или изменение контактов? Для M2 ответ не предполагается: одобренный `READ_CONTACTS` используется только для доставки Android callbacks; `WRITE_CONTACTS` не одобрен.
- M6 notification и `POST_NOTIFICATIONS` одобрены; нужны ли когда-либо foreground service, phone state/call log, overlay или иные роли/permissions? По умолчанию для них — нет.
- M6 product owner одобрил полный caller identity в secure-lock notification; подтвердят ли это formal privacy/security owners и каковы остальные screenshot, recents и clipboard policies?
- Есть ли требования accessibility, локали, font scale, dark mode, tablets/foldables?
- Каковы storage pressure, backup/restore, rooted/unlocked bootloader и device compromise policies?
- Какие устройства доступны разработке/CI/device farm и кто проводит physical acceptance?

## Call behavior PoC

Проверено 2026-07-23: `ACTION_POST_CALL` на API 30+ воспроизводимо дал coarse disconnect category/duration bucket на Pixel_7 API 36 Emulator и Xiaomi/HyperOS API 35. На Xiaomi четыре входящих сценария совпали с remote/local/missed/rejected. Это не отвечает на вопросы о точных timestamps/duration, API 29, других OEM и сложных конфигурациях ниже.

- Принимает ли продукт внутренний target 100 ms для `respondToCall` после завершения physical-device M2 matrix, и какой percentile/sample threshold станет release gate?
- Какие звонки `CallScreeningService` получает на каждом API/Phone app/OEM при saved/unsaved/private numbers?
- Какие роли/permissions/пользовательские шаги нужны и сохраняются ли они после update/reboot?
- Какой API 29 fallback допустим и нужны ли вообще точные ringing/answered/ended timestamps или достаточно подтверждённых post-call категорий?
- Принимает ли product/security post-call category как достаточный источник после решения риска spoofed explicit intent?
- Как ведут себя outgoing, second/call waiting, conference, VoIP, Bluetooth, dual-SIM и caller hang-up scenarios?
- Какая confidence semantics приемлема, если точный event недоступен?
- Какой UX surface для карточки разрешён Android/OEM и безопасен на lock screen?

## Data, offline и synchronization

M7 local decision 2026-07-24: outcome optional; approved codes поддержаны DRAFT CRM endpoint; notes/editing отсутствуют. Stable UUID/outbox, 10-attempt bound и history 30 days default/forever реализованы. Production governance ниже остаётся открытым.

- Какие события enqueue, когда создаётся UUID/callRef и какие поля минимально необходимы?
- Как production OPONX CRM будет версионировать/локализовать текущие DRAFT outcome codes?
- Нужно ли в M7 разрешить edit уже выбранного outcome, какой edit window и как отправлять исправление идемпотентно?
- Подтвердят ли privacy/legal выбор 30 дней default или forever и нужна ли кнопка немедленной очистки?
- Нужна ли строгая ordering между observation и outcome; может ли outcome прийти первым?
- Как долго хранить pending, delivered и permanent-failure rows?
- Достаточны ли текущие 10 attempts/WorkManager exponential backoff и нужен ли `Retry-After`/jitter policy?
- Кто и как видит/исправляет/retries permanent failure; можно ли редактировать payload?
- Что делать при logout, смене сотрудника/tenant, revoke, uninstall/reinstall и device replacement?
- Нужны ли encryption at rest, database key lifecycle и backup exclusion?
- Как разрешать clock skew/timezone, server receipt и duplicate после crash-before-ack?
- Нужны ли remote config/feature flags/kill switch и кто ими управляет?
- Какова production migration/rollback policy после одобренной PoC Room v4/CRM additive schema?

## Security, privacy и operations

### M9 inputs — нужны до production build

- Device model и auth direction подтверждены 2026-07-25: один корпоративный телефон закрепляется за сотрудником; CRM выдаёт одноразовый activation code и обменивает его на отзывной per-device credential. Открыты точный API/schema contract, TTL, scopes, rotation/revoke и recovery.
- Каковы постоянные production и staging CRM HTTPS base URLs? Временный ngrok URL не принимается M9 gate.
- Какой IdP/enrollment service, issuer/registration endpoints, scopes, tenant claims, TTL/refresh/revoke/logout и lost-device flow утверждены?
- Канал принят 2026-07-25: `managed_direct`, подписанный APK устанавливается владельцем на один корпоративный телефон. Ещё нужны защищённое место передачи, реестр hash/version/device и rollback procedure.
- Кто назначен `release.owner`, `support.contact`, `privacy.owner` и `security.owner`?
- Кто владеет production signing key, где backup, кто/какой CI подписывает и применяется ли Play App Signing?
- Application IDs приняты ADR 0005: `com.oponexis.companion` production и `.dev` debug. Какой финальный versionCode/versionName назначить 1.0?
- Какая физическая support matrix обязательна для 1.0: Xiaomi/HyperOS, Realme UI, reference Android и API 29; допускаются ли явно исключённые устройства/версии?
- Кто формально утверждает показ полного номера/имени на secure lock screen, retention `forever`, permissions и production diagnostic policy?
- Где хранить подписанный rollback artifact, release notes, SBOM/provenance и кто проводит rollback drill?

- Кто security/privacy/legal owner и каково lawful basis обработки call/customer data?
- Нужны ли DPIA/privacy notice/employee policy/consent и в каких юрисдикциях?
- Каковы точные retention/deletion/access/correction/legal hold/data residency rules?
- Какие identity, credential storage, MFA, revoke and compromised-device controls обязательны?
- Разрешены ли crash reporting/analytics; какой vendor/data region и какие allowlisted fields?
- Можно ли использовать keyed digest номера для диагностики, кем управляется ключ и TTL?
- Каков diagnostic export format, TTL, encryption, destination, consent and support workflow?
- Кто получает permanent-failure/incident alerts без PII?
- Каковы signing key custody, CI secrets, SBOM, vulnerability SLA и dependency approval procedure?
- Каковы incident response, breach notification, server kill switch and support escalation owners?
- Какой internal distribution, staged rollout, minimum version и signed rollback process?

## Engineering governance

- Где будет CI/repository/issue tracker и какие branch/review/release rules?
- Какой application ID, namespace, app label, branding/assets и licensing policy?
- Кто одобряет permissions, dependencies, API changes, DB migrations и architecture changes?
- Какие quality gates, coverage expectations, static analysis and formatting tools утверждены?
- Как фиксировать device evidence и кто принимает известные ограничения?
- Где находится полный внешний `AGENTS.md`, заменённый плейсхолдером в исходном запросе, и нужно ли объединить его с текущим файлом?
