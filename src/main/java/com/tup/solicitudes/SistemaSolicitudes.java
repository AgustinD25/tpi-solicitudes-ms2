package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SistemaSolicitudes {
    public List<Solicitud> lst = new ArrayList<>();
    public List<Estudiante> est = new ArrayList<>();
    public List<Responsable> resp = new ArrayList<>();
    int cont = 0;
    String archivo;

    public SistemaSolicitudes(String archivo) {
        this.archivo = archivo;
    }

    // ---------- responsables ----------

    public Responsable registrarResponsable(int id, String nombre, String email, String area) {
        Responsable r = new Responsable(id, nombre, email, area);
        if (!r.emailValido()) {
            System.out.println("Error: email de responsable invalido");
            return null;
        }
        if (!area.equals("Alumnado") && !area.equals("Academica") && !area.equals("Docencia")) {
            System.out.println("Error: area invalida");
            return null;
        }
        resp.add(r);
        return r;
    }

    // ---------- registro ----------

    // Devuelve el id de la solicitud, o un codigo negativo si hubo error.
    public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                                  String descripcion, int prioridad, boolean urgente) {
        return registrarSolicitud(legajo, nombre, email, tipo, descripcion, prioridad, urgente, LocalDate.now());
    }

    public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                                  String descripcion, int prioridad, boolean urgente, LocalDate fecha) {
        if (legajo == null || legajo.trim().length() == 0) {
            System.out.println("Error: legajo vacio");
            return -1;
        }
        if (nombre == null || nombre.trim().length() == 0) {
            System.out.println("Error: nombre vacio");
            return -1;
        }
        if (!tipo.equals("CERTIFICADO") && !tipo.equals("EQUIVALENCIA")
                && !tipo.equals("RECLAMO_NOTA") && !tipo.equals("BAJA_MATERIA")) {
            System.out.println("Error: tipo de solicitud invalido");
            return -3;
        }
        if (prioridad < 1 || prioridad > 3) {
            System.out.println("Error: prioridad fuera de rango");
            return -4;
        }
        Estudiante aux = new Estudiante(legajo, nombre, email);
        if (!aux.emailValido()) {
            System.out.println("Error: email invalido");
            return -1;
        }
        int pendientes = 0;
        for (Solicitud s : lst) {
            if (s.estudiante.legajo.equals(legajo) && s.estado.equals("PENDIENTE")) {
                pendientes++;
            }
        }
        if (pendientes >= 3) {
            System.out.println("Error: el estudiante ya tiene 3 solicitudes pendientes");
            return -2;
        }
        Estudiante e = null;
        for (Estudiante x : est) {
            if (x.legajo.equals(legajo)) {
                e = x;
            }
        }
        if (e == null) {
            e = aux;
            est.add(e);
        }
        if (urgente) {
            prioridad = prioridad + 1;
            if (prioridad > 3) {
                prioridad = 3;
            }
        }
        Solicitud s2 = new Solicitud();
        cont = cont + 1;
        s2.id = cont;
        s2.estudiante = e;
        s2.tipo = tipo;
        s2.descripcion = descripcion;
        s2.prioridad = prioridad;
        s2.urgente = urgente;
        s2.estado = "PENDIENTE";
        s2.fechaCreacion = fecha;
        lst.add(s2);
        guardar();
        System.out.println("Solicitud #" + s2.id + " registrada (" + tipo + ", prioridad " + prioridad + ")");
        return s2.id;
    }

    // ---------- estados ----------

    public boolean cambiarEstado(int id, String nuevo) {
        Solicitud s = proc(id);
        if (s == null) {
            System.out.println("Error: no existe la solicitud " + id);
            return false;
        }
        boolean flag = false;
        if (s.estado.equals("PENDIENTE")) {
            if (nuevo.equals("EN_PROCESO")) {
                flag = true;
            } else if (nuevo.equals("RECHAZADA")) {
                flag = true;
            }
        } else if (s.estado.equals("EN_PROCESO")) {
            if (nuevo.equals("RESUELTA")) {
                flag = true;
            } else if (nuevo.equals("RECHAZADA")) {
                flag = true;
            }
        }
        if (!flag) {
            System.out.println("Error: transicion invalida de " + s.estado + " a " + nuevo);
            return false;
        }
        s.estado = nuevo;
        guardar();
        System.out.println("Solicitud #" + id + " ahora esta " + nuevo);
        return true;
    }

    // ---------- responsables ----------

    public boolean asignarResponsable(int idSolicitud, int idResponsable) {
        Solicitud s = proc(idSolicitud);
        if (s == null) {
            System.out.println("Error: no existe la solicitud " + idSolicitud);
            return false;
        }
        Responsable r = null;
        for (Responsable x : resp) {
            if (x.id == idResponsable) {
                r = x;
            }
        }
        if (r == null) {
            System.out.println("Error: no existe el responsable " + idResponsable);
            return false;
        }
        String area = null;
        if (s.tipo.equals("CERTIFICADO")) {
            area = "Alumnado";
        } else if (s.tipo.equals("EQUIVALENCIA")) {
            area = "Academica";
        } else if (s.tipo.equals("RECLAMO_NOTA")) {
            area = "Docencia";
        } else if (s.tipo.equals("BAJA_MATERIA")) {
            area = "Alumnado";
        }
        if (!r.area.equals(area)) {
            System.out.println("Error: el responsable es del area " + r.area + " y la solicitud requiere " + area);
            return false;
        }
        s.responsable = r;
        guardar();
        System.out.println("Solicitud #" + idSolicitud + " asignada a " + r.nombre);
        return true;
    }

    // ---------- busquedas ----------

    Solicitud proc(int id) {
        for (Solicitud s : lst) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }

    public List<Solicitud> filtrarPorEstado(String estado) {
        List<Solicitud> res = new ArrayList<>();
        for (Solicitud s : lst) {
            if (s.estado.equals(estado)) {
                res.add(s);
            }
        }
        return res;
    }

    public List<Solicitud> filtrarPorTipo(String tipo) {
        List<Solicitud> res = new ArrayList<>();
        for (Solicitud s : lst) {
            if (s.tipo.equals(tipo)) {
                res.add(s);
            }
        }
        return res;
    }

    public List<Solicitud> filtrarPorPrioridad(int prioridad) {
        List<Solicitud> res = new ArrayList<>();
        for (Solicitud s : lst) {
            if (s.prioridad == prioridad) {
                res.add(s);
            }
        }
        return res;
    }

    // ---------- vencimientos ----------

    public boolean estaVencida(Solicitud s, LocalDate hoy) {
        int plazo = 0;
        if (s.tipo.equals("CERTIFICADO")) {
            plazo = 5;
        } else if (s.tipo.equals("EQUIVALENCIA")) {
            plazo = 30;
        } else if (s.tipo.equals("RECLAMO_NOTA")) {
            plazo = 10;
        } else if (s.tipo.equals("BAJA_MATERIA")) {
            plazo = 7;
        }
        if (s.estado.equals("RESUELTA") || s.estado.equals("RECHAZADA")) {
            return false;
        }
        return hoy.isAfter(s.fechaCreacion.plusDays(plazo));
    }

    // ---------- reporte ----------

    public String reporte(LocalDate hoy) {
        int p = 0, ep = 0, r = 0, rz = 0;
        int c = 0, e = 0, rn = 0, b = 0;
        for (Solicitud s : lst) {
            if (s.estado.equals("PENDIENTE")) {
                p++;
            } else if (s.estado.equals("EN_PROCESO")) {
                ep++;
            } else if (s.estado.equals("RESUELTA")) {
                r++;
            } else if (s.estado.equals("RECHAZADA")) {
                rz++;
            }
            if (s.tipo.equals("CERTIFICADO")) {
                c++;
            } else if (s.tipo.equals("EQUIVALENCIA")) {
                e++;
            } else if (s.tipo.equals("RECLAMO_NOTA")) {
                rn++;
            } else if (s.tipo.equals("BAJA_MATERIA")) {
                b++;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE ===\n");
        sb.append("Por estado:\n");
        sb.append("  PENDIENTE: ").append(p).append("\n");
        sb.append("  EN_PROCESO: ").append(ep).append("\n");
        sb.append("  RESUELTA: ").append(r).append("\n");
        sb.append("  RECHAZADA: ").append(rz).append("\n");
        sb.append("Por tipo:\n");
        sb.append("  CERTIFICADO: ").append(c).append("\n");
        sb.append("  EQUIVALENCIA: ").append(e).append("\n");
        sb.append("  RECLAMO_NOTA: ").append(rn).append("\n");
        sb.append("  BAJA_MATERIA: ").append(b).append("\n");
        sb.append("Vencidas:\n");
        int venc = 0;
        for (Solicitud s : lst) {
            if (estaVencida(s, hoy)) {
                sb.append("  #").append(s.id).append(" ").append(s.tipo)
                  .append(" (").append(s.estudiante.legajo).append(") creada ")
                  .append(s.fechaCreacion).append("\n");
                venc++;
            }
        }
        if (venc == 0) {
            sb.append("  (ninguna)\n");
        }
        String res = sb.toString();
        System.out.print(res);
        return res;
    }

    // ---------- impresion ----------

    public void imprimir(Solicitud s) {
        String resp = s.responsable == null ? "sin asignar" : s.responsable.nombre;
        System.out.println("#" + s.id + " | " + s.estudiante.legajo + " " + s.estudiante.nombre
                + " | " + s.tipo + " | prioridad " + s.prioridad + (s.urgente ? " (urgente)" : "")
                + " | " + s.estado + " | " + resp + " | " + s.fechaCreacion);
    }

    public void imprimirLista(List<Solicitud> l) {
        if (l.isEmpty()) {
            System.out.println("(sin resultados)");
        }
        for (Solicitud s : l) {
            imprimir(s);
        }
    }

    // ---------- persistencia ----------

    void guardar() {
        if (archivo == null) {
            return;
        }
        try {
            FileWriter fw = new FileWriter(archivo);
            for (Solicitud s : lst) {
                fw.write(s.id + ";" + s.estudiante.legajo + ";" + s.estudiante.nombre + ";"
                        + s.estudiante.email + ";" + s.tipo + ";" + s.descripcion.replace(';', ',') + ";"
                        + s.prioridad + ";" + s.urgente + ";" + s.estado + ";"
                        + (s.responsable == null ? -1 : s.responsable.id) + ";" + s.fechaCreacion + "\n");
            }
            fw.close();
        } catch (Exception ex) {
        }
    }

    public void cargar() {
        if (archivo == null) {
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(archivo));
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] p = linea.split(";");
                Solicitud s = new Solicitud();
                s.id = Integer.parseInt(p[0]);
                Estudiante e = null;
                for (Estudiante x : est) {
                    if (x.legajo.equals(p[1])) {
                        e = x;
                    }
                }
                if (e == null) {
                    e = new Estudiante(p[1], p[2], p[3]);
                    est.add(e);
                }
                s.estudiante = e;
                s.tipo = p[4];
                s.descripcion = p[5];
                s.prioridad = Integer.parseInt(p[6]);
                s.urgente = Boolean.parseBoolean(p[7]);
                s.estado = p[8];
                int idr = Integer.parseInt(p[9]);
                for (Responsable x : resp) {
                    if (x.id == idr) {
                        s.responsable = x;
                    }
                }
                s.fechaCreacion = LocalDate.parse(p[10]);
                lst.add(s);
                if (s.id > cont) {
                    cont = s.id;
                }
            }
            br.close();
        } catch (Exception ex) {
        }
    }
}
