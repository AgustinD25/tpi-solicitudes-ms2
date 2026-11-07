package com.tup.solicitudes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Arma el reporte basico: cantidad por estado, cantidad por tipo y listado de solicitudes vencidas. */
public class ReporteService {

    public String generar(List<Solicitud> solicitudes, LocalDate hoy) {
        Map<EstadoSolicitud, Integer> porEstado = new EnumMap<>(EstadoSolicitud.class);
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            porEstado.put(estado, 0);
        }
        Map<TipoSolicitud, Integer> porTipo = new EnumMap<>(TipoSolicitud.class);
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            porTipo.put(tipo, 0);
        }
        for (Solicitud solicitud : solicitudes) {
            porEstado.merge(solicitud.getEstadoSolicitud(), 1, Integer::sum);
            porTipo.merge(solicitud.getTipoSolicitud(), 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE ===\n");
        sb.append("Por estado:\n");
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            sb.append("  ").append(estado).append(": ").append(porEstado.get(estado)).append("\n");
        }
        sb.append("Por tipo:\n");
        for (TipoSolicitud tipo : TipoSolicitud.values()) {
            sb.append("  ").append(tipo).append(": ").append(porTipo.get(tipo)).append("\n");
        }
        sb.append("Vencidas:\n");
        int cantidadVencidas = 0;
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.estaVencida(hoy)) {
                sb.append("  #").append(solicitud.getId()).append(" ").append(solicitud.getTipo())
                  .append(" (").append(solicitud.getEstudiante().getLegajo()).append(") creada ")
                  .append(solicitud.getFechaCreacion()).append("\n");
                cantidadVencidas++;
            }
        }
        if (cantidadVencidas == 0) {
            sb.append("  (ninguna)\n");
        }
        return sb.toString();
    }
}
