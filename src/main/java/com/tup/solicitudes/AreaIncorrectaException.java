package com.tup.solicitudes;

/** El responsable elegido no pertenece al area que atiende ese tipo de solicitud. */
public class AreaIncorrectaException extends DominioException {
    public AreaIncorrectaException(Area areaDelResponsable, Area areaRequerida) {
        super("el responsable es del area " + areaDelResponsable + " y la solicitud requiere " + areaRequerida);
    }
}
