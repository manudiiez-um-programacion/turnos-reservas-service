-- Resultado final del proceso (#31, §15.6-§15.9 de la referencia).
--
-- reason guarda el motivo mas reciente de un AdditionalInformationRejected,
-- un AppointmentProcessExpired o un AppointmentProcessInvalid: se sobreescribe
-- en cada reintento de telefono rechazado, asi que es siempre el ultimo
-- motivo y no un historial. reason_message solo lo trae el rechazo
-- (AdditionalInformationRejected); los otros dos eventos no tienen message.
--
-- intentos_telefono cuenta los POST .../phone aceptados para este proceso
-- (#30), incluidos los reintentos tras un rechazo: es el contador que limita
-- los reintentos de telefono (§18.4 de la referencia, "los reintentos deben
-- ser limitados y observables"). NOT NULL DEFAULT 0 porque todo proceso
-- existente antes de esta migracion todavia no tuvo ningun intento.
ALTER TABLE proceso_reserva
    ADD COLUMN reason VARCHAR(100),
    ADD COLUMN reason_message VARCHAR(500),
    ADD COLUMN intentos_telefono SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN proceso_reserva.reason IS
    'Motivo mas reciente de un resultado no exitoso (#31): rechazo de telefono, vencimiento o proceso invalido.';
COMMENT ON COLUMN proceso_reserva.reason_message IS
    'Mensaje legible del ultimo AdditionalInformationRejected (#31). Los otros eventos de resultado no lo traen.';
COMMENT ON COLUMN proceso_reserva.intentos_telefono IS
    'Cantidad de telefonos enviados para este proceso (#31): limita los reintentos tras un rechazo.';
