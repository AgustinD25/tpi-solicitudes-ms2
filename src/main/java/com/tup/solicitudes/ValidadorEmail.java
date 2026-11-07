package com.tup.solicitudes;

/** Unica regla de validacion de email del sistema (antes estaba copiada en Estudiante y en Responsable). */
public final class ValidadorEmail {

    private ValidadorEmail() {
    }

    /**
     * Un email es valido si tiene algo antes de la arroba, un punto despues de la arroba (con al menos un
     * caracter entre ambos y algo despues del punto) y no contiene espacios.
     */
    public static boolean esValido(String email) {
        if (email == null) {
            return false;
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return false;
        }
        int punto = email.indexOf('.', arroba);
        if (punto < arroba + 2 || punto == email.length() - 1) {
            return false;
        }
        if (email.contains(" ")) {
            return false;
        }
        return true;
    }
}
