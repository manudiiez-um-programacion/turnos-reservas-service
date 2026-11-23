# ADR-0005: Máquina de estados del proceso de reserva

- **Estado**: Aceptado
- **Issue**: #26, #45

## Contexto

El enunciado deja explícitamente a cargo del alumno el diseño de la máquina
de estados local del proceso de reserva (§4.2 y §10). Esa máquina tiene que
correlacionar dos canales que llegan **sin orden garantizado entre sí**
(REST, para crear el hold y confirmar inicialmente; Kafka, para la
información adicional y el resultado final) y sobrevivir a reinicios del
servicio.

Los estados remotos que expone la cátedra en `GET /appointments` son pocos
y gruesos: `PHONE_PENDING`, `CONFIRMED`, `CANCELLED`, `FAILED`.

## Opciones consideradas

1. **Reusar literalmente los cuatro estados remotos de la cátedra** como
   estados locales. Más simple, pero pierde información que este servicio
   sí necesita distinguir: `FAILED` del lado de la cátedra puede significar
   "venció el plazo" o "el proceso quedó inválido", y son causas distintas
   que un usuario necesita poder diferenciar al consultar su proceso (y que
   #44 necesita para explicar la recuperación).
2. **Una máquina de estados más granular**, propia de este servicio, que
   además registre el paso intermedio "ya mandé el `confirm` pero todavía no
   sé qué contestó la cátedra" — necesario para poder reconciliar un timeout
   en vez de reintentar a ciegas (§18.4 de la referencia, ADR implícito en
   #36).

## Decisión

Una máquina de estados propia con **nueve estados**
(`EstadoProcesoReserva`): `HOLD_CREADO`, `CONFIRMACION_INICIAL_ENVIADA`,
`ESPERANDO_TELEFONO`, `TELEFONO_ENVIADO`, `TELEFONO_RECHAZADO`, y cuatro
finales: `CONFIRMADA`, `VENCIDA`, `INVALIDA`, `CANCELADA`. Diagrama completo
y el detalle de cada transición en
`docs/idempotencia-recuperacion-seguridad.md` §2.

Tres reglas de diseño, todas con una razón de robustez detrás:

- **`CONFIRMACION_INICIAL_ENVIADA` existe solo para poder reconciliar.** Se
  persiste *antes* de llamar a la cátedra, no después: si la llamada da
  timeout, ese estado por sí mismo es la señal de "hay una operación con
  efectos en vuelo", que es lo que #36 necesita para reconciliar por
  identificador en vez de reintentar a ciegas.
- **Los tres resultados finales (`CONFIRMADA`, `VENCIDA`, `INVALIDA`) son
  alcanzables desde cualquier estado no final**, y no solo desde el paso
  inmediatamente anterior esperado. Es la decisión central de esta máquina:
  como REST y Kafka no tienen orden garantizado, un resultado final puede
  llegar en cualquier momento del flujo, y la máquina tiene que aceptarlo
  sin importar en qué paso intermedio estaba el proceso.
- **`CONFIRMADA` es final para el proceso de reserva pero admite una
  transición de negocio distinta, `CANCELADA`** (#34): cancelar una reserva
  ya confirmada es una operación *nueva*, no una reapertura del proceso que
  la terminó.

La tabla de transiciones válidas (`EstadoProcesoReserva.TRANSICIONES_VALIDAS`)
es explícita y cerrada: una transición no listada se rechaza con una
excepción, nunca con un *no-op* silencioso.

## Consecuencias

- Un proceso puede consultarse en un estado más específico que lo que la
  cátedra expone, lo que mejora el mensaje que recibe el usuario final (por
  qué no se pudo reservar, no solo que falló).
- La máquina es más grande que el mínimo necesario para el camino feliz,
  a cambio de que cada caso de recuperación (#36, #37, #38) tenga un estado
  propio sobre el que apoyarse, en vez de inferir la situación por
  heurísticas sobre timestamps.
- Agregar un estado nuevo obliga a decidir explícitamente sus transiciones
  de entrada y salida en el mapa: no hay riesgo de que una transición nueva
  quede permitida "por accidente" por no estar excluida.
