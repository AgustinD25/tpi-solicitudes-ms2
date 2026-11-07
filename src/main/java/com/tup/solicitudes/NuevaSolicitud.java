package com.tup.solicitudes;

import java.time.LocalDate;

/**
 * Datos necesarios para registrar una solicitud. Agrupa los parametros que antes
 * viajaban sueltos por registrarSolicitud (Introduce Parameter Object).
 */
public record NuevaSolicitud(String legajo, String nombre, String email, String tipo,
                             String descripcion, int prioridad, boolean urgente, LocalDate fecha) {
}
