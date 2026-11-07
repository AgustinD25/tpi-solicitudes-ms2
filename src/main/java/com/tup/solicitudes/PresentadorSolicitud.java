package com.tup.solicitudes;

/** Da formato de texto de una linea a una solicitud (presentacion por consola). */
public class PresentadorSolicitud {

    public String formatear(Solicitud solicitud) {
        String nombreResponsable = solicitud.getResponsable() == null
                ? "sin asignar" : solicitud.getResponsable().getNombre();
        return "#" + solicitud.getId() + " | " + solicitud.getEstudiante().getLegajo()
                + " " + solicitud.getEstudiante().getNombre()
                + " | " + solicitud.getTipo() + " | prioridad " + solicitud.getPrioridad()
                + (solicitud.isUrgente() ? " (urgente)" : "")
                + " | " + solicitud.getEstado() + " | " + nombreResponsable
                + " | " + solicitud.getFechaCreacion();
    }
}
