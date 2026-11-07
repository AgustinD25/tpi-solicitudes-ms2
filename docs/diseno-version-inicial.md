# Diseño de la versión inicial (estado-inicial)

Objetivo: un sistema que FUNCIONA bien desde la mirada del usuario pero con problemas internos
reales y diagnosticables. Cada smell está plantado a propósito y tiene asignada su corrección.

## Reglas de negocio (no cambian entre versiones)
- Tipos de solicitud: CERTIFICADO (plazo 5 días, área Alumnado), EQUIVALENCIA (30, Académica),
  RECLAMO_NOTA (10, Docencia), BAJA_MATERIA (7, Alumnado).
- Prioridad 1 a 3; si la solicitud es urgente sube un nivel (tope 3).
- Un estudiante no puede tener más de 3 solicitudes PENDIENTES.
- Estados: PENDIENTE -> EN_PROCESO -> RESUELTA; PENDIENTE o EN_PROCESO -> RECHAZADA. Cualquier otra transición es inválida.
- Solo se puede asignar un responsable cuya área coincida con el área del tipo de solicitud.
- Una solicitud está vencida si pasó su plazo y no está RESUELTA ni RECHAZADA.
- Reporte: cantidad por estado, cantidad por tipo y listado de vencidas.
- Persistencia: archivo CSV (solicitudes.csv) + memoria.

## Clases de la versión inicial (paquete com.tup.solicitudes)
- Main: menú de consola con un switch grande.
- SistemaSolicitudes: clase dios (listas, validación, reglas, archivo, reporte, impresión).
- Solicitud, Estudiante, Responsable: clases de datos con campos públicos; estado/tipo como String.
- Exportable: interfaz gorda (exportarCSV, exportarJSON, exportarPDF).
- ReporteSimple implements Exportable: tira UnsupportedOperationException en exportarPDF.

## Smells plantados -> corrección prevista
| # | Smell | Dónde | Corrección | Principio / refactoring |
|---|---|---|---|---|
| 1 | God Class | SistemaSolicitudes | Separar en SolicitudService, SolicitudRepository, ReporteService, Validador | SRP, Extract Class |
| 2 | Long Method | registrarSolicitud() valida, calcula, crea, guarda e imprime | Partir en métodos chicos | Extract Method (obligatorio) |
| 3 | Long Parameter List | registrarSolicitud(legajo, nombre, email, tipo, descripcion, prioridad, urgente) | Objeto NuevaSolicitud | Introduce Parameter Object (obligatorio) |
| 4 | Switch sobre tipo | if/else por String tipo para plazo y área | enum TipoSolicitud con plazo y área | OCP, Replace Conditional with Polymorphism |
| 5 | Primitive Obsession | estado como String, transiciones con ifs repetidos | enum EstadoSolicitud con puedePasarA() | Replace Type Code, Move Method |
| 6 | Feature Envy | estaVencida(Solicitud s) en SistemaSolicitudes usa solo datos de Solicitud | mover a Solicitud | Move Method (obligatorio) |
| 7 | Código duplicado | validación de email en Estudiante y Responsable; filtrarPorEstado/Tipo/Prioridad casi iguales | Validador único; filtrar(Predicate) | DRY, Extract Method |
| 8 | Números mágicos | 3, 5, 30, 10, 7 sueltos | constantes / enum | Replace Magic Number |
| 9 | Manejo de errores pobre | catch vacíos, retorno null o -1, System.out dentro de la lógica | excepciones de dominio | Clean Code |
| 10 | Nombres pobres | proc(), lst, s2, aux, flag | nombres expresivos | Rename |
| 11 | Acoplamiento a lo concreto | la lógica escribe el CSV directo con FileWriter | interfaz SolicitudRepository inyectada (CSV / memoria) | DIP |
| 12 | Interfaz gorda + contrato roto | Exportable / ReporteSimple tira excepción en PDF | interfaces chicas (ExportadorCSV, ExportadorJSON) | ISP, LSP |
| 13 | Data Class | campos públicos sin comportamiento | encapsular, comportamiento en la entidad | Encapsulamiento |

## Estrategia de verificación
- Antes de refactorizar: tests de caracterización JUnit 5 contra SistemaSolicitudes (registrar ok, límite de
  3 pendientes, email inválido, transición válida/inválida, asignación por área, filtros, reporte, vencidas).
- SistemaSolicitudes se conserva al final como fachada que delega, así los MISMOS tests corren sin cambios
  sobre estado-inicial y estado-refactorizado (evidencia fuerte de que no hubo regresión).
- Para que sea testeable, la lógica devuelve valores (el reporte devuelve String) aunque además imprima.
- Fecha inyectable (parámetro LocalDate hoy) para testear vencimientos de forma determinista.
