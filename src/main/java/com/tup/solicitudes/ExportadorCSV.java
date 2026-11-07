package com.tup.solicitudes;

/** Capacidad de exportar a CSV. Interfaz chica: quien la implementa cumple todo lo que promete (ISP y LSP). */
public interface ExportadorCSV {
    String exportarCSV();
}
