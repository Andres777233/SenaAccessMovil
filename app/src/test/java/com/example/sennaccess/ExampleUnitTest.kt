package com.example.sennaccess

import com.example.sennaccess.datos.modelos.EstadoPuerta
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de la regla de la puerta: solo se entra sin QR si hay una Entrada
// de hoy y el último movimiento sigue siendo Entrada.
class ExampleUnitTest {
    @Test
    fun estadoPuerta_exigeEntradaDeHoyYEstarDentro() {
        assertTrue(EstadoPuerta(entrada_hoy = true, dentro = true).estaDentroHoy)
        assertFalse(EstadoPuerta(entrada_hoy = true, dentro = false).estaDentroHoy)
        assertFalse(EstadoPuerta(entrada_hoy = false, dentro = true).estaDentroHoy)
        assertFalse(EstadoPuerta().estaDentroHoy)
    }
}
