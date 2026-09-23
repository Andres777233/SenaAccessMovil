package com.example.sennaccess.datos.simulados

import com.example.sennaccess.datos.modelos.Ingreso
import com.example.sennaccess.datos.modelos.EquipoIngreso
import com.example.sennaccess.datos.modelos.Novedad
import com.example.sennaccess.datos.modelos.Notificacion
import com.example.sennaccess.datos.modelos.Rol
import com.example.sennaccess.datos.modelos.ConteoNoLeidas
import com.example.sennaccess.datos.modelos.UsuarioApi
// Conjunto de datos de ejemplo que sirve como respaldo de la app cuando no hay
// conexión real: alimenta las pantallas en modo demo y evita que queden vacías.

object DatosSimulados {

    val rolAdmin = Rol(id_rol = 1, rol_name = "admin")
    val rolInstructor = Rol(id_rol = 2, rol_name = "Instructor")
    val rolAprendiz = Rol(id_rol = 3, rol_name = "Aprendiz")

    val usuarios = listOf(
        UsuarioApi(id_usuario = 1, user_identification = "1000000000", user_name = "admin", user_lastname = "System", user_email = "admin@sena.edu.co", user_coursenumber = 0, user_program = "Administration", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 1, profile_photo_path = null, role = rolAdmin),
        UsuarioApi(id_usuario = 2, user_identification = "12345678", user_name = "Carlos", user_lastname = "Méndez", user_email = "carlos.mendez@sena.edu.co", user_coursenumber = 2876541, user_program = "ADSO", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 2, profile_photo_path = null, role = rolInstructor),
        UsuarioApi(id_usuario = 3, user_identification = "87654321", user_name = "Daniel", user_lastname = "Vega", user_email = "daniel.vega@sena.edu.co", user_coursenumber = 2876542, user_program = "Análisis y Desarrollo de Software", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 2, profile_photo_path = null, role = rolInstructor),
        UsuarioApi(id_usuario = 4, user_identification = "11223344", user_name = "María", user_lastname = "Gómez", user_email = "maria.gomez@sena.edu.co", user_coursenumber = 2876543, user_program = "Gestión Empresarial", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 2, profile_photo_path = null, role = rolInstructor),
        UsuarioApi(id_usuario = 5, user_identification = "12345678", user_name = "Pepito", user_lastname = "Perez", user_email = "pepito@sena.edu.co", user_coursenumber = 2876541, user_program = "ADSO", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 3, profile_photo_path = null, role = rolAprendiz),
        UsuarioApi(id_usuario = 6, user_identification = "87654321", user_name = "Laura", user_lastname = "Pérez", user_email = "laura.perez@sena.edu.co", user_coursenumber = 2876541, user_program = "ADSO", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 3, profile_photo_path = null, role = rolAprendiz),
        UsuarioApi(id_usuario = 7, user_identification = "11223344", user_name = "Sofía", user_lastname = "Ramírez", user_email = "sofia.ramirez@sena.edu.co", user_coursenumber = 2876542, user_program = "Diseño Gráfico", user_documento_tipo = "CC", user_telefono = null, fk_id_rol = 3, profile_photo_path = null, role = rolAprendiz)
    )

    val instructores: List<UsuarioApi> get() = usuarios.filter { it.esRol("Instructor") }
    val aprendices: List<UsuarioApi> get() = usuarios.filter { it.esRol("Aprendiz") }

    val aprendizDemo: UsuarioApi get() = aprendices.first()
    val instructorDemo: UsuarioApi get() = instructores.first()
    val adminDemo: UsuarioApi get() = usuarios.first()

    val ingresos = listOf(
        Ingreso(1, "27/5/2026, 8:45 a. m.", "CCyS", "Entrada", 2, instructores[0]),
        Ingreso(2, "27/5/2026, 8:30 a. m.", "CCyS", "Entrada", 6, aprendices[1]),
        Ingreso(3, "27/5/2026, 8:15 a. m.", "CCyS", "Entrada", 3, instructores[1]),
        Ingreso(4, "27/5/2026, 7:55 a. m.", "CCyS", "Entrada", 4, instructores[2]),
        Ingreso(5, "27/5/2026, 7:20 a. m.", "CCyS", "Entrada", 7, aprendices[2]),
        Ingreso(6, "27/5/2026, 6:58 a. m.", "CCyS", "Entrada", 5, aprendices[0])
    )

    val historialAprendiz = listOf(
        Ingreso(10, "27/5/2026, 8:45:25 a. m.", "CCyS", "Entrada", 5, aprendices[0]),
        Ingreso(11, "27/5/2026, 8:45:19 a. m.", "CCyS", "Entrada", 5, aprendices[0]),
        Ingreso(12, "27/5/2026, 8:11:10 a. m.", "CCyS", "Entrada", 5, aprendices[0])
    )

    val equipos = listOf(
        EquipoIngreso(
            id_ingreso_equipo = 1, fk_id_usuario = 5, equipo_type = "Portátil",
            equipo_brand = "Lenovo", equipo_model = "ThinkPad", equipo_color = "Negro",
            equipo_serial = "SN12345", entry_datetime = "27/5/2026, 8:40 a. m.", user = aprendices[0]
        ),
        EquipoIngreso(
            id_ingreso_equipo = 2, fk_id_usuario = 5, equipo_type = "Tablet",
            equipo_brand = "Samsung", equipo_model = "Tab S9", equipo_color = "Gris",
            equipo_serial = "SN67890", equipo_observations = "Cargador incluido",
            entry_datetime = "27/5/2026, 8:41 a. m.", user = aprendices[0]
        )
    )

    val novedades = listOf(
        Novedad(1, "Ambiente 204", "Mantenimiento preventivo de equipos", "Se realizará mantenimiento al parque tecnológico del centro de formación.", "27/5/2026, 8:30 a. m.", 2, instructores[0]),
        Novedad(2, "Oficina coordinación", "Cambio de jornada instructores", "Instructores con jornada tarde deben confirmar el nuevo horario con el coordinador.", "26/5/2026, 2:00 p. m.", 3, instructores[1])
    )

    val notificaciones = listOf(
        Notificacion(
            id_notificacion = 1, fk_id_usuario = 1,
            notification_title = "Nueva novedad registrada",
            notification_body = "Carlos Méndez reportó: Mantenimiento preventivo de equipos (Ambiente 204).",
            notification_type = "novedad", is_read = false,
            created_at = "27/5/2026, 8:30 a. m."
        ),
        Notificacion(
            id_notificacion = 2, fk_id_usuario = 1,
            notification_title = "Nuevo equipo registrado",
            notification_body = "Daniel Vega registró un(a) Portátil Lenovo (Serial: SN12345).",
            notification_type = "equipo", is_read = false,
            created_at = "26/5/2026, 3:10 p. m."
        ),
        Notificacion(
            id_notificacion = 3, fk_id_usuario = 1,
            notification_title = "Bienvenido a SenaAccess",
            notification_body = "Tu cuenta fue creada correctamente. Ya puedes ingresar al centro.",
            notification_type = "sistema", is_read = true,
            created_at = "25/5/2026, 9:00 a. m."
        )
    )

    val unreadCountDemo = ConteoNoLeidas(unread = 2)
}
