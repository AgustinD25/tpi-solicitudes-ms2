package com.tup.solicitudes;

import java.time.LocalDate;

public class Solicitud {
    static final int PLAZO_CERTIFICADO = 5;
    static final int PLAZO_EQUIVALENCIA = 30;
    static final int PLAZO_RECLAMO_NOTA = 10;
    static final int PLAZO_BAJA_MATERIA = 7;

    public int id;
    public Estudiante estudiante;
    public String tipo;
    public String descripcion;
    public int prioridad;
    public boolean urgente;
    public String estado;
    public Responsable responsable;
    public LocalDate fechaCreacion;

    public boolean estaVencida(LocalDate hoy) {
        int plazo = 0;
        if (tipo.equals("CERTIFICADO")) {
            plazo = PLAZO_CERTIFICADO;
        } else if (tipo.equals("EQUIVALENCIA")) {
            plazo = PLAZO_EQUIVALENCIA;
        } else if (tipo.equals("RECLAMO_NOTA")) {
            plazo = PLAZO_RECLAMO_NOTA;
        } else if (tipo.equals("BAJA_MATERIA")) {
            plazo = PLAZO_BAJA_MATERIA;
        }
        if (estado.equals("RESUELTA") || estado.equals("RECHAZADA")) {
            return false;
        }
        return hoy.isAfter(fechaCreacion.plusDays(plazo));
    }
}
