package com.tup.solicitudes;

/**
 * Tipos de solicitud con su plazo de resolucion y el area que las atiende.
 * Agregar un tipo nuevo es agregar una constante aca (OCP).
 */
public enum TipoSolicitud {
    CERTIFICADO(5, Area.ALUMNADO),
    EQUIVALENCIA(30, Area.ACADEMICA),
    RECLAMO_NOTA(10, Area.DOCENCIA),
    BAJA_MATERIA(7, Area.ALUMNADO);

    private final int plazoDias;
    private final Area areaResponsable;

    TipoSolicitud(int plazoDias, Area areaResponsable) {
        this.plazoDias = plazoDias;
        this.areaResponsable = areaResponsable;
    }

    public int getPlazoDias() {
        return plazoDias;
    }

    public Area getAreaResponsable() {
        return areaResponsable;
    }

    /** Devuelve el tipo con ese nombre exacto (distingue mayusculas) o null si no existe o el nombre es null. */
    public static TipoSolicitud desde(String nombre) {
        for (TipoSolicitud tipo : values()) {
            if (tipo.name().equals(nombre)) {
                return tipo;
            }
        }
        return null;
    }
}
