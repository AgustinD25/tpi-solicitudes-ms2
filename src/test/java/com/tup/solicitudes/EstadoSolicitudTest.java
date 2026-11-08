package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Tests unitarios (CR-07): la matriz completa de transiciones, sin pasar por la fachada. */
class EstadoSolicitudTest {
    private static final EstadoSolicitud PENDIENTE = EstadoSolicitud.PENDIENTE;
    private static final EstadoSolicitud EN_PROCESO = EstadoSolicitud.EN_PROCESO;
    private static final EstadoSolicitud RESUELTA = EstadoSolicitud.RESUELTA;
    private static final EstadoSolicitud RECHAZADA = EstadoSolicitud.RECHAZADA;

    private static Set<EstadoSolicitud> destinosPermitidosDesde(EstadoSolicitud origen) {
        Set<EstadoSolicitud> permitidos = EnumSet.noneOf(EstadoSolicitud.class);
        for (EstadoSolicitud destino : EstadoSolicitud.values()) {
            if (origen.puedePasarA(destino)) {
                permitidos.add(destino);
            }
        }
        return permitidos;
    }

    @Test
    void desdePendientePuedeIrAEnProcesoOARechazada() {
        assertEquals(EnumSet.of(EN_PROCESO, RECHAZADA), destinosPermitidosDesde(PENDIENTE));
    }

    @Test
    void desdeEnProcesoPuedeIrAResueltaORechazada() {
        assertEquals(EnumSet.of(RESUELTA, RECHAZADA), destinosPermitidosDesde(EN_PROCESO));
    }

    @Test
    void losEstadosFinalesNoTienenSalida() {
        assertTrue(destinosPermitidosDesde(RESUELTA).isEmpty());
        assertTrue(destinosPermitidosDesde(RECHAZADA).isEmpty());
    }

    @Test
    void soloResueltaYRechazadaSonFinales() {
        assertFalse(PENDIENTE.esFinal());
        assertFalse(EN_PROCESO.esFinal());
        assertTrue(RESUELTA.esFinal());
        assertTrue(RECHAZADA.esFinal());
    }

    @Test
    void nadiePuedePasarASiMismoNiVolverAPendiente() {
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            assertFalse(estado.puedePasarA(estado), estado + " -> " + estado);
            assertFalse(estado.puedePasarA(PENDIENTE), estado + " -> PENDIENTE");
        }
    }

    @Test
    void desdeBuscaPorNombreExactoYDevuelveVacioSiNoExisteOEsNulo() {
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            assertEquals(estado, EstadoSolicitud.desde(estado.name()).orElseThrow());
        }
        assertTrue(EstadoSolicitud.desde("pendiente").isEmpty());
        assertTrue(EstadoSolicitud.desde("INVENTADO").isEmpty());
        assertTrue(EstadoSolicitud.desde(null).isEmpty());
    }
}
