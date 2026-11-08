package com.tup.solicitudes;

import java.util.Objects;

public class Estudiante {
    private final String legajo;
    private final String nombre;
    private final String email;

    public Estudiante(String legajo, String nombre, String email) {
        this.legajo = Objects.requireNonNull(legajo, "legajo");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.email = Objects.requireNonNull(email, "email");
    }

    public String getLegajo() {
        return legajo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }
}
