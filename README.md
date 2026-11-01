<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# turnos-reservas-service

Servicio de turnos y reservas del proyecto integrador de Programación 2
(Universidad de Mendoza).

## Levantar el entorno local

Requiere Docker. No hace falta tener Java ni Maven instalados: la imagen compila
el servicio adentro.

```bash
cp .env.example .env     # y completar DB_PASSWORD y POSTGRES_SUPERUSER_PASSWORD
docker compose up --build
curl http://localhost:8081/api/health
# {"nombre":"turnos-reservas-service","version":"0.0.1-SNAPSHOT","estado":"UP",...}
```

Qué levanta: PostgreSQL 18 y este servicio. El servicio espera a que la base pase
su healthcheck antes de arrancar, así que un solo comando alcanza y no hay que
reintentar.

| URL | Qué es |
|---|---|
| `http://localhost:8081/api/health` | Estado del servicio |
| `http://localhost:8081/swagger-ui.html` | Swagger UI |
| `http://localhost:8081/v3/api-docs` | OpenAPI en JSON |

Ciclo de vida:

```bash
docker compose up -d          # levantar en segundo plano
docker compose ps             # ver el estado y los healthchecks
docker compose logs -f        # seguir los logs
docker compose down           # parar y borrar los contenedores; LOS DATOS QUEDAN
docker compose down -v        # parar y borrar tambien el volumen: datos borrados
```

La diferencia entre las dos últimas importa: los datos viven en un volumen con
nombre (`turnos-reservas-service-postgres-data`) y sobreviven a `down`. Solo
`down -v` los borra, y es lo que hay que hacer para que el script de
inicialización de la base vuelva a correr (el entrypoint de PostgreSQL lo ejecuta
únicamente cuando el volumen está vacío).

### Correr la aplicación fuera de Docker

Útil para iterar desde el IDE. La base sigue viniendo de Compose:

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Hay dos perfiles y la única diferencia real entre ellos es cómo se alcanza la
base: `local` usa `localhost` y el puerto **publicado** por Compose, y `docker`
usa el nombre del servicio en la red de Compose y el puerto **interno**. El perfil
`docker` lo activa `compose.yaml`; no hay que pasarlo a mano.

### Puertos

| Servicio | Puerto | Por qué |
|---|---|---|
| App de turnos y reservas | `8081` | El servicio de catálogo usa el 8080. |
| PostgreSQL de turnos y reservas | `5433` | El PostgreSQL del catálogo usa el 5432. |

Los dos backends tienen que poder correr al mismo tiempo, de ahí los números
distintos. Si alguno de esos puertos ya está ocupado en la máquina, se cambian con
`SERVER_PORT` y `DB_PORT` en el `.env` sin tocar ningún archivo versionado (si se
mueve `DB_PORT`, ajustar también `DB_URL`, que es el que usa el perfil `local`).

### Base de datos: una instancia, dos bases lógicas

La solución completa usa **una sola instancia de PostgreSQL con dos bases
lógicas**: `catalogo` y `turnos_reservas`. Cada una tiene su propio rol, y ninguno
de los dos puede conectarse a la base del otro. El enunciado (§3) permite compartir
la instancia física exactamente bajo esa condición, y así se evita tener dos
motores corriendo en la máquina de desarrollo para un proyecto de dos servicios.

El aislamiento no es una convención, está impuesto por la base
(`docker/postgres/init/02-crear-base-turnos.sh`):

- el rol de la aplicación se crea `NOSUPERUSER NOCREATEDB NOCREATEROLE`, así que no
  puede ver objetos de otras bases ni escalarse creando una base o un rol nuevos;
- a la base se le hace `REVOKE ALL ... FROM PUBLIC` y se le devuelve `CONNECT` solo
  al dueño, porque por defecto PostgreSQL deja que cualquier rol se conecte a
  cualquier base;
- al esquema `public` se le hace lo mismo y queda a nombre del dueño de la base.

El superusuario de la instancia queda solo como cuenta de administración: la
aplicación nunca lo usa.

**Este `compose.yaml` levanta su propia instancia**, porque sirve para desarrollar
el servicio de turnos solo, sin depender de que el otro repo esté clonado. La
instancia compartida se materializa en un **compose de nivel superior**, que
orquesta los dos backends juntos y no vive en ninguno de los dos repos de
servicio. Los scripts de inicialización de los dos repos están escritos para poder
montarse juntos en ese contenedor compartido: prefijo numérico distinto
(`01-` el catálogo, `02-` este) para que el orden sea determinístico, y variables
prefijadas (`CATALOG_DB_*` y `TURNOS_DB_*`) para que convivan en el mismo entorno.

Redis y Kafka **no** se levantan localmente: son instancias administradas por la
cátedra y se consumen por red (§3 de la referencia de integración).

### Migraciones del esquema

El esquema lo crean **migraciones versionadas con Flyway**, nunca Hibernate:
`spring.jpa.hibernate.ddl-auto` se queda en `validate`, que solo verifica que el
mapeo de las entidades coincida con las tablas existentes. Los scripts viven en
`src/main/resources/db/migration` y se aplican solos al arrancar la aplicación;
Flyway anota lo aplicado en `flyway_schema_history`, así que un reinicio no
vuelve a ejecutar nada.

