package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests nuevos del code review (CR-04): las areas viven en un solo enum y un area nula es un area invalida. */
class ResponsableServiceTest {
    private PrintStream salidaOriginal;
    private ByteArrayOutputStream salida;

    @BeforeEach
    void capturarConsola() {
        salidaOriginal = System.out;
        salida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restaurarConsola() {
        System.setOut(salidaOriginal);
    }

    @Test
    void registraUnResponsableConUnAreaConocida() {
        ResponsableService servicio = new ResponsableService();

        Responsable responsable = servicio.registrar(1, "Laura", "laura@ejemplo.edu", "Alumnado");

        assertEquals(Area.ALUMNADO, responsable.getArea());
        assertTrue(servicio.buscarPorId(1).isPresent());
    }

    @Test
    void unAreaNulaEsUnAreaInvalidaYNoUnNullPointerException() {
        ResponsableService servicio = new ResponsableService();

        ResponsableInvalidoException error = assertThrows(ResponsableInvalidoException.class,
                () -> servicio.registrar(1, "Laura", "laura@ejemplo.edu", null));

        assertEquals("area invalida", error.getMessage());
        assertTrue(servicio.listarTodos().isEmpty());
    }

    @Test
    void elAreaDistingueMayusculasYRechazaNombresDesconocidos() {
        ResponsableService servicio = new ResponsableService();

        assertThrows(ResponsableInvalidoException.class,
                () -> servicio.registrar(1, "Laura", "laura@ejemplo.edu", "alumnado"));
        assertThrows(ResponsableInvalidoException.class,
                () -> servicio.registrar(1, "Laura", "laura@ejemplo.edu", "Cocina"));
    }

    @Test
    void elEmailSeValidaAntesQueElArea() {
        ResponsableService servicio = new ResponsableService();

        ResponsableInvalidoException error = assertThrows(ResponsableInvalidoException.class,
                () -> servicio.registrar(1, "Laura", "sin-arroba", null));

        assertEquals("email de responsable invalido", error.getMessage());
    }

    @Test
    void laFachadaConAreaNulaDevuelveNullYAvisaEnVezDeLanzarExcepcion() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);

        Responsable resultado = sis.registrarResponsable(1, "Laura", "laura@ejemplo.edu", null);

        assertNull(resultado);
        assertEquals("Error: area invalida" + System.lineSeparator(), salida.toString(StandardCharsets.UTF_8));
    }

    @Test
    void cadaAreaTieneUnSoloTextoQueSeReconoceDeVuelta() {
        for (Area area : Area.values()) {
            assertEquals(area, Area.desde(area.getNombre()).orElseThrow());
        }
        assertTrue(Area.desde(null).isEmpty());
    }
}
