# ADR 0007: Event-driven SMS activity through FCM

Status: Accepted and implemented, 2026-08-15

## Context

Companion must show actual manual and automatic SMS delivery activity and alert the work phone when a Gateway message remains queued for four minutes. A frequent heartbeat or status poll would consume phone, Vercel and Neon resources while still not representing an individual SMS result.

## Decision

- Use Firebase Cloud Messaging with the current Firebase Installation ID API, not deprecated registration tokens.
- Register only the approved `com.oponexis.companion` and `com.oponexis.companion.dev` applications.
- Store enabled FIDs server-side and send privacy-safe data messages after durable SMS contact events.
- Fetch PII-bearing activity only through the authenticated mobile API.
- Persist recent activity in Room and show the latest status per provider message.
- Replace the legacy five-second delivery polling loop with event-driven updates and one four-minute WorkManager verification.

## Consequences

Firebase Messaging and Google Services are new approved Android dependencies. Firebase Admin credentials remain server-only Vercel sensitive variables. FCM failure is fail-open for SMS sending: a push problem cannot make an accepted Gateway send appear failed to the caller. If push delivery is delayed, app startup sync and the four-minute verification reconcile state.

## Validation

Required checks are Android unit tests, lint, debug assembly, Next.js lint/build, a production registration round trip, an FCM delivery event, a four-minute queued alert, and an in-place update on the managed Realme phone.
