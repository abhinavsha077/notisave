# Threat model

## Scope

The protected asset is notification-derived content stored on a device controlled by the user. The product is not designed for covert monitoring, another person's device, enterprise surveillance, or remote collection.

## Trust boundaries

- Android delivers third-party notification payloads to the listener.
- The app process normalizes and stores selected content.
- Android Keystore protects non-exportable wrapping/HMAC keys.
- SQLCipher protects the database at rest.
- The user may deliberately create a readable export outside the app sandbox.

## Primary threats and mitigations

| Threat | Mitigation | Residual risk |
|---|---|---|
| Another app reads the database | Private app sandbox plus SQLCipher | Rooted/compromised OS can bypass sandbox controls |
| Device backup copies sensitive records | Backup disabled and extraction domains excluded | OEM defects or forensic acquisition may still expose data |
| Notification content leaves device | No Internet permission or network SDK | User-created exports may be copied by the selected provider |
| Screenshots/recent-app previews leak data | `FLAG_SECURE` defaults on | Accessibility services, cameras, or compromised OS remain out of scope |
| App UI opened by another person | Optional biometric/device credential lock | Lock is user-configurable; unlocked device sessions remain trusted |
| Different OTPs collapse after redaction | Keystore HMAC identities | A compromised live process can observe source payloads during capture |
| Reused Android notification key loses data | Key plus original post time lifecycle boundary | Source-app/OEM callback anomalies may still cause gaps |
| Notification update overwrites earlier messages | Immutable snapshots plus ordered LCS diff | Content never exposed by Android cannot be reconstructed |
| Burst causes memory exhaustion | Bounded queue and explicit dropped-event counter | Events beyond capacity are lost and reported |
| Retention ignores user setting at startup | Background jobs await first persisted DataStore snapshot | Corrupt DataStore may prevent work and require recovery |
| FTS rows survive deletion | Explicit row deletion before cascades plus contract tests | Future schema changes need matching maintenance updates |
| Malicious notification payload crashes parser | Primitive copy, defensive extraction, bounded strings from framework, parser error accounting | Extremely large valid payloads can still consume resources |
| Malicious Gradle distribution | Host allowlist, pinned SHA-256, ZIP-slip protection | Build host and upstream checksum source remain trusted |

## Out of scope

- A fully compromised Android kernel or system server.
- Physical extraction from an unlocked rooted device.
- Source applications that intentionally provide false notification content.
- Recovery of end-to-end encrypted source databases.
- Covert deployment or hidden launcher behavior.
- Remote account compromise after the user exports data elsewhere.

## Abuse prevention

The app must not add:

- hidden launcher mode;
- remote upload or remote viewing;
- device-owner enrollment for surveillance;
- attempts to conceal Notification Access;
- “spy,” “stalker,” or deleted-message guarantees;
- automated collection from a device the user is not authorized to manage.
