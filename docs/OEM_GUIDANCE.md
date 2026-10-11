# OEM reliability guidance

Notification Access is a system service, but device vendors may still restrict application processes or listener reconnection. Echo Inbox surfaces connection state instead of promising perfect delivery.

## Universal steps

1. Confirm Android Notification Access is granted.
2. Open Capture Health and confirm the listener is connected.
3. Run the local capture test.
4. Confirm the source app itself is allowed to show notification content.
5. Exclude the source app from lock-screen privacy only when appropriate; Echo Inbox cannot capture text the source app does not expose.
6. Review battery/background restrictions for Echo Inbox when the listener repeatedly disconnects.

## Samsung

- Check **Settings → Battery → Background usage limits**.
- Remove Echo Inbox from Sleeping or Deep sleeping apps when reliability is poor.

## Xiaomi / Redmi / Poco

- Enable autostart where available.
- Set battery use to **No restrictions**.
- Keep Notification Access enabled after system/security app updates.

## Oppo / Realme / OnePlus

- Allow background activity and autostart in App management.
- Review battery optimization after OS updates.

## Vivo / iQOO

- Enable autostart/background power usage.
- Review high-background-power restrictions.

Menu names vary by OS version and region. Do not request unrelated permissions or hide the app to work around vendor behavior.
