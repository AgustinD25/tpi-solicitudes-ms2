# TPI-Solicitudes

Trabajo Práctico Integrador — Metodología de Sistemas II (TUP).
Sistema de Gestión de Solicitudes Académicas con menú de consola.

El objetivo del TPI es mejorar la **calidad interna** del sistema manteniendo su comportamiento externo:
una versión inicial con deuda técnica (tag `estado-inicial`) y una versión refactorizada (tag `estado-refactorizado`).

## Requisitos
- Java 17 o superior
- Maven 3.8+

## Compilar y ejecutar
```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.tup.solicitudes.Main
```

O generando el jar:
```bash
mvn package
java -jar target/solicitudes-1.0-SNAPSHOT.jar
```

## Tests
```bash
mvn test
```

## Datos
Los datos son sintéticos. Las solicitudes se guardan en `solicitudes.csv` (en el directorio de ejecución).

## Documentación
- `docs/diseno-version-inicial.md`: diseño y smells plantados en la versión inicial.
- `docs/prompts.md`: prompts utilizados.
