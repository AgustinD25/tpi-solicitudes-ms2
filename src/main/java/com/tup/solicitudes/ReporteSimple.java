package com.tup.solicitudes;

import java.util.List;

public class ReporteSimple implements ExportadorCSV, ExportadorJSON {
    private final List<Solicitud> datos;

    public ReporteSimple(List<Solicitud> datos) {
        this.datos = datos;
    }

    @Override
    public String exportarCSV() {
        StringBuilder sb = new StringBuilder("id;legajo;tipo;prioridad;estado\n");
        for (Solicitud s : datos) {
            sb.append(s.getId()).append(";").append(FormatoCsv.escapar(s.getEstudiante().getLegajo())).append(";")
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
              .append(", \"legajo\": \"").append(escaparJson(s.getEstudiante().getLegajo())).append("\"")
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

    // Escapa lo que no puede ir tal cual dentro de un texto JSON: comillas, barra invertida y caracteres de control.
    private static String escaparJson(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"':
                    resultado.append("\\\"");
                    break;
                case '\\':
                    resultado.append("\\\\");
                    break;
                case '\n':
                    resultado.append("\\n");
                    break;
                case '\r':
                    resultado.append("\\r");
                    break;
                case '\t':
                    resultado.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        resultado.append(String.format("\\u%04x", (int) c));
                    } else {
                        resultado.append(c);
                    }
            }
        }
        return resultado.toString();
    }
}
