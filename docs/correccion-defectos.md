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
