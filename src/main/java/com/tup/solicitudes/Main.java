package com.tup.solicitudes;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Menu de consola: lee los datos, llama a la fachada y muestra el resultado. No contiene reglas de negocio. */
public class Main {
    private final SistemaSolicitudes sistema;
    private final Scanner entrada;

    private Main(SistemaSolicitudes sistema, Scanner entrada) {
        this.sistema = sistema;
        this.entrada = entrada;
    }

    public static void main(String[] args) {
        SistemaSolicitudes sistema = new SistemaSolicitudes("solicitudes.csv");
        registrarResponsablesDeEjemplo(sistema);
        sistema.cargar();
        new Main(sistema, new Scanner(System.in)).ejecutar();
    }

    private static void registrarResponsablesDeEjemplo(SistemaSolicitudes sistema) {
        sistema.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", Area.ALUMNADO.getNombre());
        sistema.registrarResponsable(2, "Carlos Ruiz", "carlos.ruiz@ejemplo.edu", Area.ACADEMICA.getNombre());
        sistema.registrarResponsable(3, "Marta Diaz", "marta.diaz@ejemplo.edu", Area.DOCENCIA.getNombre());
        sistema.registrarResponsable(4, "Pablo Soto", "pablo.soto@ejemplo.edu", Area.ALUMNADO.getNombre());
    }

    private void ejecutar() {
        boolean seguir = true;
        try {
            while (seguir) {
                mostrarMenu();
                seguir = ejecutarOpcion(entrada.nextLine().trim());
            }
        } catch (NoSuchElementException finDeLaEntrada) {
            System.out.println();          // la entrada se cerro (por ejemplo Ctrl+D): se sale como con la opcion 0
        }
        System.out.println("Hasta luego");
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("=== SOLICITUDES ACADEMICAS ===");
        System.out.println("1. Registrar solicitud");
        System.out.println("2. Cambiar estado");
        System.out.println("3. Asignar responsable");
        System.out.println("4. Listar todas");
        System.out.println("5. Filtrar por estado");
        System.out.println("6. Filtrar por tipo");
        System.out.println("7. Filtrar por prioridad");
        System.out.println("8. Reporte");
        System.out.println("9. Exportar (CSV / JSON)");
        System.out.println("0. Salir");
        System.out.print("Opcion: ");
    }

    /** Ejecuta la opcion elegida. Devuelve false cuando hay que salir del menu. */
    private boolean ejecutarOpcion(String opcion) {
        try {
            switch (opcion) {
                case "1": registrarSolicitud(); break;
                case "2": cambiarEstado(); break;
                case "3": asignarResponsable(); break;
                case "4": sistema.imprimirLista(sistema.getSolicitudes()); break;
                case "5": sistema.imprimirLista(sistema.filtrarPorEstado(pedirTexto("Estado: "))); break;
                case "6": sistema.imprimirLista(sistema.filtrarPorTipo(pedirTexto("Tipo: "))); break;
                case "7": sistema.imprimirLista(sistema.filtrarPorPrioridad(pedirPrioridad())); break;
                case "8": sistema.reporte(LocalDate.now()); break;
                case "9": exportar(); break;
                case "0": return false;
                default: System.out.println("Opcion invalida");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Error: se esperaba un numero");
        }
        return true;
    }

    private void registrarSolicitud() {
        String legajo = pedirLinea("Legajo: ");
        String nombre = pedirLinea("Nombre: ");
        String email = pedirLinea("Email: ");
        String tipo = pedirTexto("Tipo (CERTIFICADO/EQUIVALENCIA/RECLAMO_NOTA/BAJA_MATERIA): ");
        String descripcion = pedirLinea("Descripcion: ");
        int prioridad = pedirPrioridad();
        boolean urgente = pedirTexto("Es urgente? (s/n): ").equalsIgnoreCase("s");
        sistema.registrarSolicitud(legajo, nombre, email, tipo, descripcion, prioridad, urgente);
    }

    private void cambiarEstado() {
        int idSolicitud = pedirNumero("Id de solicitud: ");
        String nuevoEstado = pedirTexto("Nuevo estado (EN_PROCESO/RESUELTA/RECHAZADA): ");
        sistema.cambiarEstado(idSolicitud, nuevoEstado);
    }

    private void asignarResponsable() {
        int idSolicitud = pedirNumero("Id de solicitud: ");
        System.out.println("Responsables:");
        for (Responsable r : sistema.getResponsables()) {
            System.out.println("  " + r.getId() + " - " + r.getNombre() + " (" + r.getArea() + ")");
        }
        int idResponsable = pedirNumero("Id de responsable: ");
        sistema.asignarResponsable(idSolicitud, idResponsable);
    }

    private void exportar() {
        String formato = pedirTexto("Formato (CSV/JSON): ");
        ReporteSimple reporte = new ReporteSimple(sistema.getSolicitudes());
        if (formato.equals("CSV")) {
            System.out.println(reporte.exportarCSV());
        } else if (formato.equals("JSON")) {
            System.out.println(reporte.exportarJSON());
        } else {
            System.out.println("Formato no soportado");
        }
    }

    // ---------- lectura de datos ----------

    /** Muestra el mensaje y devuelve la linea tal cual la escribio el usuario. */
    private String pedirLinea(String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine();
    }

    /** Igual que pedirLinea, pero sin espacios en los extremos y en mayusculas (para tipos, estados y formatos). */
    private String pedirTexto(String mensaje) {
        return pedirLinea(mensaje).trim().toUpperCase();
    }

    /** Lanza NumberFormatException si no se escribe un entero; el menu lo informa. */
    private int pedirNumero(String mensaje) {
        return Integer.parseInt(pedirLinea(mensaje).trim());
    }

    private int pedirPrioridad() {
        return pedirNumero("Prioridad (1-3): ");
    }
}