Se eligió Flyway y no Liquibase por coherencia con el servicio de catálogo, que ya
lo había elegido: los dos backends comparten la instancia de PostgreSQL y conviene
que las migraciones se lean y se operen igual en los dos repos. Además el SQL plano
deja el esquema a la vista en el diff.

Reglas de trabajo con las migraciones:

- **una migración mergeada no se edita nunca.** Flyway guarda el checksum de cada
  script y `validate-on-migrate` falla si cambia; para corregir algo se agrega una
  migración nueva;
- el nombre sigue el formato `V<n>__<descripcion>.sql`, con doble guion bajo;
- `clean` está deshabilitado en todos los perfiles: borraría el esquema completo;
- `baseline-on-migrate` queda en `false`, porque la base arranca vacía: si
  aparecieran tablas sin historial de Flyway es un error y conviene que falle;
- en el perfil `test` las migraciones están **apagadas**, porque `./mvnw verify` no
  necesita una base levantada. Verificarlas contra un PostgreSQL real es parte de la
  suite con Testcontainers.

La aplicación corre las migraciones con su propio rol común, no con el superusuario
de la instancia: el rol es dueño de su base y de su esquema `public`, que es
exactamente lo que necesita para crear sus objetos y nada más.

### Propiedad de los datos

Cada servicio es propietario exclusivo de sus datos (§3 del enunciado). La base
`turnos_reservas` guarda:

| Dato | Para qué |
|---|---|
| Usuarios finales | Este servicio es dueño del registro, del hasheo de contraseñas y de la emisión del JWT de usuario. |
| Procesos de reserva | El estado local de cada proceso y su máquina de estados, identificado por `reservationProcessId`. |
| Reservas | El resultado confirmado, con el `reservationId` de la cátedra y el usuario final dueño. |
| Eventos procesados | El `eventId` de los eventos de Kafka ya procesados, para que un duplicado no cambie el resultado. |

Cada tabla llega con el issue que la necesita, para que el historial muestre el
esquema creciendo junto a la funcionalidad. La primera migración crea solo lo
transversal: la función `set_updated_at()`, el trigger de auditoría que van a
compartir todas las tablas propias.

Lo que esta base **no** guarda nunca es una **copia del catálogo vigente**
(categorías, profesionales, horarios semanales): eso es del servicio de catálogo y
se le consulta antes de cada operación. Dentro de una reserva sí puede quedar
información descriptiva histórica —el nombre del profesional al momento de
reservar—, pero como dato congelado de esa reserva, y nunca como catálogo para
buscar ni para validar una reserva nueva.

## Análisis de calidad: SonarCloud

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=manudiiez-um-programacion_turnos-reservas-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=manudiiez-um-programacion_turnos-reservas-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service)

Cada pull request y cada push se analiza en
[SonarCloud](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service):
bugs potenciales, code smells, duplicación y problemas de seguridad, más la
cobertura que mide JaCoCo durante las pruebas.

Lo dispara **GitHub Actions**, al final del job de `build.yml` y después de que
`./mvnw verify` generó el reporte de cobertura. El *automatic analysis* de
SonarCloud está desactivado a propósito: tener las dos formas de análisis
compitiendo produce resultados contradictorios.

Qué esperar al leerlo:

- **El quality gate es informativo.** No se usa `sonar.qualitygate.wait`, así que
  un gate rojo no rompe el build ni bloquea el merge. El enunciado no exige un
  porcentaje de cobertura, y el ruleset de las ramas protegidas lo administra la
  cátedra: este repo no toca su configuración.
- **La cobertura solo se puede leer en `main`.** En otras ramas el análisis se
  procesa igual, pero la organización no tiene permitido exponer esos datos
  (limitación del plan). El número aparece cuando el código llega a `main`.
- **El código generado por Lombok no cuenta.** Lo resuelve `lombok.config`, no una
  exclusión de Sonar: Lombok marca lo que genera con `@lombok.Generated` y JaCoCo
  ignora por convención todo lo anotado "Generated".
- No hay exclusiones de análisis declaradas, y el quality gate es el "Sonar way"
  por defecto, sin reglas propias.

### Configurarlo en un clon propio

El análisis necesita un token que **no** está en el repositorio. Sin él, el paso
se omite con un aviso y el build sigue en verde, así que el proyecto se clona y
compila sin configurar nada.

Para habilitarlo hace falta ser dueño del repositorio y de la organización en
SonarCloud:

1. Entrar a [sonarcloud.io](https://sonarcloud.io) con la cuenta de GitHub y
   crear o usar la organización `manudiiez-um-programacion`.
2. Agregar el repositorio eligiendo **"With GitHub Actions"** como método de
   análisis (no *automatic analysis*). El project key queda
   `manudiiez-um-programacion_turnos-reservas-service`, igual que en el `pom.xml`.
3. Generar el token en *My Account → Security → Generate Token*.
4. Guardarlo como secret de repositorio:

   ```bash
   gh secret set SONAR_TOKEN
   ```

El token es el único secreto que usa la integración continua de este repo: las
pruebas no hablan con el servicio central ni necesitan PostgreSQL.
