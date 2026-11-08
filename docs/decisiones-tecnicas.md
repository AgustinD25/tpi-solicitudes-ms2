# Decisiones técnicas

Registro breve de las decisiones de diseño tomadas durante el trabajo, con el motivo y la alternativa descartada.
Cada una indica dónde está el detalle.

## 1. La clase `SistemaSolicitudes` se conserva como fachada (Adapter)
- **Decisión:** después de repartir las responsabilidades en servicios y repositorios, `SistemaSolicitudes` quedó como
  una fachada que atrapa las excepciones de dominio y devuelve los mismos códigos (-1/-2/-3/-4, `false`, `null`) y
  mensajes de siempre.
- **Por qué:** permite demostrar que **los mismos tests** pasan antes y después del refactoring, que es la evidencia
  central del TPI.
- **Descartado:** borrar la clase y reescribir los tests contra la API nueva (se perdería la comparación directa).
- **Detalle:** `refactorings.md` (R12).

## 2. Excepciones de dominio adentro, códigos de retorno afuera
- **Decisión:** `SolicitudService` y `ResponsableService` informan los errores con excepciones (`DominioException` y
  sus hijas). No imprimen ni devuelven códigos.
- **Por qué:** un código negativo obliga a quien llama a memorizar su significado y deja pasar el error si lo ignora; la
  lógica tampoco debería imprimir (no se podría reutilizar desde otra interfaz).
- **Detalle:** `refactorings.md` (R12).

## 3. Tipo y estado como `enum`, con las reglas dentro
- **Decisión:** `TipoSolicitud` lleva su plazo y su área; `EstadoSolicitud` lleva las transiciones permitidas.
- **Por qué:** antes había cuatro cadenas if/else por tipo y una de transiciones; agregar un tipo exigía editar cinco
  lugares. Ahora es agregar una constante (OCP).
- **Detalle:** `refactorings.md` (R6, R7).

## 4. Los getters de estado y tipo devuelven `String`
- **Decisión:** `Solicitud.getEstado()` y `getTipo()` devuelven el nombre como texto; para el código nuevo existen
  `getEstadoSolicitud()` y `getTipoSolicitud()`.
- **Por qué:** así los tests de caracterización (que comparan con `"PENDIENTE"`, `"CERTIFICADO"`) no cambiaron cuando
  por dentro pasaron a ser enums.

## 5. Excepción autorizada a los tests: acceso por getters
- **Decisión:** para poder encapsular las entidades se cambió **solo la forma de acceso** en los tests (campo → getter),
  en un commit aparte que no toca `src/main`.
- **Por qué:** sin eso los campos públicos no se podían volver privados. Se verificó con `git diff --word-diff` y por
  transformación inversa exacta; la salida está en `docs/evidencias/cambio-tests-acceso-campos.txt`.
- **Detalle:** `refactorings.md` (E1).

## 6. Repositorios como interfaces chicas
- **Decisión:** `SolicitudRepository` y `EstudianteRepository` separados; `RepositorioEnMemoria` y `RepositorioCsv` las
  implementan. El sistema recibe cualquier almacenamiento por constructor.
- **Por qué:** la lógica de negocio dejó de conocer `FileWriter` (DIP) y quien solo necesita estudiantes no depende de
  los métodos de solicitudes (ISP).
- **Detalle:** `refactorings.md` (R11).

## 7. Exportación por capacidades, sin PDF
- **Decisión:** `Exportable` (CSV, JSON y PDF) se reemplazó por `ExportadorCSV` y `ExportadorJSON`. Se eliminó la opción
  de menú "Exportar PDF".
- **Por qué:** `ReporteSimple` no podía exportar PDF y lanzaba `UnsupportedOperationException`; el cliente tenía que
  saber qué implementación recibía (violación de LSP). Es el único cambio visible del menú.
- **Detalle:** `refactorings.md` (R14).

## 8. Los defectos latentes se corrigieron después del refactoring
- **Decisión:** los tests fijaron primero el comportamiento tal cual era (defectos incluidos) y recién cuando el
  refactoring terminó se corrigió cada defecto en su propio commit, cambiando solo el test afectado.
