package com.tup.solicitudes;

import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SistemaSolicitudes sis = new SistemaSolicitudes("solicitudes.csv");
        sis.registrarResponsable(1, "Laura Gomez", "laura.gomez@ejemplo.edu", "Alumnado");
        sis.registrarResponsable(2, "Carlos Ruiz", "carlos.ruiz@ejemplo.edu", "Academica");
        sis.registrarResponsable(3, "Marta Diaz", "marta.diaz@ejemplo.edu", "Docencia");
        sis.registrarResponsable(4, "Pablo Soto", "pablo.soto@ejemplo.edu", "Alumnado");
        sis.cargar();

        Scanner sc = new Scanner(System.in);
        boolean seguir = true;
        while (seguir) {
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
            System.out.println("10. Exportar PDF");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            String op = sc.nextLine().trim();
            try {
                switch (op) {
                    case "1": {
                        System.out.print("Legajo: ");
                        String legajo = sc.nextLine();
                        System.out.print("Nombre: ");
                        String nombre = sc.nextLine();
                        System.out.print("Email: ");
                        String email = sc.nextLine();
                        System.out.print("Tipo (CERTIFICADO/EQUIVALENCIA/RECLAMO_NOTA/BAJA_MATERIA): ");
                        String tipo = sc.nextLine().trim().toUpperCase();
                        System.out.print("Descripcion: ");
                        String desc = sc.nextLine();
                        System.out.print("Prioridad (1-3): ");
                        int prio = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("Es urgente? (s/n): ");
                        boolean urg = sc.nextLine().trim().equalsIgnoreCase("s");
                        sis.registrarSolicitud(legajo, nombre, email, tipo, desc, prio, urg);
                        break;
                    }
                    case "2": {
                        System.out.print("Id de solicitud: ");
                        int id = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("Nuevo estado (EN_PROCESO/RESUELTA/RECHAZADA): ");
                        String ne = sc.nextLine().trim().toUpperCase();
                        sis.cambiarEstado(id, ne);
                        break;
                    }
                    case "3": {
                        System.out.print("Id de solicitud: ");
                        int id = Integer.parseInt(sc.nextLine().trim());
                        System.out.println("Responsables:");
                        for (Responsable r : sis.getResponsables()) {
                            System.out.println("  " + r.getId() + " - " + r.getNombre() + " (" + r.getArea() + ")");
                        }
                        System.out.print("Id de responsable: ");
                        int idr = Integer.parseInt(sc.nextLine().trim());
                        sis.asignarResponsable(id, idr);
                        break;
                    }
                    case "4":
                        sis.imprimirLista(sis.getSolicitudes());
                        break;
                    case "5": {
                        System.out.print("Estado: ");
                        String e = sc.nextLine().trim().toUpperCase();
                        sis.imprimirLista(sis.filtrarPorEstado(e));
                        break;
                    }
                    case "6": {
                        System.out.print("Tipo: ");
                        String t = sc.nextLine().trim().toUpperCase();
                        sis.imprimirLista(sis.filtrarPorTipo(t));
                        break;
                    }
                    case "7": {
                        System.out.print("Prioridad (1-3): ");
                        int p = Integer.parseInt(sc.nextLine().trim());
                        sis.imprimirLista(sis.filtrarPorPrioridad(p));
                        break;
                    }
                    case "8":
                        sis.reporte(LocalDate.now());
                        break;
                    case "9": {
                        System.out.print("Formato (CSV/JSON): ");
                        String f = sc.nextLine().trim().toUpperCase();
                        ReporteSimple rs = new ReporteSimple(sis.getSolicitudes());
                        if (f.equals("CSV")) {
                            System.out.println(rs.exportarCSV());
                        } else if (f.equals("JSON")) {
                            System.out.println(rs.exportarJSON());
                        } else {
                            System.out.println("Formato no soportado");
                        }
                        break;
                    }
                    case "10": {
                        Exportable ex = new ReporteSimple(sis.getSolicitudes());
                        try {
                            System.out.println(ex.exportarPDF());
                        } catch (UnsupportedOperationException uoe) {
                            System.out.println("Error: esta implementacion no soporta exportar a PDF");
                        }
                        break;
                    }
                    case "0":
                        seguir = false;
                        break;
                    default:
                        System.out.println("Opcion invalida");
                }
            } catch (NumberFormatException ex) {
                System.out.println("Error: se esperaba un numero");
            }
        }
        System.out.println("Hasta luego");
    }
}
