# Registro de refactorings (etapa 5)

Un refactoring por commit. Después de cada uno se corrió `mvn test`: los tests de
`SistemaSolicitudesCaracterizacionTest`, `DefectosLatentesCaracterizacionTest` y
`ExportacionCaracterizacionTest` **no se modificaron** (49 tests, mismos resultados).
Los números de smell son los de `docs/diagnostico-deuda-tecnica.md`.

Restricción de diseño: los tests acceden a `sis.lst`, `sis.est`, `sis.resp` y a los campos públicos de
`Solicitud`/`Estudiante`/`Responsable` (por ejemplo `s.estado` como `String`). Mientras esos tests no se
reescriban a propósito, esos miembros no se pueden renombrar ni encapsular (ver "Pendientes" al final).

---

## R1. Replace Magic Number with Constant

- **Smell que cierra:** 8 (números mágicos).
- **Archivo:** `SistemaSolicitudes.java`.
- **Qué se hizo:** cada número con significado de negocio pasó a una constante con nombre: límite de pendientes,
  rango de prioridad, plazos por tipo, valor "sin responsable" del CSV y los cuatro códigos de retorno de
  `registrarSolicitud` (-1/-2/-3/-4), que son contrato público y ahora tienen nombre.

Antes:
```java
if (prioridad < 1 || prioridad > 3) {
    System.out.println("Error: prioridad fuera de rango");
    return -4;
}
...
if (pendientes >= 3) {
    System.out.println("Error: el estudiante ya tiene 3 solicitudes pendientes");
    return -2;
}
...
plazo = 5;   // CERTIFICADO
plazo = 30;  // EQUIVALENCIA
```

Después:
```java
if (prioridad < PRIORIDAD_MINIMA || prioridad > PRIORIDAD_MAXIMA) {
    System.out.println("Error: prioridad fuera de rango");
    return COD_PRIORIDAD_INVALIDA;
}
...
if (pendientes >= MAX_PENDIENTES_POR_ESTUDIANTE) {
    System.out.println("Error: el estudiante ya tiene " + MAX_PENDIENTES_POR_ESTUDIANTE
            + " solicitudes pendientes");
    return COD_LIMITE_PENDIENTES;
}
...
plazo = PLAZO_CERTIFICADO;
plazo = PLAZO_EQUIVALENCIA;
```

- **Por qué:** el `3` del límite de pendientes y el `3` de la prioridad máxima eran dos reglas distintas con el mismo
  número; el mensaje de error repetía el límite como texto. Ahora cambiar la regla es cambiar una constante y el
  mensaje queda sincronizado. El texto impreso es idéntico (los tests comparan el mensaje).
- **Verificación:** `mvn test` → 49 tests, 0 fallas.
- **Nota:** los plazos pasan a ser atributos del enum `TipoSolicitud` cuando se resuelva el smell 4.

---

## R2. Rename (nombres que dicen qué son)

- **Smell que cierra:** 10 (nombres pobres), salvo `lst`, `est` y `resp` (ver nota).
- **Archivo:** `SistemaSolicitudes.java`.
- **Qué se hizo:** renombrado de campos privados, métodos auxiliares y variables locales. Sin cambios de lógica.

| Antes | Después |
|---|---|
| `cont` | `ultimoId` |
| `archivo` | `rutaArchivo` |
| `proc(id)` | `buscarSolicitudPorId(id)` |
| `aux` | `estudianteIngresado` |
| `s2` | `nueva` |
| `flag` | `transicionValida` |
| `p, ep, r, rz, c, e, rn, b` (en `reporte`) | `pendientes, enProceso, resueltas, rechazadas, certificados, equivalencias, reclamosNota, bajasMateria` |
| `x`, `e`, `s`, `r` en los `for` | `candidato`, `estudiante`, `solicitud`, `responsable` |
| `resp` local en `imprimir` (tapaba el campo `resp`) | `nombreResponsable` |
| `l` | `solicitudes` |
| `p` (arreglo de `cargar`) | `campos` |

Antes:
```java
boolean flag = false;
if (s.estado.equals("PENDIENTE")) {
    if (nuevo.equals("EN_PROCESO")) {
        flag = true;
    } ...
}
if (!flag) { ... }
```

Después:
```java
boolean transicionValida = false;
if (solicitud.estado.equals("PENDIENTE")) {
    if (nuevoEstado.equals("EN_PROCESO")) {
        transicionValida = true;
    } ...
}
if (!transicionValida) { ... }
```

