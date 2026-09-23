# testing-flujos (MÓVIL)
- Flujos: login, 2FA overlay + polling + deep-link `senaaccess://2fa`, huella registro/login, excusa PIN, guest QR + CameraX/MLKit.
- Dónde: `app/src/test/` (JUnit) + `androidTest/` (Espresso/Compose). Comando: `./gradlew assembleDebug` mínimo; ideal `testDebugUnitTest`.
- Exigir: test de `RolSeguro` (rol inválido no navega) + `VersionGuard` (purga al bump) en cada release.
