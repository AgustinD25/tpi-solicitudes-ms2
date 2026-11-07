# Corrección de defectos latentes (después del refactoring)

Los defectos D1 a D8 y el smell 14 se descubrieron con los tests de caracterización (ver
`docs/diagnostico-deuda-tecnica.md`, sección 5). Los tests los fijaron tal como estaban para poder refactorizar
sin cambiar el comportamiento. **Ahora que el refactoring terminó**, cada uno se corrige en su propio commit,
cambiando a propósito **solo el test afectado** y explicando por qué. La consigna admite que el comportamiento
"se mantiene o mejora"; lo que no se admite es cambiarlo sin documentarlo.

Cada entrada dice: qué pasaba, qué pasa ahora, qué test cambió y por qué es una mejora.

---

## D1. No se podía impedir asignar un responsable a una solicitud cerrada

- **Antes:** `asignarResponsable` no miraba el estado: se le podía asignar o reasignar un responsable a una solicitud
  RESUELTA o RECHAZADA.
- **Ahora:** una solicitud cerrada no admite asignaciones: devuelve `false` y muestra
  `Error: la solicitud ya esta cerrada` (nueva `SolicitudCerradaException`). Reasignar mientras sigue abierta
  (PENDIENTE o EN_PROCESO) se sigue permitiendo. El chequeo va después de comprobar que la solicitud existe y antes de
  buscar al responsable.
- **Código:** `SolicitudService.asignarResponsable`.
- **Test cambiado:** `sePuedeReasignarYAsignarEnCualquierEstado` → `sePuedeReasignarMientrasNoEsteCerradaPeroNoUnaCerrada`.
  Antes afirmaba que asignar a una RESUELTA funcionaba; ahora afirma que falla, que el responsable anterior no cambia y
  que se muestra el mensaje.
- **Por qué mejora:** asignar a alguien una solicitud que ya terminó no tiene sentido de negocio y falseaba el
  reporte de quién resolvió qué.

---

## D2. Un tipo nulo rompía el registro con una excepción

- **Antes:** `registrarSolicitud(..., tipo = null, ...)` lanzaba `NullPointerException` en vez de informar el error
  como con cualquier otro tipo inválido.
- **Ahora:** un tipo nulo es un tipo inválido más: devuelve `-3` y muestra `Error: tipo de solicitud invalido`.
- **Código:** `TipoSolicitud.desde(null)` devuelve `null` en vez de lanzar (se quitó el `Objects.requireNonNull`).
- **Test cambiado:** `tipoNuloLanzaNullPointerException` → `tipoNuloEsTipoInvalidoYDevuelveMenosTres`.
- **Por qué mejora:** la API no debería caerse por un dato de entrada; todos los errores de datos se informan igual.

---

## D5. Un estado nulo rompía el cambio de estado con una excepción

- **Antes:** `cambiarEstado(id, null)` lanzaba `NullPointerException` (salvo en solicitudes ya cerradas, donde devolvía
  `false`): comportamiento irregular según el estado de la solicitud.
- **Ahora:** un estado nulo es una transición inválida más, en cualquier estado: devuelve `false` y muestra
  `Error: transicion invalida de PENDIENTE a null`. El estado de la solicitud no cambia.
- **Código:** se quitó el `Objects.requireNonNull` de `SolicitudService.cambiarEstado`, que se había dejado a
  propósito durante el refactoring.
- **Test cambiado:** `d5_cambiarEstadoConEstadoNuloLanzaNullPointerException` →
  `d5_cambiarEstadoConEstadoNuloEsUnaTransicionInvalida`. Para poder comprobar el mensaje, la clase de test pasó a
  guardar la salida de consola en un campo (antes la descartaba).
- **Por qué mejora:** mismo criterio que D2: los datos inválidos se informan, no rompen. Además elimina la
  irregularidad entre estados abiertos y cerrados.

---

## D7. `cargar()` no era idempotente: llamarlo dos veces duplicaba las solicitudes

- **Antes:** `cargar()` agregaba lo leído a lo que ya había en memoria. Con un archivo de 1 solicitud, dos
  llamadas dejaban 2 solicitudes con el mismo id 1.
