package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la exportacion (ReporteSimple).
 *
 * REESCRITO A PROPOSITO en el refactoring de ISP/LSP (smell 12): es la excepcion prevista en CLAUDE.md.
 * - Los tests de CSV y JSON conservan exactamente los mismos valores esperados; solo cambia el tipo con el
 *   que se declara la variable (Exportable -> ExportadorCSV / ExportadorJSON).
 * - El test de PDF cambia de sentido: antes comprobaba que exportarPDF() lanzaba
 *   UnsupportedOperationException (un contrato roto, violacion de LSP); ahora comprueba que ReporteSimple
 *   ya no promete PDF, porque no puede cumplirlo.
 */
class ExportacionCaracterizacionTest {

    private static List<Solicitud> dosSolicitudes() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);
        LocalDate hoy = LocalDate.of(2026, 3, 10);
        sis.registrarSolicitud("L001", "Ana", "ana@ejemplo.edu", "CERTIFICADO", "d", 1, false, hoy);
        sis.registrarSolicitud("L002", "Beto", "beto@ejemplo.edu", "EQUIVALENCIA", "d", 2, false, hoy);
        sis.cambiarEstado(2, "EN_PROCESO");
        return new ArrayList<>(sis.getSolicitudes());
    }

    @Test
    void exportarCsvDelReporteSimple() {
        ExportadorCSV rep = new ReporteSimple(dosSolicitudes());

        assertEquals("id;legajo;tipo;prioridad;estado\n"
                + "1;L001;CERTIFICADO;1;PENDIENTE\n"
                + "2;L002;EQUIVALENCIA;2;EN_PROCESO\n", rep.exportarCSV());
    }

    @Test
    void exportarJsonDelReporteSimple() {
        ExportadorJSON rep = new ReporteSimple(dosSolicitudes());

        assertEquals("[\n"
                + "  {\"id\": 1, \"legajo\": \"L001\", \"tipo\": \"CERTIFICADO\", \"prioridad\": 1, \"estado\": \"PENDIENTE\"},\n"
                + "  {\"id\": 2, \"legajo\": \"L002\", \"tipo\": \"EQUIVALENCIA\", \"prioridad\": 2, \"estado\": \"EN_PROCESO\"}\n"
                + "]", rep.exportarJSON());
    }

    @Test
    void elReporteSimpleNoPrometeExportarAPdf() {
        boolean tieneMetodoPdf = Arrays.stream(ReporteSimple.class.getMethods())
                .map(Method::getName)
                .anyMatch(nombre -> nombre.equals("exportarPDF"));

        assertFalse(tieneMetodoPdf);
        // y lo que si ofrece, lo cumple: ambas capacidades estan disponibles sin excepciones
        assertTrue(ExportadorCSV.class.isAssignableFrom(ReporteSimple.class));
        assertTrue(ExportadorJSON.class.isAssignableFrom(ReporteSimple.class));
    }
}
