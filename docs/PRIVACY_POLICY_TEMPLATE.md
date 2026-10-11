# Privacy policy template

> Replace every bracketed field before publication. This template must be reviewed against the final merged manifest, dependencies, distribution channel, support process, and applicable law.

**Effective date:** [DATE]

**Developer:** [LEGAL NAME]

**Contact:** [PRIVACY EMAIL]

## Overview

Echo Inbox saves notification content from applications that the user selects on the user's Android device. The purpose is to provide a private, searchable notification archive and reconstructed notification timelines.

## Notification access

When the user grants Android Notification Access, Echo Inbox may receive notification titles, text, sender or conversation labels, timestamps, application identifiers, grouping information, and other content exposed by the posting application. Notifications may include private messages, emails, verification codes, financial alerts, health information, and other sensitive data.

Echo Inbox cannot read a source application's encrypted message database and cannot recover content that was never exposed through an Android notification.

## Storage and processing

Captured data is processed on the device and stored in an encrypted local database. The application does not require an account and the release described by this policy does not declare Android Internet permission. Captured notification content is not uploaded by Echo Inbox.

## Application selection and controls

The user chooses which applications are captured. The user may pause all capture, disable individual applications, set retention, delete application or conversation data, or delete all captured data.

## Verification-code redaction

When enabled, Echo Inbox replaces qualifying numeric verification codes in readable stored text. An opaque device-keyed identity may be retained inside the encrypted database to distinguish different redacted notifications. The original code is not included in exports by that mechanism.

## Export

The user may create a readable JSON export through Android's document picker. Exports are not encrypted by Echo Inbox and are stored in the destination chosen by the user. After export, the selected storage provider and any service to which the user copies the file govern that copy.

## Backups

Echo Inbox disables Android backup and excludes application files, preferences, and databases from configured cloud backup and device-transfer rules.

## Permissions

- **Notification Access:** required for the core archive feature.
- **Post notifications:** used only to post a user-triggered local Capture Health test on Android versions that require this permission.

[LIST ANY FINAL ADDITIONAL PERMISSIONS, OR STATE THAT THERE ARE NONE.]

## Retention

The default retention period is [DEFAULT]. The user can choose another available period or keep records until manual deletion. Android or device-level data clearing and app uninstallation may remove all local data.

## Third parties

[STATE FINAL SDK/SERVICE LIST. FOR THE LOCAL-ONLY BUILD: “Echo Inbox includes no advertising, analytics, cloud-sync, or remote crash-reporting service.”]

## Children's privacy

[INSERT AGE/REGION-APPROPRIATE LANGUAGE.]

## Security

Echo Inbox uses Android application sandboxing, SQLCipher database encryption, Android Keystore-protected keys, optional device authentication, and optional screenshot blocking. No security control can guarantee protection on a rooted, compromised, or unlocked device.

## Changes

Material changes will be reflected in an updated policy and, where required, a new in-app disclosure or consent flow.

## Contact

Questions or requests: [PRIVACY EMAIL / ADDRESS]
