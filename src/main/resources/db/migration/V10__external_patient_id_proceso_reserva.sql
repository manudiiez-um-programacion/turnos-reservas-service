-- externalPatientId del proceso (#37): se necesita para reconciliar contra
-- la catedra (#36, GET /api/appointments?externalPatientId=...) sin depender
-- de resolver de nuevo al usuario autenticado, que no existe como sesion
-- HTTP cuando se recuperan procesos pendientes al arrancar el servicio.
-- No es una columna nueva de catalogo: es el mismo dato que ya se le manda a
-- la catedra en cada hold (#27), ahora tambien persistido en este proceso.
ALTER TABLE proceso_reserva
    ADD COLUMN external_patient_id VARCHAR(100) NOT NULL;

COMMENT ON COLUMN proceso_reserva.external_patient_id IS
    'externalPatientId que la catedra usa para este proceso (#27); necesario para reconciliar (#36) sin una sesion HTTP activa (#37).';
