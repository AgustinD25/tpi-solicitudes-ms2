package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.Iterator;
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

    private final String rutaArchivo;
    private final Function<Integer, Responsable> buscarResponsable;
    private final Consumer<String> avisarError;
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
                solicitud.asignarResponsable(responsable);
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
     */
    private void escribir() {
        StringBuilder contenido = new StringBuilder();
        for (Solicitud solicitud : listarTodas()) {
            contenido.append(aLinea(solicitud));
        }
        try (FileWriter escritor = new FileWriter(rutaArchivo)) {
            escritor.write(contenido.toString());
        } catch (IOException ex) {
            avisarError.accept("no se pudo guardar el archivo " + rutaArchivo + ": " + ex.getMessage());
        }
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
     */
    @Override
    public void cargar() {
        if (!new File(rutaArchivo).isFile()) {
            return;
        }
        vaciar();
        try (BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                List<String> campos = FormatoCsv.dividir(linea);
                if (campos.size() != CANTIDAD_DE_CAMPOS) {
                    throw new IllegalArgumentException("se esperaban " + CANTIDAD_DE_CAMPOS
                            + " campos y la linea tiene " + campos.size());
                }
                int id = Integer.parseInt(campos.get(0));
                Estudiante estudiante = buscarPorLegajo(campos.get(1)).orElse(null);
                if (estudiante == null) {
                    estudiante = new Estudiante(campos.get(1), campos.get(2), campos.get(3));
                    agregar(estudiante);
                }
                TipoSolicitud tipo = TipoSolicitud.desde(campos.get(4));
                if (tipo == null) {
                    throw new IllegalArgumentException("tipo desconocido en el archivo: " + campos.get(4));
                }
                int prioridad = Integer.parseInt(campos.get(6));
                boolean urgente = Boolean.parseBoolean(campos.get(7));
                EstadoSolicitud estado = EstadoSolicitud.desde(campos.get(8));
                if (estado == null) {
                    throw new IllegalArgumentException("estado desconocido en el archivo: " + campos.get(8));
                }
                int idResponsable = Integer.parseInt(campos.get(9));
                Responsable responsable = buscarResponsable.apply(idResponsable);
                LocalDate fechaCreacion = LocalDate.parse(campos.get(10));
                Solicitud solicitud = new Solicitud(id, estudiante, tipo, campos.get(5), prioridad, urgente, fechaCreacion);
                solicitud.restaurar(estado, responsable);
                if (responsable == null && idResponsable != ID_SIN_RESPONSABLE) {
                    responsablePendiente.put(solicitud, idResponsable);
                }
                incorporar(solicitud);
            }
        } catch (Exception ex) {
            avisarError.accept("no se pudo leer el archivo " + rutaArchivo + ": " + ex);
        }
    }
}
