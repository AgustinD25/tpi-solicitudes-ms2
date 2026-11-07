# Mapa de smells, refactorings y principios

Resumen para el informe y el video. Une, para cada smell del diagnóstico (`docs/diagnostico-deuda-tecnica.md`),
el refactoring que lo corrige (`docs/refactorings.md`), el principio que mejora y el commit.

La consigna pide **al menos 8 smells**. El diagnóstico registra 14, agrupados así: un **núcleo de 8** que
cubre los refactorings obligatorios y los cinco principios SOLID, y **5 complementarios** (Clean Code y
mantenimiento). Los smells 4 y 5 (tipo y estado como `String`) cuentan juntos porque son el mismo patrón.

## Núcleo (8)

| # | Smell | Refactoring | Principio | Commits |
|---|---|---|---|---|
| 1 | God Class (`SistemaSolicitudes`, 376 líneas, 8 responsabilidades) | R10 Extract Class (reporte y presentación), R11 repositorios, R12 servicios + fachada | SRP | `a40b119`, `ef10491`, `7afa693` |
| 2 | Long Method (`registrarSolicitud`, 65 líneas, complejidad ≈ 21) | R4 **Extract Method** (obligatorio) | Clean Code | `bf1f7c8` |
| 3 | Long Parameter List (7 y 8 parámetros) | R5 **Introduce Parameter Object** (obligatorio) | Clean Code | `711c91b` |
| 4 y 5 | Tipo y estado como `String`, con if/else repetidos (Shotgun Surgery, Primitive Obsession) | R6 `enum TipoSolicitud`, R7 `enum EstadoSolicitud` (Replace Type Code with Enum, Replace Conditional with Polymorphism) | OCP | `30dcfac`, `549d781` |
| 6 | Feature Envy (`estaVencida` usa solo datos de `Solicitud`) | R3 **Move Method** (obligatorio) | SRP / cohesión | `9023b3e` |
| 9 | Manejo de errores pobre (códigos negativos, catch vacíos, `System.out` en la lógica) | R12 Replace Error Code with Exception; fachada Adapter que conserva el contrato | Clean Code | `7afa693` |
| 11 | Acoplamiento a lo concreto (`FileWriter` en la lógica) | R11 `SolicitudRepository` y `EstudianteRepository` | DIP | `ef10491` |
| 12 | Interfaz gorda y contrato roto (`Exportable` con `exportarPDF` que lanzaba excepción) | R14 `ExportadorCSV` y `ExportadorJSON` | ISP y LSP | `bf99fee` |

## Complementarios (5)

| # | Smell | Refactoring | Commits |
|---|---|---|---|
| 7 | Código duplicado (`emailValido` x2, tres filtros casi iguales) | R13 `ValidadorEmail`; R12 `filtrar(Predicate)` | `782a992`, `7afa693` |
| 8 | Números mágicos | R1 constantes con nombre | `a9b75db` |
| 10 | Nombres pobres | R2 Rename; R9 nombres definitivos | `524d3ce`, `6dff016` |
| 13 | Data Class / encapsulamiento roto | R8 entidades con campos privados y comportamiento, R9 listas privadas | `23be11a`, `6dff016` |
| 14 | Acoplamiento temporal (`cargar()` depende del orden de las llamadas) | Cerrado al corregir los defectos: `RepositorioCsv` completa el responsable cuando aparece (ver `docs/correccion-defectos.md`) | ver `docs/correccion-defectos.md` |

## Cobertura de lo que pide la consigna

| Pide la consigna | Cómo se cumple |
|---|---|
| Al menos 4 refactorings, con Extract Method, Move Method e Introduce Parameter Object | 14 refactorings (R1 a R14); los tres obligatorios son R4, R3 y R5 |
| SRP | smell 1 (R10, R11, R12), smell 6 (R3) |
| OCP | smells 4 y 5 (R6, R7): un tipo nuevo es una constante |
| LSP | smell 12 (R14): `ReporteSimple` ya no lanza `UnsupportedOperationException` |
| ISP | smell 12 (R14) y `SolicitudRepository` / `EstudianteRepository` (R11) |
| DIP | smell 11 (R11): la lógica depende de interfaces de repositorio |
| Mejorar nombres, funciones, errores, cohesión y acoplamiento | smells 10, 2, 9, 6 y 11 |
| Comparación antes/después sin regresión | 49 tests de caracterización que pasan antes y después; únicas excepciones deliberadas y documentadas: acceso por getters (E1, con evidencia) y la reescritura de `ExportacionCaracterizacionTest` (R14) |

## Para la defensa: qué decir de cada grupo
- **Núcleo:** conviene saber explicar el antes/después de cada uno con el fragmento de código de `docs/refactorings.md`.
- **Complementarios:** alcanza con una frase cada uno (qué era, cómo se arregló).
- **Smell 14 y defectos D1 a D8:** se presentan como hallazgos de la caracterización que se corrigen después del
  refactoring, cada uno con su commit y su test modificado a propósito.