- **Por qué no se renombró `lst`, `est`, `resp`:** son campos públicos que usan los tests de caracterización
  (`sis.lst`, `sis.est`, `sis.resp`), que no se pueden modificar. Se dejó un comentario en el código que lo explica.
  Se cierran cuando se encapsule la clase (ver "Pendientes").
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R3. Move Method: `estaVencida` pasa a `Solicitud`

- **Smell que cierra:** 6 (Feature Envy). **Refactoring obligatorio.**
- **Archivos:** `Solicitud.java` (recibe el método y las constantes de plazo), `SistemaSolicitudes.java`.
- **Qué se hizo:** `estaVencida` solo usaba `tipo`, `estado` y `fechaCreacion` de la solicitud, así que se movió
  a la clase que tiene esos datos. En `SistemaSolicitudes` queda una delegación de una línea con la firma de
  siempre (`estaVencida(Solicitud, LocalDate)`), porque es contrato público que usan los tests. `reporte` ahora
  pregunta directamente a la solicitud.

Antes (`SistemaSolicitudes`):
```java
public boolean estaVencida(Solicitud solicitud, LocalDate hoy) {
    int plazo = 0;
    if (solicitud.tipo.equals("CERTIFICADO")) {
        plazo = PLAZO_CERTIFICADO;
    } else if ...
    if (solicitud.estado.equals("RESUELTA") || solicitud.estado.equals("RECHAZADA")) {
        return false;
    }
    return hoy.isAfter(solicitud.fechaCreacion.plusDays(plazo));
}
...
if (estaVencida(solicitud, hoy)) {      // en reporte()
```

Después:
```java
// Solicitud.java
public boolean estaVencida(LocalDate hoy) {
    int plazo = 0;
    if (tipo.equals("CERTIFICADO")) {
        plazo = PLAZO_CERTIFICADO;
    } else if ...
    if (estado.equals("RESUELTA") || estado.equals("RECHAZADA")) {
        return false;
    }
    return hoy.isAfter(fechaCreacion.plusDays(plazo));
}

// SistemaSolicitudes.java
public boolean estaVencida(Solicitud solicitud, LocalDate hoy) {
    return solicitud.estaVencida(hoy);
}
...
if (solicitud.estaVencida(hoy)) {       // en reporte()
```

- **Por qué:** la regla de vencimiento vive junto a los datos que usa. Se mantiene el comportamiento exacto,
  incluido el plazo 0 para un tipo desconocido (ver smell 4).
- **Verificación:** `mvn test` → 49 tests, 0 fallas (incluye `plazoDeCadaTipoVenceElDiaSiguienteAlLimite`).

---

## R4. Extract Method en `registrarSolicitud`

- **Smell que cierra:** 2 (Long Method). **Refactoring obligatorio.**
- **Archivo:** `SistemaSolicitudes.java`.
- **Qué se hizo:** el método de 65 líneas y complejidad ciclomática ≈ 21 se partió en pasos con nombre.
  `registrarSolicitud` queda en 24 líneas (61–84) con 3 decisiones propias (complejidad ≈ 4).

| Método extraído | Responsabilidad |
|---|---|
| `validarDatosBasicos` | legajo, nombre, tipo y prioridad, en el orden original (ese orden fija el código devuelto) |
| `esTipoValido` | la lista de tipos permitidos |
| `contarPendientes` | pendientes del estudiante |
| `buscarOCrearEstudiante` | reutiliza el estudiante por legajo o registra el nuevo |
| `calcularPrioridad` | regla de urgencia: sube un nivel con tope en `PRIORIDAD_MAXIMA` |
| `crearSolicitud` | arma la entidad con estado PENDIENTE |

Antes (fragmento):
```java
int pendientes = 0;
for (Solicitud solicitud : lst) {
    if (solicitud.estudiante.legajo.equals(legajo) && solicitud.estado.equals("PENDIENTE")) {
        pendientes++;
    }
}
if (pendientes >= MAX_PENDIENTES_POR_ESTUDIANTE) { ... }
...
if (urgente) {
    prioridad = prioridad + 1;
    if (prioridad > PRIORIDAD_MAXIMA) {
        prioridad = PRIORIDAD_MAXIMA;
    }
}
Solicitud nueva = new Solicitud();
ultimoId = ultimoId + 1;
nueva.id = ultimoId;
... (9 asignaciones más)
```