- **Ahora:** `cargar()` reemplaza lo que hay en memoria por el contenido del archivo, así que repetirlo da el mismo
  resultado y los ids siguen en el valor correcto. Si el archivo todavía no existe (primera ejecución) no hace nada y no
  borra lo que hubiera en memoria.
- **Código:** `RepositorioCsv.cargar` y un método `vaciar()` protegido en `RepositorioEnMemoria`.
- **Test cambiado:** `d7_cargarDosVecesDuplicaLasSolicitudes` → `d7_cargarDosVecesDejaLoMismoQueCargarUnaVez`.
- **Por qué mejora:** una operación de carga no debería poder corromper los datos por repetirse; también evita ids
  duplicados, que rompían la búsqueda por id.

---

## D4. Pérdida silenciosa de datos al guardar con una descripción nula

- **Antes:** `registrarSolicitud(..., descripcion = null, ...)` devolvía un id válido, pero al guardar el CSV el sistema
  ya había abierto (y por lo tanto truncado) el archivo; el `NullPointerException` posterior quedaba oculto y el archivo
  quedaba **vacío**: se perdían también las solicitudes anteriores. Además el archivo no se cerraba.
- **Ahora:** (1) `RepositorioCsv` arma todo el contenido **antes** de abrir el archivo, así que un error al armarlo ya
  no puede dejar el archivo a medias; (2) usa `try-with-resources`, que siempre lo cierra; (3) una descripción nula se
  guarda como texto vacío. Con la descripción nula el archivo conserva las dos solicitudes y al recargar vuelven ambas.
- **Código:** `RepositorioCsv.escribir` y `aLinea`.
- **Test cambiado:** `d4_descripcionNulaVaciaElCsvSinAvisarYDevuelveUnIdValido` →
  `d4_descripcionNulaSeGuardaVaciaYNoSeTruncaElCsv`. Antes exigía `0` líneas en el archivo; ahora exige `2`, y que
  la recarga devuelva las dos solicitudes.
- **Por qué mejora:** el peor efecto del smell de los `catch` vacíos era este: un dato de entrada raro destruía datos
  ya guardados sin ningún aviso. Además desaparece la fuga del archivo abierto (por la que los tests tuvieron que
  limpiar el directorio temporal a mano en Windows).

---

## D3. Los errores de lectura y escritura del CSV se perdían en silencio

- **Antes:** `guardar()` y `cargar()` tenían un `catch (Exception) {}` vacío. Si el archivo no se podía escribir (ruta
  inválida, disco lleno, permisos), el registro devolvía un id válido y el usuario creía que sus datos estaban
  guardados.
- **Ahora:** `RepositorioCsv` informa el problema con una función que recibe al construirse. La fachada la conecta con
  la consola: `Error: no se pudo guardar el archivo <ruta>: <causa>` (y `no se pudo leer ...` al cargar). La operación
  sigue funcionando en memoria. El repositorio no depende de la consola: solo recibe un `Consumer<String>`.
- **Código:** `RepositorioCsv` (constructor con `avisarError`, `escribir`, `cargar`) y `SistemaSolicitudes.crearRepositorio`.
  `cargar()` también pasó a `try-with-resources`.
- **Test cambiado:** `siNoSePuedeEscribirElArchivoElErrorSeTragaEnSilencio` →
  `siNoSePuedeEscribirElArchivoSeAvisaPeroElRegistroSigueEnMemoria`. Mantiene que el id y la memoria están bien, y suma
  que el mensaje de error aparece.
- **Por qué mejora:** un error de persistencia deja de ser invisible. Se decidió avisar y no abortar el registro, para no
  perder la solicitud que ya está en memoria.

---

## D6 y D8. Un `;` en el nombre o el email corrompía el archivo, y en la descripción se cambiaba por `,`

Se corrigen en un solo commit porque comparten la causa: el formato del CSV no escapaba los separadores.

- **Antes (D6):** un `;` en el nombre o en el email (el validador de email lo acepta) partía la línea en más
  campos de los esperados; al recargar, esa línea y **todas las siguientes** se perdían. De 3 solicitudes guardadas,
  volvía 1.
- **Antes (D8):** en la descripción el `;` se reemplazaba por `,`, de modo que el texto no volvía igual
  (`Constancia; para beca` volvía como `Constancia, para beca`).
