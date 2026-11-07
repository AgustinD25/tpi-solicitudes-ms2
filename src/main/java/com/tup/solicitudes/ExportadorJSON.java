package com.tup.solicitudes;

/** Capacidad de exportar a JSON. Interfaz chica: quien la implementa cumple todo lo que promete (ISP y LSP). */
public interface ExportadorJSON {
    String exportarJSON();
}
