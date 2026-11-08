# Cobertura de pruebas (JaCoCo)

Generada con `mvn test` (JaCoCo 0.8.12, configurado en `pom.xml`). El detalle por clase está en
[`jacoco-cobertura.csv`](jacoco-cobertura.csv); el reporte HTML completo queda en `target/site/jacoco/index.html`
después de correr los tests.

- **Tests:** 101 (51 de caracterización y de corrección de defectos, 50 unitarios agregados en el code review).
- **Cobertura de líneas excluyendo `Main`:** 535 de 554 = **96,6 %** (umbral propuesto por el revisor: 80 %).
- **Cobertura de ramas excluyendo `Main`:** 183 de 192 = 95,3 %.
- **`Main`** (la interfaz de consola) no tiene tests automáticos; se verificó a mano. Está excluido del umbral.
- Las clases con menos cobertura son `RepositorioCsv` (116/130 líneas: sobre todo ramas de errores de disco difíciles de
  provocar), `ReporteSimple` (37/40) y `Solicitud` (40/41).