- **Por qué:** mezclar refactoring con cambios de comportamiento hace imposible saber si un test falla por una cosa o
  por la otra. La consigna admite que el comportamiento "se mantiene o mejora" siempre que se documente.
- **Detalle:** `correccion-defectos.md`.

## 9. Formato del CSV con escape
- **Decisión:** los textos libres se escapan con `\` (`\\`, `\;`, `\n`, `\r`) y cada línea debe tener 11 campos.
- **Por qué:** un `;` en el nombre o en el email corrompía el archivo y se perdían esa línea y las siguientes.
  Los archivos viejos siguen leyéndose.

## 10. Errores de archivo: avisar sin abortar
- **Decisión:** si no se puede leer o escribir el CSV, `RepositorioCsv` avisa mediante una función recibida por
  constructor y el sistema sigue funcionando en memoria.
- **Por qué:** antes se ignoraban en silencio; abortar el registro haría perder una solicitud que ya está en memoria.
  La función recibida evita que el repositorio dependa de la consola.

## 11. Herramientas
- **Java 17 y `record`** para `NuevaSolicitud` (inmutable, sin código repetitivo).
- **JUnit 5** para los tests y **PMD** como evidencia de análisis estático (su conjunto de reglas por defecto solo marca
  los `catch` vacíos; el resto del diagnóstico es manual y está justificado en `diagnostico-deuda-tecnica.md`).
- **Sin framework de persistencia ni de inyección de dependencias:** el sistema es chico y alcanza con pasar las
  dependencias por constructor.

## 12. La fachada traduce excepciones a códigos con una cadena de `instanceof` (CR-10)
- **Decisión:** `SistemaSolicitudes.codigoDeError` decide el código negativo (-1/-2/-3/-4) según el tipo de la excepción
  de dominio, con una cadena de `instanceof`, y se dejó así a propósito.
- **Por qué:** el trabajo eliminó los `if/else` por tipo del *dominio* (smell 5) porque ahí cada alta de un tipo obligaba
  a tocar varios lugares. Acá es distinto: la traducción es responsabilidad exclusiva del adaptador, que existe para
  conservar el contrato heredado. La alternativa de que cada excepción conozca su código metería un detalle del
  contrato viejo dentro del dominio (acoplamiento al revés). Son solo tres casos, no crecen (el contrato original está
  congelado) y un `Map<Class<?>, Integer>` sería más declarativo pero no más simple ni más seguro.
- **Cuándo se reconsideraría:** si el contrato heredado dejara de estar congelado o los códigos pasaran de unos pocos.

## 13. Las consultas del repositorio CSV completan los responsables pendientes (CR-11)
- **Decisión:** `RepositorioCsv.listarTodas()` y `buscarPorId()` resuelven, antes de responder, los responsables que el
  archivo nombraba pero que todavía no estaban registrados al cargar. Se documentó y no se cambió.
- **Por qué existe:** el archivo guarda solo el id del responsable y los responsables se registran *después* de
  `cargar()`. Hay que guardar ese id y completarlo cuando el responsable aparezca; si no, la asignación se perdería al
  reescribir el archivo.
- **Por qué no se movió:** la alternativa (que `ResponsableService` avise al repositorio al registrar) crea una
  dependencia nueva y circular entre catálogo y persistencia, y complica el armado de `ConfiguracionSistema` por un
  caso que solo ocurre al arrancar.
- **Por qué es aceptable:** es idempotente (consultar dos veces da lo mismo), solo toca memoria (nunca escribe el
  archivo), es un `Map` acotado que se vacía solo a medida que se resuelve, y tiene tests (`DefectosLatentesCaracterizacionTest`: el orden de cargar y registrar responsables no importa, y volver a guardar no pierde la asignación).
- **Cuándo se reconsideraría:** si el repositorio se usara desde varios hilos, donde una lectura que modifica sí sería
  un problema.
