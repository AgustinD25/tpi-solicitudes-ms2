package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests unitarios (CR-07) de la unica regla de validacion de email. */
class ValidadorEmailTest {

    @Test
    void aceptaEmailsConArrobaYPuntoDespues() {
        assertTrue(ValidadorEmail.esValido("a@b.co"));
        assertTrue(ValidadorEmail.esValido("ana.perez@ejemplo.edu"));
        assertTrue(ValidadorEmail.esValido("be;to@b.com"));      // el ';' no invalida el email (se escapa al guardar)
    }

    @Test
    void rechazaEmailsMalFormados() {
        String[] invalidos = {null, "", "sin-arroba", "@ejemplo.com", "ana@", "ana@ejemplo", "ana@.com",
                "ana@ejemplo.", "ana perez@ejemplo.com", "ana@ejemplo .com"};
        for (String email : invalidos) {
            assertFalse(ValidadorEmail.esValido(email), "email: " + email);
        }
    }
}
