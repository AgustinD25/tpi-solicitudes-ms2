package com.tup.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/** Tests unitarios (CR-07): lo que se escapa al guardar vuelve igual al leer, con cualquier contenido. */
class FormatoCsvTest {

    private static String idaYVuelta(String texto) {
        List<String> campos = FormatoCsv.dividir(FormatoCsv.escapar(texto));
        assertEquals(1, campos.size(), "el texto escapado no debe partirse en varios campos: " + texto);
        return campos.get(0);
    }

    @Test
    void textosConCaracteresEspecialesVuelvenIgual() {
        String[] textos = {"", "normal", "a;b", ";", ";;", "\\", "\\;", "a\\;b", "linea1\nlinea2", "\r\n", "\\n",
                "Núñez ñandú", "fin\\"};
        for (String texto : textos) {
            assertEquals(texto, idaYVuelta(texto), "texto: [" + texto + "]");
        }
    }

    @Test
    void unaLineaConVariosCamposSeDivideEnLosSeparadoresSinEscapar() {
        String linea = String.join(";", FormatoCsv.escapar("a;b"), FormatoCsv.escapar(""), "c",
                FormatoCsv.escapar("d\\e"));

        List<String> campos = FormatoCsv.dividir(linea);

        assertEquals(List.of("a;b", "", "c", "d\\e"), campos);
    }

    @Test
    void losCamposVaciosDelFinalNoSePierden() {
        assertEquals(List.of("a", "b", ""), FormatoCsv.dividir("a;b;"));
        assertEquals(List.of("", ""), FormatoCsv.dividir(";"));
        assertEquals(List.of(""), FormatoCsv.dividir(""));
    }

    @Test
    void elEscapeUsaBarraInvertidaParaCadaCaracterEspecial() {
        assertEquals("a\\;b", FormatoCsv.escapar("a;b"));
        assertEquals("a\\\\b", FormatoCsv.escapar("a\\b"));
        assertEquals("a\\nb", FormatoCsv.escapar("a\nb"));
        assertEquals("a\\rb", FormatoCsv.escapar("a\rb"));
    }

    @Test
    void elTextoEscapadoNuncaContieneUnSeparadorSinEscapar() {
        String escapado = FormatoCsv.escapar("x;y;;z");

        String sinPares = escapado.replace("\\;", "");
        assertEquals("", sinPares.chars().filter(c -> c == ';').mapToObj(c -> "x").collect(Collectors.joining()));
    }
}
