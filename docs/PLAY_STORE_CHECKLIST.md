# Play Store checklist

## Product claims

- Describe the product as a notification archive.
- State that only content exposed through Android notifications can be captured.
- Do not claim access to WhatsApp/Instagram databases or guaranteed deleted-message recovery.
- Do not market covert monitoring, spying, or use on another person's device.

## Disclosure and consent

- Keep the prominent in-app Notification Access disclosure before opening system settings.
- Explain sensitive categories: messages, email, OTPs, financial and health alerts.
- Require affirmative continuation.
- Preserve app selection and sensitive-app warnings.

## Manifest and SDK review

- Inspect the **release merged manifest**.
- Confirm no `INTERNET`, `QUERY_ALL_PACKAGES`, accessibility, SMS, contacts, call-log, or broad-storage permission.
- Confirm no transitive ad/analytics/network SDK.
- Confirm target SDK and Play deadline requirements at submission time.
- Run Play pre-launch report on the release candidate.

## Data Safety

For the local-only build, validate the final form against actual behavior. Developer declarations must reflect any future SDK, export, support, telemetry, or network change. Do not copy declarations from this document without review.

## Store assets

- Screenshots must contain fixture/demo data only.
- Privacy policy URL must be public and match the released build.
- Support email and data-deletion instructions must be functional.
- Explain that app uninstall or Privacy Center deletes local data.

## Release evidence

- Full unit/lint/build passes.
- Instrumentation tests pass on API 26 and API 36.
- Physical OEM matrix reviewed.
- R8 release tested.
- No sensitive content in logs or crash output.
- Security/privacy docs reviewed by the release owner.
