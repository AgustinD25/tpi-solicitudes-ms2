# Diagnóstico de deuda técnica (etapa 3)

Versión analizada: tag `estado-inicial` (commit `e1a4f00`). Las líneas citadas son de esa versión.
Ruta base de los archivos: `src/main/java/com/tup/solicitudes/`.

## 1. Criterios

| Escala | Impacto (qué tan caro es mantener/entender/extender) | Riesgo (probabilidad de introducir un defecto al tocar o por dejarlo así) |
|---|---|---|
| 1 | Molestia local, no frena cambios | Poco probable que cause un error |
| 2 | Obliga a tocar varios lugares o a leer de más | Puede causar errores al cambiar reglas |
| 3 | Frena la evolución o esconde errores de datos | Ya causa o puede causar errores reales |

**Prioridad = Impacto × Riesgo**: Alta (6–9), Media (3–4), Baja (1–2).

## 2. Métricas de partida

| Dato | Valor | Dónde |
|---|---|---|
| Líneas de `SistemaSolicitudes` | 376 de 641 de producción (59 %) | `SistemaSolicitudes.java` |
| Métodos de `SistemaSolicitudes` | 15, en 8 responsabilidades (registro y validación, estados, asignación, búsqueda, vencimientos, reporte, impresión, persistencia) | todo el archivo |
| Método más largo | `registrarSolicitud` con 65 líneas y 20 puntos de decisión (complejidad ciclomática ≈ 21) | líneas 45–109 |
| Otros métodos largos | `reporte` (52 líneas), `cargar` (43 líneas) | 243–294, 333–375 |
| Comprobaciones sobre el tipo repartidas en el código | 4 (más una lista de áreas válidas) | ver smell 4 |
| Comparaciones con literales de estado o tipo | 26 (12 de estado, 14 de tipo) | `SistemaSolicitudes` |
| Catch vacíos | 2 | 329–330, 373–374 |
| Llamadas a `System.out` dentro de la lógica | 19 | ver smell 9 |

## 3. Tabla de smells

| # | Smell | Ubicación | Impacto | Riesgo | Prioridad |
|---|---|---|---|---|---|
| 1 | God Class | `SistemaSolicitudes` (todo) | 3 | 3 | **Alta (9)** |
| 2 | Long Method | `SistemaSolicitudes.registrarSolicitud` 45–109 | 3 | 3 | **Alta (9)** |
| 4 | Switch sobre tipo (Shotgun Surgery) | `SistemaSolicitudes` 29, 55–56, 162–170, 226–234, 256–263 | 3 | 3 | **Alta (9)** |
| 9 | Manejo de errores pobre | `SistemaSolicitudes` (varias) | 3 | 3 | **Alta (9)** |
| 5 | Primitive Obsession (estado y tipo como String) | `SistemaSolicitudes` 71, 120–132, 195, 235, 247–264 | 2 | 3 | **Alta (6)** |
| 11 | Acoplamiento a lo concreto (viola DIP) | `SistemaSolicitudes` 3–5, 321, 338; `Main` 8 | 3 | 2 | **Alta (6)** |
| 3 | Long Parameter List | `SistemaSolicitudes` 40–41 y 45–46 | 2 | 2 | Media (4) |
| 7 | Código duplicado | `Estudiante` 14–30 y `Responsable` 16–32; `SistemaSolicitudes` 192–220 | 2 | 2 | Media (4) |
| 8 | Números mágicos | `SistemaSolicitudes` 75–76, 90–92, 227–233 | 2 | 2 | Media (4) |
| 12 | Interfaz gorda + contrato roto (ISP/LSP) | `Exportable` 3–9; `ReporteSimple` 42–45; `Main` 110–118 | 2 | 2 | Media (4) |
| 13 | Data Class | `Solicitud` 6–14; `Estudiante` 4–6; `Responsable` 4–7 | 2 | 2 | Media (4) |
| 14 | Acoplamiento temporal | `Main` 9–13; `SistemaSolicitudes.cargar` 333–375 (en especial 361–365) | 2 | 2 | Media (4) |
| 6 | Feature Envy | `SistemaSolicitudes.estaVencida` 224–239 | 2 | 1 | Baja (2) |
| 10 | Nombres pobres | `SistemaSolicitudes` 11–14, 64, 95, 119, 183, 244–245 | 2 | 1 | Baja (2) |

