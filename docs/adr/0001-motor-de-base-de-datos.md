# ADR-0001: PostgreSQL como motor de base de datos

- **Estado**: Aceptado
- **Issue**: #5, #45

## Contexto

Este servicio necesita persistir usuarios finales, procesos de reserva y
eventos de Kafka procesados (§3 del enunciado). El enunciado exige
explícitamente un **gestor servidor**: rechaza H2, SQLite y cualquier base
embebida como almacenamiento principal. La plantilla de la skill
`hexagonal-slicing` que se usó como punto de partida propone H2 para
iterar rápido en el slice de ejemplo.

El proyecto integrador son dos backends propios (este y
`turnos-catalogo-sync`) que conviven en la misma máquina de desarrollo y en
el mismo entorno de despliegue.

## Opciones consideradas

1. **H2 embebida** (la que trae la plantilla de la skill). Descartada de
   entrada: el enunciado la prohíbe como almacenamiento principal, sin
   excepción para desarrollo o para pruebas.
2. **Un motor servidor distinto por backend** (por ejemplo, PostgreSQL para
   uno y MySQL para el otro). Válido en principio, pero obliga a mantener
   dos motores corriendo para un proyecto de dos servicios, sin ningún
   beneficio a cambio: no hay un requisito que empuje a motores distintos.
3. **PostgreSQL**, el mismo motor para los dos backends.

## Decisión

**PostgreSQL**, coordinado con el servicio de catálogo para que los dos usen
el mismo motor. No se usa H2 **ni para los tests**: el `pom.xml` de este
repo reemplaza el driver que trae la plantilla de `hexagonal-slicing` por el
de PostgreSQL, y las pruebas que necesitan una base real la levantan con
Testcontainers (#41) en vez de una embebida en memoria.

Consecuencia directa de compartir el motor: **una instancia de PostgreSQL
con dos bases lógicas** (`catalogo` y `turnos_reservas`), decisión separada
y documentada en `docs/contrato-y-modelo-de-datos.md` §5 y en el README
("Base de datos: una instancia, dos bases lógicas").

## Consecuencias

- Un solo motor que aprender y operar para los dos repos del proyecto
  propio, en vez de dos.
- Las migraciones (Flyway, ver también la elección de herramienta en
  `CLAUDE.md` §9) corren contra un dialecto real desde el principio: no hay
  sorpresas de compatibilidad al pasar de una base de pruebas a la real.
- Desarrollar o correr pruebas necesita Docker (para levantar PostgreSQL),
  a cambio de no arrastrar divergencias de comportamiento entre una base
  embebida y la de producción — el propio hallazgo de #41 (un desajuste
  `SMALLINT`/`int` entre una migración y una entidad JPA que solo una base
  real detectó) es evidencia de ese riesgo evitado.
