# Etapa 7 — Code review profesional

- **Versión revisada:** `main` en el commit `b3d6bed` (posterior al tag `etapa-pruebas`).
- **Revisor:** una sesión de Claude (chat de claude.ai) que no escribió el código. Leyó el repositorio, los documentos
  de `/docs` y ejecutó pruebas propias, que no se incorporaron al repositorio, para confirmar cada hallazgo marcado
  como "verificado".
- **Alcance:** todo `src/main`, los tests de `src/test`, `pom.xml`, `README.md` y `/docs`.
- **Fuera de alcance:** rendimiento y seguridad. Es una aplicación de consola local con datos sintéticos.

## 1. Escala de severidad

| Severidad | Criterio |
|---|---|
| **Alta** | Pérdida o corrupción de datos, o contrato público roto. Bloquea la aceptación. |
| **Media** | Defecto acotado, o debilidad de diseño que contradice un principio declarado en el trabajo (SOLID, límites entre componentes). |
| **Baja** | Legibilidad, consistencia o documentación. No bloquea. |

## 2. Checklist técnico

Resultado: ✅ cumple · ⚠️ parcial · ❌ no cumple. La columna Ref remite a los hallazgos de la sección 3.

### 2.1 Diseño y SOLID
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| Cada clase tiene una sola razón de cambio (SRP) | ✅ | Servicios, repositorios, reporte y presentación están separados; la fachada solo traduce |
| Agregar un tipo de solicitud no exige tocar código existente (OCP) | ✅ | `TipoSolicitud`. Las áreas siguen sin cumplirlo: CR-04 |
| Las implementaciones cumplen todo lo que promete su interfaz (LSP) | ✅ | `ExportadorCSV` y `ExportadorJSON`; ya no hay `UnsupportedOperationException` |
| Ningún cliente depende de métodos que no usa (ISP) | ✅ | `SolicitudRepository` y `EstudianteRepository` separados |
| La lógica depende de abstracciones y el armado está en un único lugar (DIP) | ⚠️ | CR-06 |
| Las entidades protegen sus propias invariantes | ⚠️ | CR-03, CR-14 |
| La organización del código refleja los límites entre componentes | ❌ | CR-05 |

### 2.2 Clean Code
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| Los nombres expresan intención | ⚠️ | Correcto en dominio y servicios; persisten nombres cortos en `Main` (CR-08) |
| Los métodos son cortos y hacen una sola cosa | ⚠️ | Correcto salvo `Main.main` (114 líneas, CR-08) |
| No hay números ni textos mágicos | ⚠️ | Los números están resueltos; los textos de área se repiten (CR-04) |
| Los comentarios explican el porqué, no el qué | ✅ | Por ejemplo `RepositorioCsv.escribir`, `SolicitudService.validarDatosBasicos` |
| No hay código ni archivos sobrantes | ⚠️ | CR-13 |

### 2.3 Manejo de errores
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| No hay catch vacíos | ✅ | Revisado con grep: los tres `catch` de `src/main` informan el error |
| Los errores de negocio se informan con excepciones de dominio | ✅ | `DominioException` y sus hijas |
| Ninguna excepción no prevista escapa de la fachada | ❌ | CR-04: un área nula provoca `NullPointerException` |
| No se usa `null` como señal de error | ⚠️ | CR-09 |
| Las capturas son específicas | ⚠️ | `RepositorioCsv:172` captura `Exception` (ver CR-01) |

### 2.4 Persistencia y datos
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| Los textos libres se escapan al guardar | ✅ | `FormatoCsv` |
| Un archivo dañado no provoca pérdida de datos | ❌ | **CR-01** |
| Las exportaciones generan un formato válido | ❌ | CR-02 |
| La codificación de caracteres es explícita | ❌ | CR-12 |
| El dominio no conoce el almacenamiento | ✅ | Solo `RepositorioCsv` importa `java.io` |

### 2.5 Pruebas
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| Hay tests de caracterización anteriores al refactoring | ✅ | 51 tests en 3 clases |
| Los mismos tests pasan antes y después | ✅ | Con la excepción E1, documentada y con evidencia en `docs/evidencias/` |
| Cada cambio de test está justificado | ✅ | `docs/correccion-defectos.md` |
| Las clases nuevas tienen tests unitarios propios | ❌ | CR-07 |
| La cobertura está medida | ❌ | CR-07 (el propio diagnóstico la marca como pendiente) |

