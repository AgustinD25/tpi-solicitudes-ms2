package com.tup.solicitudes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Repositorio que ademas guarda las solicitudes en un archivo CSV (una linea por solicitud):
 * id;legajo;nombre;email;tipo;descripcion;prioridad;urgente;estado;idResponsable;fecha
 *
 * Los responsables no se guardan en el archivo, solo su id; al cargar se resuelven con la funcion recibida.
 *
 * Si no se puede leer o escribir el archivo, el repositorio NO lanza excepciones ni se queda callado: informa el
 * problema con la funcion {@code avisarError} y las operaciones siguen funcionando en memoria.
 */
public class RepositorioCsv extends RepositorioEnMemoria {
    static final int ID_SIN_RESPONSABLE = -1;

    private final String rutaArchivo;
    private final Function<Integer, Responsable> buscarResponsable;
    private final Consumer<String> avisarError;

    public RepositorioCsv(String rutaArchivo, Function<Integer, Responsable> buscarResponsable,
                          Consumer<String> avisarError) {
        this.rutaArchivo = rutaArchivo;
        this.buscarResponsable = buscarResponsable;
        this.avisarError = avisarError;
    }

    @Override
    public void guardar(Solicitud nueva) {
        super.guardar(nueva);
        escribir();
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
        return solicitud.getId() + ";" + solicitud.getEstudiante().getLegajo() + ";"
                + solicitud.getEstudiante().getNombre() + ";"
                + solicitud.getEstudiante().getEmail() + ";" + solicitud.getTipo() + ";"
                + descripcion.replace(';', ',') + ";"
                + solicitud.getPrioridad() + ";" + solicitud.isUrgente() + ";" + solicitud.getEstado() + ";"
                + (solicitud.getResponsable() == null ? ID_SIN_RESPONSABLE : solicitud.getResponsable().getId())
                + ";" + solicitud.getFechaCreacion() + "\n";
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
                String[] campos = linea.split(";");
                int id = Integer.parseInt(campos[0]);
                Estudiante estudiante = buscarPorLegajo(campos[1]).orElse(null);
                if (estudiante == null) {
                    estudiante = new Estudiante(campos[1], campos[2], campos[3]);
                    agregar(estudiante);
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
                Responsable responsable = buscarResponsable.apply(Integer.parseInt(campos[9]));
                LocalDate fechaCreacion = LocalDate.parse(campos[10]);
                Solicitud solicitud = new Solicitud(id, estudiante, tipo, campos[5], prioridad, urgente, fechaCreacion);
                solicitud.restaurar(estado, responsable);
                incorporar(solicitud);
            }
        } catch (Exception ex) {
            avisarError.accept("no se pudo leer el archivo " + rutaArchivo + ": " + ex);
        }
    }
}
