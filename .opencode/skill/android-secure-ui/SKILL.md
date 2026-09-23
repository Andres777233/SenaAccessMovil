# android-secure-ui
- UI: `ui/ds/` (`SenaComponents`, `SenaTokens`), `verdeMarca()` para contraste en claro, píldora 28dp, touch 48dp, `SenaSearchField`/`SenaBadge`/`SenaCell`.
- Seguridad: `RolSeguro` (lista cerrada, resto inválido sin navegar), `VersionGuard` (bump purga sesión), `device_id` en login, pass min 8, foto JPEG max 1024px.
- Red: solo Railway HTTPS, `network_security_config` sin pin-set corrupto, BODY log solo DEBUG.
- Convenciones: `CargaUiState`, comentarios solo `//`, foto persistente vía SessionManager.
