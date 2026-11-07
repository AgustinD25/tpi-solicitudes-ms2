package com.tup.solicitudes;

import java.util.List;
import java.util.Optional;

/** Almacen de estudiantes, separado del de solicitudes (ISP): quien solo necesita uno no depende del otro. */
public interface EstudianteRepository {

    /** Todos los estudiantes, como copia no modificable. */
    List<Estudiante> listarTodos();

    Optional<Estudiante> buscarPorLegajo(String legajo);

    void agregar(Estudiante estudiante);
}
