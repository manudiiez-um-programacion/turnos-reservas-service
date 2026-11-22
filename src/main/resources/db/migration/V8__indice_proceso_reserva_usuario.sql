-- Indice de soporte para "consultar las reservas propias del usuario" (#33):
-- el filtro por usuario_id es obligatorio en toda consulta de ese endpoint
-- (nunca se expone sin filtrar), y se combina seguido con el rango de fecha.
-- Sin este indice esa consulta haria un seq scan completo de proceso_reserva
-- a medida que crece la tabla.
CREATE INDEX idx_proceso_reserva_usuario_id_fecha
    ON proceso_reserva (usuario_id, fecha);
