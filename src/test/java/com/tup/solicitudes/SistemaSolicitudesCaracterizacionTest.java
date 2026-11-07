package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de caracterizacion: registran el comportamiento ACTUAL de la version inicial
 * (incluidos codigos de retorno negativos y mensajes por consola), no el ideal.
 * Solo usan la API publica de SistemaSolicitudes para poder correr sin cambios
 * sobre la version refactorizada.
 */
class SistemaSolicitudesCaracterizacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 10);

    @TempDir
    Path tmp;

    private Path csv;
    private SistemaSolicitudes sis;
    private PrintStream salidaOriginal;
    private ByteArrayOutputStream salida;

    @BeforeEach
    void setUp() {
        salidaOriginal = System.out;
        salida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));

        csv = tmp.resolve("solicitudes.csv");
        sis = new SistemaSolicitudes(csv.toString());
        sis.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");
        sis.registrarResponsable(2, "Carlos Ruiz", "carlos.ruiz@ejemplo.edu", "Academica");
        sis.registrarResponsable(3, "Marta Diaz", "marta.diaz@ejemplo.edu", "Docencia");
        salida.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(salidaOriginal);
    }

    // ---------- helpers ----------

    private int reg(String legajo, String tipo, int prioridad, boolean urgente, LocalDate fecha) {
        return sis.registrarSolicitud(legajo, "Estudiante " + legajo, legajo.toLowerCase() + "@ejemplo.edu",
                tipo, "descripcion", prioridad, urgente, fecha);
    }

    private int reg(String legajo, String tipo) {
        return reg(legajo, tipo, 1, false, HOY);
    }

    private String consola() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private Solicitud buscar(int id) {
        for (Solicitud s : sis.getSolicitudes()) {
            if (s.getId() == id) {
                return s;
            }
        }
        return null;
    }

    private static List<Integer> ids(List<Solicitud> l) {
        return l.stream().map(s -> s.getId()).toList();
    }

    // ---------- registro ----------

    @Test
    void registroOkDevuelveIdYDejaLaSolicitudPendiente() {
        int id = sis.registrarSolicitud("L001", "Ana Perez", "ana@ejemplo.edu", "CERTIFICADO",
                "Constancia", 2, false, LocalDate.of(2026, 3, 1));

        assertEquals(1, id);
        assertEquals(1, sis.getSolicitudes().size());
        Solicitud s = sis.getSolicitudes().get(0);
        assertEquals("PENDIENTE", s.getEstado());
        assertEquals("CERTIFICADO", s.getTipo());
        assertEquals("Constancia", s.getDescripcion());
        assertEquals(2, s.getPrioridad());
        assertFalse(s.isUrgente());
        assertEquals(LocalDate.of(2026, 3, 1), s.getFechaCreacion());
        assertNull(s.getResponsable());
        assertEquals("L001", s.getEstudiante().getLegajo());
        assertEquals("Ana Perez", s.getEstudiante().getNombre());
        assertEquals("ana@ejemplo.edu", s.getEstudiante().getEmail());
        assertEquals("Solicitud #1 registrada (CERTIFICADO, prioridad 2)\n".replace("\n", System.lineSeparator()),
                consola());
    }

    @Test
    void losIdsSonSecuencialesYElEstudianteSeReutilizaPorLegajo() {
        assertEquals(1, reg("L001", "CERTIFICADO"));
        assertEquals(2, reg("L001", "EQUIVALENCIA"));
        assertEquals(3, reg("L002", "RECLAMO_NOTA"));

        assertEquals(2, sis.getEstudiantes().size());
        assertTrue(sis.getSolicitudes().get(0).getEstudiante() == sis.getSolicitudes().get(1).getEstudiante());
    }

    @Test
    void sinFechaExplicitaUsaLaFechaDeHoy() {
        int id = sis.registrarSolicitud("L001", "Ana Perez", "ana@ejemplo.edu", "CERTIFICADO",
                "d", 1, false);

        assertEquals(1, id);
        assertEquals(LocalDate.now(), sis.getSolicitudes().get(0).getFechaCreacion());
    }

    // ---------- prioridad / urgencia ----------

    @Test
    void sinUrgenciaLaPrioridadSeMantiene() {
        reg("L001", "CERTIFICADO", 1, false, HOY);
        reg("L002", "CERTIFICADO", 2, false, HOY);
        reg("L003", "CERTIFICADO", 3, false, HOY);

        assertEquals(1, buscar(1).getPrioridad());
        assertEquals(2, buscar(2).getPrioridad());
        assertEquals(3, buscar(3).getPrioridad());
    }

    @Test
    void laUrgenciaSubeUnNivelConTopeEnTres() {
        reg("L001", "CERTIFICADO", 1, true, HOY);
        reg("L002", "CERTIFICADO", 2, true, HOY);
        reg("L003", "CERTIFICADO", 3, true, HOY);

        assertEquals(2, buscar(1).getPrioridad());
        assertEquals(3, buscar(2).getPrioridad());
        assertEquals(3, buscar(3).getPrioridad());
        assertTrue(buscar(3).isUrgente());
    }

    // ---------- validaciones de entrada ----------

    @Test
    void emailSinArrobaDevuelveMenosUnoYNoRegistra() {
        int id = sis.registrarSolicitud("L001", "Ana", "anaejemplo.edu", "CERTIFICADO", "d", 1, false, HOY);

        assertEquals(-1, id);
        assertTrue(sis.getSolicitudes().isEmpty());
        assertTrue(sis.getEstudiantes().isEmpty());
        assertTrue(consola().contains("Error: email invalido"));
    }

    @Test
    void emailInvalidoEnVariasFormas() {
        String[] invalidos = {null, "", "@ejemplo.edu", "ana@", "ana@ejemplo", "ana@.com", "ana@ejemplo.",
                "ana perez@ejemplo.edu"};
        for (String email : invalidos) {
            assertEquals(-1, sis.registrarSolicitud("L001", "Ana", email, "CERTIFICADO", "d", 1, false, HOY),
                    "email: " + email);
        }
        assertTrue(sis.getSolicitudes().isEmpty());
    }

    @Test
    void emailValidoSeAcepta() {
        assertEquals(1, sis.registrarSolicitud("L001", "Ana", "a@b.co", "CERTIFICADO", "d", 1, false, HOY));
    }

    @Test
    void legajoOnombreVaciosDevuelvenMenosUno() {
        assertEquals(-1, sis.registrarSolicitud(null, "Ana", "a@b.com", "CERTIFICADO", "d", 1, false, HOY));
        assertEquals(-1, sis.registrarSolicitud("  ", "Ana", "a@b.com", "CERTIFICADO", "d", 1, false, HOY));
        assertEquals(-1, sis.registrarSolicitud("L001", null, "a@b.com", "CERTIFICADO", "d", 1, false, HOY));
        assertEquals(-1, sis.registrarSolicitud("L001", " ", "a@b.com", "CERTIFICADO", "d", 1, false, HOY));
        assertTrue(sis.getSolicitudes().isEmpty());
    }

    @Test
    void tipoInvalidoDevuelveMenosTres() {
        assertEquals(-3, sis.registrarSolicitud("L001", "Ana", "a@b.com", "FOO", "d", 1, false, HOY));
        assertEquals(-3, sis.registrarSolicitud("L001", "Ana", "a@b.com", "certificado", "d", 1, false, HOY));
        assertTrue(consola().contains("Error: tipo de solicitud invalido"));
        assertTrue(sis.getSolicitudes().isEmpty());
    }

    @Test
    void tipoNuloLanzaNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> sis.registrarSolicitud("L001", "Ana", "a@b.com", null, "d", 1, false, HOY));
    }

    @Test
    void prioridadFueraDeRangoDevuelveMenosCuatro() {
        assertEquals(-4, sis.registrarSolicitud("L001", "Ana", "a@b.com", "CERTIFICADO", "d", 0, false, HOY));
        assertEquals(-4, sis.registrarSolicitud("L001", "Ana", "a@b.com", "CERTIFICADO", "d", 4, false, HOY));
        assertTrue(consola().contains("Error: prioridad fuera de rango"));
    }

    @Test
    void laValidacionDeTipoVaAntesQueLaDeEmail() {
        assertEquals(-3, sis.registrarSolicitud("L001", "Ana", "malo", "FOO", "d", 1, false, HOY));
    }

    // ---------- limite de pendientes ----------

    @Test
    void elCuartoPendienteDelMismoEstudianteSeRechazaConMenosDos() {
        assertEquals(1, reg("L001", "CERTIFICADO"));
        assertEquals(2, reg("L001", "EQUIVALENCIA"));
        assertEquals(3, reg("L001", "RECLAMO_NOTA"));

        assertEquals(-2, reg("L001", "BAJA_MATERIA"));
        assertEquals(3, sis.getSolicitudes().size());
        assertTrue(consola().contains("Error: el estudiante ya tiene 3 solicitudes pendientes"));
    }

    @Test
    void elLimiteSoloCuentaPendientesDeEseEstudiante() {
        reg("L001", "CERTIFICADO");
        reg("L001", "EQUIVALENCIA");
        reg("L001", "RECLAMO_NOTA");

        // otro estudiante no esta afectado
        assertEquals(4, reg("L002", "CERTIFICADO"));
        // al sacar una de PENDIENTE se libera un lugar
        assertTrue(sis.cambiarEstado(1, "EN_PROCESO"));
        assertEquals(5, reg("L001", "BAJA_MATERIA"));
        // y vuelve a estar en el limite
        assertEquals(-2, reg("L001", "CERTIFICADO"));
    }

    // ---------- transiciones de estado ----------

    @Test
    void transicionesValidas() {
        reg("L001", "CERTIFICADO");
        reg("L002", "CERTIFICADO");
        reg("L003", "CERTIFICADO");

        assertTrue(sis.cambiarEstado(1, "EN_PROCESO"));
        assertEquals("EN_PROCESO", buscar(1).getEstado());
        assertTrue(sis.cambiarEstado(1, "RESUELTA"));
        assertEquals("RESUELTA", buscar(1).getEstado());

        assertTrue(sis.cambiarEstado(2, "RECHAZADA"));
        assertEquals("RECHAZADA", buscar(2).getEstado());

        assertTrue(sis.cambiarEstado(3, "EN_PROCESO"));
        assertTrue(sis.cambiarEstado(3, "RECHAZADA"));
        assertEquals("RECHAZADA", buscar(3).getEstado());
    }

    @Test
    void transicionesInvalidasDevuelvenFalseYNoCambianElEstado() {
        reg("L001", "CERTIFICADO");

        assertFalse(sis.cambiarEstado(1, "RESUELTA"));          // PENDIENTE -> RESUELTA
        assertFalse(sis.cambiarEstado(1, "PENDIENTE"));         // PENDIENTE -> PENDIENTE
        assertFalse(sis.cambiarEstado(1, "INVENTADO"));         // estado desconocido
        assertEquals("PENDIENTE", buscar(1).getEstado());
        assertTrue(consola().contains("Error: transicion invalida de PENDIENTE a RESUELTA"));

        sis.cambiarEstado(1, "EN_PROCESO");
        assertFalse(sis.cambiarEstado(1, "PENDIENTE"));         // EN_PROCESO -> PENDIENTE
        assertFalse(sis.cambiarEstado(1, "EN_PROCESO"));        // EN_PROCESO -> EN_PROCESO
        assertEquals("EN_PROCESO", buscar(1).getEstado());
    }

    @Test
    void losEstadosFinalesNoTienenSalida() {
        reg("L001", "CERTIFICADO");
        reg("L002", "CERTIFICADO");
        sis.cambiarEstado(1, "EN_PROCESO");
        sis.cambiarEstado(1, "RESUELTA");
        sis.cambiarEstado(2, "RECHAZADA");

        for (String destino : new String[] {"PENDIENTE", "EN_PROCESO", "RESUELTA", "RECHAZADA"}) {
            assertFalse(sis.cambiarEstado(1, destino), "RESUELTA -> " + destino);
            assertFalse(sis.cambiarEstado(2, destino), "RECHAZADA -> " + destino);
        }
        assertEquals("RESUELTA", buscar(1).getEstado());
        assertEquals("RECHAZADA", buscar(2).getEstado());
    }

    @Test
    void cambiarEstadoDeSolicitudInexistenteDevuelveFalse() {
        assertFalse(sis.cambiarEstado(99, "EN_PROCESO"));
        assertTrue(consola().contains("Error: no existe la solicitud 99"));
    }

    // ---------- asignacion de responsable ----------

    @Test
    void asignacionConAreaCorrectaParaCadaTipo() {
        reg("L001", "CERTIFICADO");     // Alumnado
        reg("L002", "EQUIVALENCIA");    // Academica
        reg("L003", "RECLAMO_NOTA");    // Docencia
        reg("L004", "BAJA_MATERIA");    // Alumnado

        assertTrue(sis.asignarResponsable(1, 1));
        assertTrue(sis.asignarResponsable(2, 2));
        assertTrue(sis.asignarResponsable(3, 3));
        assertTrue(sis.asignarResponsable(4, 1));

        assertEquals("Laura Gomez", buscar(1).getResponsable().getNombre());
        assertEquals("Carlos Ruiz", buscar(2).getResponsable().getNombre());
        assertEquals("Marta Diaz", buscar(3).getResponsable().getNombre());
        assertEquals("Laura Gomez", buscar(4).getResponsable().getNombre());
        assertTrue(consola().contains("Solicitud #1 asignada a Laura Gomez"));
    }

    @Test
    void asignacionConAreaIncorrectaDevuelveFalseYNoAsigna() {
        reg("L001", "CERTIFICADO");     // requiere Alumnado

        assertFalse(sis.asignarResponsable(1, 2));
        assertFalse(sis.asignarResponsable(1, 3));
        assertNull(buscar(1).getResponsable());
        assertTrue(consola().contains(
                "Error: el responsable es del area Academica y la solicitud requiere Alumnado"));
    }

    @Test
    void asignacionConIdsInexistentesDevuelveFalse() {
        reg("L001", "CERTIFICADO");

        assertFalse(sis.asignarResponsable(99, 1));
        assertFalse(sis.asignarResponsable(1, 99));
        assertTrue(consola().contains("Error: no existe la solicitud 99"));
        assertTrue(consola().contains("Error: no existe el responsable 99"));
    }

    // CAMBIO DELIBERADO (correccion del defecto D1): antes este test se llamaba
    // sePuedeReasignarYAsignarEnCualquierEstado y exigia que se pudiera asignar un responsable a una
    // solicitud RESUELTA. Ahora una solicitud cerrada (RESUELTA o RECHAZADA) no admite asignaciones.
    // Sigue siendo valido reasignar mientras la solicitud no este cerrada.
    @Test
    void sePuedeReasignarMientrasNoEsteCerradaPeroNoUnaCerrada() {
        sis.registrarResponsable(4, "Pablo Soto", "pablo.soto@ejemplo.edu", "Alumnado");
        reg("L001", "CERTIFICADO");
        reg("L002", "CERTIFICADO");
        sis.cambiarEstado(1, "EN_PROCESO");

        assertTrue(sis.asignarResponsable(1, 1));
        assertTrue(sis.asignarResponsable(1, 4));          // reasignar en proceso: se permite
        assertEquals("Pablo Soto", buscar(1).getResponsable().getNombre());

        sis.cambiarEstado(1, "RESUELTA");
        sis.cambiarEstado(2, "RECHAZADA");
        salida.reset();

        assertFalse(sis.asignarResponsable(1, 1));         // RESUELTA
        assertFalse(sis.asignarResponsable(2, 1));         // RECHAZADA
        assertEquals("Pablo Soto", buscar(1).getResponsable().getNombre());   // no cambio
        assertNull(buscar(2).getResponsable());
        assertTrue(consola().contains("Error: la solicitud ya esta cerrada"));
    }

    @Test
    void registrarResponsableValidaEmailYArea() {
        assertNull(sis.registrarResponsable(9, "X", "sin-arroba", "Alumnado"));
        assertNull(sis.registrarResponsable(9, "X", "x@ejemplo.edu", "Cocina"));
        assertNotNull(sis.registrarResponsable(9, "X", "x@ejemplo.edu", "Docencia"));
        assertEquals(4, sis.getResponsables().size());
    }

    // ---------- filtros ----------

    private void cargarEscenarioDeFiltros() {
        reg("L001", "CERTIFICADO", 1, false, HOY);   // #1 prio 1 PENDIENTE
        reg("L002", "EQUIVALENCIA", 2, false, HOY);  // #2 prio 2
        reg("L003", "CERTIFICADO", 3, false, HOY);   // #3 prio 3
        reg("L004", "BAJA_MATERIA", 1, true, HOY);   // #4 prio 2 (urgente)
        sis.cambiarEstado(2, "EN_PROCESO");
        sis.cambiarEstado(3, "RECHAZADA");
    }

    @Test
    void filtrarPorEstado() {
        cargarEscenarioDeFiltros();

        assertEquals(List.of(1, 4), ids(sis.filtrarPorEstado("PENDIENTE")));
        assertEquals(List.of(2), ids(sis.filtrarPorEstado("EN_PROCESO")));
        assertEquals(List.of(3), ids(sis.filtrarPorEstado("RECHAZADA")));
        assertTrue(sis.filtrarPorEstado("RESUELTA").isEmpty());
        assertTrue(sis.filtrarPorEstado("pendiente").isEmpty());
    }

    @Test
    void filtrarPorTipo() {
        cargarEscenarioDeFiltros();

        assertEquals(List.of(1, 3), ids(sis.filtrarPorTipo("CERTIFICADO")));
        assertEquals(List.of(2), ids(sis.filtrarPorTipo("EQUIVALENCIA")));
        assertEquals(List.of(4), ids(sis.filtrarPorTipo("BAJA_MATERIA")));
        assertTrue(sis.filtrarPorTipo("RECLAMO_NOTA").isEmpty());
    }

    @Test
    void filtrarPorPrioridad() {
        cargarEscenarioDeFiltros();

        assertEquals(List.of(1), ids(sis.filtrarPorPrioridad(1)));
        assertEquals(List.of(2, 4), ids(sis.filtrarPorPrioridad(2)));
        assertEquals(List.of(3), ids(sis.filtrarPorPrioridad(3)));
        assertTrue(sis.filtrarPorPrioridad(5).isEmpty());
    }

    // ---------- vencimientos ----------

    @Test
    void plazoDeCadaTipoVenceElDiaSiguienteAlLimite() {
        Object[][] casos = {
                {"CERTIFICADO", 5}, {"EQUIVALENCIA", 30}, {"RECLAMO_NOTA", 10}, {"BAJA_MATERIA", 7}};
        LocalDate creada = LocalDate.of(2026, 1, 1);
        int n = 0;
        for (Object[] c : casos) {
            n++;
            int id = reg("L00" + n, (String) c[0], 1, false, creada);
            int plazo = (Integer) c[1];
            Solicitud s = buscar(id);

            assertFalse(sis.estaVencida(s, creada), c[0] + " el mismo dia");
            assertFalse(sis.estaVencida(s, creada.plusDays(plazo)), c[0] + " justo en el limite");
            assertTrue(sis.estaVencida(s, creada.plusDays(plazo + 1)), c[0] + " un dia despues");
        }
    }

    @Test
    void lasSolicitudesResueltasORechazadasNuncaEstanVencidas() {
        LocalDate vieja = LocalDate.of(2025, 1, 1);
        reg("L001", "CERTIFICADO", 1, false, vieja);
        reg("L002", "CERTIFICADO", 1, false, vieja);
        reg("L003", "CERTIFICADO", 1, false, vieja);
        sis.cambiarEstado(1, "EN_PROCESO");
        sis.cambiarEstado(1, "RESUELTA");
        sis.cambiarEstado(2, "RECHAZADA");

        assertFalse(sis.estaVencida(buscar(1), HOY));
        assertFalse(sis.estaVencida(buscar(2), HOY));
        assertTrue(sis.estaVencida(buscar(3), HOY));       // PENDIENTE
        sis.cambiarEstado(3, "EN_PROCESO");
        assertTrue(sis.estaVencida(buscar(3), HOY));       // EN_PROCESO tambien vence
    }

    // ---------- reporte ----------

    @Test
    void reporteSinSolicitudes() {
        String esperado = String.join("\n",
                "=== REPORTE ===",
                "Por estado:",
                "  PENDIENTE: 0",
                "  EN_PROCESO: 0",
                "  RESUELTA: 0",
                "  RECHAZADA: 0",
                "Por tipo:",
                "  CERTIFICADO: 0",
                "  EQUIVALENCIA: 0",
                "  RECLAMO_NOTA: 0",
                "  BAJA_MATERIA: 0",
                "Vencidas:",
                "  (ninguna)") + "\n";

        assertEquals(esperado, sis.reporte(HOY));
    }

    @Test
    void reporteConEstadosTiposYVencidas() {
        reg("A1", "CERTIFICADO", 1, false, LocalDate.of(2026, 3, 1));    // #1 PENDIENTE, vencida (vencio 03-06)
        reg("A1", "EQUIVALENCIA", 1, false, LocalDate.of(2026, 3, 1));   // #2 PENDIENTE, vigente (vence 03-31)
        reg("B2", "RECLAMO_NOTA", 1, false, LocalDate.of(2026, 2, 1));   // #3 RESUELTA
        reg("C3", "BAJA_MATERIA", 1, false, LocalDate.of(2026, 3, 1));   // #4 RECHAZADA
        reg("D4", "CERTIFICADO", 1, false, LocalDate.of(2026, 2, 20));   // #5 EN_PROCESO, vencida
        sis.cambiarEstado(3, "EN_PROCESO");
        sis.cambiarEstado(3, "RESUELTA");
        sis.cambiarEstado(4, "RECHAZADA");
        sis.cambiarEstado(5, "EN_PROCESO");
        salida.reset();

        String esperado = String.join("\n",
                "=== REPORTE ===",
                "Por estado:",
                "  PENDIENTE: 2",
                "  EN_PROCESO: 1",
                "  RESUELTA: 1",
                "  RECHAZADA: 1",
                "Por tipo:",
                "  CERTIFICADO: 2",
                "  EQUIVALENCIA: 1",
                "  RECLAMO_NOTA: 1",
                "  BAJA_MATERIA: 1",
                "Vencidas:",
                "  #1 CERTIFICADO (A1) creada 2026-03-01",
                "  #5 CERTIFICADO (D4) creada 2026-02-20") + "\n";

        String reporte = sis.reporte(HOY);

        assertEquals(esperado, reporte);
        assertEquals(esperado, consola());   // ademas de devolverlo, lo imprime
    }

    @Test
    void elReporteDependeDeLaFechaRecibida() {
        reg("A1", "CERTIFICADO", 1, false, LocalDate.of(2026, 3, 1));

        assertTrue(sis.reporte(LocalDate.of(2026, 3, 6)).endsWith("Vencidas:\n  (ninguna)\n"));
        assertTrue(sis.reporte(LocalDate.of(2026, 3, 7)).endsWith(
                "Vencidas:\n  #1 CERTIFICADO (A1) creada 2026-03-01\n"));
    }

    // ---------- impresion ----------

    @Test
    void imprimirMuestraUnaLineaPorSolicitud() {
        reg("L001", "CERTIFICADO", 1, true, HOY);
        sis.asignarResponsable(1, 1);
        reg("L002", "EQUIVALENCIA", 2, false, HOY);
        salida.reset();

        sis.imprimirLista(sis.getSolicitudes());

        String nl = System.lineSeparator();
        assertEquals("#1 | L001 Estudiante L001 | CERTIFICADO | prioridad 2 (urgente) | PENDIENTE | Laura Gomez | 2026-03-10" + nl
                + "#2 | L002 Estudiante L002 | EQUIVALENCIA | prioridad 2 | PENDIENTE | sin asignar | 2026-03-10" + nl,
                consola());
    }

    @Test
    void imprimirListaVaciaAvisaQueNoHayResultados() {
        sis.imprimirLista(sis.filtrarPorEstado("RESUELTA"));

        assertEquals("(sin resultados)" + System.lineSeparator(), consola());
    }

    // ---------- persistencia ----------

    @Test
    void cadaOperacionGuardaElCsvCompleto() throws IOException {
        sis.registrarSolicitud("L001", "Ana Perez", "ana@ejemplo.edu", "CERTIFICADO",
                "Constancia; para beca", 1, true, LocalDate.of(2026, 3, 1));
        sis.registrarSolicitud("L002", "Beto Diaz", "beto@ejemplo.edu", "EQUIVALENCIA",
                "Equiv", 3, false, LocalDate.of(2026, 3, 2));
        sis.cambiarEstado(1, "EN_PROCESO");
        sis.asignarResponsable(1, 1);

        String esperado = "1;L001;Ana Perez;ana@ejemplo.edu;CERTIFICADO;Constancia, para beca;2;true;EN_PROCESO;1;2026-03-01\n"
                + "2;L002;Beto Diaz;beto@ejemplo.edu;EQUIVALENCIA;Equiv;3;false;PENDIENTE;-1;2026-03-02\n";
        assertEquals(esperado, Files.readString(csv).replace("\r\n", "\n"));
    }

    @Test
    void cargarReconstruyeElEstadoYContinuaLosIds() {
        sis.registrarSolicitud("L001", "Ana Perez", "ana@ejemplo.edu", "CERTIFICADO",
                "Constancia; para beca", 1, true, LocalDate.of(2026, 3, 1));
        sis.registrarSolicitud("L001", "Ana Perez", "ana@ejemplo.edu", "EQUIVALENCIA",
                "Equiv", 3, false, LocalDate.of(2026, 3, 2));
        sis.cambiarEstado(1, "EN_PROCESO");
        sis.asignarResponsable(1, 1);

        SistemaSolicitudes otro = new SistemaSolicitudes(csv.toString());
        otro.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");
        otro.cargar();

        assertEquals(2, otro.getSolicitudes().size());
        assertEquals(1, otro.getEstudiantes().size());
        Solicitud s1 = otro.getSolicitudes().get(0);
        assertEquals("EN_PROCESO", s1.getEstado());
        assertEquals("Constancia, para beca", s1.getDescripcion());
        assertEquals(2, s1.getPrioridad());
        assertTrue(s1.isUrgente());
        assertEquals("Laura Gomez", s1.getResponsable().getNombre());
        assertEquals(LocalDate.of(2026, 3, 1), s1.getFechaCreacion());
        assertNull(otro.getSolicitudes().get(1).getResponsable());

        assertEquals(3, otro.registrarSolicitud("L009", "Zoe", "zoe@ejemplo.edu", "CERTIFICADO",
                "d", 1, false, HOY));
    }

    @Test
    void cargarConArchivoInexistenteNoFallaYNoCargaNada() {
        SistemaSolicitudes otro = new SistemaSolicitudes(tmp.resolve("no-existe.csv").toString());

        otro.cargar();

        assertTrue(otro.getSolicitudes().isEmpty());
    }

    @Test
    void sinArchivoConfiguradoFuncionaSoloEnMemoria() {
        SistemaSolicitudes enMemoria = new SistemaSolicitudes(null);

        assertEquals(1, enMemoria.registrarSolicitud("L001", "Ana", "a@b.com", "CERTIFICADO", "d", 1, false, HOY));
        enMemoria.cargar();

        assertEquals(1, enMemoria.getSolicitudes().size());
    }

    @Test
    void siNoSePuedeEscribirElArchivoElErrorSeTragaEnSilencio() {
        // la ruta apunta a un directorio existente: FileWriter falla y el catch vacio lo oculta
        SistemaSolicitudes malo = new SistemaSolicitudes(tmp.toString());

        int id = malo.registrarSolicitud("L001", "Ana", "a@b.com", "CERTIFICADO", "d", 1, false, HOY);

        assertEquals(1, id);
        assertEquals(1, malo.getSolicitudes().size());
    }

}
