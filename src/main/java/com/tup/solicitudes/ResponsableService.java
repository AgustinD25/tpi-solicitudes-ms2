package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Catalogo de responsables: se registran en memoria al iniciar el sistema (no se guardan en el CSV). */
public class ResponsableService implements CatalogoResponsables {
    private final List<Responsable> responsables = new ArrayList<>();

    @Override
    public Responsable registrar(int id, String nombre, String email, String area) {
        if (!ValidadorEmail.esValido(email)) {
            throw new ResponsableInvalidoException("email de responsable invalido");
        }
        Area areaValida = Area.desde(area)
                .orElseThrow(() -> new ResponsableInvalidoException("area invalida"));
        Responsable responsable = new Responsable(id, nombre, email, areaValida);
        responsables.add(responsable);
        return responsable;
    }

    /** Si hubiera ids repetidos devuelve el ultimo registrado, como la version original. */
    @Override
    public Optional<Responsable> buscarPorId(int id) {
        Responsable encontrado = null;
        for (Responsable candidato : responsables) {
            if (candidato.getId() == id) {
                encontrado = candidato;
            }
        }
        return Optional.ofNullable(encontrado);
    }

    @Override
    public List<Responsable> listarTodos() {
        return List.copyOf(responsables);
    }
}
