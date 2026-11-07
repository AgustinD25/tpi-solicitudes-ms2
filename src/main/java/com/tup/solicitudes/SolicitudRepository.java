package com.tup.solicitudes;

import java.util.List;
import java.util.Optional;

/**
 * Almacen de solicitudes. La logica de negocio depende de esta interfaz y no de como se guardan
 * los datos (memoria, archivo CSV, base de datos): principio de inversion de dependencias (DIP).
 */
public interface SolicitudRepository {

    /** Todas las solicitudes, como copia no modificable. */
    List<Solicitud> listarTodas();

    Optional<Solicitud> buscarPorId(int id);

    /** Devuelve el id que corresponde a la proxima solicitud. */
    int generarId();

    /** Agrega una solicitud nueva y la persiste. */
    void guardar(Solicitud nueva);

    /** Persiste los cambios hechos sobre una solicitud que ya estaba guardada. */
    void actualizar(Solicitud existente);

    /** Incorpora lo que haya en el almacenamiento persistente; no hace nada si es solo en memoria. */
    void cargar();
}
