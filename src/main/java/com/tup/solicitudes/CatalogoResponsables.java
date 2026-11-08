package com.tup.solicitudes;

import java.util.List;
import java.util.Optional;

/** Catalogo de responsables. Los servicios dependen de esta interfaz y no de una implementacion concreta. */
public interface CatalogoResponsables {

    /** Registra un responsable; lanza ResponsableInvalidoException si el email o el area no son validos. */
    Responsable registrar(int id, String nombre, String email, String area);

    Optional<Responsable> buscarPorId(int id);

    List<Responsable> listarTodos();
}
