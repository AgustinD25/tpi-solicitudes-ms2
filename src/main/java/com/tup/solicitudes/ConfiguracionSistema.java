package com.tup.solicitudes;

/**
 * Unico lugar donde se arman las dependencias del sistema: elige el almacenamiento, crea los servicios y se los
 * pasa a la fachada. Ninguna otra clase crea sus propias dependencias.
 */
public final class ConfiguracionSistema {

    /** Las piezas que necesita la fachada {@link SistemaSolicitudes}. */
    public record Componentes(SolicitudService solicitudes, CatalogoResponsables responsables,
                              ReporteService reportes, PresentadorSolicitud presentador) {
    }

    private ConfiguracionSistema() {
    }

    /** Sin ruta, los datos quedan solo en memoria; con ruta, se guardan ademas en un archivo CSV. */
    public static Componentes paraArchivo(String rutaArchivo) {
        ResponsableService responsables = new ResponsableService();
        RepositorioEnMemoria repositorio = rutaArchivo == null
                ? new RepositorioEnMemoria()
                : new RepositorioCsv(rutaArchivo, id -> responsables.buscarPorId(id),
                        mensaje -> System.out.println("Error: " + mensaje));
        return con(repositorio, repositorio, responsables);
    }

    /**
     * Arma el sistema con el almacenamiento y el catalogo que se indiquen (por ejemplo un repositorio CSV cuyo
     * buscador de responsables consulta el mismo catalogo que se pasa aca).
     */
    public static Componentes con(SolicitudRepository solicitudes, EstudianteRepository estudiantes,
                                  CatalogoResponsables responsables) {
        return new Componentes(new SolicitudService(solicitudes, estudiantes, responsables), responsables,
                new ReporteService(), new PresentadorSolicitud());
    }
}
