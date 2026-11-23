# ADR-0006: Estrategia de deduplicación, timeouts, backoff y reintentos

- **Estado**: Aceptado
- **Issue**: #18, #20, #30, #31, #36, #38, #45

## Contexto

El enunciado deja a cargo del alumno la estrategia de deduplicación,
timeouts, backoff y límites de reintentos (§10), sujeta a las reglas de
robustez del §18 de la referencia: entrega al menos una vez (hay
duplicados), un timeout no prueba que la operación falló, y los reintentos
tienen que ser limitados y observables (nunca un ciclo infinito).

Hay tres situaciones distintas que esta estrategia tiene que cubrir, y no
admiten la misma solución:

1. un evento de Kafka que llega duplicado;
2. una operación con efectos (crear/confirmar un hold) que da timeout, sin
   saber si la cátedra la aplicó o no;
3. un mensaje de Kafka que ni siquiera se puede leer (JSON inválido, falta
   un campo común obligatorio).

## Opciones consideradas

### Deduplicación (situación 1)

1. Deduplicar en memoria (un `Set` de `eventId` vistos). Descartado: se
   pierde en cada reinicio, que es exactamente cuando más probable es que
   la cátedra reenvíe algo que este servicio ya había procesado antes de
   caerse.
2. Deduplicar con una consulta previa ("¿existe este `eventId`?") seguida de
   un `INSERT` si no existe. Descartado: dos hilos procesando el mismo
   evento al mismo tiempo pueden pasar los dos la consulta antes de que
   cualquiera inserte — una carrera de *check-then-act*.
3. Deduplicar con `event_id` como **clave primaria** de una tabla, dejando
   que el motor de base rechace el segundo `INSERT` concurrente.

### Operación con efectos ante timeout (situación 2)

1. Reintentar la misma llamada sin más. Descartado explícitamente por
   §18.4: si la primera llamada sí llegó a aplicarse y solo se perdió la
   respuesta, reintentar duplicaría el efecto (dos holds, una reserva
   confirmada dos veces).
2. No reintentar nunca, propagar el error al usuario. Descartado: desperdicia
   información que la propia cátedra expone (`GET /api/appointments`) para
   saber si la operación ya se aplicó.
3. **Reconciliar antes de reintentar**: buscar el resultado por un
   identificador persistido (`reservationProcessId`) antes de decidir si
   hace falta repetir la llamada, y si hace falta, un reintento acotado.

### Mensaje de Kafka no procesable (situación 3)

1. Descartarlo sin reintentar. Descartado: un fallo transitorio (por
   ejemplo, una excepción inesperada al deserializar bajo carga) merece al
   menos un reintento antes de darse por perdido.
2. Reintentar indefinidamente hasta que se pueda procesar. Descartado
   explícitamente por §18.4: bloquearía la partición para siempre si el
   mensaje nunca se puede leer (por ejemplo, un JSON realmente inválido).
3. **Reintentos acotados con espera fija**, y después registrar el fallo y
   avanzar el *offset*.

## Decisión

- **Deduplicación**: `evento_procesado.event_id` como `PRIMARY KEY` (no una
  columna con `UNIQUE` aparte). El registro ocurre en la **misma
  transacción** que el efecto del evento: si el efecto falla, la marca de
  "procesado" se revierte con él. Detalle completo en
  `docs/idempotencia-recuperacion-seguridad.md` §1, verificado con
  Testcontainers (`ReservaPostgresKafkaIT#dosInsercionesConcurrentesDelMismoEventIdSoloUnaTieneExito`).
- **Reconciliar antes de reintentar una operación con efectos** (#36): se
  busca el `reservationProcessId` contra `GET /api/appointments`; si está,
  se alinea el estado local sin repetir la llamada; si no, se permite
  **exactamente un** reintento acotado. Igual criterio al recuperar
  procesos pendientes al arrancar (#37). Detalle en
  `docs/idempotencia-recuperacion-seguridad.md` §3.1–§3.2.
- **Reintentos de un `GET` idempotente** (ocupaciones, agenda vigente) ante
  5xx o error de red: 3 intentos con backoff exponencial desde 0.2 s
  (`CATEDRA_API_MAX_RETRIES`/`CATALOGO_API_MAX_RETRIES` +
  `*_RETRY_BACKOFF`, configurables porque son ajuste operativo con default,
  no un dato que entregue la cátedra).
- **Reintentos de teléfono rechazado**: máximo 3 intentos
  (`MAX_INTENTOS_TELEFONO`, fijo en el código).
- **Reintentos del oyente de Kafka ante un mensaje no procesable**: 3
  intentos, 500 ms de espera fija (`FixedBackOff`, fijo en el código);
  agotados, se registra el fallo y se confirma el *offset* igual, para no
  bloquear la partición.
- **Cierre de vencidos**: tarea periódica (`TURNOS_VENCIMIENTO_FRECUENCIA`,
  default 30 s) con un margen de gracia (`TURNOS_VENCIMIENTO_MARGEN`,
  default 5 s) antes de considerar vencido un proceso, para no cerrarlo por
  una simple diferencia de reloj con la cátedra.

Los valores de Kafka y de teléfono están **fijos en el código** y no en
variables de entorno: son ajuste operativo propio del servicio, no datos
que la cátedra entregue o pueda cambiar. Los de REST hacia la cátedra y el
catálogo sí son configurables (con default), porque dependen de la latencia
real de una red que varía según el entorno de despliegue.

## Consecuencias

- Ninguna de las estrategias de reintento puede bloquearse indefinidamente:
  todas tienen un tope explícito y, agotado, dejan el proceso en un estado
  observable (una excepción hacia el usuario, un `offset` confirmado con el
  fallo registrado, o un proceso que sigue esperando el próximo evento o la
  próxima corrida de la tarea periódica).
- Reconciliar antes de reintentar agrega una llamada adicional (`GET
  /api/appointments`) en el camino de error, a cambio de no arriesgar una
  reserva duplicada — un costo aceptado porque el camino de error es, por
  definición, el menos frecuente.
- Los valores concretos (3 reintentos, 500 ms, 0.2 s de backoff base, 30 s
  de frecuencia de cierre) son puntos de partida razonables y no una
  medición exhaustiva bajo carga: si la latencia real contra la cátedra en
  producción resulta muy distinta de lo esperado en desarrollo, estos
  valores son el primer lugar para revisar.
