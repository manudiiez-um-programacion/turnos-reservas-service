-- Deduplicacion de eventos de Kafka por eventId (#20, §15.1 y §18.1 de la
-- referencia: la entrega es al menos una vez y eventId es la clave de
-- idempotencia funcional).
--
-- event_id es la PRIMARY KEY a proposito y no un VARCHAR con una restriccion
-- UNIQUE separada: ante procesamiento concurrente del mismo evento, el
-- segundo INSERT tiene que fallar por la restriccion de la propia clave, no
-- por una consulta previa que otra transaccion concurrente todavia no ve.
--
-- Sin created_at/updated_at ni trigger de set_updated_at: esta tabla es un
-- registro inmutable, nunca se actualiza una fila ya insertada.
--
-- Politica de retencion: todavia no definida, queda documentada en #44 junto
-- con el resto de la estrategia de idempotencia y recuperacion.
CREATE TABLE evento_procesado (
    event_id VARCHAR(100) PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    reservation_process_id VARCHAR(100) NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE evento_procesado IS
    'Deduplicacion por eventId (#20): un eventId ya presente significa que el evento ya se proceso y no hay que repetir su efecto.';