Después:
```java
int codigo = validarDatosBasicos(legajo, nombre, tipo, prioridad);
if (codigo != SIN_ERROR) {
    return codigo;
}
Estudiante estudianteIngresado = new Estudiante(legajo, nombre, email);
if (!estudianteIngresado.emailValido()) { ... }
if (contarPendientes(legajo) >= MAX_PENDIENTES_POR_ESTUDIANTE) { ... }
Estudiante estudiante = buscarOCrearEstudiante(estudianteIngresado);
int prioridadFinal = calcularPrioridad(prioridad, urgente);
Solicitud nueva = crearSolicitud(estudiante, tipo, descripcion, prioridadFinal, urgente, fecha);
lst.add(nueva);
guardar();
```

- **Por qué:** cada regla se puede leer, probar y cambiar por separado. El orden de las validaciones (y por lo tanto
  el código devuelto) se preserva; lo cubre `laValidacionDeTipoVaAntesQueLaDeEmail`.
- **Cuidado:** `SIN_ERROR = 0` es un valor interno privado; es seguro porque ningún id válido es 0 (empiezan en 1).
- **Pendiente:** `crearSolicitud` recibe 6 parámetros; se reduce en R5 (Parameter Object).
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R5. Introduce Parameter Object: `NuevaSolicitud`

- **Smell que cierra:** 3 (Long Parameter List). **Refactoring obligatorio.**
- **Archivos:** `NuevaSolicitud.java` (nuevo, un `record`), `SistemaSolicitudes.java`.
- **Qué se hizo:** los 8 datos de una solicitud nueva se agruparon en `NuevaSolicitud`. La lógica vive ahora en
  `registrarSolicitud(NuevaSolicitud)` y los métodos auxiliares de R4 reciben el objeto en vez de listas de
  parámetros (`crearSolicitud` pasó de 6 parámetros a 3). Las dos firmas viejas (7 y 8 parámetros) se conservan
  como delegaciones: son contrato público que usan los tests y `Main`.

Antes:
```java
public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                              String descripcion, int prioridad, boolean urgente, LocalDate fecha) {
    int codigo = validarDatosBasicos(legajo, nombre, tipo, prioridad);
    ...
    Solicitud nueva = crearSolicitud(estudiante, tipo, descripcion, prioridadFinal, urgente, fecha);
```

Después:
```java
public record NuevaSolicitud(String legajo, String nombre, String email, String tipo,
                             String descripcion, int prioridad, boolean urgente, LocalDate fecha) { }

public int registrarSolicitud(String legajo, String nombre, String email, String tipo,
                              String descripcion, int prioridad, boolean urgente, LocalDate fecha) {
    return registrarSolicitud(new NuevaSolicitud(legajo, nombre, email, tipo, descripcion,
            prioridad, urgente, fecha));
}

public int registrarSolicitud(NuevaSolicitud datos) {
    int codigo = validarDatosBasicos(datos);
    ...
    Solicitud nueva = crearSolicitud(estudiante, datos, prioridadFinal);
```

- **Por qué:** seis de los ocho parámetros eran `String` consecutivos: se podía invertir `nombre` con `email` sin que
  compilara con error. El objeto nombra cada campo y agregar un dato nuevo deja de cambiar todas las firmas.
  Se eligió un `record` (Java 17) porque es inmutable y no necesita getters escritos a mano.
- **Verificación:** `mvn test` → 49 tests, 0 fallas.
- **Pendiente:** las firmas viejas se podrán retirar cuando `Main` y los tests dejen de usarlas; los tests no se
  tocan, así que se conservan como parte de la fachada.

---

## E1. Excepción autorizada: los tests acceden por getters (no es un refactoring)

- **Para qué:** habilitar los smells 4, 5 y 13 (enums y encapsulamiento). Los tests leían los campos públicos
  (`assertEquals("PENDIENTE", s.estado)`, `sis.lst`, `sis.est`, `sis.resp`); pasar `estado` y `tipo` a `enum` o volver
  privados los campos los rompía, y `CLAUDE.md` prohíbe modificar los tests. Agus autorizó cambiar **solo la forma
  de acceso**, con condiciones. La regla quedó anotada en `CLAUDE.md`.
