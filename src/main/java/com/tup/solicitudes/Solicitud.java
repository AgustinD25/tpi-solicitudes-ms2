package com.tup.solicitudes;

import java.time.LocalDate;

public class Solicitud {
    private final int id;
    private final Estudiante estudiante;
    private final TipoSolicitud tipo;
    private final String descripcion;
    private final int prioridad;
    private final boolean urgente;
    private final LocalDate fechaCreacion;
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    private Responsable responsable;

    public Solicitud(int id, Estudiante estudiante, TipoSolicitud tipo, String descripcion,
                     int prioridad, boolean urgente, LocalDate fechaCreacion) {
        this.id = id;
        this.estudiante = estudiante;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.urgente = urgente;
        this.fechaCreacion = fechaCreacion;
    }

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

    public boolean puedePasarA(EstadoSolicitud destino) {
        return estado.puedePasarA(destino);
    }

    /** Cambia de estado respetando las transiciones permitidas. */
    public void pasarA(EstadoSolicitud destino) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException("transicion invalida de " + estado + " a " + destino);
        }
        estado = destino;
    }

    /** Una solicitud solo puede ser atendida por un responsable del area que corresponde a su tipo. */
    public boolean puedeSerAtendidaPor(Responsable candidato) {
        return candidato.getArea().equals(tipo.getAreaResponsable());
    }

    /**
     * Asigna (o reasigna) el responsable. La propia entidad protege sus reglas: una solicitud cerrada no admite
     * asignaciones y el responsable debe ser del area que atiende ese tipo de solicitud.
     */
    public void asignarResponsable(Responsable nuevoResponsable) {
        if (estado.esFinal()) {
            throw new SolicitudCerradaException();
        }
        if (!puedeSerAtendidaPor(nuevoResponsable)) {
            throw new AreaIncorrectaException(nuevoResponsable.getArea(), tipo.getAreaResponsable());
        }
        this.responsable = nuevoResponsable;
    }

    /** Usado solo al cargar desde el archivo: reconstruye el estado y el responsable guardados. */
    void restaurar(EstadoSolicitud estadoGuardado, Responsable responsableGuardado) {
        this.estado = estadoGuardado;
        this.responsable = responsableGuardado;
    }

    /** Usado solo al cargar desde el archivo: completa un responsable que todavia no estaba registrado. */
    void restaurarResponsable(Responsable responsableGuardado) {
        this.responsable = responsableGuardado;
    }

    public boolean estaVencida(LocalDate hoy) {
        if (estado.esFinal()) {
            return false;
        }
        return hoy.isAfter(fechaCreacion.plusDays(tipo.getPlazoDias()));
    }
}
