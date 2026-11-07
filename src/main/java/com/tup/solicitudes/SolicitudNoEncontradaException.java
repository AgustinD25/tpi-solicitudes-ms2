package com.tup.solicitudes;

public class SolicitudNoEncontradaException extends DominioException {
    public SolicitudNoEncontradaException(int id) {
        super("no existe la solicitud " + id);
    }
}
