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

## R8. Encapsulate Field + Move Method: entidades con campos privados y comportamiento propio

- **Smell que cierra:** 13 (Data Class / encapsulamiento roto). Principio: encapsulamiento, SRP.
- **Archivos:** `Solicitud.java`, `Estudiante.java`, `Responsable.java`, `ReporteSimple.java`, `Main.java`,
  `SistemaSolicitudes.java`.
- **Qué se hizo:** todos los campos de las tres entidades son privados. Los datos que no cambian son `final` y se
  fijan por constructor (antes `Solicitud` podía existir sin estado ni fecha). Las reglas que estaban en la clase
  dios y solo usaban datos de la solicitud pasaron a `Solicitud`.

| Antes (en `SistemaSolicitudes`) | Después (en `Solicitud`) |
|---|---|
| `solicitud.estado = destino` sin validar | `pasarA(destino)`: lanza `IllegalStateException` si la transición no es válida |
| `responsable.area.equals(areaRequerida)` | `puedeSerAtendidaPor(responsable)` |
| `solicitud.responsable = responsable` | `asignarResponsable(responsable)` |
| `new Solicitud()` + 9 asignaciones de campos | `new Solicitud(id, estudiante, tipo, descripcion, prioridad, urgente, fecha)` |

Antes:
```java
public class Solicitud {
    public int id;
    public Estudiante estudiante;
    public EstadoSolicitud estado;
    ...
}
Solicitud nueva = new Solicitud();
nueva.id = ultimoId;
nueva.estudiante = estudiante;
... (7 asignaciones más)
```

Después:
```java
public class Solicitud {
    private final int id;
    private final Estudiante estudiante;
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    ...
    public void pasarA(EstadoSolicitud destino) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException("transicion invalida de " + estado + " a " + destino);
        }
        estado = destino;
    }
}
return new Solicitud(ultimoId, estudiante, TipoSolicitud.desde(datos.tipo()), datos.descripcion(),
        prioridadFinal, datos.urgente(), datos.fecha());
```

- **Por qué:** ninguna clase puede dejar una solicitud en un estado inválido salteándose las reglas (antes bastaba
  `s.estado = "XX"`). `SistemaSolicitudes` valida con `puedePasarA` y mensajes como siempre; `pasarA` es la red de
  seguridad de la entidad.
- **Detalle:** `cargar()` reconstruye el estado y el responsable guardados con un método de paquete,
  `restaurar(...)`, para no abrir setters públicos. El constructor de `Solicitud` tiene 7 parámetros; no es la
  lista larga que se corrigió en R5 (esos eran datos de entrada del usuario sin nombre), pero se podrá reducir cuando
  el repositorio se encargue de reconstruir entidades.
- **Pendiente:** `Estudiante.emailValido` y `Responsable.emailValido` siguen duplicados (smell 7).
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R9. Encapsulate Field + Rename: listas privadas y nombres definitivos

- **Smells que cierra:** 13 (parte que faltaba: las listas públicas de la clase dios) y 10 (nombres, parte que
  R2 había dejado). Principio: encapsulamiento.
- **Archivos:** `SistemaSolicitudes.java`, `Main.java`.
- **Qué se hizo:** `lst`, `est` y `resp` pasaron a `solicitudes`, `estudiantes` y `responsables`, privadas y `final`
  (también `ultimoId` y `rutaArchivo`). Desde afuera se leen con `getSolicitudes()`, `getEstudiantes()` y
  `getResponsables()`, que devuelven copias no modificables. `Main` dejó de leer los campos y usa los getters.

Antes:
```java
public List<Solicitud> lst = new ArrayList<>();
public List<Estudiante> est = new ArrayList<>();
public List<Responsable> resp = new ArrayList<>();
int ultimoId = 0;
...
for (Responsable r : sis.resp) { ... }        // Main
sis.imprimirLista(sis.lst);                    // Main
```

Después:
```java
private final List<Solicitud> solicitudes = new ArrayList<>();
private final List<Estudiante> estudiantes = new ArrayList<>();
private final List<Responsable> responsables = new ArrayList<>();
private int ultimoId = 0;
...
for (Responsable r : sis.getResponsables()) { ... }
sis.imprimirLista(sis.getSolicitudes());
```

- **Por qué:** esto era posible gracias a la excepción E1 (los tests ya leían por getters). Antes, cualquier clase
  podía hacer `sis.lst.clear()` o agregar una solicitud salteándose todas las reglas.
- **Verificación:** `mvn test` → 49 tests, 0 fallas; los archivos de test no cambiaron desde el commit de la
  excepción E1.

---

## R10. Extract Class: `ReporteService` y `PresentadorSolicitud`

- **Smell que cierra (parcialmente):** 1 (God Class). Principio: SRP.
- **Archivos:** `ReporteService.java` y `PresentadorSolicitud.java` (nuevos), `SistemaSolicitudes.java`.
- **Qué se hizo:** la lógica del reporte (conteos por estado y tipo y listado de vencidas) y el formato de una
  solicitud en una línea salieron de la clase dios. Ambas clases solo **devuelven texto**: la impresión por consola
  queda en la fachada.

