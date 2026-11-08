package com.tup.solicitudes;

import java.util.Optional;

/**
 * Areas que atienden solicitudes. Es el unico lugar donde figura el texto de cada area:
 * agregar un area es agregar una constante aca.
 */
public enum Area {
    ALUMNADO("Alumnado"),
    ACADEMICA("Academica"),
    DOCENCIA("Docencia");

    private final String nombre;

    Area(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    /** Devuelve el area con ese nombre exacto (distingue mayusculas), o vacio si no existe o el nombre es null. */
    public static Optional<Area> desde(String nombre) {
        for (Area area : values()) {
            if (area.nombre.equals(nombre)) {
                return Optional.of(area);
            }
        }
        return Optional.empty();
    }
}