### 2.6 Repositorio y trazabilidad
| Ítem | Resultado | Ref / evidencia |
|---|---|---|
| Un refactoring por commit, con mensaje descriptivo | ✅ | R1 a R14, D1 a D8 |
| Tags obligatorios | ⚠️ | Están 4 de 6; `etapa-docs` y `estado-refactorizado` van al final, como está previsto |
| README con descripción, instalación, uso y tecnologías | ✅ | |
| No se versionan datos generados ni archivos personales | ✅ | `.gitignore` excluye `solicitudes.csv`, `CLAUDE.md` y `docs/prompts.md` |
| La documentación coincide con el código | ⚠️ | CR-13 |

## 3. Hallazgos

| ID | Severidad | Ubicación | Hallazgo |
|---|---|---|---|
| CR-01 | **Alta** | `RepositorioCsv` 132-175 y 96-106 | Una línea dañada provoca una carga parcial; el siguiente guardado borra el resto y reutiliza ids |
| CR-02 | Media | `ReporteSimple` 13-40 | Las exportaciones JSON y CSV no escapan los textos |
| CR-03 | Media | `Solicitud` 88-90 | La entidad acepta cualquier responsable; las reglas de asignación solo las controla el servicio |
| CR-04 | Media | `TipoSolicitud` 8-11, `ResponsableService` 15, `Main` 9-12 | Las áreas son textos repetidos, y un área nula provoca un `NullPointerException` que atraviesa la fachada |
| CR-05 | Media | `src/main/java/com/tup/solicitudes` | Las 32 clases están en un solo paquete; las capas solo existen en el README |
| CR-06 | Media | `SistemaSolicitudes` 27-58, `SolicitudService` 18 | El armado de dependencias está repartido y el constructor "DIP" no se puede usar con CSV |
| CR-07 | Media | `src/test`, `pom.xml` | No hay tests unitarios de las clases nuevas ni cobertura medida |
| CR-08 | Baja | `Main` 7-120 | La interfaz de consola mantiene un método largo y nombres cortos |
| CR-09 | Baja | `TipoSolicitud` 30-37, `EstadoSolicitud` 35-42, `RepositorioCsv` 33 | `null` sigue usándose como "no encontrado" |
| CR-10 | Baja | `SistemaSolicitudes` 111-122 | `codigoDeError` es una cadena de `instanceof` |
| CR-11 | Baja | `RepositorioCsv` 45-55, 70-85 | Las consultas modifican datos (completan responsables pendientes) |
| CR-12 | Baja | `RepositorioCsv` 101, 138 | Lectura y escritura con la codificación del sistema operativo |
| CR-13 | Baja | `docs/refactorings.md` 8-10, `docs/diagnostico-deuda-tecnica.md` 186, `ReporteSimple`, `.gitkeep` | Documentación desactualizada y restos menores |
| CR-14 | Baja | `Solicitud` 16-25, 76-81; `Estudiante` 8-12 | Los constructores no validan, y `pasarA` lanza `IllegalStateException` en lugar de una excepción de dominio |

### CR-01 — Pérdida de datos al cargar un archivo dañado (Alta, verificado)
- **Qué pasa:** `cargar()` vacía la memoria (línea 137) y procesa línea por línea. Si una línea es inválida, el `catch`
  (línea 172) avisa del error, pero en memoria quedan solo las solicitudes anteriores a esa línea. La siguiente
  operación que guarda (`escribir()`) reescribe el archivo con esa carga parcial.
- **Evidencia:** un archivo con 3 solicitudes donde la segunda tiene el tipo `CERTIFICAD0`. Al cargarlo se avisa el
  error y quedan 1 en memoria. Al registrar una nueva, el archivo queda con 2 líneas: la #1 y la nueva, que recibe el
  **id 2**. Las solicitudes #2 y #3 originales se pierden, y el id 2 pasa a identificar a otra solicitud.
- **Por qué importa:** es la misma clase de defecto que D4 (perder datos en silencio), pero ocurre en el código ya
  refactorizado. El aviso existe, aunque el sistema sigue operando y destruye el archivo.
