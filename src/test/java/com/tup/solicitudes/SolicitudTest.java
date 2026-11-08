package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Tests unitarios de la entidad (code review): las reglas se prueban sin pasar por la fachada ni el servicio. */
class SolicitudTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private final Estudiante ana = new Estudiante("L001", "Ana", "ana@ejemplo.edu");
    private final Responsable deAlumnado = new Responsable(1, "Laura", "laura@ejemplo.edu", "Alumnado");
    private final Responsable deAcademica = new Responsable(2, "Carlos", "carlos@ejemplo.edu", "Academica");

    private Solicitud solicitudDe(TipoSolicitud tipo) {
        return new Solicitud(1, ana, tipo, "descripcion", 1, false, FECHA);
    }

    // ---------- CR-03: la entidad protege la asignacion ----------

    @Test
    void asignaUnResponsableDeLaAreaCorrecta() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.CERTIFICADO);

        solicitud.asignarResponsable(deAlumnado);

        assertSame(deAlumnado, solicitud.getResponsable());
    }

    @Test
    void rechazaUnResponsableDeOtraArea() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.EQUIVALENCIA);   // requiere Academica

        AreaIncorrectaException error = assertThrows(AreaIncorrectaException.class,
                () -> solicitud.asignarResponsable(deAlumnado));

        assertEquals("el responsable es del area Alumnado y la solicitud requiere Academica", error.getMessage());
        assertNull(solicitud.getResponsable());
    }

    @Test
    void rechazaLaAsignacionEnUnaSolicitudResuelta() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.CERTIFICADO);
        solicitud.pasarA(EstadoSolicitud.EN_PROCESO);
        solicitud.pasarA(EstadoSolicitud.RESUELTA);

        assertThrows(SolicitudCerradaException.class, () -> solicitud.asignarResponsable(deAlumnado));
        assertNull(solicitud.getResponsable());
    }

    @Test
    void rechazaLaAsignacionEnUnaSolicitudRechazada() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.CERTIFICADO);
        solicitud.pasarA(EstadoSolicitud.RECHAZADA);

        assertThrows(SolicitudCerradaException.class, () -> solicitud.asignarResponsable(deAlumnado));
    }

    @Test
    void permiteReasignarMientrasEstaAbierta() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.CERTIFICADO);
        Responsable otroDeAlumnado = new Responsable(3, "Pablo", "pablo@ejemplo.edu", "Alumnado");

        solicitud.asignarResponsable(deAlumnado);
        solicitud.asignarResponsable(otroDeAlumnado);

        assertSame(otroDeAlumnado, solicitud.getResponsable());
    }

    @Test
    void sinAreaCorrectaNoSePuedeAsignarAunqueSeaReasignacion() {
        Solicitud solicitud = solicitudDe(TipoSolicitud.EQUIVALENCIA);
        solicitud.asignarResponsable(deAcademica);

        assertThrows(AreaIncorrectaException.class, () -> solicitud.asignarResponsable(deAlumnado));

        assertSame(deAcademica, solicitud.getResponsable());     // la anterior se mantiene
    }
}