(La numeración 1–13 es la del documento de diseño; el smell 14 no estaba previsto y se encontró al analizar el código. La tabla va ordenada por prioridad.)

## 4. Detalle de cada smell

### 1. God Class: `SistemaSolicitudes` (Alta)
- **Evidencia:** una sola clase con 376 líneas y 15 métodos que mezclan 8 responsabilidades: validación (47–68), reglas de estado (113–141), asignación (145–179), búsqueda (192–220), vencimientos (224–239), reporte (243–294), impresión por consola (298–312) y persistencia en archivo (316–375). Además guarda su estado en tres listas públicas (11–13).
- **Impacto:** cualquier cambio de negocio, de formato de salida o de almacenamiento toca esta clase. No se puede probar una regla sin arrastrar el archivo y la consola.
- **Riesgo:** los cambios interfieren entre sí; un ajuste del reporte puede romper el registro.
- **Corrección prevista:** `SolicitudService`, `SolicitudRepository`, `ReporteService`, `Validador`. Principio SRP. Refactoring: Extract Class.

### 2. Long Method: `registrarSolicitud` (Alta)
- **Evidencia:** 65 líneas (45–109) con 20 puntos de decisión. En un solo método: valida legajo y nombre (47–54), tipo (55–59), prioridad (60–63) y email (64–68); cuenta pendientes (69–78); busca o crea el estudiante (79–88); aplica urgencia (89–94); arma la solicitud (95–104); guarda (106) e imprime (107).
- **Impacto:** difícil de leer y de testear por partes; el orden de las validaciones (que fija los códigos de error) queda implícito.
- **Riesgo:** reordenar o agregar una validación cambia el código devuelto. Los tests de caracterización lo fijan (`laValidacionDeTipoVaAntesQueLaDeEmail`).
- **Corrección prevista:** Extract Method (obligatorio): `validar…`, `contarPendientes`, `buscarOCrearEstudiante`, `calcularPrioridad`.

### 3. Long Parameter List (Media)
- **Evidencia:** `registrarSolicitud(legajo, nombre, email, tipo, descripcion, prioridad, urgente)` tiene 7 parámetros (40–41) y la sobrecarga con fecha tiene 8 (45–46). Seis de ellos son `String`, lo que permite pasarlos en el orden equivocado sin error de compilación. `Main` lo llama en la línea 50.
- **Impacto:** llamadas difíciles de leer; agregar un dato obliga a cambiar la firma y a todos los llamadores.
- **Riesgo:** confundir `nombre` con `email` o `tipo` con `descripcion` compila sin quejas.
- **Corrección prevista:** Introduce Parameter Object (obligatorio): `NuevaSolicitud`.

### 4. Switch sobre tipo, con Shotgun Surgery (Alta)
- **Evidencia:** el conocimiento de "qué es un tipo" está repartido en cuatro lugares: lista de tipos válidos (55–56), área requerida (162–170), plazo en días (226–234) y conteo del reporte (256–263). A eso se suma la lista de áreas válidas (29), que está relacionada con el tipo porque cada tipo exige un área.
- **Impacto:** agregar un tipo nuevo exige editar cinco lugares, y el compilador no avisa si se olvida uno.
- **Riesgo:** un tipo olvidado en `estaVencida` queda con plazo 0 (línea 225) y se marca vencido al día siguiente; en `asignarResponsable` queda con área `null` y nadie puede ser asignado.
- **Corrección prevista:** `enum TipoSolicitud` con plazo y área propios. Principio OCP. Refactoring: Replace Conditional with Polymorphism.

### 5. Primitive Obsession (Alta)
- **Evidencia:** `estado` y `tipo` son `String` (`Solicitud` 8 y 12). Se comparan con literales 26 veces en `SistemaSolicitudes` (12 de estado, por ejemplo 71, 120–131, 235, 247–254; y 14 de tipo). Las transiciones válidas están codificadas con ifs anidados y un booleano (119–132).
- **Impacto:** un error de tipeo (`"PENDIENTE "`, `"Pendiente"`) no falla al compilar. Las reglas de transición no tienen un lugar propio para consultarlas.
- **Riesgo:** el filtro `filtrarPorEstado("pendiente")` devuelve vacío en silencio (fijado en el test `filtrarPorEstado`).
- **Corrección prevista:** `enum EstadoSolicitud` con `puedePasarA(...)`. Refactoring: Replace Type Code with Enum; Move Method.

