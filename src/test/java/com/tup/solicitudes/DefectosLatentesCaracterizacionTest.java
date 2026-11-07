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
    private ByteArrayOutputStream salida;

    @BeforeEach
    void prepararEntorno() throws IOException {
        tmp = Files.createTempDirectory("solicitudes-defectos-");
        salidaOriginal = System.out;
        salida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
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

    // ---------- D4: descripcion nula (solo por API) ----------

    // CAMBIO DELIBERADO (correccion del defecto D4): antes se llamaba
    // d4_descripcionNulaVaciaElCsvSinAvisarYDevuelveUnIdValido y exigia que el archivo quedara VACIO (0 lineas,
    // se perdia tambien la solicitud anterior). Ahora la descripcion nula se guarda como texto vacio, el archivo
    // conserva todas las solicitudes y al recargar vuelven las dos.
    @Test
    void d4_descripcionNulaSeGuardaVaciaYNoSeTruncaElCsv() throws IOException {
        Path archivo = csv("d4.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);
        assertEquals(1, lineas(archivo));

        int id = sis.registrarSolicitud("L2", "Beto", "b@b.com", "CERTIFICADO", null, 1, false, FECHA);

        assertEquals(2, id);
        assertEquals(2, sis.getSolicitudes().size());
        assertEquals(2, lineas(archivo));            // el archivo conserva las dos solicitudes

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();
        assertEquals(2, recargado.getSolicitudes().size());
        assertEquals("", recargado.getSolicitudes().get(1).getDescripcion());
    }

    // ---------- D5: estado nulo (solo por API) ----------

    // CAMBIO DELIBERADO (correccion del defecto D5): antes se llamaba
    // d5_cambiarEstadoConEstadoNuloLanzaNullPointerException y exigia la excepcion.
    // Ahora un estado nulo es una transicion invalida mas: devuelve false y avisa.
    @Test
    void d5_cambiarEstadoConEstadoNuloEsUnaTransicionInvalida() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);
        sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);
        salida.reset();

        assertFalse(sis.cambiarEstado(1, null));

        assertEquals("PENDIENTE", sis.getSolicitudes().get(0).getEstado());
        assertEquals("Error: transicion invalida de PENDIENTE a null" + System.lineSeparator(), salida.toString(StandardCharsets.UTF_8));
    }

    @Test
    void d5_conSolicitudInexistenteElEstadoNuloNoImportaYDevuelveFalse() {
        SistemaSolicitudes sis = new SistemaSolicitudes(null);

        assertFalse(sis.cambiarEstado(99, null));
    }

    // ---------- D6 y D8: separadores y caracteres especiales en el CSV (alcanzable desde el menu) ----------

    // CAMBIO DELIBERADO (correccion de los defectos D6 y D8): antes estos tests se llamaban
    // d6_emailConPuntoYComaCorrompeElCsv... y d6_nombreConPuntoYComaCorrompeElCsv... y exigian que, con un ';'
    // en el email o en el nombre, al recargar se perdieran esa linea y todas las siguientes (1 de 3 solicitudes).
    // Ahora los textos se escapan al guardar y se recuperan tal cual: vuelven las 3 solicitudes, con los mismos datos.
    @Test
    void d6_emailConPuntoYComaSeGuardaYRecargaIntacto() {
        Path archivo = csv("d6-email.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(2, sis.registrarSolicitud("L2", "Beto", "be;to@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(3, sis.registrarSolicitud("L3", "Carla", "c@b.com", "CERTIFICADO", "ok", 1, false, FECHA));

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();

        assertEquals(3, recargado.getSolicitudes().size());
        assertEquals("be;to@b.com", recargado.getSolicitudes().get(1).getEstudiante().getEmail());
        assertEquals("c@b.com", recargado.getSolicitudes().get(2).getEstudiante().getEmail());
    }

    @Test
    void d6_nombreConPuntoYComaSeGuardaYRecargaIntacto() {
        Path archivo = csv("d6-nombre.csv");
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(2, sis.registrarSolicitud("L2", "Be;to", "b@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
        assertEquals(3, sis.registrarSolicitud("L3", "Carla", "c@b.com", "CERTIFICADO", "ok", 1, false, FECHA));

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();

        assertEquals(3, recargado.getSolicitudes().size());
        assertEquals("Be;to", recargado.getSolicitudes().get(1).getEstudiante().getNombre());
        assertEquals("Carla", recargado.getSolicitudes().get(2).getEstudiante().getNombre());
    }

    @Test
    void d8_laDescripcionConCaracteresEspecialesVuelveExactamenteIgual() {
        Path archivo = csv("d8.csv");
        String descripcion = "Pedido; con punto y coma, una \\ barra\ny un salto de linea";
        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        assertEquals(1, sis.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", descripcion, 1, false, FECHA));

        SistemaSolicitudes recargado = new SistemaSolicitudes(archivo.toString());
        recargado.cargar();

        assertEquals(1, recargado.getSolicitudes().size());
        assertEquals(descripcion, recargado.getSolicitudes().get(0).getDescripcion());
    }

    // ---------- D7: cargar() es idempotente ----------

    // CAMBIO DELIBERADO (correccion del defecto D7): antes se llamaba d7_cargarDosVecesDuplicaLasSolicitudes
    // y exigia que cargar() dos veces dejara 2 solicitudes con el mismo id. Ahora cargar() reemplaza lo que hay
    // en memoria por lo que dice el archivo, asi que repetirlo no cambia nada.
    @Test
    void d7_cargarDosVecesDejaLoMismoQueCargarUnaVez() {
        Path archivo = csv("d7.csv");
        SistemaSolicitudes original = new SistemaSolicitudes(archivo.toString());
        original.registrarSolicitud("L1", "Ana", "a@b.com", "CERTIFICADO", "ok", 1, false, FECHA);

        SistemaSolicitudes sis = new SistemaSolicitudes(archivo.toString());
        sis.cargar();
        sis.cargar();

        assertEquals(1, sis.getSolicitudes().size());
        assertEquals(1, sis.getSolicitudes().get(0).getId());
        assertEquals(1, sis.getEstudiantes().size());
        // y los ids siguen donde corresponde
        assertEquals(2, sis.registrarSolicitud("L2", "Beto", "b@b.com", "CERTIFICADO", "ok", 1, false, FECHA));
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

        assertEquals("Laura Gomez", ordenCorrecto.getSolicitudes().get(0).getResponsable().getNombre());
        assertNull(ordenInvertido.getSolicitudes().get(0).getResponsable());   // sin error ni aviso
    }
}
