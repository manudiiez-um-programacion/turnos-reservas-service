# Decisiones de diseño (ADR)

Registro de las decisiones de arquitectura y diseño que le corresponden al
alumno (#45, enunciado §10): la cátedra define los contratos externos, pero
no la arquitectura interna de este servicio. Un ADR por decisión, con
contexto, opciones consideradas, decisión y consecuencias — el objetivo es
poder defenderlas, no solo mostrar el resultado.

| ADR | Decisión |
|---|---|
| [0001](0001-motor-de-base-de-datos.md) | PostgreSQL como motor de base de datos, por qué no una embebida. |
| [0002](0002-arquitectura-hexagonal-sin-jhipster.md) | Spring Boot a mano con arquitectura hexagonal por slices verticales, por qué no JHipster. |
| [0003](0003-identidad-y-autenticacion-entre-servicios.md) | Identidad y autenticación entre los dos backends propios: dónde viven los usuarios, el JWT técnico interno y la trazabilidad. Replica D1/D4/D5 del ADR 0002 de `turnos-catalogo-sync`. |
| [0004](0004-hasheo-de-contrasenas-argon2id.md) | Hasheo de contraseñas con Argon2id: parámetros y por qué no BCrypt. |
| [0005](0005-maquina-de-estados-del-proceso-de-reserva.md) | Máquina de estados local del proceso de reserva. |
| [0006](0006-deduplicacion-timeouts-y-reintentos.md) | Estrategia de deduplicación, timeouts, backoff y límites de reintentos. |
| [0007](0007-datos-historicos-dentro-de-una-reserva.md) | Qué datos descriptivos históricos se guardan en una reserva y por qué no violan la prohibición de replicar el catálogo. |

## Decisiones compartidas con el otro backend

ADR-0003 replica una decisión que se discutió y cerró en el **ADR 0002** del
repo `turnos-catalogo-sync`
(`manudiiez-um-programacion/turnos-catalogo-sync#59`): la identidad y
autenticación entre los dos backends propios del proyecto. Si esa decisión
cambia, el ADR de **los dos repos** se actualiza junto: una versión distinta
de la misma decisión en cada lado es peor que no documentarla.

## Un ADR superado, no reescrito

Si una de estas decisiones cambia más adelante, el ADR anterior se deja
como **superado** (con un enlace al que lo reemplaza) en vez de editarse o
borrarse: el historial de cómo cambió el razonamiento es parte de lo que la
cátedra pide poder ver (§11 del enunciado).
