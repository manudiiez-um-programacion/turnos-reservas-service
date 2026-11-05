-- Datos del ultimo AdditionalInformationRequested recibido para un proceso
-- (#29, §15.4 de la referencia).
--
-- request_required_field guarda el valor crudo, no una restriccion CHECK ni
-- un tipo enumerado: hoy la catedra solo envia PHONE_NUMBER, pero el campo
-- existe en el contrato para otros valores futuros, que hay que registrar y
-- no rechazar.
--
-- Las dos columnas son nulas mientras el proceso no llego a ese punto del
-- flujo, igual que request_event_id (V4__proceso_reserva.sql).
ALTER TABLE proceso_reserva
    ADD COLUMN request_required_field VARCHAR(50),
    ADD COLUMN request_message VARCHAR(500);

COMMENT ON COLUMN proceso_reserva.request_required_field IS
    'requiredField del ultimo AdditionalInformationRequested recibido (#29). Valor crudo: uno distinto de PHONE_NUMBER se registra, no se rechaza.';
COMMENT ON COLUMN proceso_reserva.request_message IS
    'message del ultimo AdditionalInformationRequested recibido (#29).';
