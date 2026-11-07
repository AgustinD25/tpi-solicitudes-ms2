package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SistemaSolicitudes {
    // Codigos de retorno de registrarSolicitud (contrato publico que se mantiene)
    public static final int COD_DATOS_INVALIDOS = -1;
    public static final int COD_LIMITE_PENDIENTES = -2;
    public static final int COD_TIPO_INVALIDO = -3;
    public static final int COD_PRIORIDAD_INVALIDA = -4;
    private static final int SIN_ERROR = 0;   // interno: ningun id valido es 0

    // Reglas de negocio
    static final int MAX_PENDIENTES_POR_ESTUDIANTE = 3;
    static final int PRIORIDAD_MINIMA = 1;
    static final int PRIORIDAD_MAXIMA = 3;

    // Formato del CSV: valor de la columna "responsable" cuando no hay uno asignado
    static final int ID_SIN_RESPONSABLE = -1;

    // lst, est y resp conservan su nombre: forman parte del contrato que usan los tests de caracterizacion
    public List<Solicitud> lst = new ArrayList<>();
    public List<Estudiante> est = new ArrayList<>();
    public List<Responsable> resp = new ArrayList<>();
    int ultimoId = 0;
    String rutaArchivo;

    public SistemaSolicitudes(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    // Las listas se exponen como copias no modificables: quien las lee no puede alterar el estado interno.
    public List<Solicitud> getSolicitudes() {
        return List.copyOf(lst);
    }

    public List<Estudiante> getEstudiantes() {
        return List.copyOf(est);
    }

    public List<Responsable> getResponsables() {
        return List.copyOf(resp);
    }

    // ---------- responsables ----------

    public Responsable registrarResponsable(int id, String nombre, String email, String area) {
        Responsable responsable = new Responsable(id, nombre, email, area);
        if (!responsable.emailValido()) {
            System.out.println("Error: email de responsable invalido");
            return null;
        }
        if (!area.equals("Alumnado") && !area.equals("Academica") && !area.equals("Docencia")) {
            System.out.println("Error: area invalida");
            return null;
        }
        resp.add(responsable);
        return responsable;
    }

    // ---------- registro ----------

    // Devuelve el id de la solicitud, o un codigo negativo si hubo error.
    public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                                  String descripcion, int prioridad, boolean urgente) {
        return registrarSolicitud(legajo, nombre, email, tipo, descripcion, prioridad, urgente, LocalDate.now());
    }

    public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                                  String descripcion, int prioridad, boolean urgente, LocalDate fecha) {
        return registrarSolicitud(new NuevaSolicitud(legajo, nombre, email, tipo, descripcion,
                prioridad, urgente, fecha));
    }

    public int registrarSolicitud(NuevaSolicitud datos) {
        int codigo = validarDatosBasicos(datos);
        if (codigo != SIN_ERROR) {
            return codigo;
        }
        Estudiante estudianteIngresado = new Estudiante(datos.legajo(), datos.nombre(), datos.email());
        if (!estudianteIngresado.emailValido()) {
            System.out.println("Error: email invalido");
            return COD_DATOS_INVALIDOS;
        }
        if (contarPendientes(datos.legajo()) >= MAX_PENDIENTES_POR_ESTUDIANTE) {
            System.out.println("Error: el estudiante ya tiene " + MAX_PENDIENTES_POR_ESTUDIANTE
                    + " solicitudes pendientes");
            return COD_LIMITE_PENDIENTES;
        }
        Estudiante estudiante = buscarOCrearEstudiante(estudianteIngresado);
        int prioridadFinal = calcularPrioridad(datos.prioridad(), datos.urgente());
        Solicitud nueva = crearSolicitud(estudiante, datos, prioridadFinal);
        lst.add(nueva);
        guardar();
        System.out.println("Solicitud #" + nueva.id + " registrada (" + datos.tipo()
                + ", prioridad " + prioridadFinal + ")");
        return nueva.id;
    }

    // Valida legajo, nombre, tipo y prioridad, en ese orden (el orden determina el codigo devuelto).
    private int validarDatosBasicos(NuevaSolicitud datos) {
        if (datos.legajo() == null || datos.legajo().trim().length() == 0) {
            System.out.println("Error: legajo vacio");
            return COD_DATOS_INVALIDOS;
        }
        if (datos.nombre() == null || datos.nombre().trim().length() == 0) {
            System.out.println("Error: nombre vacio");
            return COD_DATOS_INVALIDOS;
        }
        if (!esTipoValido(datos.tipo())) {
            System.out.println("Error: tipo de solicitud invalido");
            return COD_TIPO_INVALIDO;
        }
        if (datos.prioridad() < PRIORIDAD_MINIMA || datos.prioridad() > PRIORIDAD_MAXIMA) {
            System.out.println("Error: prioridad fuera de rango");
            return COD_PRIORIDAD_INVALIDA;
        }
        return SIN_ERROR;
    }

    private boolean esTipoValido(String tipo) {
        return tipo.equals("CERTIFICADO") || tipo.equals("EQUIVALENCIA")
                || tipo.equals("RECLAMO_NOTA") || tipo.equals("BAJA_MATERIA");
    }

    private int contarPendientes(String legajo) {
        int pendientes = 0;
        for (Solicitud solicitud : lst) {
            if (solicitud.estudiante.legajo.equals(legajo) && solicitud.estado.equals("PENDIENTE")) {
                pendientes++;
            }
        }
        return pendientes;
    }

    // Devuelve el estudiante ya registrado con ese legajo o, si no existe, registra el ingresado.
    private Estudiante buscarOCrearEstudiante(Estudiante estudianteIngresado) {
        Estudiante estudiante = null;
        for (Estudiante candidato : est) {
            if (candidato.legajo.equals(estudianteIngresado.legajo)) {
                estudiante = candidato;
            }
        }
        if (estudiante == null) {
            estudiante = estudianteIngresado;
            est.add(estudiante);
        }
        return estudiante;
    }

    // Una solicitud urgente sube un nivel de prioridad, con tope en PRIORIDAD_MAXIMA.
    private int calcularPrioridad(int prioridad, boolean urgente) {
        if (!urgente) {
            return prioridad;
        }
        return Math.min(prioridad + 1, PRIORIDAD_MAXIMA);
    }

    private Solicitud crearSolicitud(Estudiante estudiante, NuevaSolicitud datos, int prioridadFinal) {
        Solicitud nueva = new Solicitud();
        ultimoId = ultimoId + 1;
        nueva.id = ultimoId;
        nueva.estudiante = estudiante;
        nueva.tipo = datos.tipo();
        nueva.descripcion = datos.descripcion();
        nueva.prioridad = prioridadFinal;
        nueva.urgente = datos.urgente();
        nueva.estado = "PENDIENTE";
        nueva.fechaCreacion = datos.fecha();
        return nueva;
    }

    // ---------- estados ----------

    public boolean cambiarEstado(int id, String nuevoEstado) {
        Solicitud solicitud = buscarSolicitudPorId(id);
        if (solicitud == null) {
            System.out.println("Error: no existe la solicitud " + id);
            return false;
        }
        boolean transicionValida = false;
        if (solicitud.estado.equals("PENDIENTE")) {
            if (nuevoEstado.equals("EN_PROCESO")) {
                transicionValida = true;
            } else if (nuevoEstado.equals("RECHAZADA")) {
                transicionValida = true;
            }
        } else if (solicitud.estado.equals("EN_PROCESO")) {
            if (nuevoEstado.equals("RESUELTA")) {
                transicionValida = true;
            } else if (nuevoEstado.equals("RECHAZADA")) {
                transicionValida = true;
            }
        }
        if (!transicionValida) {
            System.out.println("Error: transicion invalida de " + solicitud.estado + " a " + nuevoEstado);
            return false;
        }
        solicitud.estado = nuevoEstado;
        guardar();
        System.out.println("Solicitud #" + id + " ahora esta " + nuevoEstado);
        return true;
    }

    // ---------- responsables ----------

    public boolean asignarResponsable(int idSolicitud, int idResponsable) {
        Solicitud solicitud = buscarSolicitudPorId(idSolicitud);
        if (solicitud == null) {
            System.out.println("Error: no existe la solicitud " + idSolicitud);
            return false;
        }
        Responsable responsable = null;
        for (Responsable candidato : resp) {
            if (candidato.id == idResponsable) {
                responsable = candidato;
            }
        }
        if (responsable == null) {
            System.out.println("Error: no existe el responsable " + idResponsable);
            return false;
        }
        String areaRequerida = null;
        if (solicitud.tipo.equals("CERTIFICADO")) {
            areaRequerida = "Alumnado";
        } else if (solicitud.tipo.equals("EQUIVALENCIA")) {
            areaRequerida = "Academica";
        } else if (solicitud.tipo.equals("RECLAMO_NOTA")) {
            areaRequerida = "Docencia";
        } else if (solicitud.tipo.equals("BAJA_MATERIA")) {
            areaRequerida = "Alumnado";
        }
        if (!responsable.area.equals(areaRequerida)) {
            System.out.println("Error: el responsable es del area " + responsable.area
                    + " y la solicitud requiere " + areaRequerida);
            return false;
        }
        solicitud.responsable = responsable;
        guardar();
        System.out.println("Solicitud #" + idSolicitud + " asignada a " + responsable.nombre);
        return true;
    }

    // ---------- busquedas ----------

    Solicitud buscarSolicitudPorId(int id) {
        for (Solicitud solicitud : lst) {
            if (solicitud.id == id) {
                return solicitud;
            }
        }
        return null;
    }

    public List<Solicitud> filtrarPorEstado(String estado) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : lst) {
            if (solicitud.estado.equals(estado)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorTipo(String tipo) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : lst) {
            if (solicitud.tipo.equals(tipo)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorPrioridad(int prioridad) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : lst) {
            if (solicitud.prioridad == prioridad) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    // ---------- vencimientos ----------

    // Se conserva en la clase como delegacion: es parte del contrato publico que usan los tests.
    public boolean estaVencida(Solicitud solicitud, LocalDate hoy) {
        return solicitud.estaVencida(hoy);
    }

    // ---------- reporte ----------

    public String reporte(LocalDate hoy) {
        int pendientes = 0, enProceso = 0, resueltas = 0, rechazadas = 0;
        int certificados = 0, equivalencias = 0, reclamosNota = 0, bajasMateria = 0;
        for (Solicitud solicitud : lst) {
            if (solicitud.estado.equals("PENDIENTE")) {
                pendientes++;
            } else if (solicitud.estado.equals("EN_PROCESO")) {
                enProceso++;
            } else if (solicitud.estado.equals("RESUELTA")) {
                resueltas++;
            } else if (solicitud.estado.equals("RECHAZADA")) {
                rechazadas++;
            }
            if (solicitud.tipo.equals("CERTIFICADO")) {
                certificados++;
            } else if (solicitud.tipo.equals("EQUIVALENCIA")) {
                equivalencias++;
            } else if (solicitud.tipo.equals("RECLAMO_NOTA")) {
                reclamosNota++;
            } else if (solicitud.tipo.equals("BAJA_MATERIA")) {
                bajasMateria++;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE ===\n");
        sb.append("Por estado:\n");
        sb.append("  PENDIENTE: ").append(pendientes).append("\n");
        sb.append("  EN_PROCESO: ").append(enProceso).append("\n");
        sb.append("  RESUELTA: ").append(resueltas).append("\n");
        sb.append("  RECHAZADA: ").append(rechazadas).append("\n");
        sb.append("Por tipo:\n");
        sb.append("  CERTIFICADO: ").append(certificados).append("\n");
        sb.append("  EQUIVALENCIA: ").append(equivalencias).append("\n");
        sb.append("  RECLAMO_NOTA: ").append(reclamosNota).append("\n");
        sb.append("  BAJA_MATERIA: ").append(bajasMateria).append("\n");
        sb.append("Vencidas:\n");
        int cantidadVencidas = 0;
        for (Solicitud solicitud : lst) {
            if (solicitud.estaVencida(hoy)) {
                sb.append("  #").append(solicitud.id).append(" ").append(solicitud.tipo)
                  .append(" (").append(solicitud.estudiante.legajo).append(") creada ")
                  .append(solicitud.fechaCreacion).append("\n");
                cantidadVencidas++;
            }
        }
        if (cantidadVencidas == 0) {
            sb.append("  (ninguna)\n");
        }
        String texto = sb.toString();
        System.out.print(texto);
        return texto;
    }

    // ---------- impresion ----------

    public void imprimir(Solicitud solicitud) {
        String nombreResponsable = solicitud.responsable == null ? "sin asignar" : solicitud.responsable.nombre;
        System.out.println("#" + solicitud.id + " | " + solicitud.estudiante.legajo + " " + solicitud.estudiante.nombre
                + " | " + solicitud.tipo + " | prioridad " + solicitud.prioridad + (solicitud.urgente ? " (urgente)" : "")
                + " | " + solicitud.estado + " | " + nombreResponsable + " | " + solicitud.fechaCreacion);
    }

    public void imprimirLista(List<Solicitud> solicitudes) {
        if (solicitudes.isEmpty()) {
            System.out.println("(sin resultados)");
        }
        for (Solicitud solicitud : solicitudes) {
            imprimir(solicitud);
        }
    }

    // ---------- persistencia ----------

    void guardar() {
        if (rutaArchivo == null) {
            return;
        }
        try {
            FileWriter escritor = new FileWriter(rutaArchivo);
            for (Solicitud solicitud : lst) {
                escritor.write(solicitud.id + ";" + solicitud.estudiante.legajo + ";" + solicitud.estudiante.nombre + ";"
                        + solicitud.estudiante.email + ";" + solicitud.tipo + ";"
                        + solicitud.descripcion.replace(';', ',') + ";"
                        + solicitud.prioridad + ";" + solicitud.urgente + ";" + solicitud.estado + ";"
                        + (solicitud.responsable == null ? ID_SIN_RESPONSABLE : solicitud.responsable.id) + ";"
                        + solicitud.fechaCreacion + "\n");
            }
            escritor.close();
        } catch (Exception ex) {
        }
    }

    public void cargar() {
        if (rutaArchivo == null) {
            return;
        }
        try {
            BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo));
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] campos = linea.split(";");
                Solicitud solicitud = new Solicitud();
                solicitud.id = Integer.parseInt(campos[0]);
                Estudiante estudiante = null;
                for (Estudiante candidato : est) {
                    if (candidato.legajo.equals(campos[1])) {
                        estudiante = candidato;
                    }
                }
                if (estudiante == null) {
                    estudiante = new Estudiante(campos[1], campos[2], campos[3]);
                    est.add(estudiante);
                }
                solicitud.estudiante = estudiante;
                solicitud.tipo = campos[4];
                solicitud.descripcion = campos[5];
                solicitud.prioridad = Integer.parseInt(campos[6]);
                solicitud.urgente = Boolean.parseBoolean(campos[7]);
                solicitud.estado = campos[8];
                int idResponsable = Integer.parseInt(campos[9]);
                for (Responsable candidato : resp) {
                    if (candidato.id == idResponsable) {
                        solicitud.responsable = candidato;
                    }
                }
                solicitud.fechaCreacion = LocalDate.parse(campos[10]);
                lst.add(solicitud);
                if (solicitud.id > ultimoId) {
                    ultimoId = solicitud.id;
                }
            }
            lector.close();
        } catch (Exception ex) {
        }
    }
}