- **Cómo se hizo, en dos commits para que el cambio de tests sea aislable:**
  1. `3203fbe` (solo `src/main`): getters en `Solicitud`, `Estudiante` y `Responsable`, y en `SistemaSolicitudes`
     `getSolicitudes()`, `getEstudiantes()` y `getResponsables()`. Los campos siguen públicos. 49 tests en verde.
  2. `00544ff` (solo `src/test`): campo → getter en los tres archivos de test. Sigue con los campos públicos.
     49 tests en verde.

Antes (test):
```java
assertEquals("PENDIENTE", s.estado);
assertEquals("Laura Gomez", buscar(1).responsable.nombre);
assertEquals(3, sis.lst.size());
```

Después (test):
```java
assertEquals("PENDIENTE", s.getEstado());
assertEquals("Laura Gomez", buscar(1).getResponsable().getNombre());
assertEquals(3, sis.getSolicitudes().size());
```

Getters nuevos (producción):
```java
public String getEstado() { return estado; }            // sigue siendo String; el enum será interno
public List<Solicitud> getSolicitudes() { return List.copyOf(lst); }   // copia no modificable
```

- **Evidencia de que solo cambió la forma de acceso** (`docs/evidencias/cambio-tests-acceso-campos.txt`, generada con
  `git diff --word-diff` entre `3203fbe` y `00544ff`):
  - `src/main`: 0 archivos modificados en el commit de tests.
  - Transformación inversa getter → campo sobre los tests nuevos: **idéntica byte a byte** a los tests viejos en
    los 3 archivos.
  - Líneas con `assert`, cantidad de `@Test` y literales de texto: iguales antes y después (154/154, 6/6, 26/26;
    39, 3 y 7 tests).
  - 91 cambios de token, todos de la forma `[-campo-]{+getter+}` según una tabla fija de 16 pares; 0 líneas con
    cambios fuera de esa tabla. (El control se comprobó con casos falsos: un par inventado, un par cruzado y un cambio
    de valor se detectan.)
- **Por qué los getters de `estado` y `tipo` devuelven `String`:** así los valores esperados de los tests
  (`"PENDIENTE"`, `"CERTIFICADO"`) no cambian cuando por dentro pasen a ser enums.
- **Por qué las listas son copias no modificables:** `List.copyOf` impide que quien llama agregue o saque
  elementos y rompa el estado interno (era parte del smell 13).
- **Límite de la excepción:** no autoriza ningún otro cambio en los tests.

---

## R6. Replace Type Code with Enum + Replace Conditional with Polymorphism: `TipoSolicitud`

- **Smell que cierra:** 4 (Switch sobre tipo / Shotgun Surgery). Principio: OCP.
- **Archivos:** `TipoSolicitud.java` (nuevo), `Solicitud.java`, `SistemaSolicitudes.java`.
- **Qué se hizo:** el conocimiento de cada tipo (plazo y área que lo atiende) pasó de cuatro cadenas if/else
  repartidas a atributos del enum. `Solicitud.tipo` es ahora un `TipoSolicitud`; `getTipo()` sigue devolviendo
  `String` (el nombre) y se agregó `getTipoSolicitud()` para el código nuevo.

Antes (había cuatro copias de este patrón: validación, área, plazo, conteo del reporte):
```java
if (!tipo.equals("CERTIFICADO") && !tipo.equals("EQUIVALENCIA")
        && !tipo.equals("RECLAMO_NOTA") && !tipo.equals("BAJA_MATERIA")) { ... }
...
String areaRequerida = null;
if (solicitud.tipo.equals("CERTIFICADO")) {
    areaRequerida = "Alumnado";
} else if (solicitud.tipo.equals("EQUIVALENCIA")) {
    areaRequerida = "Academica";
} else if ...
...
int certificados = 0, equivalencias = 0, reclamosNota = 0, bajasMateria = 0;
```

Después:
```java
public enum TipoSolicitud {
    CERTIFICADO(5, "Alumnado"),
    EQUIVALENCIA(30, "Academica"),
    RECLAMO_NOTA(10, "Docencia"),
    BAJA_MATERIA(7, "Alumnado");
    ...
}

if (TipoSolicitud.desde(datos.tipo()) == null) { ... }
String areaRequerida = solicitud.tipo.getAreaResponsable();
return hoy.isAfter(fechaCreacion.plusDays(tipo.getPlazoDias()));
Map<TipoSolicitud, Integer> porTipo = new EnumMap<>(TipoSolicitud.class);   // reporte
```

