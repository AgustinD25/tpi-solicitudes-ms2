package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final List<Solicitud> solicitudes = new ArrayList<>();
    private final List<Estudiante> estudiantes = new ArrayList<>();
    private final List<Responsable> responsables = new ArrayList<>();
    private int ultimoId = 0;
    private final String rutaArchivo;

    public SistemaSolicitudes(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    // Las listas se exponen como copias no modificables: quien las lee no puede alterar el estado interno.
    public List<Solicitud> getSolicitudes() {
        return List.copyOf(solicitudes);
    }

    public List<Estudiante> getEstudiantes() {
        return List.copyOf(estudiantes);
    }

    public List<Responsable> getResponsables() {
        return List.copyOf(responsables);
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
        responsables.add(responsable);
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
        solicitudes.add(nueva);
        guardar();
        System.out.println("Solicitud #" + nueva.getId() + " registrada (" + datos.tipo()
                + ", prioridad " + prioridadFinal + ")");
        return nueva.getId();
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
        if (TipoSolicitud.desde(datos.tipo()) == null) {
            System.out.println("Error: tipo de solicitud invalido");
            return COD_TIPO_INVALIDO;
        }
        if (datos.prioridad() < PRIORIDAD_MINIMA || datos.prioridad() > PRIORIDAD_MAXIMA) {
            System.out.println("Error: prioridad fuera de rango");
            return COD_PRIORIDAD_INVALIDA;
        }
        return SIN_ERROR;
    }

    private int contarPendientes(String legajo) {
        int pendientes = 0;
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getEstudiante().getLegajo().equals(legajo)
                    && solicitud.getEstadoSolicitud() == EstadoSolicitud.PENDIENTE) {
                pendientes++;
            }
        }
        return pendientes;
    }

    // Devuelve el estudiante ya registrado con ese legajo o, si no existe, registra el ingresado.
    private Estudiante buscarOCrearEstudiante(Estudiante estudianteIngresado) {
        Estudiante estudiante = null;
        for (Estudiante candidato : estudiantes) {
            if (candidato.getLegajo().equals(estudianteIngresado.getLegajo())) {
                estudiante = candidato;
            }
        }
        if (estudiante == null) {
            estudiante = estudianteIngresado;
            estudiantes.add(estudiante);
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
        ultimoId = ultimoId + 1;
        return new Solicitud(ultimoId, estudiante, TipoSolicitud.desde(datos.tipo()), datos.descripcion(),
                prioridadFinal, datos.urgente(), datos.fecha());
    }

    // ---------- estados ----------

    public boolean cambiarEstado(int id, String nuevoEstado) {
        Solicitud solicitud = buscarSolicitudPorId(id);
        if (solicitud == null) {
            System.out.println("Error: no existe la solicitud " + id);
            return false;
        }
        if (!solicitud.getEstadoSolicitud().esFinal()) {
            // Defecto D5 conservado a proposito: con estado nulo la version original lanzaba
            // NullPointerException (salvo si la solicitud ya estaba en un estado final).
            Objects.requireNonNull(nuevoEstado);
        }
        EstadoSolicitud destino = EstadoSolicitud.desde(nuevoEstado);
        if (destino == null || !solicitud.puedePasarA(destino)) {
            System.out.println("Error: transicion invalida de " + solicitud.getEstado() + " a " + nuevoEstado);
            return false;
        }
        solicitud.pasarA(destino);
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
        for (Responsable candidato : responsables) {
            if (candidato.getId() == idResponsable) {
                responsable = candidato;
            }
        }
        if (responsable == null) {
            System.out.println("Error: no existe el responsable " + idResponsable);
            return false;
        }
        if (!solicitud.puedeSerAtendidaPor(responsable)) {
            System.out.println("Error: el responsable es del area " + responsable.getArea()
                    + " y la solicitud requiere " + solicitud.getTipoSolicitud().getAreaResponsable());
            return false;
        }
        solicitud.asignarResponsable(responsable);
        guardar();
        System.out.println("Solicitud #" + idSolicitud + " asignada a " + responsable.getNombre());
        return true;
    }

    // ---------- busquedas ----------

    Solicitud buscarSolicitudPorId(int id) {
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getId() == id) {
                return solicitud;
            }
        }
        return null;
    }

    public List<Solicitud> filtrarPorEstado(String estado) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getEstado().equals(estado)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorTipo(String tipo) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getTipo().equals(tipo)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorPrioridad(int prioridad) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getPrioridad() == prioridad) {
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
        String texto = sb.toString();
        System.out.print(texto);
        return texto;
    }

    // ---------- impresion ----------

    public void imprimir(Solicitud solicitud) {
        String nombreResponsable = solicitud.getResponsable() == null
                ? "sin asignar" : solicitud.getResponsable().getNombre();
        System.out.println("#" + solicitud.getId() + " | " + solicitud.getEstudiante().getLegajo()
                + " " + solicitud.getEstudiante().getNombre()
                + " | " + solicitud.getTipo() + " | prioridad " + solicitud.getPrioridad()
                + (solicitud.isUrgente() ? " (urgente)" : "")
                + " | " + solicitud.getEstado() + " | " + nombreResponsable
                + " | " + solicitud.getFechaCreacion());
    }

    public void imprimirLista(List<Solicitud> listado) {
        if (listado.isEmpty()) {
            System.out.println("(sin resultados)");
        }
        for (Solicitud solicitud : listado) {
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
            for (Solicitud solicitud : solicitudes) {
                escritor.write(solicitud.getId() + ";" + solicitud.getEstudiante().getLegajo() + ";"
                        + solicitud.getEstudiante().getNombre() + ";"
                        + solicitud.getEstudiante().getEmail() + ";" + solicitud.getTipo() + ";"
                        + solicitud.getDescripcion().replace(';', ',') + ";"
                        + solicitud.getPrioridad() + ";" + solicitud.isUrgente() + ";" + solicitud.getEstado() + ";"
                        + (solicitud.getResponsable() == null ? ID_SIN_RESPONSABLE : solicitud.getResponsable().getId())
                        + ";" + solicitud.getFechaCreacion() + "\n");
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
                int id = Integer.parseInt(campos[0]);
                Estudiante estudiante = null;
                for (Estudiante candidato : estudiantes) {
                    if (candidato.getLegajo().equals(campos[1])) {
                        estudiante = candidato;
                    }
                }
                if (estudiante == null) {
                    estudiante = new Estudiante(campos[1], campos[2], campos[3]);
                    estudiantes.add(estudiante);
                }
                TipoSolicitud tipo = TipoSolicitud.desde(campos[4]);
                if (tipo == null) {
                    throw new IllegalArgumentException("tipo desconocido en el archivo: " + campos[4]);
                }
                int prioridad = Integer.parseInt(campos[6]);
                boolean urgente = Boolean.parseBoolean(campos[7]);
                EstadoSolicitud estado = EstadoSolicitud.desde(campos[8]);
                if (estado == null) {
                    throw new IllegalArgumentException("estado desconocido en el archivo: " + campos[8]);
                }
                int idResponsable = Integer.parseInt(campos[9]);
                Responsable responsable = null;
                for (Responsable candidato : responsables) {
                    if (candidato.getId() == idResponsable) {
                        responsable = candidato;
                    }
                }
                LocalDate fechaCreacion = LocalDate.parse(campos[10]);
                Solicitud solicitud = new Solicitud(id, estudiante, tipo, campos[5], prioridad, urgente, fechaCreacion);
                solicitud.restaurar(estado, responsable);
                solicitudes.add(solicitud);
                if (solicitud.getId() > ultimoId) {
                    ultimoId = solicitud.getId();
                }
            }
            lector.close();
        } catch (Exception ex) {
        }
    }
}
