package com.tup.solicitudes;

public class PrioridadInvalidaException extends DominioException {
    public PrioridadInvalidaException() {
        super("prioridad fuera de rango");
    }
}
