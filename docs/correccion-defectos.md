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
