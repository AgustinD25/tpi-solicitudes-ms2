package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests propios del almacenamiento en CSV (agregados en la etapa de code review). A diferencia de los tests de
 * caracterizacion, estos no existian en la version inicial: prueban comportamiento nuevo.
 */
class RepositorioCsvTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    @TempDir
    Path tmp;

    private PrintStream salidaOriginal;
    private ByteArrayOutputStream salida;
    private Path archivo;

    @BeforeEach
    void setUp() {
        salidaOriginal = System.out;
        salida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
        archivo = tmp.resolve("solicitudes.csv");
    }

    @AfterEach
    void tearDown() {
        System.setOut(salidaOriginal);
    }

    private static String linea(int id, String tipo) {
        return id + ";L00" + id + ";Estudiante " + id + ";e" + id + "@ejemplo.edu;" + tipo
                + ";descripcion;1;false;PENDIENTE;-1;2026-03-01\n";
    }

    private String consola() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private List<Path> copiasDeSeguridad() throws IOException {
        try (Stream<Path> archivos = Files.list(tmp)) {
            return archivos.filter(p -> p.getFileName().toString().startsWith("solicitudes.csv.bak-")).toList();
        }
    }

    // ---------- CR-01: archivo danado ----------

    @Test
    void unArchivoConUnaLineaInvalidaNoSeCargaParcialmenteNiSeDestruyeAlGuardar() throws IOException {
        String original = linea(1, "CERTIFICADO") + linea(2, "CERTIFICAD0") + linea(3, "CERTIFICADO");
        Files.writeString(archivo, original);
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());

        sis.cargar();

        // todo o nada: no queda una carga parcial en memoria, y el error indica la linea
        assertTrue(sis.getSolicitudes().isEmpty());
        assertTrue(consola().contains("Error: no se pudo leer el archivo"));
        assertTrue(consola().contains("linea 2"));
        assertEquals(original, Files.readString(archivo));

        // el siguiente guardado no reutiliza ninguno de los ids 1, 2 y 3
        int idNuevo = sis.registrarSolicitud("L009", "Zoe", "zoe@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);
        assertEquals(4, idNuevo);

        // las lineas originales siguen existiendo, en una copia de seguridad
        List<Path> copias = copiasDeSeguridad();
        assertEquals(1, copias.size());
        assertEquals(original, Files.readString(copias.get(0)));
        assertTrue(consola().contains("se guardo una copia"));

        // y el archivo principal tiene la solicitud nueva
        assertEquals(1, Files.readAllLines(archivo).size());
        assertTrue(Files.readString(archivo).startsWith("4;L009;"));
    }

    @Test
    void despuesDeLaCopiaElSiguienteGuardadoNoVuelveAHacerOtra() throws IOException {
        Files.writeString(archivo, linea(1, "NO_EXISTE"));
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        sis.cargar();

        sis.registrarSolicitud("L009", "Zoe", "zoe@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);
        sis.registrarSolicitud("L010", "Ian", "ian@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA);

        assertEquals(1, copiasDeSeguridad().size());
        assertEquals(2, Files.readAllLines(archivo).size());
    }

    @Test
    void unArchivoValidoSigueCargandoseCompleto() throws IOException {
        Files.writeString(archivo, linea(1, "CERTIFICADO") + linea(2, "EQUIVALENCIA") + "\n" + linea(3, "RECLAMO_NOTA"));
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());

        sis.cargar();

        assertEquals(3, sis.getSolicitudes().size());           // la linea en blanco se ignora
        assertEquals(4, sis.registrarSolicitud("L009", "Zoe", "zoe@ejemplo.edu", "CERTIFICADO", "d", 1, false, FECHA));
        assertTrue(copiasDeSeguridad().isEmpty());
        assertFalse(consola().contains("Error"));
    }

    @Test
    void siUnaLineaTieneCantidadIncorrectaDeCamposSeReportaYNoSeCarga() throws IOException {
        Files.writeString(archivo, linea(1, "CERTIFICADO") + "2;L002;solo tres campos\n");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());

        sis.cargar();

        assertTrue(sis.getSolicitudes().isEmpty());
        assertTrue(consola().contains("se esperaban 11 campos"));
    }
}
