package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests nuevos (code review, CR-02): las exportaciones deben producir CSV y JSON validos con cualquier texto. */
class ReporteSimpleEscapesTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    private PrintStream salidaOriginal;

    @BeforeEach
    void silenciarConsola() {
        salidaOriginal = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restaurarConsola() {
        System.setOut(salidaOriginal);
    }

    private static ReporteSimple conLegajo(String legajo) {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);
        sis.registrarSolicitud(legajo, "Ana", "ana@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);
        return new ReporteSimple(sis.getSolicitudes());
    }

    @Test
    void elJsonEscapaLasComillas() {
        String json = conLegajo("L\"1").exportarJSON();

        assertTrue(json.contains("\"legajo\": \"L\\\"1\""), json);
    }

    @Test
    void elJsonEscapaLaBarraInvertidaYLosCaracteresDeControl() {
        String json = conLegajo("a\\b\nc\td").exportarJSON();

        assertTrue(json.contains("\"legajo\": \"a\\\\b\\nc\\td\""), json);
    }

    @Test
    void elCsvEscapaElPuntoYComaYMantieneLaCantidadDeColumnas() {
        String csv = conLegajo("L;2").exportarCSV();
        String fila = csv.split("\n")[1];

        List<String> columnas = FormatoCsv.dividir(fila);

        assertEquals(5, columnas.size());
        assertEquals("L;2", columnas.get(1));
    }

    @Test
    void conTextosNormalesElFormatoNoCambia() {
        String csv = conLegajo("L001").exportarCSV();

        assertEquals("id;legajo;tipo;prioridad;estado\n1;L001;CERTIFICADO;1;PENDIENTE\n", csv);
    }
}
