package com.tup.solicitudes;

public class Estudiante {
    private final String legajo;
    private final String nombre;
    private final String email;

    public Estudiante(String legajo, String nombre, String email) {
        this.legajo = legajo;
        this.nombre = nombre;
        this.email = email;
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

    public boolean emailValido() {
        if (email == null) {
            return false;
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return false;
        }
        int punto = email.indexOf('.', arroba);
        if (punto < arroba + 2 || punto == email.length() - 1) {
            return false;
        }
        if (email.contains(" ")) {
            return false;
        }
        return true;
    }
}
