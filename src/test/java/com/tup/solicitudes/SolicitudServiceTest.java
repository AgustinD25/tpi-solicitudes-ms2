package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios (CR-07) del servicio, armado a mano con repositorios en memoria: no hay consola ni archivos, y
 * los errores se prueban como excepciones, que es lo que informa el servicio.
 */
class SolicitudServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    private ResponsableService responsables;
    private SolicitudService servicio;

    @BeforeEach
    void armar() {
        RepositorioEnMemoria memoria = new RepositorioEnMemoria();
        responsables = new ResponsableService();
        servicio = new SolicitudService(memoria, memoria, responsables);
        responsables.registrar(1, "Laura", "laura@ejemplo.edu", "Alumnado");
        responsables.registrar(2, "Carlos", "carlos@ejemplo.edu", "Academica");
    }

    private NuevaSolicitud datos(String legajo, String tipo, int prioridad, boolean urgente) {
        return new NuevaSolicitud(legajo, "Estudiante " + legajo, legajo + "@ejemplo.edu", tipo, "d", prioridad,
                urgente, FECHA);
    }

    private Solicitud registrar(String legajo, String tipo) {
        return servicio.registrar(datos(legajo, tipo, 1, false));
    }

    // ---------- registrar ----------

    @Test
    void registraUnaSolicitudPendienteConIdsSecuenciales() {
        Solicitud primera = registrar("L1", "CERTIFICADO");
        Solicitud segunda = registrar("L2", "EQUIVALENCIA");

        assertEquals(1, primera.getId());
        assertEquals(2, segunda.getId());
        assertEquals(EstadoSolicitud.PENDIENTE, primera.getEstadoSolicitud());
        assertEquals(TipoSolicitud.EQUIVALENCIA, segunda.getTipoSolicitud());
    }

    @Test
    void laUrgenciaSubeUnNivelConTopeEnTres() {
        assertEquals(2, servicio.registrar(datos("L1", "CERTIFICADO", 1, true)).getPrioridad());
        assertEquals(3, servicio.registrar(datos("L2", "CERTIFICADO", 2, true)).getPrioridad());
        assertEquals(3, servicio.registrar(datos("L3", "CERTIFICADO", 3, true)).getPrioridad());
        assertEquals(1, servicio.registrar(datos("L4", "CERTIFICADO", 1, false)).getPrioridad());
    }

    @Test
    void reutilizaElEstudianteDeUnLegajoYaRegistrado() {
        Solicitud primera = registrar("L1", "CERTIFICADO");
        Solicitud segunda = registrar("L1", "EQUIVALENCIA");

        assertSame(primera.getEstudiante(), segunda.getEstudiante());
        assertEquals(1, servicio.listarEstudiantes().size());
    }

    @Test
    void validaEnOrdenLegajoNombreTipoPrioridadYEmail() {
        assertEquals("legajo vacio", assertThrows(DatosInvalidosException.class,
                () -> servicio.registrar(new NuevaSolicitud(" ", "Ana", "a@b.com", "FOO", "d", 9, false, FECHA)))
                .getMessage());
        assertEquals("nombre vacio", assertThrows(DatosInvalidosException.class,
                () -> servicio.registrar(new NuevaSolicitud("L1", null, "a@b.com", "FOO", "d", 9, false, FECHA)))
                .getMessage());
        assertThrows(TipoInvalidoException.class,
                () -> servicio.registrar(new NuevaSolicitud("L1", "Ana", "malo", "FOO", "d", 9, false, FECHA)));
        assertThrows(PrioridadInvalidaException.class,
                () -> servicio.registrar(new NuevaSolicitud("L1", "Ana", "malo", "CERTIFICADO", "d", 0, false, FECHA)));
        assertEquals("email invalido", assertThrows(DatosInvalidosException.class,
                () -> servicio.registrar(new NuevaSolicitud("L1", "Ana", "malo", "CERTIFICADO", "d", 1, false, FECHA)))
                .getMessage());
        assertTrue(servicio.listarTodas().isEmpty());
    }

    @Test
    void elCuartoPendienteDeUnEstudianteSeRechazaYLiberarUnoPermiteOtro() {
        registrar("L1", "CERTIFICADO");
        registrar("L1", "EQUIVALENCIA");
        registrar("L1", "RECLAMO_NOTA");

        LimitePendientesException error = assertThrows(LimitePendientesException.class,
                () -> registrar("L1", "BAJA_MATERIA"));
        assertEquals("el estudiante ya tiene 3 solicitudes pendientes", error.getMessage());

        servicio.cambiarEstado(1, "EN_PROCESO");
        assertEquals(4, registrar("L1", "BAJA_MATERIA").getId());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiaDeEstadoSiLaTransicionEsValida() {
        registrar("L1", "CERTIFICADO");

        assertEquals(EstadoSolicitud.EN_PROCESO, servicio.cambiarEstado(1, "EN_PROCESO").getEstadoSolicitud());
        assertEquals(EstadoSolicitud.RESUELTA, servicio.cambiarEstado(1, "RESUELTA").getEstadoSolicitud());
    }

    @Test
    void unaTransicionInvalidaOUnEstadoDesconocidoLanzaTransicionInvalida() {
        registrar("L1", "CERTIFICADO");

        TransicionInvalidaException error = assertThrows(TransicionInvalidaException.class,
                () -> servicio.cambiarEstado(1, "RESUELTA"));
        assertEquals("transicion invalida de PENDIENTE a RESUELTA", error.getMessage());
        assertThrows(TransicionInvalidaException.class, () -> servicio.cambiarEstado(1, "INVENTADO"));
        assertThrows(TransicionInvalidaException.class, () -> servicio.cambiarEstado(1, null));
    }

    @Test
    void cambiarElEstadoDeUnaSolicitudInexistenteLanzaSolicitudNoEncontrada() {
        SolicitudNoEncontradaException error = assertThrows(SolicitudNoEncontradaException.class,
                () -> servicio.cambiarEstado(99, "EN_PROCESO"));

        assertEquals("no existe la solicitud 99", error.getMessage());
    }

    // ---------- asignarResponsable ----------

    @Test
    void asignaUnResponsableDelAreaCorrecta() {
        registrar("L1", "CERTIFICADO");

        Solicitud solicitud = servicio.asignarResponsable(1, 1);

        assertEquals("Laura", solicitud.getResponsable().getNombre());
    }

    @Test
    void laAsignacionFallaConSolicitudInexistenteResponsableInexistenteOAreaIncorrecta() {
        registrar("L1", "CERTIFICADO");

        assertThrows(SolicitudNoEncontradaException.class, () -> servicio.asignarResponsable(99, 1));
        assertEquals("no existe el responsable 99", assertThrows(ResponsableNoEncontradoException.class,
                () -> servicio.asignarResponsable(1, 99)).getMessage());
        assertEquals("el responsable es del area Academica y la solicitud requiere Alumnado",
                assertThrows(AreaIncorrectaException.class, () -> servicio.asignarResponsable(1, 2)).getMessage());
    }

    @Test
    void unaSolicitudCerradaNoAdmiteAsignacion() {
        registrar("L1", "CERTIFICADO");
        servicio.cambiarEstado(1, "RECHAZADA");

        assertThrows(SolicitudCerradaException.class, () -> servicio.asignarResponsable(1, 1));
    }

    // ---------- filtrar ----------

    @Test
    void filtraPorUnCriterioCualquiera() {
        registrar("L1", "CERTIFICADO");
        registrar("L2", "EQUIVALENCIA");
        registrar("L3", "CERTIFICADO");
        servicio.cambiarEstado(2, "EN_PROCESO");

        List<Solicitud> certificados = servicio.filtrar(s -> s.getTipoSolicitud() == TipoSolicitud.CERTIFICADO);
        List<Solicitud> enProceso = servicio.filtrar(s -> s.getEstadoSolicitud() == EstadoSolicitud.EN_PROCESO);

        assertEquals(List.of(1, 3), certificados.stream().map(Solicitud::getId).toList());
        assertEquals(List.of(2), enProceso.stream().map(Solicitud::getId).toList());
        assertTrue(servicio.filtrar(s -> false).isEmpty());
    }
}
