# Credenciales de prueba — SenaAccess

Cuentas activas y verificadas contra el backend en producción:

- **API**: `https://senaaccessweb-production-4c6e.up.railway.app/api/`
- **Login**: `POST /api/login` con `user_email`, `user_password` y `device_id`.
- Todas las cuentas están **aprobadas** y **sin verificación en dos pasos**,
  para poder entrar de una sola vez.

| Correo | Contraseña | Rol | Para qué sirve |
|---|---|---|---|
| `admin@sena.edu.co` | `12345678` | **Superadmin** | Panel completo: usuarios (crear/editar/aprobar/rechazar), ambientes, equipos, historial, predicción, novedades, notificaciones. |
| `andres.vargas@gmail.com` | `12345678` | **Admin (portería)** | Operativa de portería: validar PIN/QR/invitado, equipos, historial del día. |
| `alejandro@sena.edu.co` | `123456` | **Instructor** | Sus ambientes, aprendices, permisos de salida con PIN, novedades, equipos. |
| `juan.pablo@sena.edu.co` | `12345678` | **Instructor** | Igual que el anterior (otra cuenta de instructor). |
| `laura.medina@sena.edu.co` | `12345678` | **Aprendiz** | Historial, equipos, permisos de salida, carnet digital. |
| `katherin@sena.edu.co` | `123456` | **Aprendiz** | Cuenta de aprendiz alternativa. |
| `sebastian@sena.edu.co` | `123456` | **Aprendiz** | Cuenta de aprendiz alternativa. |
| `funcionario@sena.edu.co` | `12345678` | **Funcionario** | Panel del aprendiz sin permisos de salida (personal administrativo). |

## Notas

- Las cuentas `@sena.edu.co` usan el dominio institucional; las `@gmail.com`,
  `@hotmail.com`, `@outlook.com` y `@soy.sena.edu.co` también están permitidas.
- El registro público crea **solo aprendices**: el Superadmin es quien cambia
  el rol (Instructor, Admin, Funcionario, etc.) desde **Usuarios**.
- Si una cuenta no entra, el Superadmin puede aprobarla o corregir su rol
  desde **Usuarios** (bandejas Pendientes / Aprendices / Instructores / Admin).
