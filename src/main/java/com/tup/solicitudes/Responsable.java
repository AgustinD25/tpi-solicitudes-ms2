package com.tup.solicitudes;

public class Responsable {
    private final int id;
    private final String nombre;
    private final String email;
    private final String area;

    public Responsable(int id, String nombre, String email, String area) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.area = area;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getArea() {
        return area;
    }
}
