package com.tup.solicitudes;

public class ResponsableNoEncontradoException extends DominioException {
    public ResponsableNoEncontradoException(int id) {
        super("no existe el responsable " + id);
    }
}
