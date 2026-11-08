# TPI-Solicitudes

Trabajo Práctico Integrador de **Metodología de Sistemas II** (Tecnicatura Universitaria en Programación, UTN a distancia).
Sistema de Gestión de Solicitudes Académicas con menú de consola.

El trabajo **no agrega funcionalidad**: parte de un sistema chico con problemas reales de diseño (tag `estado-inicial`),
los diagnostica, los refactoriza sin cambiar el comportamiento visible y evidencia el antes/después con tests
(tag `estado-refactorizado`).

> Todos los datos son sintéticos. No hay datos personales reales.

## Funcionalidades

Menú de consola:

| Opción | Qué hace |
|---|---|
| 1 | Registrar una solicitud (legajo, nombre, email, tipo, descripción, prioridad, urgente) |
| 2 | Cambiar el estado de una solicitud |
| 3 | Asignar un responsable |
| 4 | Listar todas las solicitudes |
| 5, 6, 7 | Filtrar por estado, por tipo o por prioridad |
| 8 | Reporte: cantidad por estado, cantidad por tipo y solicitudes vencidas |
| 9 | Exportar el listado a CSV o JSON |
| 0 | Salir |

### Reglas de negocio

- **Tipos y plazos:** `CERTIFICADO` (5 días, área Alumnado), `EQUIVALENCIA` (30, Academica), `RECLAMO_NOTA` (10, Docencia)
  y `BAJA_MATERIA` (7, Alumnado).
- **Prioridad** de 1 a 3; si la solicitud es urgente sube un nivel (tope 3).
- Un estudiante no puede tener más de **3 solicitudes pendientes**.
- **Estados:** `PENDIENTE` → `EN_PROCESO` → `RESUELTA`; `PENDIENTE` o `EN_PROCESO` → `RECHAZADA`. Cualquier otra
  transición es inválida.
- Solo se puede asignar un responsable del **área** que atiende ese tipo de solicitud, y no a una solicitud ya cerrada.
- Una solicitud está **vencida** si pasó su plazo y no está resuelta ni rechazada.
- **Persistencia:** archivo `solicitudes.csv` (en el directorio desde donde se ejecuta) además de la memoria.

Al iniciar se registran cuatro responsables de ejemplo (Laura Gomez y Pablo Soto de Alumnado, Carlos Ruiz de Academica y
Marta Diaz de Docencia).

## Tecnologías

- Java 17 o superior (usa `record` y `Optional`)
- Maven 3.8+
- JUnit 5 (101 tests) y JaCoCo (cobertura de líneas del 96,6 % sin contar `Main`)
- PMD (análisis estático): reporte en `docs/evidencias/`

## Instalación y uso

Requisitos: JDK 17+ y Maven instalados.

```bash
mvn package
java -jar target/solicitudes-1.0-SNAPSHOT.jar
```

Para solo compilar o correr los tests:

```bash
mvn compile
mvn test
```

## Estructura del código (`src/main/java/com/tup/solicitudes`)

| Capa | Clases |
|---|---|
| Entrada por consola | `Main` |
| Fachada (conserva el contrato original: códigos de retorno y mensajes) | `SistemaSolicitudes` |
| Casos de uso | `SolicitudService`, `ResponsableService`, `ReporteService` |
| Dominio | `Solicitud`, `Estudiante`, `Responsable`, `TipoSolicitud`, `EstadoSolicitud`, `NuevaSolicitud` |
| Errores de dominio | `DominioException` y sus hijas (`TipoInvalidoException`, `LimitePendientesException`, …) |
| Persistencia | `SolicitudRepository`, `EstudianteRepository`, `RepositorioEnMemoria`, `RepositorioCsv`, `FormatoCsv` |
| Utilidades | `ValidadorEmail`, `PresentadorSolicitud`, `ExportadorCSV`, `ExportadorJSON`, `ReporteSimple` |

La lógica depende de interfaces de repositorio (no de archivos) y los errores de negocio se informan con excepciones;
`SistemaSolicitudes` las traduce a los códigos y mensajes de siempre para que el comportamiento externo no cambie.

## Tests

`mvn test` ejecuta los **tests de caracterización**: fijan el comportamiento de la versión inicial y son los mismos que
validan la versión refactorizada. Los cambios hechos a propósito sobre ellos (corrección de defectos y acceso por
getters) están documentados en `docs/correccion-defectos.md` y `docs/refactorings.md`.

## Documentación (`docs/`)

| Archivo | Contenido |
|---|---|
| `diseno-version-inicial.md` | Diseño de la versión inicial y los smells plantados |
| `diagnostico-deuda-tecnica.md` | Diagnóstico: smells con evidencia, ubicación, impacto, riesgo y prioridad; defectos latentes |
| `mapa-smells-refactorings.md` | Resumen: smell → refactoring → principio → commit |
| `refactorings.md` | Cada refactoring con el fragmento antes/después y el smell que cierra |
| `correccion-defectos.md` | Defectos detectados con los tests y su corrección, uno por commit |
| `decisiones-tecnicas.md` | Decisiones de diseño y su justificación |
| `evidencias/` | Reporte PMD del estado inicial y evidencia del cambio de acceso en los tests |

## Hitos (tags de Git)

| Tag | Hito |
|---|---|
| `estado-inicial` | Sistema base con problemas detectables |
| `etapa-diagnostico` | Diagnóstico de deuda técnica |
| `etapa-refactoring` | Refactorings aplicados (R1 a R14) |
| `etapa-pruebas` | Defectos corregidos y pruebas en verde |
