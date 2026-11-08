package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests unitarios (CR-07): plazos y areas de cada tipo, que son reglas de negocio. */
class TipoSolicitudTest {

    @Test
    void cadaTipoTieneSuPlazoYSuArea() {
        assertEquals(5, TipoSolicitud.CERTIFICADO.getPlazoDias());
        assertEquals(Area.ALUMNADO, TipoSolicitud.CERTIFICADO.getAreaResponsable());
        assertEquals(30, TipoSolicitud.EQUIVALENCIA.getPlazoDias());
        assertEquals(Area.ACADEMICA, TipoSolicitud.EQUIVALENCIA.getAreaResponsable());
        assertEquals(10, TipoSolicitud.RECLAMO_NOTA.getPlazoDias());
        assertEquals(Area.DOCENCIA, TipoSolicitud.RECLAMO_NOTA.getAreaResponsable());
        assertEquals(7, TipoSolicitud.BAJA_MATERIA.getPlazoDias());
        assertEquals(Area.ALUMNADO, TipoSolicitud.BAJA_MATERIA.getAreaResponsable());
    }

    @Test
    void desdeBuscaPorNombreExactoYDevuelveVacioSiNoExisteOEsNulo() {
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            assertEquals(tipo, TipoSolicitud.desde(tipo.name()).orElseThrow());
        }
        assertTrue(TipoSolicitud.desde("certificado").isEmpty());
        assertTrue(TipoSolicitud.desde("FOO").isEmpty());
        assertTrue(TipoSolicitud.desde(null).isEmpty());
    }
}
