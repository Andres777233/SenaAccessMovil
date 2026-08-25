# AGENTS.md — Movilvistas ("SenaAccess MÓVIL")

App Android (Kotlin + Jetpack Compose, estilo glass verde) de SennAccess. Ruta: `/home/andres/Escritorio/VistasAndroidDef1-main./VistasAndroidDef1-main` (la carpeta padre termina en punto; citar siempre entre comillas).
No es repo git local; el código está en `git@github.com:Andres777233/VistasAndroidDef1.git` (rama `main`). Para push: clonar → superponer código → commitear → pushear.

## Comandos y entorno
- Compilar: `./gradlew assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk`.
- Instalar (moto g86): `/home/andres/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`. El `adb reverse tcp:8000` lo aplica solo el watcher (≤3 s tras conectar USB).
- Backend: servicio systemd de usuario `sennaccess` (`systemctl --user status|restart sennaccess`); el watcher lo relanza si muere.
- Android Studio Flatpak: si el sync falla con "initialization script ... does not exist", ejecutar `./gradlew --stop` antes.
- Credenciales (BD Railway + local, ver seeder del WEB): `admin@sena.edu.co` con `12345678`; instructores: `juan.pablo@sena.edu.co` (`12345678`), `alejandro/gustavo/raul@sena.edu.co` (`123456`); aprendices: `andres.vargas/laura.medina@sena.edu.co` (`12345678`), `katherin/sebastian/camilo@sena.edu.co` (`123456`). NO existen `instructor@` ni `aprendiz@sena.edu.co`.

## Convenciones
- API: `RetrofitClient.conServicio{}` intenta USB (127.0.0.1:8000) y fallback WiFi (`BASE_URL_WIFI`, DHCP — actualizar si cambia); timeout 3 s.
- Patrón `CargaUiState` (Loading/Success/Error); mocks SOLO en `data/mock/MockData.kt` y solo si `SessionManager.token == null`.
- Paquetes: dashboards en `com.example.sennaccess.Aprendiz` (comparten StatCard/TableContainer), ViewModels en `ui`, repos en `data`.
- Comentarios nuevos solo `//` en línea propia (sin asteriscos/emojis/cajas); cabecera 2-4 líneas `//` por archivo.
- Biometría LOCAL (`data/HuellaCredentialStore.kt`, Keystore AES256-GCM); el WebAuthn del backend quedó sin uso desde el móvil.

## Retomar trabajo (IMPORTANTE)
- Este proyecto = **"SenaAccess MÓVIL"**.
- Palabra clave: **"continuemos con Sena access movil"** (o "continuemos"/"retomemos"/"seguimos"). En ese caso NO leas archivos extra y NO re-explores el repo con grep/glob — el estado completo y todo el historial están AQUÍ MISMO (sección `## Historial`, entradas más recientes al final).

## Autoguardar historial (OBLIGATORIO)
Al terminar cada tarea/sesión, agrega al FINAL de `## Historial` una entrada fechada `- YYYY-MM-DD — <qué cambiaste y por qué>`: MUY concreta, directa y clara (1-2 líneas máximo). Nunca borres ni reescribas entradas anteriores.