- **Ahora:** los textos libres (legajo, nombre, email y descripción) se escapan al guardar y se recuperan tal cual al
  leer: `\` → `\`, `;` → `\;`, salto de línea → `\n`, retorno de carro → `\r`. La clase `FormatoCsv` concentra
  las reglas. Al cargar se exige que cada línea tenga 11 campos; si no, se informa el error (D3) en vez de
  interpretar mal la línea.
- **Compatibilidad:** los archivos guardados por la versión anterior se siguen leyendo (no tienen barras).
- **Código:** `FormatoCsv` (nueva), `RepositorioCsv.aLinea` y `cargar`.
- **Tests cambiados:**
  - `d6_emailConPuntoYComaCorrompeElCsv...` → `d6_emailConPuntoYComaSeGuardaYRecargaIntacto` y
    `d6_nombreConPuntoYComaCorrompeElCsv...` → `d6_nombreConPuntoYComaSeGuardaYRecargaIntacto`: antes exigían 1
    solicitud recargada de 3; ahora exigen las 3 con sus datos.
  - `cadaOperacionGuardaElCsvCompleto`: la línea esperada del archivo pasa de `Constancia, para beca` a
    `Constancia\; para beca`.
  - `cargarReconstruyeElEstadoYContinuaLosIds`: la descripción recargada pasa de `Constancia, para beca` a
    `Constancia; para beca`.
  - Test nuevo `d8_laDescripcionConCaracteresEspecialesVuelveExactamenteIgual`: prueba `;`, `,`, barra invertida y
    salto de línea en una misma descripción.
- **Por qué mejora:** ya no se pierden datos ni se alteran por el contenido que escribe el usuario.

---

## Smell 14. Acoplamiento temporal: `cargar()` dependía de registrar antes a los responsables

- **Antes:** el archivo guarda solo el id del responsable. Si `cargar()` se llamaba antes de registrar a los
  responsables (como en `Main`, que depende de ese orden), las solicitudes quedaban con responsable `null` sin ningún
  aviso, y la próxima vez que se guardaba el archivo, la asignación se perdía para siempre.
- **Ahora:** el orden no importa. Si al cargar el responsable todavía no está registrado, `RepositorioCsv` recuerda
  el id y completa la solicitud en cuanto el responsable aparece (al listar o buscar). Mientras tanto, al volver a
  guardar el archivo se conserva el id original. Si mientras tanto se le asigna otro responsable a mano, gana la
  asignación nueva.
- **Código:** `RepositorioCsv` (`responsablePendiente`, `resolverResponsablesPendientes`, `idResponsableParaGuardar`).
- **Tests:**
  - Cambiado: `smell14_cargarAntesDeRegistrarResponsablesDejaLasAsignacionesEnNull` →
    `smell14_elOrdenDeCargarYRegistrarResponsablesNoImporta` (antes exigía `null`; ahora exige "Laura Gomez").
  - Nuevo: `smell14_laAsignacionPendienteNoSePierdeAlVolverAGuardar`.
- **Por qué mejora:** se elimina una dependencia oculta del orden de llamadas (el objeto ya no queda en un estado
  inválido hasta que alguien llame en el orden correcto) y una pérdida de datos silenciosa.

---

## Resumen

| Defecto | Estado | Commit |
|---|---|---|
| D1 asignar a solicitud cerrada | Corregido | `8b11059` |
| D2 tipo nulo | Corregido | `63dadc0` |
| D3 errores de archivo ignorados | Corregido | `a6e27de` |
| D4 pérdida de datos con descripción nula | Corregido | `10d2c8b` |
| D5 estado nulo | Corregido | `e4d69d5` |
| D6 `;` en nombre o email / D8 `;` en descripción | Corregidos | `0883f46` |
| D7 `cargar()` no idempotente | Corregido | `f5aaf57` |
| Smell 14 acoplamiento temporal | Cerrado | commit "Cierre del smell 14" (el siguiente a D6 y D8) |

Los 51 tests pasan después de cada commit. En cada uno solo se cambiaron los tests del defecto corregido, y cada
cambio está marcado con un comentario `CAMBIO DELIBERADO` en el propio test.
