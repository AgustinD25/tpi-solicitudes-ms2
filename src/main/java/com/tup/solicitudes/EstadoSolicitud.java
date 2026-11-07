package com.tup.solicitudes;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una solicitud y las transiciones permitidas entre ellos:
 * PENDIENTE -> EN_PROCESO -> RESUELTA, y PENDIENTE o EN_PROCESO -> RECHAZADA.
 * RESUELTA y RECHAZADA son estados finales.
 */
public enum EstadoSolicitud {
    PENDIENTE,
    EN_PROCESO,
    RESUELTA,
    RECHAZADA;

    private Set<EstadoSolicitud> destinosPermitidos;

    static {
        PENDIENTE.destinosPermitidos = EnumSet.of(EN_PROCESO, RECHAZADA);
        EN_PROCESO.destinosPermitidos = EnumSet.of(RESUELTA, RECHAZADA);
        RESUELTA.destinosPermitidos = EnumSet.noneOf(EstadoSolicitud.class);
        RECHAZADA.destinosPermitidos = EnumSet.noneOf(EstadoSolicitud.class);
    }

    public boolean puedePasarA(EstadoSolicitud destino) {
        return destinosPermitidos.contains(destino);
    }

    public boolean esFinal() {
        return destinosPermitidos.isEmpty();
    }

    /** Devuelve el estado con ese nombre exacto (distingue mayusculas) o null si no existe o es null. */
    public static EstadoSolicitud desde(String nombre) {
        for (EstadoSolicitud estado : values()) {
            if (estado.name().equals(nombre)) {
                return estado;
            }
        }
        return null;
    }
}
