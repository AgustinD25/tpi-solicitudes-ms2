# Conclusiones y aprendizaje (etapa 8)

Borrador de base para el informe (apartado 10 de la consigna). Los números salen de los documentos de `docs/` y de
`mvn test`; las reflexiones personales conviene reescribirlas con palabras propias antes de la entrega.

## 1. Qué se logró

| | Estado inicial (`estado-inicial`) | Estado final (`estado-refactorizado`) |
|---|---|---|
| Estructura | 1 clase dios de 376 líneas con 8 responsabilidades | fachada + 3 servicios + repositorios + entidades (35 archivos, casi todos chicos) |
| Smells | 14 diagnosticados (8 núcleo + 5 complementarios + acoplamiento temporal) | 14 cerrados |
| Refactorings | — | 14 (R1 a R14), un commit cada uno; los obligatorios son R4 (Extract Method), R3 (Move Method) y R5 (Introduce Parameter Object) |
| Defectos latentes | 8 ocultos (D1 a D8) | 8 corregidos, cada uno en su commit y con su test marcado `CAMBIO DELIBERADO` |
| Tests | 42 de caracterización | 106 (51 de caracterización y defectos + 55 unitarios del code review) |
| Cobertura | no medida | 97,5 % de líneas y 96,2 % de ramas, sin contar `Main` (JaCoCo) |
| Code review | — | 14 hallazgos: 11 corregidos en código, 2 resueltos con una decisión documentada (CR-10, CR-11) y 1 diferido (CR-05) |

El comportamiento externo se conservó: `SistemaSolicitudes` sigue siendo la fachada con los mismos códigos de retorno
(-1/-2/-3/-4, `false`, `null`) y los mismos mensajes. Las únicas diferencias visibles están documentadas:
la opción "Exportar PDF" que se quitó (R14), los defectos corregidos (`correccion-defectos.md`) y el orden de los
errores cuando una solicitud cerrada se asigna a un responsable inexistente (CR-03).

## 2. Decisiones que más pesaron

- **Caracterizar antes de tocar.** Los tests sobre la versión inicial dieron la red de seguridad y, de paso,
  encontraron los defectos D1 a D8, que de otro modo no se habrían visto: el sistema "funcionaba" desde afuera
  (calidad externa) y escondía problemas por dentro (calidad interna).
- **Separar refactoring de corrección de defectos.** Primero se refactorizó con los tests fijando incluso el
  comportamiento defectuoso, y recién después se corrigió cada defecto por separado. Así, un test en rojo siempre
  tenía una sola causa posible.
- **Excepciones adentro, códigos en la fachada (Adapter).** Permitió modernizar el manejo de errores sin romper el
  contrato público ni los tests (decisiones 1 y 2 de `decisiones-tecnicas.md`).
- **Reglas estrictas sobre los tests.** Nunca se modificó un test para que pase. Las excepciones fueron pocas, con
  evidencia y condiciones: el cambio de acceso por getters (E1), la reescritura de `ExportacionCaracterizacionTest` y
  los defectos corregidos a propósito.

## 3. Dificultades

- **Encapsular sin poder tocar los tests.** Los tests accedían a campos públicos, así que encapsular las entidades
  (smells 4, 5 y 13) obligó a negociar una excepción acotada y verificable (E1) en vez de editar tests a la ligera.
- **Persistencia en archivo.** Fue la zona con más problemas: el escape de `;` y saltos de línea, el archivo que se
  truncaba antes de escribir, las referencias a responsables que aún no estaban registrados y los manejadores de
  archivo sin cerrar (que en Windows impedían borrar los archivos temporales de los tests).
- **El código nuevo también tiene defectos.** El code review encontró, en el código ya refactorizado, el mismo tipo de
  defecto que el trabajo había corregido en el viejo (CR-01: una línea inválida hacía perder datos al reescribir el
  archivo). Refactorizar no garantiza que el código nuevo sea correcto; hace falta revisarlo y probarlo igual.
- **Medir antes de afirmar.** En el primer borrador del diagnóstico hubo cifras mal contadas (porcentaje de código en
  la clase dios, cantidad de `System.out`, cantidad de comparaciones). Se corrigieron contando con herramientas, y
  quedó como regla: toda cifra del informe sale de un comando o de un reporte.

## 4. Límites del trabajo

- Es un sistema chico y sintético: sin concurrencia, sin base de datos y con un CSV como persistencia simulada.
- Los tests de caracterización fijan lo que el sistema *hace*, no lo que *debería* hacer; por eso sirven para
  refactorizar, pero no prueban que las reglas de negocio sean las correctas.
- `Main` (la consola) no tiene tests automáticos; se verificó a mano.
- El análisis estático usó solo las reglas por defecto de PMD, que apenas detectan los `catch` vacíos. Los demás
  smells se respaldan con análisis manual y con las métricas citadas en el diagnóstico.
- El code review lo hizo una sesión distinta de la que escribió el código, pero sigue siendo asistido por IA y no
  reemplaza la revisión de otra persona.

## 5. Mejoras pendientes

| Mejora | Por qué quedó pendiente |
|---|---|
| **Paquetes por capa** (`dominio`, `servicio`, `persistencia`, `consola`) — **CR-05, diferido** | Hoy las capas están separadas por interfaces, por `ConfiguracionSistema` y por la fachada, pero no por el compilador: nada impide que el dominio use la persistencia. Hacerlo exige abrir como `public` miembros hoy de paquete (`Solicitud.restaurar`, `FormatoCsv`, `RepositorioEnMemoria.incorporar`) y cambiar los `import` de todos los tests, incluidos los de caracterización. Es la mejora más importante y la única que responde a una frase literal de la consigna (límites entre componentes). |
| Tests automáticos de `Main` | Requiere inyectar la entrada y la salida estándar. |
| Renombrar `ReporteSimple` a un nombre de exportador | Obliga a editar `ExportacionCaracterizacionTest` (CR-13). |
| Reglas propias de PMD o CPD | Para respaldar con una herramienta los smells que el conjunto por defecto no detecta. |
| Persistencia real y versionado del formato del CSV | Fuera del alcance de un TPI sobre calidad interna. |

## 6. Relación con los contenidos de la materia

- **Calidad externa vs interna:** el sistema inicial pasaba todas las pruebas "desde afuera" y aun así tenía deuda
  técnica real; la deuda se midió con evidencia (ubicación, impacto, riesgo y prioridad) y se pagó por prioridad.
- **Code smells y refactoring:** cada smell se asoció a un refactoring concreto y a un commit (`mapa-smells-refactorings.md`).
- **SOLID:** SRP (clase dios dividida), OCP (tipos y estados como enums), LSP e ISP (exportadores chicos sin método que
  lanza excepción), DIP (la lógica depende de interfaces de repositorio y no de archivos).
- **Clean Code:** nombres que dicen qué son, funciones cortas, errores como excepciones, cohesión (Move Method) y
  acoplamiento (un único punto de armado).
- **Pruebas y verificación:** tests de caracterización antes y después, tests unitarios y cobertura para evidenciar que
  no hubo regresión.
- **Code review:** checklist aplicado, hallazgos priorizados, correcciones con commit y criterios de aceptación.

## 7. Aprendizaje personal (completar con palabras propias)

- Qué fue lo más difícil de defender y por qué.
- Qué haría distinto si empezara de nuevo (por ejemplo, medir la cobertura desde la etapa de pruebas, no al final).
- Justificación de la entrega individual: por una cuestión de tiempos y de organización personal.
