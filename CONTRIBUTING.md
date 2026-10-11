# Contributing

1. Read `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, and `docs/THREAT_MODEL.md`.
2. Keep production code local-only and never log notification content.
3. Add a regression test for every parser, lifecycle, diff, retention, or FTS change.
4. Run:

```bash
./scripts/verify_all.sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :fixture:assembleDebug
```

5. For Room schema changes, increment the database version, add an explicit migration, export the schema, and add migration/instrumentation tests.
6. Use fixture data in screenshots, issues, and test reports. Never submit real notification content.
7. Keep commits focused and explain privacy/permission changes prominently in the pull request.