- **Acción correctiva:** procesar todo el archivo en una lista temporal y reemplazar la memoria solo si no hubo
  errores. Si hubo, no reescribir el archivo hasta que se resuelva: guardar una copia (`.bak`) antes de sobrescribirlo,
  o dejar el repositorio en modo solo lectura y avisarlo. Capturar `IOException`, `NumberFormatException`,
  `DateTimeParseException` e `IllegalArgumentException` en lugar de `Exception`.
- **Criterio de aceptación:** un test que daña una línea, carga, registra una solicitud y verifica dos cosas: que las
  líneas originales siguen en el archivo o en el `.bak`, y que el id nuevo no coincide con ningún id existente.
  `mvn test` en verde.

### CR-02 — Exportaciones con formato inválido (Media, verificado)
- **Qué pasa:** el legajo se escribe tal cual. Con un legajo `L"1`, `exportarJSON()` genera `"legajo": "L"1"`, que
  no es JSON válido. Con `L;2`, `exportarCSV()` genera una fila con una columna de más.
- **Acción correctiva:** escapar `"` y `\` en JSON, y en CSV usar el mismo criterio que el archivo de datos (o
  comillas dobles según RFC 4180). Alternativa: limitar el formato del legajo en la validación de entrada, que
  además es una regla de negocio razonable.
- **Criterio de aceptación:** tests de exportación con comillas, `;` y `\` en los textos. El JSON resultante se
  puede volver a leer y el CSV tiene la cantidad de columnas correcta.

### CR-03 — La entidad no protege la regla de asignación (Media, verificado)
- **Qué pasa:** `Solicitud.asignarResponsable` es público y no controla nada. Llamándolo directamente se le puede
  asignar a una EQUIVALENCIA un responsable de Alumnado, o asignar una solicitud cerrada. Así lo saltea D1, aunque
  esté corregido en el servicio. `pasarA` sí protege su regla, lo que vuelve inconsistente el diseño.
- **Acción correctiva:** que `asignarResponsable` valide el área (ya existe `puedeSerAtendidaPor`) y el estado, y
  lance `AreaIncorrectaException` o `SolicitudCerradaException`. El servicio pasa a orquestar y deja de repetir la
  regla.
- **Criterio de aceptación:** tests unitarios de `Solicitud` para los dos casos inválidos. Los tests de la fachada
  no cambian.

### CR-04 — Áreas como texto repetido y `NullPointerException` en la fachada (Media, verificado)
- **Qué pasa:** `"Alumnado"`, `"Academica"` y `"Docencia"` aparecen en `TipoSolicitud`, `ResponsableService` y
  `Main`, así que agregar un área implica tocar tres lugares (es el smell 5 resuelto a medias). Además
  `registrarResponsable(..., area = null)` hace `area.equals(...)` y lanza `NullPointerException`. Como la fachada
  solo captura `DominioException`, la excepción escapa del contrato "devuelve `null` y avisa".
- **Acción correctiva:** un `enum Area` que usen `TipoSolicitud` y `Responsable`, y validar `null` como área
  inválida, igual que se hizo con el tipo en D2.
- **Criterio de aceptación:** `registrarResponsable` con área nula devuelve `null` y muestra
  `Error: area invalida`; en el código no queda ningún texto de área fuera del enum.

### CR-05 — Sin límites entre componentes en el código (Media)
- **Qué pasa:** el README describe cinco capas, pero las 32 clases conviven en `com.tup.solicitudes`. Nada impide
  que el dominio use la persistencia, y la consigna (etapa 4) pide explícitamente trabajar "límites entre
  componentes".
- **Acción correctiva, si se hace:** paquetes `dominio`, `servicio`, `persistencia` y `consola`, con
  `SistemaSolicitudes` en el paquete raíz. Ojo: `Solicitud.restaurar` es de paquete y lo usa `RepositorioCsv`, así
  que habría que cambiarlo por un constructor o una fábrica de reconstrucción. Los tests solo cambiarían en los
  `import`, como otra excepción de sintaxis documentada.
- **Alternativa válida:** no moverlo y declararlo como mejora pendiente en las conclusiones, explicando por qué.
- **Criterio de aceptación:** ningún import de `dominio` apunta a `persistencia` o `consola`; `mvn test` en verde.

### CR-06 — Armado de dependencias repartido (Media)
- **Qué pasa:** `SistemaSolicitudes` tiene cinco constructores encadenados y arma todo adentro, incluida la salida
  por consola (línea 57). El constructor público que recibe repositorios (32-34) crea su propio
  `ResponsableService`, y como `RepositorioCsv` necesita una función que busque en ese servicio, ese constructor no
  sirve para usar CSV. `SolicitudService` depende de la clase concreta `ResponsableService`.
- **Acción correctiva:** un único punto de armado (por ejemplo `ConfiguracionSistema.crear(rutaArchivo)` o el propio
  `Main`) que cree los repositorios y los servicios y se los pase a la fachada. Una interfaz para el catálogo de
  responsables.
- **Criterio de aceptación:** la fachada tiene como máximo dos constructores, y ninguna clase de servicio crea sus
  dependencias.

### CR-07 — Pruebas solo a través de la fachada (Media)
- **Qué pasa:** los 51 tests ejercitan `SistemaSolicitudes`. Eso es correcto para caracterizar, pero las clases
  nuevas (`EstadoSolicitud`, `TipoSolicitud`, `FormatoCsv`, `SolicitudService`, `RepositorioCsv`) no tienen tests
  propios, y no se midió la cobertura.
- **Acción correctiva:** tests unitarios mínimos. Por ejemplo, la matriz completa de transiciones de
  `EstadoSolicitud` e ida y vuelta de `FormatoCsv` con `;`, `\`, saltos de línea y textos vacíos. Además, el plugin
  de JaCoCo en `pom.xml` y el reporte en `docs/evidencias/`.
- **Criterio de aceptación:** reporte de JaCoCo versionado como evidencia, con cobertura de líneas igual o mayor a
  80 % excluyendo `Main`. El umbral es una propuesta del revisor.

### CR-08 — `Main` sin refactorizar (Baja)
- `main` tiene 114 líneas, con un `switch` de 10 casos que lee datos, convierte valores y llama a la lógica. Usa
  nombres como `sis`, `ne`, `idr`, `e`, `t`, `p`, `f` y `rs`, y depende de la clase concreta `ReporteSimple`. Si la
  entrada se termina, `Scanner` lanza `NoSuchElementException`.
- **Acción:** un Extract Method por opción del menú y Rename. **Aceptación:** ningún método de `Main` supera las 25
  líneas.

### CR-09 — `null` como "no encontrado" (Baja)
- `TipoSolicitud.desde` y `EstadoSolicitud.desde` devuelven `null`, y la función `buscarResponsable` de
  `RepositorioCsv` también, mientras el resto del código usa `Optional`.
- **Acción:** devolver `Optional` en todos los casos. **Aceptación:** ningún método de búsqueda devuelve `null`.

### CR-10 — Cadena de `instanceof` en la fachada (Baja)
- `codigoDeError` vuelve a decidir según el tipo, que es justo lo que el trabajo eliminó en otras partes. Es un caso
  aceptable, porque la traducción es responsabilidad exclusiva del adaptador y no conviene que las excepciones de
  dominio conozcan los códigos heredados. Una tabla `Map<Class<?>, Integer>` lo haría más declarativo.
- **Acción:** dejarlo, pero justificarlo en `docs/decisiones-tecnicas.md`. Es preferible que el corrector lo vea
  explicado a que lo marque como inconsistencia.

### CR-11 — Consultas con efectos secundarios (Baja)
- `listarTodas()` y `buscarPorId()` de `RepositorioCsv` asignan responsables pendientes mientras consultan. Funciona,
  pero una lectura que modifica datos es difícil de razonar.
- **Acción:** completar los pendientes en el momento en que se registra un responsable (aviso desde
  `ResponsableService`), o documentar la decisión.

### CR-12 — Codificación implícita (Baja)
- `FileWriter` y `FileReader` usan la codificación del sistema. En Windows, un nombre con tilde guardado en una
  máquina puede leerse mal en otra.
- **Acción:** usar `Files.newBufferedWriter` y `Files.newBufferedReader` con `StandardCharsets.UTF_8`.
  **Aceptación:** un test de ida y vuelta con `"Núñez"`.

### CR-13 — Documentación y restos (Baja)
- `docs/refactorings.md` 8-10 todavía dice que los miembros públicos "no se pueden renombrar ni encapsular", algo que
  dejó de ser cierto con E1 y R8/R9. Hay que marcarlo como histórico o actualizarlo.
- `docs/diagnostico-deuda-tecnica.md` 186 marca la cobertura de JaCoCo como pendiente; se cierra con CR-07.
- `ReporteSimple` ya no es un reporte sino un exportador, y su campo `datos` no es `final`.
- `src/test/java/com/tup/solicitudes/.gitkeep` ya no hace falta.

### CR-14 — Invariantes de construcción (Baja)
- `new Solicitud(...)` acepta `tipo` o `fechaCreacion` nulos, y `estaVencida` falla después con
  `NullPointerException`. `Estudiante` tampoco valida.
- `pasarA` lanza `IllegalStateException`, mientras el resto del dominio usa `DominioException`.
- **Acción:** `Objects.requireNonNull` en los constructores y `TransicionInvalidaException` en `pasarA`.

## 4. Feedback general

El trabajo cumple con lo central de la consigna: el diagnóstico tiene evidencia línea por línea, hay un refactoring
por commit, los tests de caracterización quedaron fijados antes de tocar el código y cada cambio de comportamiento
está documentado. La decisión de conservar `SistemaSolicitudes` como fachada que traduce excepciones a los códigos
originales está bien justificada, y es lo que hace creíble el "antes/después". También es destacable que los
defectos se separaron del refactoring.

Las debilidades se concentran en dos zonas. Una es la persistencia: CR-01 repite, en el código nuevo, el tipo de
defecto que el trabajo corrigió en el viejo. La otra es la consistencia del diseño: las reglas que protege la entidad
y las que no (CR-03), las áreas como texto (CR-04), el armado de dependencias (CR-06) y los límites de paquetes
(CR-05). Ninguna invalida el trabajo, pero un corrector que conozca SOLID las va a encontrar.

## 5. Criterios de aceptación técnica

El repositorio se acepta para el tag `etapa-docs` cuando:

1. No queda ningún hallazgo **Alta** abierto (CR-01).
2. Cada hallazgo **Media** está corregido o tiene registrada en la sección 6 una justificación de por qué se
   difiere, y figura como mejora pendiente en las conclusiones.
3. Los hallazgos **Baja** pueden quedar abiertos, pero con su estado registrado.
4. `mvn test` pasa completo después de cada corrección, y cada corrección va en su propio commit con el ID del
   hallazgo en el mensaje (por ejemplo `CR-01: ...`).
5. Ningún test de caracterización cambia salvo para agregar casos. Si cambia uno, se documenta como en
   `correccion-defectos.md`.

## 6. Registro de correcciones

A completar a medida que se resuelven los hallazgos.

| ID | Estado (Corregido / Diferido / Rechazado) | Commit | Justificación o nota |
|---|---|---|---|
| CR-01 | Corregido | `5d8bd02` | Carga todo o nada: si alguna linea es invalida no se carga nada, se informan las lineas, se reservan los ids hasta el mayor visto y, antes de reescribir, se hace una copia `.bak-<fecha>`. Captura excepciones especificas. 4 tests nuevos (`RepositorioCsvTest`). |
| CR-02 | Corregido | `005bd81` | El JSON escapa comillas, barra invertida y caracteres de control; el CSV usa el mismo escape que el archivo de datos (`FormatoCsv`). 4 tests nuevos (`ReporteSimpleEscapesTest`). |
| CR-03 | Corregido | `98a840b` | `Solicitud.asignarResponsable` valida que este abierta y que el area coincida (`SolicitudCerradaException` / `AreaIncorrectaException`); el servicio solo orquesta. 6 tests unitarios (`SolicitudTest`). Unica diferencia observable: con solicitud cerrada y responsable inexistente se informa primero el responsable. |
| CR-04 | Corregido | `931a89a` | `enum Area` concentra los textos (antes en `TipoSolicitud`, `ResponsableService` y `Main`); `registrarResponsable` con area nula devuelve `null` y muestra `Error: area invalida`. Ya no queda ningun texto de area fuera del enum. 6 tests nuevos (`ResponsableServiceTest`). |
| CR-05 | Diferido | (sin cambios de codigo) | Decision del autor. Los limites entre componentes ya estan expresados en el diseno con interfaces (`SolicitudRepository`, `EstudianteRepository`, `CatalogoResponsables`), un unico punto de armado (`ConfiguracionSistema`) y una fachada que traduce, y estan descritos en el README. Pasar las mas de 35 clases a paquetes (`dominio`, `servicio`, `persistencia`, `consola`) exige abrir como `public` miembros hoy de paquete (`Solicitud.restaurar`, `FormatoCsv`, `RepositorioEnMemoria.incorporar`) y cambiar los `import` de todos los tests, incluidos los de caracterizacion: es un cambio estructural sin mejora de comportamiento. Riesgo reconocido: la consigna (etapa 4) menciona los limites entre componentes y el checklist lo marca como no cumplido. Pasa a la seccion de mejoras pendientes de las conclusiones. |
| CR-06 | Corregido | `9676e07` | `ConfiguracionSistema` es el unico lugar de armado; la fachada tiene dos constructores (`String` y uno privado con las piezas, expuesto como `SistemaSolicitudes.con(...)` para que `new SistemaSolicitudes(null)` no sea ambiguo); `SolicitudService` depende de la interfaz `CatalogoResponsables`; el sistema se puede armar con CSV y su catalogo. 3 tests nuevos (`ConfiguracionSistemaTest`). |
| CR-07 | Corregido | `804b86b` | 27 tests unitarios nuevos de `EstadoSolicitud`, `TipoSolicitud`, `FormatoCsv`, `ValidadorEmail` y `SolicitudService`, que se suman a los agregados en CR-01 a CR-06 (101 en total). JaCoCo 0.8.12 en el `pom.xml`: 97,5 % de lineas sin contar `Main` al cierre de la correccion, 96,6 % cuando se agrego (umbral propuesto: 80 %). Evidencia en `docs/evidencias/jacoco-resumen.md` y `jacoco-cobertura.csv`. |
| CR-08 | Corregido | `866cb74` | `Main` pasa de un `main` de 114 lineas a metodos de 20 lineas o menos: uno por opcion del menu y helpers de lectura; nombres descriptivos. Si la entrada se cierra, sale con "Hasta luego" en lugar de lanzar `NoSuchElementException`. Mismos textos y orden de preguntas (verificado a mano con una sesion por stdin). Sigue usando `ReporteSimple` por el nombre (ver CR-13). |
| CR-09 | Corregido | `6dcf249` | `TipoSolicitud.desde`, `EstadoSolicitud.desde` y la busqueda de responsables de `RepositorioCsv` devuelven `Optional`; los usos pasan a `orElseThrow`/`filter`. Solo se ajustaron los tests propios de los enums, no los de caracterizacion. |
| CR-10 | Corregido (justificado) | `4118695` | Se deja la cadena de `instanceof` a proposito y se explica en `decisiones-tecnicas.md` (decision 12): la traduccion al contrato heredado es responsabilidad del adaptador y las excepciones de dominio no deben conocer los codigos. |
| CR-11 | Corregido (documentado) | `b060bb6` | Se documenta en `decisiones-tecnicas.md` (decision 13) por que las consultas completan los responsables pendientes: es idempotente, solo toca memoria y evita una dependencia circular catalogo-persistencia. Cubierto por tests existentes. |
| CR-12 | Corregido | `a857015` | Lectura y escritura con `Files.newBufferedReader/Writer` y UTF-8. Un archivo que no se puede decodificar se trata como danado (copia antes de reescribir). 2 tests nuevos en `RepositorioCsvTest` (ida y vuelta con "Núñez" y archivo Latin-1). |
| CR-13 | Corregido (parcial) | `a234767` | `refactorings.md` marca como historica la restriccion de campos publicos; el diagnostico cierra el pendiente de JaCoCo; `ReporteSimple.datos` es `final`; se elimino el `.gitkeep`. **No se renombro `ReporteSimple`**: obligaria a editar `ExportacionCaracterizacionTest`, y no se justifica una excepcion mas por un nombre. |
| CR-14 | Corregido | `d900bbb` | `Solicitud` (estudiante, tipo, fecha) y `Estudiante` (legajo, nombre, email) usan `Objects.requireNonNull`; la descripcion puede faltar. `pasarA` lanza `TransicionInvalidaException` con el mismo mensaje. 3 tests nuevos en `SolicitudTest`. |
