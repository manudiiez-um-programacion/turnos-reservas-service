# Integración con el servicio de catálogo

Documento vivo (#17): clasifica qué operaciones propias necesitan una consulta vigente
al servicio de catálogo (`turnos-catalogo-sync`) y cómo se comportan si el catálogo no
responde. Se actualiza en cada milestone que agregue un endpoint nuevo que consuma el
catálogo — no es un checklist cerrado de un solo issue.

## Cómo se comporta la indisponibilidad

Toda llamada al catálogo pasa por `CatalogoClient` (`config/catalogo`). Ante un error de
infraestructura (5xx) o de red (timeout, conexión rechazada), reintenta con backoff
exponencial hasta `catalogo.api.max-retries` veces (#16). Agotados los reintentos, se
envuelve en `CatalogoNoDisponibleException` (#17), que `CatalogoExceptionHandler`
traduce a un `application/problem+json` con:

- `status`: `503 Service Unavailable`.
- `code`: `CATALOGO_NO_DISPONIBLE`.

Es un único código estable sin importar si el motivo de fondo fue un 503, un 500 o un
error de red: quien consume este servicio solo necesita saber que el catálogo no está
disponible ahora, no el detalle técnico. Un error funcional del catálogo (404
`PROFESSIONAL_NOT_FOUND`, 409 `PROFESSIONAL_DISABLED`) es un caso distinto: no es
indisponibilidad, se traduce aparte (`CatalogoClientErrorException`) y nunca se
reintenta.

## Clasificación de endpoints

| Endpoint propio | ¿Necesita el catálogo vigente? | Si el catálogo está caído |
|---|---|---|
| `POST /api/auth/register` | No | Sigue funcionando: el registro es dato propio. |
| `POST /api/auth/login` | No | Sigue funcionando (es el argumento con el que se decidió D1 en CLAUDE.md §7: los usuarios viven en este servicio). |
| `GET /api/health` | No | Sigue funcionando: es un endpoint de infraestructura, no de negocio. |

Pendiente de completar cuando existan (no implementados todavía):

| Endpoint futuro | Milestone | ¿Necesita el catálogo vigente? |
|---|---|---|
| Consultar disponibilidad de un profesional | M9 | Sí: combina las reglas semanales vigentes con las ocupaciones de la cátedra. |
| Crear un hold | M10 | Sí: valida al profesional contra el catálogo antes de pedir el hold a la cátedra. |
| Consultar reservas propias | M12 | No: son datos propios de este servicio. |
| Cancelar una reserva propia | M12 | No: opera sobre una reserva ya confirmada, con sus datos históricos. |

Cuando M9 y M10 agreguen su endpoint, deben:

1. Mover su fila de "pendiente" a la tabla de arriba.
2. Dejar que una indisponibilidad del catálogo les llegue como
   `CatalogoNoDisponibleException` y, si no tienen un motivo propio para manejarla
   distinto, dejar que `CatalogoExceptionHandler` la traduzca sin agregar su propio
   manejo.
3. Verificar con una prueba que, con el catálogo caído, la operación falla rápido (no se
   cuelga más allá de los timeouts y reintentos configurados) y no deja un proceso local
   en un estado irrecuperable (CLAUDE.md §6).

## Posible mejora, no implementada

El catálogo deja `/actuator/health` público
(`manudiiez-um-programacion/turnos-catalogo-sync#17`) y expone el estado y los errores
de sincronización en `turnos-catalogo-sync#28`. Hoy no se consulta: podría usarse para
distinguir "catálogo caído" de "catálogo arriba pero atrasado" antes de intentar una
operación, en vez de descubrirlo recién al fallar.
