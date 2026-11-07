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
            sb.append(s.getId()).append(";").append(s.getEstudiante().getLegajo()).append(";")
              .append(s.getTipo()).append(";").append(s.getPrioridad()).append(";")
              .append(s.getEstado()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String exportarJSON() {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < datos.size(); i++) {
            Solicitud s = datos.get(i);
            sb.append("  {\"id\": ").append(s.getId())
              .append(", \"legajo\": \"").append(s.getEstudiante().getLegajo()).append("\"")
              .append(", \"tipo\": \"").append(s.getTipo()).append("\"")
              .append(", \"prioridad\": ").append(s.getPrioridad())
              .append(", \"estado\": \"").append(s.getEstado()).append("\"}");
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
