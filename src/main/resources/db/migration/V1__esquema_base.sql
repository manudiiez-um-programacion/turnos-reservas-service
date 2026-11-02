-- Esquema base del servicio de turnos y reservas.
--
-- Esta primera migracion no crea tablas a proposito: cada tabla llega con el
-- issue que la necesita, para que el historial muestre el esquema creciendo
-- junto a la funcionalidad y no un volcado inicial de todo. Lo que va aca es
-- lo TRANSVERSAL: lo que no pertenece a ningun slice y de lo que van a
-- depender las tablas que vengan despues.
--
-- Propiedad de los datos (§3 del enunciado): esta base es exclusiva de este
-- servicio. Las tablas que van a vivir aca son los usuarios finales (#9), los
-- procesos de reserva y su estado (#26), las reservas confirmadas y los
-- eventos de Kafka ya procesados para deduplicar por eventId (#20).
--
-- Lo que esta base NO guarda NUNCA: una copia del catalogo vigente
-- (categorias, profesionales, horarios semanales). Eso es del servicio de
-- catalogo y se le consulta antes de cada operacion. Dentro de una reserva si
-- puede quedar informacion descriptiva historica (el nombre del profesional al
-- momento de reservar), pero como dato congelado de esa reserva y nunca como
-- catalogo para buscar o para validar una reserva nueva.


-- Convencion de auditoria del servicio: las tablas propias llevan created_at y
-- updated_at en UTC. created_at lo pone el DEFAULT de la columna; updated_at lo
-- mantiene este trigger, para que ninguna escritura pueda olvidarse de
-- actualizarlo ni falsearlo desde la aplicacion.
--
-- Se define aca, en la migracion base, porque la van a compartir todas las
-- tablas: cada una la enchufa con su propio CREATE TRIGGER.
--
-- La funcion es IMMUTABLE respecto del esquema: no referencia ninguna tabla, asi
-- que crearla antes que ellas no genera dependencias. search_path queda fijado
-- para que no dependa del search_path de quien dispare el trigger.
CREATE FUNCTION set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = pg_catalog, public
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION set_updated_at() IS
    'Trigger BEFORE UPDATE: mantiene updated_at. Lo usan las tablas propias de este servicio.';


-- El comentario va en el esquema y no en la base: el nombre de la base es
-- configurable (DB_NAME) y COMMENT ON DATABASE lo obligaria a estar fijo aca.
COMMENT ON SCHEMA public IS
    'Datos propios del servicio de turnos y reservas. No contiene copia del catalogo vigente.';
