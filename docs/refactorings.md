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
