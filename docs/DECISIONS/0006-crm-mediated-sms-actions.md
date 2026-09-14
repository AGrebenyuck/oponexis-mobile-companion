# 0006 — CRM-mediated SMS actions

Status: accepted for dev/internal PoC on 2026-07-25.

## Decision

Companion may request a booking-form or custom SMS after a call, but never sends SMS through Android and does not request `SEND_SMS`. The in-app editor loads templates from authenticated CRM endpoints, lets the operator review or edit the final text, and sends through the separately operated SMS Gateway.

The call reference is the idempotency key for one booking-form action; custom messages receive a fresh UUID. A successful action dismisses the pending call outcome. A failed action leaves it available for retry. Incoming SMS replies are recorded but never create or confirm a reservation. This remains dev-only until production device enrollment, permanent HTTPS deployment, authorization scopes and security review are complete.

## Consequences

- SMS templates, public form tokens, reminders and expiry are server-owned.
- A submitted form is the only reservation confirmation; it triggers a thank-you SMS.
- No SMS content or provider credential is stored in the APK.
- No new Android permission or dependency is introduced.
- Notification quick actions use a non-exported receiver and existing Room state.