Antes (`SistemaSolicitudes`, ~50 líneas armando el reporte e imprimiéndolo):
```java
public String reporte(LocalDate hoy) {
    Map<EstadoSolicitud, Integer> porEstado = new EnumMap<>(EstadoSolicitud.class);
    ...
    StringBuilder sb = new StringBuilder();
    sb.append("=== REPORTE ===\n");
    ...
    String texto = sb.toString();
    System.out.print(texto);
    return texto;
}
public void imprimir(Solicitud solicitud) {
    String nombreResponsable = ...;
    System.out.println("#" + solicitud.getId() + " | " + ... );
}
```

Después:
```java
public String reporte(LocalDate hoy) {
    String texto = reporteService.generar(solicitudes, hoy);
    System.out.print(texto);
    return texto;
}
public void imprimir(Solicitud solicitud) {
    System.out.println(presentador.formatear(solicitud));
}
```

- **Por qué:** `ReporteService.generar(List<Solicitud>, LocalDate)` es una función pura (recibe datos y fecha, devuelve
  texto): se puede probar y reutilizar desde otra interfaz sin que aparezca salida por consola. Cambiar el formato
  del reporte ya no obliga a abrir la clase que registra solicitudes.
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R11. Extract Interface + Extract Class: repositorios (memoria y CSV)

- **Smell que cierra:** 11 (acoplamiento a lo concreto). También avanza el smell 1. Principios: DIP e ISP.
- **Archivos nuevos:** `SolicitudRepository`, `EstudianteRepository` (interfaces), `RepositorioEnMemoria`,
  `RepositorioCsv`. **Modificado:** `SistemaSolicitudes.java`.
- **Qué se hizo:** la lógica de negocio dejó de crear `FileWriter`/`FileReader`. Ahora depende de dos interfaces
  chicas; el CSV es una implementación (`RepositorioCsv` extiende a la de memoria). El sistema tiene dos
  constructores: `SistemaSolicitudes(String ruta)` (como siempre: `null` = solo memoria) y
  `SistemaSolicitudes(SolicitudRepository, EstudianteRepository)` para elegir cualquier almacenamiento.

Antes (`SistemaSolicitudes`, dependencia directa de `java.io`):
```java
import java.io.FileWriter;
...
void guardar() {
    FileWriter escritor = new FileWriter(rutaArchivo);
    for (Solicitud solicitud : solicitudes) { escritor.write(...); }
    ...
}
solicitudes.add(nueva);
guardar();
```

Después:
```java
public interface SolicitudRepository {
    List<Solicitud> listarTodas();
    Optional<Solicitud> buscarPorId(int id);
    int generarId();
    void guardar(Solicitud nueva);
    void actualizar(Solicitud existente);
    void cargar();
}

solicitudRepository.guardar(nueva);                 // en la fachada: no sabe si es memoria o archivo
public SistemaSolicitudes(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository)
```

- **Por qué dos interfaces:** quien solo trabaja con estudiantes no depende de los métodos de solicitudes (ISP).
  `RepositorioEnMemoria` implementa las dos.
- **Qué quedó fuera de la clase dios:** el formato del CSV, la lectura, la escritura y la numeración de ids.
- **Se conserva a propósito el manejo de errores del archivo:** `RepositorioCsv` mantiene tal cual el catch vacío, el
  archivo que no se cierra si falla y el truncado (defectos D3, D4, D6 y D7), porque los tests de caracterización
  los fijan. Se corrigen después del refactoring, uno por commit. Está escrito en el comentario de la clase.
- **Smell 14 sigue abierto:** `RepositorioCsv` resuelve cada responsable con una función que recibe al construirse y
  la consulta **al cargar**. Si se carga antes de registrar responsables, siguen quedando en `null` (test
  `smell14_cargarAntesDeRegistrarResponsablesDejaLasAsignacionesEnNull`). Se corrige con el defecto correspondiente.
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R12. Replace Error Code with Exception + Extract Class: servicios y fachada (Adapter)

- **Smells que cierra:** 9 (manejo de errores pobre, en la lógica de negocio) y 1 (God Class). Principios: SRP,
  DIP, Clean Code (errores con excepciones, sin salida por consola en la lógica).
- **Archivos nuevos:** `SolicitudService`, `ResponsableService` y 10 clases de excepción (`DominioException` y sus
  hijas: `DatosInvalidosException`, `TipoInvalidoException`, `PrioridadInvalidaException`,
  `LimitePendientesException`, `TransicionInvalidaException`, `SolicitudNoEncontradaException`,
  `ResponsableNoEncontradoException`, `AreaIncorrectaException`, `ResponsableInvalidoException`).
  **Modificado:** `SistemaSolicitudes.java`, que pasa de clase dios a fachada.