### 6. Feature Envy: `estaVencida` (Baja)
- **Evidencia:** 224–239. Recibe una `Solicitud` y solo usa `s.tipo`, `s.estado` y `s.fechaCreacion`; no usa ningún dato de `SistemaSolicitudes`.
- **Impacto:** la regla de vencimiento vive lejos de los datos que usa. `reporte` (281) debe pasar por la clase dios para preguntarlo.
- **Riesgo:** bajo, es un método corto, pero mantiene la dependencia con la clase dios.
- **Corrección prevista:** Move Method (obligatorio) a `Solicitud.estaVencida(LocalDate hoy)`.

### 7. Código duplicado (Media)
- **Evidencia:**
  - `emailValido()` es idéntico en `Estudiante` (14–30) y `Responsable` (16–32): 17 líneas copiadas.
  - Los tres `filtrarPor…` (192–220) repiten el mismo esqueleto de 9 líneas y solo cambia la condición.
  - La búsqueda de un objeto por clave con un `for` aparece en 80–84, 152–156, 345–349 y 361–365.
- **Impacto:** corregir la regla de email exige acordarse de las dos copias.
- **Riesgo:** que una copia quede desactualizada y estudiantes y responsables se validen distinto.
- **Corrección prevista:** un `Validador` único; `filtrar(Predicate<Solicitud>)`. DRY.

### 8. Números mágicos (Media)
- **Evidencia:** `3` como máximo de pendientes (75) repetido en el mensaje de error (76); `3` como prioridad máxima (91–92); plazos `5`, `30`, `10`, `7` (227–233); `-1` con dos significados distintos, "error" en los retornos y "sin responsable" en el CSV (326).
- **Impacto:** para cambiar el límite o un plazo hay que saber que el número está suelto y dónde.
- **Riesgo:** cambiar el `3` de pendientes (75) y olvidar el texto del mensaje (76), o confundir el `3` de pendientes con el de prioridad.
- **Corrección prevista:** constantes con nombre y plazos dentro del enum. Refactoring: Replace Magic Number with Constant.

### 9. Manejo de errores pobre (Alta)
- **Evidencia:**
  - Catch vacíos: `guardar` (329–330) y `cargar` (373–374).
  - Códigos de retorno que hay que memorizar: -1 (49, 53, 67), -3 (58), -4 (62), -2 (77), `false` y `null` (27, 31, 189).
  - Mensajes por `System.out` mezclados con la lógica en 19 puntos (por ejemplo 26, 30, 48, 76, 107, 134, 172, 292, 299–311).
- **Impacto:** el que llama no sabe por qué falló sin leer el código ni la consola. La lógica no se puede reutilizar desde otra interfaz (web, tests) sin imprimir.
- **Riesgo:** **ya produce pérdida de datos**: ver defecto D4 más abajo.
- **Corrección prevista:** excepciones de dominio dentro; `SistemaSolicitudes` como fachada que las atrapa y mantiene los códigos (ver `CLAUDE.md`). Clean Code.

### 10. Nombres pobres (Baja)
- **Evidencia:** campos `lst`, `est`, `resp`, `cont` (11–14); método `proc` (183) que en realidad busca por id; variables `aux` (64), `s2` (95), `flag` (119), `x` (80, 152, 345, 361); en `reporte` los contadores `p, ep, r, rz, c, e, rn, b` (244–245). En `imprimir` el local `resp` (299) tapa el campo `resp` (13).
- **Impacto:** hay que reconstruir qué significa cada nombre leyendo el cuerpo.
- **Riesgo:** bajo, pero el tapado de `resp` en 299 invita a errores.
- **Corrección prevista:** Rename.

### 11. Acoplamiento a lo concreto (Alta)
- **Evidencia:** la lógica de negocio crea `FileWriter` (321) y `FileReader` (338) directamente, con imports de `java.io` en el mismo archivo que las reglas (3–5). `Main` construye `SistemaSolicitudes("solicitudes.csv")` (línea 8) y `ReporteSimple` (100, 111).
- **Impacto:** no hay forma de usar otro almacenamiento (memoria, base de datos) sin editar la clase dios. Los tests dependen del sistema de archivos (por eso usan `@TempDir`).
- **Riesgo:** el que prueba una regla de negocio puede dejar basura en disco o fallar por permisos.
- **Corrección prevista:** interfaz `SolicitudRepository` inyectada (CSV / memoria). Principio DIP.

