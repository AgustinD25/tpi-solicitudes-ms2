package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de caracterizacion de DEFECTOS LATENTES de la version inicial
 * (ver docs/diagnostico-deuda-tecnica.md, seccion 5: D4, D5, D6, D7 y el
 * acoplamiento temporal del smell 14).
 *
 * Fijan el comportamiento ACTUAL, que es defectuoso. Cuando cada defecto se
 * corrija (despues del refactoring, un commit por defecto) se cambia SOLO el
 * test afectado y el commit explica por que.
 *
 * D1, D2, D3 y D8 ya estan fijados en SistemaSolicitudesCaracterizacionTest.
 */
class DefectosLatentesCaracterizacionTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    // No se usa @TempDir a proposito: en D4 y D6 el codigo de produccion deja abierto el
    // FileWriter / BufferedReader cuando falla (guardar y cargar no cierran en un finally),
    // y en Windows JUnit no puede borrar un archivo con el handle abierto. Esa fuga de
    // recursos es otra consecuencia de los catch vacios (smell 9). La limpieza se hace
    // "por mejor esfuerzo" despues de pedir al GC que libere los handles.
    private Path tmp;

    private PrintStream salidaOriginal;

    @BeforeEach
    void prepararEntorno() throws IOException {
        tmp = Files.createTempDirectory("solicitudes-defectos-");
        salidaOriginal = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void limpiarEntorno() throws IOException {
        System.setOut(salidaOriginal);
        System.gc();
        try (Stream<Path> archivos = Files.walk(tmp)) {
            archivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private Path csv(String nombre) {
        return tmp.resolve(nombre);
    }

    private static int lineas(Path archivo) throws IOException {
        return Files.readAllLines(archivo).size();
    }

    // ---------- D4: perdida silenciosa de datos con descripcion nula (solo por API) ----------

    @Test
    void d4_descripcionNulaVaciaElCsvSinAvisarYDevuelveUnIdValido() throws IOException {
        Path archivo = csv("d4.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);
        assertEquals(1, lineas(archivo));

        int id = sis.registrarSolicitud("L2", "Beto", "b@b.com", "CERTIFICADO", null, 1, false, FECHA);

        assertEquals(2, id);                         // el metodo informa exito
        assertEquals(2, sis.lst.size());             // y la memoria tiene las 2
        assertEquals(0, lineas(archivo));            // pero el archivo quedo vacio: se perdio la 1ra tambien
    }

    // ---------- D5: estado nulo (solo por API) ----------

    @Test
    void d5_cambiarEstadoConEstadoNuloLanzaNullPointerException() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);
        sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);

        assertThrows(NullPointerException.class, () -> sis.cambiarEstado(1, null));
    }

    @Test
    void d5_conSolicitudInexistenteElEstadoNuloNoImportaYDevuelveFalse() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);

        assertFalse(sis.cambiarEstado(99, null));
    }

    // ---------- D6: ';' en nombre o email corrompe el CSV (alcanzable desde el menu) ----------

    @Test
    void d6_emailConPuntoYComaCorrompeElCsvYSePierdenEsaLineaYLasSiguientes() {
        Path archivo = csv("d6-email.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(2, sis.registrarSolicitud("L2", "Beto", "be;to@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(3, sis.registrarSolicitud("L3", "Carla", "c@b.com", "CERTIFICADO", "ok", 1, false, FECHA));

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();

        assertEquals(3, sis.lst.size());
        assertEquals(1, recargado.lst.size());       // solo sobrevive la anterior a la linea corrupta
    }

    @Test
    void d6_nombreConPuntoYComaCorrompeElCsvYSePierdenEsaLineaYLasSiguientes() {
        Path archivo = csv("d6-nombre.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(2, sis.registrarSolicitud("L2", "Be;to", "b@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(3, sis.registrarSolicitud("L3", "Carla", "c@b.com", "CERTIFICADO", "ok", 1, false, FECHA));

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();

        assertEquals(3, sis.lst.size());
        assertEquals(1, recargado.lst.size());
    }

    // ---------- D7: cargar() no es idempotente ----------

    @Test
    void d7_cargarDosVecesDuplicaLasSolicitudes() {
        Path archivo = csv("d7.csv");
        SistemaSolicitudes original = new SistemaSolicitudes(archivo.toString());
        original.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);

        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        sis.cargar();
        sis.cargar();

        assertEquals(2, sis.lst.size());
        assertEquals(1, sis.lst.get(0).id);
        assertEquals(1, sis.lst.get(1).id);          // mismo id repetido
        assertEquals(1, sis.est.size());             // el estudiante si se reutiliza
    }

    // ---------- Smell 14: acoplamiento temporal ----------

    @Test
    void smell14_cargarAntesDeRegistrarResponsablesDejaLasAsignacionesEnNull() {
        Path archivo = csv("temporal.csv");
        SistemaSolicitudes original = new SistemaSolicitudes(archivo.toString());
        original.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");
        original.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);
        original.asignarResponsable(1, 1);

        SistemaSolicitudes ordenCorrecto = new SistemaSolicitudes(archivo.toString());
        ordenCorrecto.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");
        ordenCorrecto.cargar();

        SistemaSolicitudes ordenInvertido = new SistemaSolicitudes(archivo.toString());
        ordenInvertido.cargar();
        ordenInvertido.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");

        assertEquals("Laura Gomez", ordenCorrecto.lst.get(0).responsable.nombre);
        assertNull(ordenInvertido.lst.get(0).responsable);   // sin error ni aviso
    }
}
