package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementacion en memoria de los repositorios. Los datos se pierden al cerrar el programa. */
public class RepositorioEnMemoria implements SolicitudRepository, EstudianteRepository {
    private final List<Solicitud> solicitudes = new ArrayList<>();
    private final List<Estudiante> estudiantes = new ArrayList<>();
    private int ultimoId = 0;

    @Override
    public List<Solicitud> listarTodas() {
        return List.copyOf(solicitudes);
    }

    @Override
    public Optional<Solicitud> buscarPorId(int id) {
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getId() == id) {
                return Optional.of(solicitud);
            }
        }
        return Optional.empty();
    }

    @Override
    public int generarId() {
        ultimoId = ultimoId + 1;
        return ultimoId;
    }

    @Override
    public void guardar(Solicitud nueva) {
        solicitudes.add(nueva);
    }

    @Override
    public void actualizar(Solicitud existente) {
        // En memoria el objeto ya esta actualizado: no hay nada que persistir.
    }

    @Override
    public void cargar() {
        // Sin almacenamiento persistente.
    }

    @Override
    public List<Estudiante> listarTodos() {
        return List.copyOf(estudiantes);
    }

    @Override
    public Optional<Estudiante> buscarPorLegajo(String legajo) {
        Estudiante encontrado = null;
        for (Estudiante candidato : estudiantes) {
            if (candidato.getLegajo().equals(legajo)) {
                encontrado = candidato;
            }
        }
        return Optional.ofNullable(encontrado);
    }

    @Override
    public void agregar(Estudiante estudiante) {
        estudiantes.add(estudiante);
    }

    /** Para las subclases que releen su almacenamiento: descarta lo que hay en memoria y reinicia los ids. */
    protected void vaciar() {
        solicitudes.clear();
        estudiantes.clear();
        ultimoId = 0;
    }

    /** Para las subclases que leen de un archivo: agrega una solicitud ya existente y avanza el contador de ids. */
    protected void incorporar(Solicitud cargada) {
        solicitudes.add(cargada);
        if (cargada.getId() > ultimoId) {
            ultimoId = cargada.getId();
        }
    }
}
