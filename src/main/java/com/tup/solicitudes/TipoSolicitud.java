package com.tup.solicitudes;

import java.util.Objects;

/**
 * Tipos de solicitud con su plazo de resolucion y el area que las atiende.
 * Agregar un tipo nuevo es agregar una constante aca (OCP).
 */
public enum TipoSolicitud {
    CERTIFICADO(5, "Alumnado"),
    EQUIVALENCIA(30, "Academica"),
    RECLAMO_NOTA(10, "Docencia"),
    BAJA_MATERIA(7, "Alumnado");

    private final int plazoDias;
    private final String areaResponsable;

    TipoSolicitud(int plazoDias, String areaResponsable) {
        this.plazoDias = plazoDias;
        this.areaResponsable = areaResponsable;
    }

    public int getPlazoDias() {
        return plazoDias;
    }

    public String getAreaResponsable() {
        return areaResponsable;
    }

    /**
     * Devuelve el tipo con ese nombre exacto (distingue mayusculas) o null si no existe.
     * Con nombre null lanza NullPointerException, igual que la version original (defecto D2).
     */
    public static TipoSolicitud desde(String nombre) {
        Objects.requireNonNull(nombre);
        for (TipoSolicitud tipo : values()) {
            if (tipo.name().equals(nombre)) {
                return tipo;
            }
        }
        return null;
    }
}
