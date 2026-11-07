package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Catalogo de responsables: se registran en memoria al iniciar el sistema (no se guardan en el CSV). */
public class ResponsableService {
    private final List<Responsable> responsables = new ArrayList<>();

    public Responsable registrar(int id, String nombre, String email, String area) {
        Responsable responsable = new Responsable(id, nombre, email, area);
        if (!responsable.emailValido()) {
            throw new ResponsableInvalidoException("email de responsable invalido");
        }
        if (!area.equals("Alumnado") && !area.equals("Academica") && !area.equals("Docencia")) {
            throw new ResponsableInvalidoException("area invalida");
        }
        responsables.add(responsable);
        return responsable;
    }

    /** Si hubiera ids repetidos devuelve el ultimo registrado, como la version original. */
    public Optional<Responsable> buscarPorId(int id) {
        Responsable encontrado = null;
        for (Responsable candidato : responsables) {
            if (candidato.getId() == id) {
                encontrado = candidato;
            }
        }
        return Optional.ofNullable(encontrado);
    }

    public List<Responsable> listarTodos() {
        return List.copyOf(responsables);
    }
}
