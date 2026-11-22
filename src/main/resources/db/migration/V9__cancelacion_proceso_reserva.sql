-- Columnas de soporte para "cancelar una reserva confirmada propia" (#34):
-- cuando se cancela, el estado local pasa a CANCELADA y se registra cuando
-- paso (cancelled_at, informado por la catedra) y el motivo opcional que
-- mando el usuario (cancel_reason, hasta 500 caracteres, §12 de la referencia).
ALTER TABLE proceso_reserva
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN cancel_reason VARCHAR(500);

COMMENT ON COLUMN proceso_reserva.cancelled_at IS
    'Cuando se cancelo la reserva, segun lo informa la catedra (#34).';
COMMENT ON COLUMN proceso_reserva.cancel_reason IS
    'Motivo opcional que mando el usuario al cancelar (#34, hasta 500 caracteres).';
