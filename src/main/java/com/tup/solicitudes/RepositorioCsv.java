package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Repositorio que ademas guarda las solicitudes en un archivo CSV (una linea por solicitud):
 * id;legajo;nombre;email;tipo;descripcion;prioridad;urgente;estado;idResponsable;fecha
 *
 * Los responsables no se guardan en el archivo, solo su id. Se resuelven con la funcion recibida, y si todavia no
 * estan registrados al cargar, la solicitud recuerda el id y se completa sola en cuanto el responsable aparece (el
 * orden de las llamadas no importa). Mientras tanto, el id se conserva al volver a guardar el archivo.
 *
 * Si no se puede leer o escribir el archivo, el repositorio NO lanza excepciones ni se queda callado: informa el
 * problema con la funcion {@code avisarError} y las operaciones siguen funcionando en memoria.
 */
public class RepositorioCsv extends RepositorioEnMemoria {
    static final int ID_SIN_RESPONSABLE = -1;
    private static final int CANTIDAD_DE_CAMPOS = 11;
    private static final DateTimeFormatter FORMATO_COPIA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final String rutaArchivo;
    private final Function<Integer, Responsable> buscarResponsable;
    private final Consumer<String> avisarError;
    // true si el archivo existe pero no se pudo cargar: antes de reescribirlo se hace una copia de seguridad
    private boolean archivoDanado = false;
    // Solicitudes cargadas cuyo responsable todavia no estaba registrado: solicitud -> id guardado en el archivo
    private final Map<Solicitud, Integer> responsablePendiente = new IdentityHashMap<>();

    public RepositorioCsv(String rutaArchivo, Function<Integer, Responsable> buscarResponsable,
                          Consumer<String> avisarError) {
        this.rutaArchivo = rutaArchivo;
        this.buscarResponsable = buscarResponsable;
        this.avisarError = avisarError;
    }

    @Override
    public List<Solicitud> listarTodas() {
        resolverResponsablesPendientes();
        return super.listarTodas();
    }

    @Override
    public Optional<Solicitud> buscarPorId(int id) {
        resolverResponsablesPendientes();
        return super.buscarPorId(id);
    }

    @Override
    public void guardar(Solicitud nueva) {
        super.guardar(nueva);
        escribir();
    }

    @Override
    protected void vaciar() {
        super.vaciar();
        responsablePendiente.clear();
    }

    // Completa las solicitudes cuyo responsable se registro despues de cargar el archivo.
    private void resolverResponsablesPendientes() {
        Iterator<Map.Entry<Solicitud, Integer>> pendientes = responsablePendiente.entrySet().iterator();
        while (pendientes.hasNext()) {
            Map.Entry<Solicitud, Integer> pendiente = pendientes.next();
            Solicitud solicitud = pendiente.getKey();
            if (solicitud.getResponsable() != null) {
                pendientes.remove();          // ya se le asigno uno a mano: gana la asignacion nueva
                continue;
            }
            Responsable responsable = buscarResponsable.apply(pendiente.getValue());
            if (responsable != null) {
                solicitud.restaurarResponsable(responsable);
                pendientes.remove();
            }
        }
    }

    @Override
    public void actualizar(Solicitud existente) {
        escribir();
    }

    /**
     * Arma TODO el contenido antes de abrir el archivo: si algo falla al armarlo, el archivo anterior queda intacto
     * (antes se abria primero, lo que lo truncaba, y un error a mitad de camino lo dejaba vacio).
     * Si el archivo estaba danado y no se pudo cargar, primero guarda una copia de seguridad: no se reescribe
     * un archivo cuyo contenido el sistema no logro leer.
     */
    private void escribir() {
        StringBuilder contenido = new StringBuilder();
        for (Solicitud solicitud : listarTodas()) {
            contenido.append(aLinea(solicitud));
        }
        if (archivoDanado && !respaldarArchivoDanado()) {
            return;
        }
        try (FileWriter escritor = new FileWriter(rutaArchivo)) {
            escritor.write(contenido.toString());
        } catch (IOException ex) {
            avisarError.accept("no se pudo guardar el archivo " + rutaArchivo + ": " + ex.getMessage());
        }
    }

    // Copia el archivo danado a <ruta>.bak-<fecha> antes de reescribirlo. Devuelve false si no se pudo copiar.
    private boolean respaldarArchivoDanado() {
        Path origen = Path.of(rutaArchivo);
        if (!Files.exists(origen)) {
            archivoDanado = false;
            return true;
        }
        Path copia = Path.of(rutaArchivo + ".bak-" + LocalDateTime.now().format(FORMATO_COPIA));
        try {
            Files.copy(origen, copia);
        } catch (IOException ex) {
            avisarError.accept("no se pudo guardar una copia del archivo danado " + rutaArchivo
                    + ", asi que no se reescribe: " + ex);
            return false;
        }
        archivoDanado = false;
        avisarError.accept("el archivo " + rutaArchivo + " estaba danado: se guardo una copia en " + copia
                + " antes de reescribirlo");
        return true;
    }

