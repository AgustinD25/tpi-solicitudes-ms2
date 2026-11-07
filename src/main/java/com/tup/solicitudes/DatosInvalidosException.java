package com.tup.solicitudes;

/** Legajo, nombre o email invalidos al registrar una solicitud. */
public class DatosInvalidosException extends DominioException {
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
