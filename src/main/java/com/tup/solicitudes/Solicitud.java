package com.tup.solicitudes;

import java.time.LocalDate;

public class Solicitud {
    public int id;
    public Estudiante estudiante;
    public String tipo;
    public String descripcion;
    public int prioridad;
    public boolean urgente;
    public String estado;
    public Responsable responsable;
    public LocalDate fechaCreacion;
}
