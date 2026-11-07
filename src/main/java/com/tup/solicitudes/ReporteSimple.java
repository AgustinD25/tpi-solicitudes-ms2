package com.tup.solicitudes;

import java.util.List;

public class ReporteSimple implements Exportable {
    private List<Solicitud> datos;

    public ReporteSimple(List<Solicitud> datos) {
        this.datos = datos;
    }

    @Override
    public String exportarCSV() {
        StringBuilder sb = new StringBuilder("id;legajo;tipo;prioridad;estado\n");
        for (Solicitud s : datos) {
            sb.append(s.id).append(";").append(s.estudiante.legajo).append(";")
              .append(s.tipo).append(";").append(s.prioridad).append(";")
              .append(s.estado).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportarJSON() {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < datos.size(); i++) {
            Solicitud s = datos.get(i);
            sb.append("  {\"id\": ").append(s.id)
              .append(", \"legajo\": \"").append(s.estudiante.legajo).append("\"")
              .append(", \"tipo\": \"").append(s.tipo).append("\"")
              .append(", \"prioridad\": ").append(s.prioridad)
              .append(", \"estado\": \"").append(s.estado).append("\"}");
            if (i < datos.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String exportarPDF() {
        throw new UnsupportedOperationException("PDF no soportado");
    }
}
