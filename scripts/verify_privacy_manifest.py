#!/usr/bin/env python3
"""Fail closed when a manifest change broadens Echo Inbox's privacy posture."""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"

FORBIDDEN_PERMISSIONS = {
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_PHONE_STATE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.REQUEST_INSTALL_PACKAGES",
}


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def parse(path: Path) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except ET.ParseError as error:
        fail(f"invalid XML in {path.relative_to(ROOT)}: {error}")


def permissions(manifest: ET.Element) -> set[str]:
    return {
        item.attrib.get(ANDROID + "name", "")
        for item in manifest.findall("uses-permission")
    }


def main() -> None:
    app_path = ROOT / "app/src/main/AndroidManifest.xml"
    fixture_path = ROOT / "fixture/src/main/AndroidManifest.xml"
    app = parse(app_path)
    fixture = parse(fixture_path)

    for label, manifest in (("app", app), ("fixture", fixture)):
        found = permissions(manifest)
        forbidden = found & FORBIDDEN_PERMISSIONS
        if forbidden:
            fail(f"{label} requests forbidden permissions: {sorted(forbidden)}")
        unexpected = found - {"android.permission.POST_NOTIFICATIONS"}
        if unexpected:
            fail(f"{label} requests unexpected permissions: {sorted(unexpected)}")
        print(f"PASS: {label} permission allowlist")

    application = app.find("application")
    if application is None:
        fail("app manifest has no application element")
    expected_attributes = {
        "allowBackup": "false",
        "fullBackupContent": "false",
        "usesCleartextTraffic": "false",
    }
    for attribute, expected in expected_attributes.items():
        actual = application.attrib.get(ANDROID + attribute)
        if actual != expected:
            fail(f"application {attribute} must be {expected!r}, got {actual!r}")
        print(f"PASS: application {attribute}={expected}")

    services = application.findall("service")
    listener = next(
        (service for service in services if service.attrib.get(ANDROID + "name") == ".capture.NotificationCaptureService"),
        None,
    )
    if listener is None:
        fail("notification listener service is missing")
    if listener.attrib.get(ANDROID + "permission") != "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE":
        fail("listener service must require BIND_NOTIFICATION_LISTENER_SERVICE")
    actions = {
        action.attrib.get(ANDROID + "name")
        for action in listener.findall("./intent-filter/action")
    }
    if "android.service.notification.NotificationListenerService" not in actions:
        fail("listener service intent action is missing")
    print("PASS: notification listener binding is protected")

    extraction_rules = ROOT / "app/src/main/res/xml/data_extraction_rules.xml"
    rules = parse(extraction_rules)
    excludes = rules.findall(".//exclude")
    excluded_domains = {item.attrib.get("domain") for item in excludes}
    required_domains = {"root", "database", "sharedpref", "file"}
    if not required_domains.issubset(excluded_domains):
        fail(f"backup exclusions are incomplete: {sorted(required_domains - excluded_domains)}")
    print("PASS: local data is excluded from cloud backup and device transfer")

    print("PRIVACY_MANIFEST_PASSED")


if __name__ == "__main__":
    main()
