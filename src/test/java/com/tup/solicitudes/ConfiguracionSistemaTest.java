package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests nuevos del code review (CR-06): el armado de dependencias esta en un solo lugar y admite cualquier repositorio. */
class ConfiguracionSistemaTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    @TempDir
    Path tmp;

    private PrintStream salidaOriginal;

    @BeforeEach
    void silenciarConsola() {
        salidaOriginal = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restaurarConsola() {
        System.setOut(salidaOriginal);
    }

    @Test
    void sePuedeArmarElSistemaConUnRepositorioCsvPropioYElMismoCatalogoDeResponsables() throws Exception {
        Path archivo = tmp.resolve("propio.csv");
        ResponsableService catalogo = new ResponsableService();
        RepositorioCsv repositorio = new RepositorioCsv(archivo.toString(),
                id -> catalogo.buscarPorId(id), mensaje -> { });
        SistemaSolicitudes sis = SistemaSolicitudes.con(ConfiguracionSistema.con(repositorio, repositorio, catalogo));

        sis.registrarResponsable(1, "Laura", "laura@ejemplo.edu", "Alumnado");
        sis.registrarSolicitud("L1", "Ana", "ana@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);
        assertTrue(sis.asignarResponsable(1, 1));

        assertTrue(Files.readString(archivo).contains(";PENDIENTE;1;"));    // el CSV se uso de verdad
    }

    @Test
    void sePuedeArmarElSistemaConRepositoriosEnMemoria() {
        RepositorioEnMemoria memoria = new RepositorioEnMemoria();
        SistemaSolicitudes sis = SistemaSolicitudes.con(
                ConfiguracionSistema.con(memoria, memoria, new ResponsableService()));

        int id = sis.registrarSolicitud("L1", "Ana", "ana@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);

        assertEquals(1, id);
        assertEquals(1, memoria.listarTodas().size());
    }

    @Test
    void elAtajoPorRutaUsaMemoriaSiLaRutaEsNula() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);

        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "ana@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA));
        assertEquals(1, sis.getSolicitudes().size());
    }
}