- **Por qué:** agregar un tipo nuevo ahora es agregar **una constante**; antes había que editar cuatro lugares y el
  compilador no avisaba si se olvidaba uno (con un tipo desconocido, `estaVencida` usaba plazo 0). Las constantes
  `PLAZO_*` de `Solicitud` desaparecen: viven en el enum. El reporte recorre `TipoSolicitud.values()`, así que un
  tipo nuevo aparece solo.
- **Comportamiento conservado a propósito:** `desde` distingue mayúsculas (`"certificado"` sigue siendo inválido,
  código -3) y lanza `NullPointerException` con `null`, igual que la versión original (defecto D2, fijado por
  `tipoNuloLanzaNullPointerException`).
- **Diferencia mínima, solo para archivos manipulados a mano:** si el CSV trae un tipo que no existe, `cargar()`
  ahora deja de leer en esa línea (como ya hacía con cualquier línea corrupta) en vez de cargar una solicitud con
  un tipo inválido. Con archivos escritos por el sistema no cambia nada.
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R7. Replace Type Code with Enum + Move Method: `EstadoSolicitud.puedePasarA`

- **Smell que cierra:** 5 (Primitive Obsession en el estado). Principio: OCP / encapsulamiento de reglas.
- **Archivos:** `EstadoSolicitud.java` (nuevo), `Solicitud.java`, `SistemaSolicitudes.java`.
- **Qué se hizo:** `Solicitud.estado` pasó de `String` a `EstadoSolicitud`. Las transiciones válidas, que estaban
  en ifs anidados con un booleano dentro de `cambiarEstado`, se movieron al enum (`puedePasarA`, `esFinal`).
  `getEstado()` sigue devolviendo `String`.

Antes (`cambiarEstado`):
```java
boolean transicionValida = false;
if (solicitud.estado.equals("PENDIENTE")) {
    if (nuevoEstado.equals("EN_PROCESO")) {
        transicionValida = true;
    } else if (nuevoEstado.equals("RECHAZADA")) {
        transicionValida = true;
    }
} else if (solicitud.estado.equals("EN_PROCESO")) {
    ...
}
if (!transicionValida) { ... }
solicitud.estado = nuevoEstado;
```

Después:
```java
public enum EstadoSolicitud {
    PENDIENTE, EN_PROCESO, RESUELTA, RECHAZADA;
    static {
        PENDIENTE.destinosPermitidos = EnumSet.of(EN_PROCESO, RECHAZADA);
        EN_PROCESO.destinosPermitidos = EnumSet.of(RESUELTA, RECHAZADA);
        ...
    }
    public boolean puedePasarA(EstadoSolicitud destino) { return destinosPermitidos.contains(destino); }
    public boolean esFinal() { return destinosPermitidos.isEmpty(); }
}

EstadoSolicitud destino = EstadoSolicitud.desde(nuevoEstado);
if (destino == null || !solicitud.estado.puedePasarA(destino)) { ... }
solicitud.estado = destino;
```

- **Por qué:** las reglas de transición tienen ahora un lugar propio y se leen como una tabla. Un error de
  tipeo en el estado ya no compila. `estaVencida`, el conteo del reporte y el límite de pendientes dejan de
  comparar con literales: usan el enum (`esFinal()`, `EnumMap`, `== PENDIENTE`).
- **Comportamiento conservado a propósito:** `desde` distingue mayúsculas; un estado desconocido sigue dando
  "transicion invalida"; y el `NullPointerException` con estado nulo (defecto D5, fijado por
  `d5_cambiarEstadoConEstadoNuloLanzaNullPointerException`) se mantiene con un `Objects.requireNonNull` marcado
  con un comentario, que se borra cuando se corrija D5. Igual que en la versión original, no se lanza si la solicitud
  ya está en un estado final.
- **Diferencia mínima, solo para archivos manipulados a mano:** igual que en R6, un estado inexistente en el CSV
  corta la carga en esa línea.
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## Pendientes y decisiones abiertas

| Smell | Estado | Nota |
|---|---|---|
| 8, 10 (parcial), 6, 2, 3 | Cerrados (R1 a R5) | `lst`, `est` y `resp` siguen públicos hasta encapsular |
| 4, 5, 13 (enums y encapsulamiento) | **Desbloqueados** por E1 | Siguiente paso: usar los getters en `Main`, pasar a enums y privatizar campos |
| 7 (duplicación), 9 (errores), 11 (DIP), 14 (acoplamiento temporal), 1 (God Class), 12 (ISP/LSP) | Pendientes | |
