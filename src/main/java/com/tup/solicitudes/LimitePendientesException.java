package com.tup.solicitudes;

public class LimitePendientesException extends DominioException {
    public LimitePendientesException(int maximo) {
        super("el estudiante ya tiene " + maximo + " solicitudes pendientes");
    }
}
