package com.tup.solicitudes;

/** Se intento modificar una solicitud que ya esta resuelta o rechazada. */
public class SolicitudCerradaException extends DominioException {
    public SolicitudCerradaException() {
        super("la solicitud ya esta cerrada");
    }
}
