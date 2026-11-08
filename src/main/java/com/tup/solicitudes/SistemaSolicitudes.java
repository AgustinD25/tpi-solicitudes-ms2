package com.tup.solicitudes;

import java.time.LocalDate;
import java.util.List;

/**
 * Fachada (Adapter) que conserva el contrato publico de la version original: devuelve codigos
 * (-1/-2/-3/-4, false, null) e imprime los mensajes por consola. Por dentro delega en
 * {@link SolicitudService}, {@link ResponsableService} y {@link ReporteService}, que informan los errores
 * con excepciones de dominio; aca se las atrapa y se las traduce al contrato de siempre.
 */
public class SistemaSolicitudes {
    // Codigos de retorno de registrarSolicitud (contrato publico que se mantiene)
    public static final int COD_DATOS_INVALIDOS = -1;
    public static final int COD_LIMITE_PENDIENTES = -2;
    public static final int COD_TIPO_INVALIDO = -3;
    public static final int COD_PRIORIDAD_INVALIDA = -4;

    private final SolicitudService solicitudService;
    private final CatalogoResponsables responsableService;
    private final ReporteService reporteService;
    private final PresentadorSolicitud presentador;

    /** Sin ruta, los datos quedan solo en memoria; con ruta, se guardan ademas en un archivo CSV. */
    public SistemaSolicitudes(String rutaArchivo) {
        this(ConfiguracionSistema.paraArchivo(rutaArchivo));
    }

    /**
     * Crea el sistema con las piezas ya armadas por {@link ConfiguracionSistema} (cualquier almacenamiento: DIP).
     * Es un metodo de fabrica y no un constructor publico para que {@code new SistemaSolicitudes(null)} siga
     * significando "solo memoria" sin ambiguedad.
     */
    public static SistemaSolicitudes con(ConfiguracionSistema.Componentes componentes) {
        return new SistemaSolicitudes(componentes);
    }

    private SistemaSolicitudes(ConfiguracionSistema.Componentes componentes) {
        this.solicitudService = componentes.solicitudes();
        this.responsableService = componentes.responsables();
        this.reporteService = componentes.reportes();
        this.presentador = componentes.presentador();
    }

    // Las listas se exponen como copias no modificables: quien las lee no puede alterar el estado interno.
    public List<Solicitud> getSolicitudes() {
        return solicitudService.listarTodas();
    }

    public List<Estudiante> getEstudiantes() {
        return solicitudService.listarEstudiantes();
    }

    public List<Responsable> getResponsables() {
        return responsableService.listarTodos();
    }

    // ---------- responsables ----------

    public Responsable registrarResponsable(int id, String nombre, String email, String area) {
        try {
            return responsableService.registrar(id, nombre, email, area);
        } catch (DominioException error) {
            System.out.println("Error: " + error.getMessage());
            return null;
        }
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
        try {
            Solicitud nueva = solicitudService.registrar(datos);
            System.out.println("Solicitud #" + nueva.getId() + " registrada (" + datos.tipo()
                    + ", prioridad " + nueva.getPrioridad() + ")");
            return nueva.getId();
        } catch (DominioException error) {
            System.out.println("Error: " + error.getMessage());
            return codigoDeError(error);
        }
    }

    // Traduce la excepcion de dominio al codigo negativo del contrato original.
    private static int codigoDeError(DominioException error) {
        if (error instanceof LimitePendientesException) {
            return COD_LIMITE_PENDIENTES;
        }
        if (error instanceof TipoInvalidoException) {
            return COD_TIPO_INVALIDO;
        }
        if (error instanceof PrioridadInvalidaException) {
            return COD_PRIORIDAD_INVALIDA;
        }
        return COD_DATOS_INVALIDOS;
    }

    // ---------- estados ----------

    public boolean cambiarEstado(int id, String nuevoEstado) {
        try {
            solicitudService.cambiarEstado(id, nuevoEstado);
            System.out.println("Solicitud #" + id + " ahora esta " + nuevoEstado);
            return true;
        } catch (DominioException error) {
            System.out.println("Error: " + error.getMessage());
            return false;
        }
    }

    // ---------- asignacion ----------

    public boolean asignarResponsable(int idSolicitud, int idResponsable) {
        try {
            Solicitud solicitud = solicitudService.asignarResponsable(idSolicitud, idResponsable);
            System.out.println("Solicitud #" + idSolicitud + " asignada a " + solicitud.getResponsable().getNombre());
            return true;
        } catch (DominioException error) {
            System.out.println("Error: " + error.getMessage());
            return false;
        }
    }

    // ---------- busquedas ----------

    public List<Solicitud> filtrarPorEstado(String estado) {
        return solicitudService.filtrar(solicitud -> solicitud.getEstado().equals(estado));
    }

    public List<Solicitud> filtrarPorTipo(String tipo) {
        return solicitudService.filtrar(solicitud -> solicitud.getTipo().equals(tipo));
    }

    public List<Solicitud> filtrarPorPrioridad(int prioridad) {
        return solicitudService.filtrar(solicitud -> solicitud.getPrioridad() == prioridad);
    }

    // ---------- vencimientos ----------

    // Se conserva en la clase como delegacion: es parte del contrato publico que usan los tests.
    public boolean estaVencida(Solicitud solicitud, LocalDate hoy) {
        return solicitud.estaVencida(hoy);
    }

    // ---------- reporte ----------

    public String reporte(LocalDate hoy) {
        String texto = reporteService.generar(solicitudService.listarTodas(), hoy);
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
     * Carga lo guardado. No importa si los responsables se registran antes o despues: las solicitudes cargadas
     * completan su responsable en cuanto este existe.
     */
    public void cargar() {
        solicitudService.cargar();
    }
}
