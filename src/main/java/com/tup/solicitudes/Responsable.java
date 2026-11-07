package com.tup.solicitudes;

public class Responsable {
    public int id;
    public String nombre;
    public String email;
    public String area;

    public Responsable(int id, String nombre, String email, String area) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.area = area;
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
