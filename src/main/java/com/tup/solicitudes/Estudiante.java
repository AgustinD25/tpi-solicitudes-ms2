package com.tup.solicitudes;

public class Estudiante {
    public String legajo;
    public String nombre;
    public String email;

    public Estudiante(String legajo, String nombre, String email) {
        this.legajo = legajo;
        this.nombre = nombre;
        this.email = email;
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
