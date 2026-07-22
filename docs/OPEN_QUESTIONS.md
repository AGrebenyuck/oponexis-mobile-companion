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

## Current OpenX CRM

- Где находится актуальная документация, schema/models и владельцы API?
- Существуют ли mobile endpoints, gateway/BFF, sandbox/staging и тестовые tenants/accounts?
- Каков base URL/environment discovery и versioning/deprecation policy?
- Какой auth: OIDC/OAuth2/SSO, PKCE, MFA, client registration, scopes, token/refresh TTL, revoke/logout и device enrollment?
- Как устроены tenant, employee identity, roles и record-level authorization?
- В каком формате CRM хранит телефоны; кто нормализует; какой default region; как обрабатываются extensions, withheld/private, malformed и international numbers?
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
- Одобряет ли пользователь `READ_CONTACTS`? Если да, какова purpose, runtime rationale, privacy notice и denied behavior?
- Допустимы ли и нужны ли notification, foreground service, phone state/call log, overlay или иные роли/permissions? По умолчанию — нет.
- Каковы lock-screen, notification preview, screenshots, recents и clipboard policies?
- Есть ли требования accessibility, локали, font scale, dark mode, tablets/foldables?
- Каковы storage pressure, backup/restore, rooted/unlocked bootloader и device compromise policies?
- Какие устройства доступны разработке/CI/device farm и кто проводит physical acceptance?

## Call behavior PoC

- Принимает ли продукт внутренний target 100 ms для `respondToCall` после завершения physical-device M2 matrix, и какой percentile/sample threshold станет release gate?
- Какие звонки `CallScreeningService` получает на каждом API/Phone app/OEM при saved/unsaved/private numbers?
- Какие роли/permissions/пользовательские шаги нужны и сохраняются ли они после update/reboot?
- Какие сигналы реально и воспроизводимо отражают ringing, answered, rejected, missed, ended и duration?
- Как ведут себя outgoing, second/call waiting, conference, VoIP, Bluetooth, dual-SIM и caller hang-up scenarios?
- Какая confidence semantics приемлема, если точный event недоступен?
- Какой UX surface для карточки разрешён Android/OEM и безопасен на lock screen?

## Data, offline и synchronization

- Какие события enqueue, когда создаётся UUID/callRef и какие поля минимально необходимы?
- Нужна ли строгая ordering между observation и outcome; может ли outcome прийти первым?
- Как долго хранить pending, delivered и permanent-failure rows?
- Каковы retry count/window/backoff/jitter/network/battery constraints?
- Кто и как видит/исправляет/retries permanent failure; можно ли редактировать payload?
- Что делать при logout, смене сотрудника/tenant, revoke, uninstall/reinstall и device replacement?
- Нужны ли encryption at rest, database key lifecycle и backup exclusion?
- Как разрешать clock skew/timezone, server receipt и duplicate после crash-before-ack?
- Нужны ли remote config/feature flags/kill switch и кто ими управляет?
- Какая approved DB schema/migration/rollback policy?

## Security, privacy и operations

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
