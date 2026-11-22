-- Corrige un desajuste entre el tipo de columna de V7 y el tipo de dato del
-- lado de Java (#41, descubierto por la suite de Testcontainers: con una base
-- real y ddl-auto=validate, SMALLINT vs. el "integer" que Hibernate espera
-- para un campo "int" rompe el arranque con SchemaManagementException. El
-- perfil de pruebas de siempre desactivaba el acceso a los metadatos JDBC, asi
-- que nunca se habia podido detectar).
--
-- Se ensancha la columna en vez de angostar el campo de Java a "short": un
-- contador que ya es "int" en todo el dominio, la entidad y los mappers no
-- necesitaba la restriccion de 2 bytes, y angostarlo ahora solo para que
-- coincida con una eleccion de V7 que nunca estuvo documentada como deliberada
-- cambiaria mucho mas codigo (domain, entidad, mappers, tests) para el mismo
-- resultado observable. V7 no se edita (ya esta mergeada): esto es una
-- migracion nueva, como exige el checksum de Flyway.
ALTER TABLE proceso_reserva
    ALTER COLUMN intentos_telefono TYPE INTEGER;
