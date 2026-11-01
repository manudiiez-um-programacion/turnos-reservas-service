#!/bin/sh
# Crea la base logica y el usuario propios de este servicio.
#
# Lo ejecuta una unica vez el entrypoint de la imagen de PostgreSQL, cuando
# el volumen de datos esta vacio. Si el volumen ya tiene datos, este script
# NO se vuelve a correr: para re-ejecutarlo hay que borrar el volumen
# (`docker compose down -v`).
#
# Por que un .sh y no un .sql: el entrypoint no interpola variables de
# entorno dentro de los archivos .sql, y el nombre de la base, el usuario y
# la contrasena vienen de .env. Nada de eso se escribe en el repo.
#
# Por que no se usa POSTGRES_USER para el servicio: el usuario que crea
# POSTGRES_USER es superusuario de toda la instancia y tendria permisos
# sobre la base del servicio de catalogo. Queda como cuenta de
# administracion, y este script crea aparte un rol comun dueno solo de su
# propia base.
#
# Por que el prefijo "02": la solucion completa usa UNA instancia de
# PostgreSQL con dos bases logicas (ver el README). Cuando el compose de
# nivel superior monta los directorios de init de los dos servicios en el
# mismo contenedor, el entrypoint los ejecuta en orden alfabetico. El
# catalogo usa "01-crear-base-catalogo.sh" y este usa "02", asi que el
# orden es determinístico y los nombres no colisionan. Las variables van
# prefijadas (TURNOS_DB_*) por la misma razon: en una instancia compartida
# las de los dos servicios conviven en el mismo entorno.

set -eu

echo "[init] creando el rol '${TURNOS_DB_USERNAME}' y la base '${TURNOS_DB_NAME}'"

psql -v ON_ERROR_STOP=1 \
     --username "${POSTGRES_USER}" \
     --dbname "${POSTGRES_DB}" \
     --set=rol="${TURNOS_DB_USERNAME}" \
     --set=clave="${TURNOS_DB_PASSWORD}" \
     --set=base="${TURNOS_DB_NAME}" <<'SQL'
-- Rol de la aplicacion: puede conectarse y nada mas. Sin SUPERUSER no ve ni
-- toca objetos de otras bases; sin CREATEDB ni CREATEROLE no puede
-- escalarse creando una base o un rol nuevos.
CREATE ROLE :"rol" WITH
    LOGIN
    PASSWORD :'clave'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT
    NOREPLICATION
    NOBYPASSRLS;

-- Base logica exclusiva de este servicio, de la que el rol es dueno: puede
-- crear sus tablas ahi (las migraciones de #6) y en ningun otro lado.
CREATE DATABASE :"base" WITH OWNER = :"rol" ENCODING = 'UTF8';

-- Por defecto PostgreSQL deja que cualquier rol se conecte a cualquier base.
-- Se quita ese permiso general y se devuelve solo al dueno. Esto es lo que
-- hace que la instancia compartida cumpla el §3 del enunciado: el usuario
-- del servicio de catalogo no puede conectarse a esta base, y el de este
-- servicio no puede conectarse a la del catalogo.
REVOKE ALL ON DATABASE :"base" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE :"base" TO :"rol";
SQL

psql -v ON_ERROR_STOP=1 \
     --username "${POSTGRES_USER}" \
     --dbname "${TURNOS_DB_NAME}" \
     --set=rol="${TURNOS_DB_USERNAME}" <<'SQL'
-- El esquema public de una base recien creada es visible para todos los
-- roles. Se cierra y se pone a nombre del dueno de la base, que es quien
-- necesita crear tablas ahi.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO :"rol";
SQL

echo "[init] base '${TURNOS_DB_NAME}' lista"
