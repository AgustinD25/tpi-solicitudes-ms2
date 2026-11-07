package com.tup.solicitudes;

public class TransicionInvalidaException extends DominioException {
    public TransicionInvalidaException(String estadoActual, String estadoPedido) {
        super("transicion invalida de " + estadoActual + " a " + estadoPedido);
    }
}