    private String aLinea(Solicitud solicitud) {
        String descripcion = solicitud.getDescripcion() == null ? "" : solicitud.getDescripcion();
        Estudiante estudiante = solicitud.getEstudiante();
        return solicitud.getId() + ";" + FormatoCsv.escapar(estudiante.getLegajo()) + ";"
                + FormatoCsv.escapar(estudiante.getNombre()) + ";"
                + FormatoCsv.escapar(estudiante.getEmail()) + ";" + solicitud.getTipo() + ";"
                + FormatoCsv.escapar(descripcion) + ";"
                + solicitud.getPrioridad() + ";" + solicitud.isUrgente() + ";" + solicitud.getEstado() + ";"
                + idResponsableParaGuardar(solicitud)
                + ";" + solicitud.getFechaCreacion() + "\n";
    }

    // Si el responsable aun no esta registrado se conserva el id del archivo, para no perder la asignacion.
    private int idResponsableParaGuardar(Solicitud solicitud) {
        if (solicitud.getResponsable() != null) {
            return solicitud.getResponsable().getId();
        }
        return responsablePendiente.getOrDefault(solicitud, ID_SIN_RESPONSABLE);
    }

    /**
     * Reemplaza lo que hay en memoria por el contenido del archivo, por lo que llamarlo varias veces da el mismo
     * resultado. Si el archivo todavia no existe (primera ejecucion) no hace nada.
     *
     * Todo o nada: se lee el archivo completo y la memoria solo se reemplaza si TODAS las lineas son validas.
     * Si alguna no lo es, no se carga nada, se informa en que lineas, y el repositorio queda protegido: al guardar,
     * antes de reescribir el archivo se hace una copia de seguridad, y los ids nuevos continuan despues del mayor id
     * que aparece en el archivo (aunque su linea sea invalida), asi no se reutiliza ninguno.
     */
    @Override
    public void cargar() {
        if (!new File(rutaArchivo).isFile()) {
            return;
        }
        List<Solicitud> leidas = new ArrayList<>();
        Map<String, Estudiante> estudiantesLeidos = new LinkedHashMap<>();
        Map<Solicitud, Integer> pendientesLeidos = new IdentityHashMap<>();
        List<String> lineasInvalidas = new ArrayList<>();
        int mayorIdVisto = 0;
        int numeroDeLinea = 0;
        try (BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                numeroDeLinea++;
                if (linea.isBlank()) {
                    continue;
                }
                mayorIdVisto = Math.max(mayorIdVisto, idDeLinea(linea));
                try {
                    leidas.add(leerLinea(linea, estudiantesLeidos, pendientesLeidos));
                } catch (IllegalArgumentException | DateTimeParseException ex) {
                    lineasInvalidas.add("linea " + numeroDeLinea + " (" + ex.getMessage() + ")");
                }
            }
        } catch (IOException ex) {
            avisarError.accept("no se pudo leer el archivo " + rutaArchivo + ": " + ex.getMessage());
            return;
        }
        if (!lineasInvalidas.isEmpty()) {
            archivoDanado = true;
            reservarIdsHasta(mayorIdVisto);
            avisarError.accept("no se pudo leer el archivo " + rutaArchivo + ": lineas invalidas "
                    + String.join(", ", lineasInvalidas) + ". No se cargo ningun dato");
            return;
        }
        vaciar();
        estudiantesLeidos.values().forEach(this::agregar);
        leidas.forEach(this::incorporar);
        responsablePendiente.putAll(pendientesLeidos);
        archivoDanado = false;
    }

    // Id de la primera columna, o 0 si no se puede leer (sirve para no reutilizar ids de lineas invalidas).
    private static int idDeLinea(String linea) {
        try {
            return Integer.parseInt(FormatoCsv.dividir(linea).get(0).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // Convierte una linea en una solicitud. Lanza IllegalArgumentException o DateTimeParseException si es invalida.
    private Solicitud leerLinea(String linea, Map<String, Estudiante> estudiantes,
                                Map<Solicitud, Integer> pendientes) {
        List<String> campos = FormatoCsv.dividir(linea);
        if (campos.size() != CANTIDAD_DE_CAMPOS) {
            throw new IllegalArgumentException("se esperaban " + CANTIDAD_DE_CAMPOS
                    + " campos y la linea tiene " + campos.size());
        }
        int id = Integer.parseInt(campos.get(0));
        TipoSolicitud tipo = TipoSolicitud.desde(campos.get(4));
        if (tipo == null) {
            throw new IllegalArgumentException("tipo desconocido: " + campos.get(4));
        }
        int prioridad = Integer.parseInt(campos.get(6));
        boolean urgente = Boolean.parseBoolean(campos.get(7));
        EstadoSolicitud estado = EstadoSolicitud.desde(campos.get(8));
        if (estado == null) {
            throw new IllegalArgumentException("estado desconocido: " + campos.get(8));
        }
        int idResponsable = Integer.parseInt(campos.get(9));
        LocalDate fechaCreacion = LocalDate.parse(campos.get(10));
        Estudiante estudiante = estudiantes.computeIfAbsent(campos.get(1),
                legajo -> new Estudiante(legajo, campos.get(2), campos.get(3)));
        Responsable responsable = buscarResponsable.apply(idResponsable);
        Solicitud solicitud = new Solicitud(id, estudiante, tipo, campos.get(5), prioridad, urgente, fechaCreacion);
        solicitud.restaurar(estado, responsable);
        if (responsable == null && idResponsable != ID_SIN_RESPONSABLE) {
            pendientes.put(solicitud, idResponsable);
        }
        return solicitud;
    }
}