## Historial
- 2026-08-22 — Paridad con el backend + refresco: quité de la app REPORTES/ASIGNACIONES/MENSAJE, arreglé dock EQUIPOS del admin (abre inventario); refetch automático al cambiar/volver de pestaña. APK compilado PENDIENTE de instalar en el moto g86.
- 2026-08-22 — USB automático permanente: servicio systemd user `sennaccess` + watcher cada 3 s (levanta artisan serve y aplica adb reverse solo con dispositivo autorizado); enable-linger activo; `iniciar-servidor.sh` quedó solo-estado.
- 2026-08-22 — Avatar/foto perfil: `ui/AvatarPerfil.kt` (Coil, rutas relativas vía `SessionManager.fotoUrl()`), CAMBIAR FOTO multipart (`updateMyProfileWithPhoto`, _method=PUT+image); admin solo muestra avatar.
- 2026-08-20 — Biometría LOCAL (reemplaza WebAuthn/passkeys): huella descifra credenciales del dispositivo (Keystore AES256-GCM, SharedPreferences `huella_store`); auto-sanitiza si el backend rechaza; WebAuthn queda sin uso desde el móvil.
- 2026-08-20 — FIX crash splash: themes.xml pasó a padre `Theme.AppCompat.Light.NoActionBar` (MainActivity extiende AppCompatActivity por BiometricPrompt).
- 2026-08-12 — Todos los GETs conectados a la API real (patrón CargaUiState, mocks solo en MockData.kt); logout best-effort crea Salida; historiales con badge ENTRADA/SALIDA; RegistrarEquipoView con accesorios dinámicos.
- 2026-08-12 — Push a GitHub VistasAndroidDef1 (estrategia clonar→superponer→push único "VersionBackend"); convención comentarios solo `//` línea propia.
- 2026-08-11 — Base técnica: Retrofit doble ruta USB/WiFi con fallback 3s; dashboards comparten StatCard/TableContainer; navbar/menú vidrio verde; RegisterScreen con diálogo "Solicitud enviada"; fix DirectoryLock de Android Studio Flatpak.
- 2026-08-24 — Preparado para Railway: RetrofitClient añade BASE_URL_REMOTE (HTTPS) como 3er fallback USB->WiFi->Railway; backend WEB con railway.json (nixpacks + migrate --seed) y .gitignore que protege .env. Ambos repos SUBIDOS a GitHub (SenaAccessWeb / SenaAccessMovil). Falta: deploy Railway + poner dominio real en BASE_URL_REMOTE y recompilar APK.
- 2026-08-24 — Fix build Railway (WEB): nixpacks.toml (PHP 8.3 + ext gmp/sodium/intl/bcmath + COMPOSER_ALLOW_SUPERUSER) para el fallo de package:discover como root; .gitkeep en storage/bootstrap (carpetas viajan en el repo, contenido ignorado); releaseCommand con mkdir/chmod + migrate --seed. Empujado a SenaAccessWeb (7d03601).
- 2026-08-24 — Fix "valid cache path": composer.json añade pre-autoload-dump con mkdir -p de storage/bootstrap antes de package:discover; probado localmente borrando views/ y corriendo dump-autoload (exit 0). Dominio Railway OK (senaaccessweb-production.up.railway.app); BASE_URL_REMOTE actualizado y APK compilado PENDIENTE instalar en moto g86 (no estaba conectado).
- 2026-08-24 — APK instalado en moto g86 y login probado contra Railway: funciona. Distribución QR: repo QR-MOVIL actualizado (SenaAccess.apk apuntando a Railway, página v1.0) + QR en ~/Escritorio/QR-SenaAccess-v1.0.png.
- 2026-08-24 — FIX login 500 con PC apagada: la BD "MySQL" de Railway estaba rota (solo placeholders, password root perdida, sin DATABASE_URL en el web). Creé BD nueva "MySQL-DK1Y" (railway add), inyecté en SenaAccessWeb DATABASE_URL+APP_KEY+APP_ENV+APP_DEBUG+APP_URL+LOG_CHANNEL=stderr, migré+sembré vía `railway ssh` al contenedor web; login verificado 200 contra Railway. RetrofitClient ahora Railway primario (REMOTE->USB->WIFI); APK compilado PENDIENTE instalar.
- 2026-08-25 — Pantalla Asignaciones (aprendiz↔instructor): Repository + ViewModel + AsignacionesView (listar/crear/eliminar con dropdowns de aprendiz/instructor/jornada); dock admin ahora tiene 6 tabs (se añadió "Asignar"). Verificación contra Railway: POST/GET/DELETE `/admin/aprendiz-instructores` funciona; equipo-a-usuario y novedades ya estaban cableados y verificados. APK compilado PENDIENTE instalar.
- 2026-08-25 — Quité la feature Asignaciones (aprendiz↔instructor) a petición del usuario (también se quitó del WEB): borré AsignacionesView/AsignacionRepository, revertí el dock a 5 tabs, limpié ViewModel/Mock/ApiService/Models. Además FIX: la pestaña EQUIPOS del admin estaba vacía porque `irATab("EQUIPOS")` no abría subScreen; ahora abre `AdminScreen.EQUIPOS` y el admin puede registrar/ver equipos con dueño (instructor/aprendiz). APK compilado PENDIENTE instalar (celular no conectado).
