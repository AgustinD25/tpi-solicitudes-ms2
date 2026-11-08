package com.tup.solicitudes;

public class Responsable {
    private final int id;
    private final String nombre;
    private final String email;
    private final Area area;

    public Responsable(int id, String nombre, String email, Area area) {
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

    public Area getArea() {
        return area;
    }
}