### 12. Interfaz gorda + contrato roto (Media)
- **Evidencia:** `Exportable` (3–9) obliga a implementar CSV, JSON y PDF. `ReporteSimple.exportarPDF` (42–45) lanza `UnsupportedOperationException`. Síntoma visible en `Main` 110–118: para no romperse, el cliente rodea la llamada con `catch (UnsupportedOperationException)`, o sea, tiene que saber qué implementación tiene adelante.
- **Impacto:** quien recibe un `Exportable` no puede confiar en que todos sus métodos funcionan.
- **Riesgo:** un cliente nuevo que no conozca el catch tira la aplicación.
- **Corrección prevista:** `ExportadorCSV` y `ExportadorJSON` separadas. ISP y LSP; el catch de `Main` desaparece.

### 13. Data Class (Media)
- **Evidencia:** `Solicitud` (6–14), `Estudiante` (4–6) y `Responsable` (4–7) tienen todos sus campos `public` y casi nada de comportamiento. Cualquiera puede dejar un objeto en estado inválido (`s.estado = "XX"`). `Main` lee la lista interna directamente (`sis.resp`, línea 65; `sis.lst`, líneas 74, 100, 111).
- **Impacto:** las reglas que deberían proteger a estos objetos están en la clase dios.
- **Riesgo:** cualquier clase puede saltarse las reglas de estado.
- **Corrección prevista:** encapsulamiento y comportamiento en la entidad.

### 14. Acoplamiento temporal (Media)
- **Evidencia:** `Main` tiene que ejecutar los pasos en un orden fijo: crear el sistema (8), registrar los cuatro responsables (9–12) y recién después llamar a `cargar()` (13). El constructor (17–19) solo guarda la ruta del archivo, así que el objeto no queda utilizable hasta completar esa secuencia. `cargar()` enlaza cada solicitud con su responsable buscándolo en la lista `resp` (361–365); si todavía no fue cargado, la solicitud queda con `responsable = null` y no se avisa.
- **Verificado:** con los responsables registrados antes de `cargar()`, la solicitud #1 recupera a "Laura"; con el orden invertido, el responsable de la solicitud #1 queda en `null` sin ningún mensaje ni excepción.
- **Impacto:** el correcto funcionamiento depende de un orden de llamadas que no está expresado en el código; cualquier cliente nuevo (otra interfaz, un test) tiene que conocerlo.
- **Riesgo:** un cambio de orden en `Main` borra en silencio las asignaciones guardadas, y la próxima vez que se guarde el CSV, la pérdida queda persistida (`-1` en la columna del responsable).
- **Corrección prevista:** que el repositorio resuelva las referencias al cargar, y que el sistema quede completo después del constructor (sin paso de inicialización aparte). DIP / Clean Code.

## 5. Defectos latentes (no son smells)

Los descubrió la caracterización. Los tests de `src/test` fijan hoy este comportamiento; se corrigen **después** del refactoring, cada uno en su propio commit y modificando a propósito solo el test afectado (ver `CLAUDE.md`).

| # | Defecto | Dónde | ¿Cubierto por test? |
|---|---|---|---|
| D1 | Se puede asignar responsable a una solicitud RESUELTA o RECHAZADA | `asignarResponsable` 145–179 no mira `s.estado` | Sí: `sePuedeReasignarYAsignarEnCualquierEstado` |
| D2 | `tipo == null` lanza `NullPointerException` en vez de devolver un código de error | línea 55 | Sí: `tipoNuloLanzaNullPointerException` |
| D3 | Si falla la escritura del CSV el error se oculta | 329–330 | Sí: `siNoSePuedeEscribirElArchivoElErrorSeTragaEnSilencio` |
| D4 | **Pérdida de datos silenciosa:** registrar con `descripcion == null` lanza `NullPointerException` dentro de `guardar` (línea 324), el catch vacío lo oculta y el `FileWriter` ya había truncado el archivo. El método devuelve un id válido, pero el CSV queda vacío | 321–330 | **No** (verificado a mano: tras la 1ª solicitud el CSV tiene 1 línea; tras registrar la 2ª con descripción nula tiene 0, mientras el método devuelve el id 2). **Solo se alcanza por API:** el menú lee la descripción con `nextLine()`, que nunca devuelve `null` |
| D5 | `cambiarEstado(id, null)` lanza `NullPointerException` | 121 | **No** (verificado a mano). **Solo se alcanza por API:** el menú obtiene el estado con `nextLine()`, que nunca devuelve `null` |
| D6 | Un `;` en el `nombre` **o en el `email`** corrompe el CSV: al recargar, esa línea y todas las siguientes se pierden en silencio (`emailValido()` acepta un `;`; el `nombre` no se valida) | 323 (nombre y email se escriben sin escapar) y 373–374 | **No** (verificado a mano en ambos casos: 3 solicitudes guardadas, 1 recargada). Alcanzable desde el menú |
| D7 | Llamar dos veces a `cargar()` duplica las solicitudes: no limpia `lst` antes de agregar | 333–375 | **No** (verificado a mano: con 1 solicitud en el archivo, tras dos `cargar()` hay 2 en memoria con el mismo id) |
| D8 | Un `;` en la descripción se reemplaza por `,` al guardar: la descripción no se recupera tal cual (pérdida silenciosa de un carácter) | 324 | Sí, fijado sin nombrarlo como defecto en `cadaOperacionGuardaElCsvCompleto` y `cargarReconstruyeElEstadoYContinuaLosIds` |

