package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Caracterizacion de Exportable / ReporteSimple (version inicial).
 * Va aparte de los tests de SistemaSolicitudes porque estas dos clases
 * cambian de forma en el refactoring (ISP / LSP), mientras que los otros tests
 * deben correr sin cambios sobre ambas versiones.
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
        Exportable rep = new ReporteSimple(dosSolicitudes());

        assertEquals("id;legajo;tipo;prioridad;estado\n"
                + "1;L001;CERTIFICADO;1;PENDIENTE\n"
                + "2;L002;EQUIVALENCIA;2;EN_PROCESO\n", rep.exportarCSV());
    }

    @Test
    void exportarJsonDelReporteSimple() {
        Exportable rep = new ReporteSimple(dosSolicitudes());

        assertEquals("[\n"
                + "  {\"id\": 1, \"legajo\": \"L001\", \"tipo\": \"CERTIFICADO\", \"prioridad\": 1, \"estado\": \"PENDIENTE\"},\n"
                + "  {\"id\": 2, \"legajo\": \"L002\", \"tipo\": \"EQUIVALENCIA\", \"prioridad\": 2, \"estado\": \"EN_PROCESO\"}\n"
                + "]", rep.exportarJSON());
    }

    @Test
    void exportarPdfLanzaUnsupportedOperationException() {
        Exportable rep = new ReporteSimple(dosSolicitudes());

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, rep::exportarPDF);

        assertEquals("PDF no soportado", ex.getMessage());
    }
}
