-- Telefono normalizado enviado a la catedra (#30, §15.5 de la referencia).
--
-- Nulo hasta que el proceso llega a ese punto del flujo, igual que
-- request_event_id (V4) y request_required_field/request_message (V5).
-- VARCHAR(20) alcanza de sobra para "+" mas los 15 digitos que permite
-- ^\+?[0-9]{7,15}$ despues de normalizar.
ALTER TABLE proceso_reserva
    ADD COLUMN telefono VARCHAR(20);

COMMENT ON COLUMN proceso_reserva.telefono IS
    'Telefono normalizado enviado a la catedra (#30). Dato personal: nunca se escribe en claro en los logs.';
