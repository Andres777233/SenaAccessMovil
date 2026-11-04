# Firma de la app (release)

Este proyecto se firma con **una sola llave** compartida por los dos
repositorios (`SenaAccessMovil` y `SenaAccessMovilDef1`), de modo que un APK
compilado en un equipo se instala encima del compilado en otro sin desinstalar.

## Qué NO está en el repositorio (y por qué)

| Archivo | Se versiona | Motivo |
|---|---|---|
| `app/release.keystore` | No | Es la llave privada de distribución |
| `app/keystore.properties` | No | Contiene la contraseña en texto plano |
| `app/debug.keystore` | No | Llave de debug compartida (no secreta, se distribuye aparte) |

`app/build.gradle.kts` ya está preparado: si encuentra `app/debug.keystore`
firma el debug con ella, y si encuentra `keystore.properties` + la llave
referenciada, firma el release de verdad. Sin las llaves el build sigue
funcionando, pero el APK de release sale **sin firmar**.

## Cómo compilar firmado en un clon nuevo

1. Consigue el kit de firma (fuera de git) y ejecuta su instalador:
   ```bash
   ./instalar-llave.sh /ruta/al/clon
   ```
   Copia a `app/`: `release.keystore`, `debug.keystore` y `keystore.properties`.
2. Compila:
   ```bash
   ./gradlew assembleRelease bundleRelease
   ```
3. Verifica que la firma es la correcta:
   ```bash
   $ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
     app/build/outputs/apk/release/app-release.apk
   ```

## Huellas de referencia

| Llave | Alias | SHA-256 |
|---|---|---|
| release | `senaaccess` | `A8:2D:32:0D:1F:94:5B:57:15:6B:B7:F3:5E:FA:BC:99:EF:B8:FC:5D:6A:5F:B4:CC:B9:DA:82:A5:83:9B:4E:B7` |
| debug compartida | `androiddebugkey` | `BA:48:F0:B5:2D:5B:D0:04:CE:74:3E:75:77:60:0D:01:86:AC:53:51:E3:0C:63:76:CD:65:E5:B2:DC:33:21:89` |

Si el SHA-256 del APK no coincide con el de la tabla, se firmó con otra llave:
ese APK **no** actualizará la app instalada.

## Reglas

- Release y debug son firmas distintas: un APK de release no se instala encima
  de uno de debug (ni al revés). Para probar sobre una instalación existente,
  usa el mismo tipo de build o desinstala antes.
- Si se pierde `release.keystore` no hay forma de volver a actualizar la app
  instalada: habría que desinstalarla y reinstalar.
- Nunca subas llaves ni `keystore.properties` al repositorio.
