package com.tup.solicitudes;

/**
 * Error de una regla del dominio (dato invalido, regla de negocio violada, entidad inexistente).
 * El mensaje explica la causa y no incluye prefijos de presentacion como "Error: ".
 */
public class DominioException extends RuntimeException {
    public DominioException(String mensaje) {
        super(mensaje);
    }
}