D4 es el más serio: el catch vacío del smell 9 convierte un error de entrada en pérdida de datos persistidos. D4 y D5 solo se pueden provocar llamando a la API directamente; desde el menú de consola no son alcanzables, por lo que su riesgo hoy es menor que el de D6 y D7. Conviene agregar tests de caracterización para D4, D5, D6 y D7 (D8 ya está cubierto) en un commit propio, antes del primer refactoring, para que el comportamiento actual quede fijado igual que el resto.

## 6. Orden sugerido de ataque

1. **Smells 10, 8** (Rename, constantes): cambios mecánicos y seguros para ganar legibilidad.
2. **Smells 4 y 5** (enums `TipoSolicitud` y `EstadoSolicitud`): son la base del resto y eliminan las comprobaciones sueltas sobre tipo y estado.
3. **Smell 6** (Move Method de `estaVencida` a `Solicitud`; obligatorio) y **13** (encapsulamiento).
4. **Smells 2 y 3** (Extract Method e Introduce Parameter Object; obligatorios), ya sobre los enums.
5. **Smell 7** (validador único y `filtrar(Predicate)`).
6. **Smells 9, 11 y 14** (excepciones de dominio con fachada que conserva los códigos; interfaz `SolicitudRepository`, que además resuelve las referencias al cargar y elimina el orden obligatorio de `Main`).
7. **Smell 1** (Extract Class final: `SolicitudService`, `ReporteService`), dejando `SistemaSolicitudes` como fachada.
8. **Smell 12** (interfaces de exportación chicas; se reescribe `ExportacionCaracterizacionTest`).
9. Defectos D1–D8, uno por commit.

Después de cada paso se corre `mvn test`: los tests de `SistemaSolicitudesCaracterizacionTest` no se modifican.

## 7. Evidencia de herramientas

Análisis estático con PMD sobre el código de producción (`src/main`) de `estado-inicial`:

```bash
mvn pmd:pmd
```

- **Herramienta:** `maven-pmd-plugin` 3.28.0 con PMD 7.17.0 y el conjunto de reglas por defecto del plugin, sin configuración propia.
- **Reporte:** [`docs/evidencias/pmd-estado-inicial.xml`](evidencias/pmd-estado-inicial.xml) (copia de `target/pmd.xml`).
- **Resultado:** 2 violaciones, ambas `EmptyCatchBlock` (prioridad 3) en `SistemaSolicitudes.guardar` (líneas 329–330) y `SistemaSolicitudes.cargar` (373–374). Coinciden exactamente con la evidencia del smell 9 y con el defecto D3.
- **Alcance:** el conjunto por defecto solo detecta problemas de ese tipo. No marca la clase dios, los métodos largos, la duplicación, los números mágicos ni los nombres pobres (smells 1, 2, 7, 8 y 10), que quedan respaldados por el análisis manual de las secciones anteriores. Para respaldarlos con una herramienta haría falta un conjunto de reglas propio (por ejemplo `GodClass`, `CyclomaticComplexity`, `ExcessiveParameterList`, `AvoidDuplicateLiterals`) o CPD; queda como mejora si se necesita más evidencia.
- **Cobertura:** medida con JaCoCo en la etapa de code review (CR-07): 97,5 % de líneas sin contar `Main` (96,6 % cuando se midió por primera vez). Ver `docs/evidencias/jacoco-resumen.md`.
