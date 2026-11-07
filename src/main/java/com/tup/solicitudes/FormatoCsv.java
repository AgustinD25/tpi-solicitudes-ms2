package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del formato del archivo: los campos se separan con ';' y los textos libres se escapan con '\' para que
 * cualquier contenido (incluso un ';', una barra o un salto de linea) vuelva exactamente igual al leerlo.
 * <ul>
 *   <li>'\' se guarda como "\\", ';' como "\;", salto de linea como "\n" y retorno de carro como "\r".</li>
 * </ul>
 */
final class FormatoCsv {
    static final char SEPARADOR = ';';
    private static final char ESCAPE = '\\';

    private FormatoCsv() {
    }

    static String escapar(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == ESCAPE || c == SEPARADOR) {
                resultado.append(ESCAPE).append(c);
            } else if (c == '\n') {
                resultado.append(ESCAPE).append('n');
            } else if (c == '\r') {
                resultado.append(ESCAPE).append('r');
            } else {
                resultado.append(c);
            }
        }
        return resultado.toString();
    }

    /** Divide una linea por los ';' que no estan escapados y devuelve cada campo ya sin escapes. */
    static List<String> dividir(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean escapando = false;
        for (char c : linea.toCharArray()) {
            if (escapando) {
                actual.append(c == 'n' ? '\n' : c == 'r' ? '\r' : c);
                escapando = false;
            } else if (c == ESCAPE) {
                escapando = true;
            } else if (c == SEPARADOR) {
                campos.add(actual.toString());
                actual = new StringBuilder();
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
