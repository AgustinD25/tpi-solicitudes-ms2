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
