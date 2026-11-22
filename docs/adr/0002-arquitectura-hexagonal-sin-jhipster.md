# ADR-0002: Spring Boot a mano con arquitectura hexagonal, sin JHipster

- **Estado**: Aceptado
- **Issue**: #45

## Contexto

El enunciado (§3.2) pide que el modelo de usuario de este servicio sea
**compatible con el contrato que genera JHipster** (login, password,
firstName, lastName, email, imageUrl, langKey; claims del JWT al estilo
`TokenProvider` de JHipster), porque la app KMP y el servicio de catálogo
fueron pensados contra ese contrato. Eso no obliga a usar la herramienta:
obliga a que el resultado final sea compatible con lo que ella generaría.

El proyecto necesita, además, una arquitectura por slices verticales
(puertos y adaptadores) para que el dominio de turnos y reservas —el
corazón de este repo— quede desacoplado de Spring y sea fácil de probar sin
levantar el framework completo.

## Opciones consideradas

1. **JHipster**, generando el proyecto completo (entidades, CRUD,
   autenticación, frontend) y después adaptando lo que no aplica (este
   servicio no tiene frontend propio, y su dominio — disponibilidad, holds,
   procesos de reserva — no es un CRUD generable).
2. **Spring Boot a mano**, siguiendo manualmente el contrato de usuario y de
   JWT que JHipster habría generado, con arquitectura hexagonal por slices
   verticales (la skill `hexagonal-slicing`) para el resto del dominio.

## Decisión

**Spring Boot a mano**, con arquitectura hexagonal por slices verticales.
El modelo de usuario (tabla `usuario`, claims del JWT) replica a mano el
contrato de JHipster donde el enunciado lo exige (§3.2), sin generar el resto
del proyecto con esa herramienta.

Motivo principal: el dominio propio de este servicio (disponibilidad que
combina catálogo + ocupaciones, holds, la máquina de estados del proceso de
reserva, idempotencia por Kafka) no es un CRUD que JHipster pueda generar —
es lógica de negocio específica del proyecto integrador. Generar el
andamiaje de JHipster y después quitarle las partes que no aplican habría
costado más que escribir directamente la arquitectura que el dominio
necesita, sin ganar nada a cambio salvo el scaffolding inicial de
registro/login, que tampoco es grande.

## Consecuencias

- Compatibilidad explícita y verificada con el contrato de JHipster donde
  hace falta (claims del JWT, forma del usuario), documentada en
  `docs/contrato-y-modelo-de-datos.md` §3 — no heredada automáticamente de
  generar el proyecto con la herramienta, así que cualquier cambio en ese
  contrato tiene que revisarse a mano contra el ADR 0002 de
  `turnos-catalogo-sync#59` (ver ADR-0003).
- Cada slice (`health`, `auth`, `disponibilidad`, `reservas`) sigue la regla
  de dependencias hacia adentro (`domain` sin framework, `application` solo
  de `domain`, `infrastructure` de ambos) de la skill `hexagonal-slicing`,
  en vez de la organización por capas horizontales que genera JHipster
  (`web`, `service`, `repository` transversales a todas las entidades).
- Sin generador que mantenga actualizado el andamiaje: cada pieza de
  seguridad, persistencia o mensajería se escribió y se revisó a mano, con
  el costo de tiempo que eso implica a cambio de que cada decisión quede
  justificada en el propio código y en estos documentos, en vez de heredada
  de un generador.