- **Qué se hizo:** las reglas de negocio (validar, límite de pendientes, prioridad, transiciones, asignación por
  área) viven ahora en `SolicitudService` y avisan los errores con **excepciones de dominio**; no devuelven
  códigos ni imprimen. `SistemaSolicitudes` conserva el contrato público de la versión original: atrapa las
  excepciones, imprime el mismo mensaje y devuelve el mismo código (-1/-2/-3/-4, `false`, `null`). Es el patrón
  Adapter entre el diseño nuevo y el contrato viejo.

Antes (`SistemaSolicitudes`, regla + mensaje + código mezclados):
```java
if (pendientes >= MAX_PENDIENTES_POR_ESTUDIANTE) {
    System.out.println("Error: el estudiante ya tiene " + MAX_PENDIENTES_POR_ESTUDIANTE
            + " solicitudes pendientes");
    return COD_LIMITE_PENDIENTES;
}
```

Después:
```java
// SolicitudService: la regla informa con una excepcion, sin imprimir
if (contarPendientes(datos.legajo()) >= MAX_PENDIENTES_POR_ESTUDIANTE) {
    throw new LimitePendientesException(MAX_PENDIENTES_POR_ESTUDIANTE);
}

// SistemaSolicitudes (fachada): traduce al contrato de siempre
try {
    Solicitud nueva = solicitudService.registrar(datos);
    System.out.println("Solicitud #" + nueva.getId() + " registrada (" + ... + ")");
    return nueva.getId();
} catch (DominioException error) {
    System.out.println("Error: " + error.getMessage());
    return codigoDeError(error);          // LimitePendientesException -> -2, TipoInvalidoException -> -3, ...
}
```

- **Por qué la fachada:** los 49 tests de caracterización pasan **sin modificarse** porque el contrato público no
  cambió. El diseño nuevo se puede usar desde otra interfaz (web, API) recibiendo excepciones, sin tocar la consola.
- **Qué mejora además:** `SolicitudService` se prueba sin archivos ni consola; el orden de las validaciones sigue
  siendo el mismo (fija cuál error se informa); `filtrar(Predicate)` reemplaza a las tres búsquedas casi iguales
  (smell 7, parte de los filtros).
- **Se conserva a propósito:** el `NullPointerException` con tipo o estado nulo (D2 y D5), y los errores de archivo
  tragados en `RepositorioCsv` (D3, D4, D6 y D7), porque los tests los fijan. El smell 9 queda cerrado en la parte de
  reglas de negocio; la parte de entrada/salida se cierra con la corrección de esos defectos.
- **Smell 14 sigue abierto:** `cargar()` aún depende de registrar los responsables antes (ver R11).
- **Verificación:** `mvn test` → 49 tests, 0 fallas.

---

## R13. Extract Class: `ValidadorEmail` (validación única)

- **Smell que cierra:** 7 (código duplicado). Principio: DRY.
- **Archivos:** `ValidadorEmail.java` (nuevo), `Estudiante.java`, `Responsable.java`, `SolicitudService.java`,
  `ResponsableService.java`.
- **Qué se hizo:** `emailValido()` estaba copiado idéntico (17 líneas) en `Estudiante` y `Responsable`. Pasó a una sola
  clase y las entidades quedaron como datos puros. Los servicios validan **antes** de crear el objeto, así que
  ya no se arma un `Estudiante` o `Responsable` para después descartarlo si el email es inválido.

Antes:
```java
// Estudiante.java y Responsable.java (copias idénticas)
public boolean emailValido() {
    if (email == null) { return false; }
    int arroba = email.indexOf('@');
    ...
}
Estudiante estudianteIngresado = new Estudiante(legajo, nombre, email);
if (!estudianteIngresado.emailValido()) { ... }
```

Después:
```java
public final class ValidadorEmail {
    public static boolean esValido(String email) { ... }      // una sola copia
}
if (!ValidadorEmail.esValido(datos.email())) {
    throw new DatosInvalidosException("email invalido");
}
```

- **Por qué:** una regla cambiada en un solo lugar no puede dejar a estudiantes y responsables validándose distinto.
  La duplicación de las tres búsquedas por filtro ya se había resuelto en R12 con `filtrar(Predicate)`.
- **Verificación:** `mvn test` → 49 tests, 0 fallas (incluye `emailInvalidoEnVariasFormas`, que prueba 8 emails).

---

## Pendientes y decisiones abiertas

| Smell | Estado | Nota |
|---|---|---|
| 8, 10, 6, 2, 3 | Cerrados (R1 a R5, R9) | |
| 4, 5, 13 | Cerrados (R6 a R9) | Posibles gracias a la excepción E1 |
| 7, 11, 1 | Cerrados (R10 a R13) | |
| 9 (errores) | Cerrado en la lógica de negocio (R12) | Los errores de archivo siguen tragados a propósito (D3, D4, D6, D7) |
| 14 (acoplamiento temporal) | **Abierto** | Se cierra con la corrección del defecto: `cargar()` antes de registrar responsables |
| 12 (ISP/LSP en la exportación) | Pendiente | Requiere reescribir `ExportacionCaracterizacionTest` a propósito (ver `CLAUDE.md`) |
