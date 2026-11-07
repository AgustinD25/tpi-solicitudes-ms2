package com.tup.solicitudes;

public class TipoInvalidoException extends DominioException {
    public TipoInvalidoException() {
        super("tipo de solicitud invalido");
    }
}
