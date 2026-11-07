package com.tup.solicitudes;

import java.time.LocalDate;

public class Solicitud {
    public int id;
    public Estudiante estudiante;
    public TipoSolicitud tipo;
    public String descripcion;
    public int prioridad;
    public boolean urgente;
    public EstadoSolicitud estado;
    public Responsable responsable;
    public LocalDate fechaCreacion;

    public int getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public String getTipo() {
        return tipo.name();
    }

    public TipoSolicitud getTipoSolicitud() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public String getEstado() {
        return estado.name();
    }

    public EstadoSolicitud getEstadoSolicitud() {
        return estado;
    }

    public Responsable getResponsable() {
        return responsable;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean estaVencida(LocalDate hoy) {
        if (estado.esFinal()) {
            return false;
        }
        return hoy.isAfter(fechaCreacion.plusDays(tipo.getPlazoDias()));
    }
}
