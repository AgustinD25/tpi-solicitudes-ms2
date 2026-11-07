package com.tup.solicitudes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final List<Responsable> responsables = new ArrayList<>();
    private final ReporteService reporteService = new ReporteService();
    private final PresentadorSolicitud presentador = new PresentadorSolicitud();

    /** Sin ruta, los datos quedan solo en memoria; con ruta, se guardan ademas en un archivo CSV. */
    public SistemaSolicitudes(String rutaArchivo) {
        RepositorioEnMemoria repositorio = rutaArchivo == null
                ? new RepositorioEnMemoria()
                : new RepositorioCsv(rutaArchivo, this::buscarResponsablePorId);
        this.solicitudRepository = repositorio;
        this.estudianteRepository = repositorio;
    }

    /** Permite elegir cualquier almacenamiento (DIP). */
    public SistemaSolicitudes(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository) {
        this.solicitudRepository = solicitudRepository;
        this.estudianteRepository = estudianteRepository;
    }

    // Las listas se exponen como copias no modificables: quien las lee no puede alterar el estado interno.
    public List<Solicitud> getSolicitudes() {
        return solicitudRepository.listarTodas();
    }

    public List<Estudiante> getEstudiantes() {
        return estudianteRepository.listarTodos();
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

    private Responsable buscarResponsablePorId(int id) {
        Responsable encontrado = null;
        for (Responsable candidato : responsables) {
            if (candidato.getId() == id) {
                encontrado = candidato;
            }
        }
        return encontrado;
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
        solicitudRepository.guardar(nueva);
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
        for (Solicitud solicitud : solicitudRepository.listarTodas()) {
            if (solicitud.getEstudiante().getLegajo().equals(legajo)
                    && solicitud.getEstadoSolicitud() == EstadoSolicitud.PENDIENTE) {
                pendientes++;
            }
        }
        return pendientes;
    }

    // Devuelve el estudiante ya registrado con ese legajo o, si no existe, registra el ingresado.
    private Estudiante buscarOCrearEstudiante(Estudiante estudianteIngresado) {
        Estudiante estudiante = estudianteRepository.buscarPorLegajo(estudianteIngresado.getLegajo()).orElse(null);
        if (estudiante == null) {
            estudiante = estudianteIngresado;
            estudianteRepository.agregar(estudiante);
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
        return new Solicitud(solicitudRepository.generarId(), estudiante, TipoSolicitud.desde(datos.tipo()),
                datos.descripcion(), prioridadFinal, datos.urgente(), datos.fecha());
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
        solicitudRepository.actualizar(solicitud);
        System.out.println("Solicitud #" + id + " ahora esta " + nuevoEstado);
        return true;
    }

    // ---------- asignacion ----------

    public boolean asignarResponsable(int idSolicitud, int idResponsable) {
        Solicitud solicitud = buscarSolicitudPorId(idSolicitud);
        if (solicitud == null) {
            System.out.println("Error: no existe la solicitud " + idSolicitud);
            return false;
        }
        Responsable responsable = buscarResponsablePorId(idResponsable);
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
        solicitudRepository.actualizar(solicitud);
        System.out.println("Solicitud #" + idSolicitud + " asignada a " + responsable.getNombre());
        return true;
    }

    // ---------- busquedas ----------

    private Solicitud buscarSolicitudPorId(int id) {
        return solicitudRepository.buscarPorId(id).orElse(null);
    }

    public List<Solicitud> filtrarPorEstado(String estado) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudRepository.listarTodas()) {
            if (solicitud.getEstado().equals(estado)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorTipo(String tipo) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudRepository.listarTodas()) {
            if (solicitud.getTipo().equals(tipo)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> filtrarPorPrioridad(int prioridad) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudRepository.listarTodas()) {
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
        String texto = reporteService.generar(solicitudRepository.listarTodas(), hoy);
        System.out.print(texto);
        return texto;
    }

    // ---------- impresion ----------

    public void imprimir(Solicitud solicitud) {
        System.out.println(presentador.formatear(solicitud));
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

    /**
     * Carga lo guardado. Los responsables deben estar registrados antes: si no, las solicitudes cargadas quedan
     * sin responsable (acoplamiento temporal, smell 14, conservado hasta corregir el defecto).
     */
    public void cargar() {
        solicitudRepository.cargar();
    }
}
