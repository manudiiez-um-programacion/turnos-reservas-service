# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio.

## 1. Qué es este repo

`turnos-reservas-service` — trabajo final de **Programación 2** (Universidad de Mendoza),
proyecto integrador 2026: *sistema distribuido de turnos*. Entrega final: últimos días
de noviembre de 2026.

Este repo es **uno de los tres** del proyecto (org `manudiiez-um-programacion`):

| Componente | Repo | Rol |
|---|---|---|
| App Android KMP | `turnos-app-android` | Interfaz de usuario final: registro, login, búsqueda, reservas. |
| Servicio de catálogo y sincronización | `turnos-catalogo-sync` | Copia local del catálogo de la cátedra (categorías, profesionales, horarios semanales) + sincronización completa e incremental. |
| **Servicio de turnos y reservas** | **este repo** | Disponibilidad, holds, procesos de reserva, consulta y cancelación. |

Arriba de todo está el **servicio central de la cátedra**: fuente autoritativa del
catálogo y de las reservas, al que se accede por REST, Redis y Kafka. No se corre
local: es una instancia administrada por la cátedra.

**Estado actual: sin código del servicio.** El repo tiene documentación de la cátedra,
convenciones de trabajo y skills. El stack está definido como objetivo (Java + Spring
Boot, arquitectura hexagonal por slices verticales) pero todavía no hay `pom.xml` ni
código. Antes de asumir estructura de paquetes, build o dependencias, confirmar con el
usuario o seguir la skill `hexagonal-slicing`.

## 2. Fuentes de verdad

- `docs/catedra/PROJECT_STATEMENT-v1.md` — enunciado: alcance obligatorio,
  responsabilidades de cada servicio, seguridad, pruebas, documentación de entrega.
- `docs/catedra/INTEGRATION_REFERENCE-v2.md` — **contrato** REST, Redis y Kafka del
  servicio central. Ante cualquier duda de integración, manda este documento: los
  ejemplos del enunciado no lo reemplazan.
- Las decisiones internas (entidades, tablas, contratos entre los dos servicios
  propios, idempotencia, reintentos, máquina de estados) las define el alumno y hay
  que **documentarlas y justificarlas**.

## 3. Responsabilidad de este servicio

Según el enunciado (§4.2), este servicio debe:

- construir la **disponibilidad** combinando los horarios semanales (que pide al
  servicio de catálogo) con las **ocupaciones** informadas por la cátedra;
- iniciar y conservar el **estado local de cada proceso de reserva**;
- crear y confirmar inicialmente **holds temporales**;
- participar del intercambio asincrónico de información adicional (teléfono) por Kafka;
- procesar confirmaciones, rechazos, vencimientos y procesos inválidos;
- consultar las reservas de la cuenta técnica y **filtrarlas por usuario final**;
- cancelar reservas confirmadas propias;
- recuperar procesos pendientes o interrumpidos (reinicios, timeouts, duplicados).

### Lo que este servicio NO hace

- **No replica el catálogo.** Prohibido guardar una copia de categorías,
  profesionales u horarios para búsquedas o para validar nuevas reservas. La
  información vigente se le pide al servicio de catálogo antes de cada operación.
  Sí puede guardar datos **descriptivos históricos** dentro de una reserva (nombre
  del profesional al momento de reservar), pero nunca usarlos como catálogo vigente.
- **No accede a la base del otro servicio.** Ni tablas, ni repositorios, ni esquemas
  compartidos como mecanismo de integración: solo contratos explícitos protegidos
  con JWT.
- **No resuelve búsquedas de profesionales.** Eso es del servicio de catálogo.
- **No expone el JWT técnico de la cátedra** a KMP ni a sus usuarios.

Si el servicio de catálogo está caído, no pueden iniciarse operaciones que requieran
validar información vigente; sí deben seguir atendiéndose las operaciones que
dependan solo de datos propios (consultar reservas locales, por ejemplo).

## 4. Modelo de dominio

Conceptos del dominio, con el vocabulario del contrato:

- **Disponibilidad**: slots libres para un profesional y una fecha = reglas semanales
  del catálogo − ocupaciones de la cátedra (`CONFIRMED` y `HELD`).
- **Hold**: bloqueo temporal de un slot. Tiene `holdId` y `expiresAt`. **El TTL lo
  manda la cátedra**: conservarlo localmente no lo extiende.
- **Proceso de reserva** (`reservationProcessId`): la unidad de trabajo local, con su
  máquina de estados. Es lo que correlaciona REST con los eventos de Kafka.
- **Reserva**: resultado confirmado, con `reservationId` del lado de la cátedra.
- **Propiedad por usuario**: la cátedra identifica todo por el `groupId` técnico del
  proyecto, así que **la asociación con el usuario final es responsabilidad de este
  servicio**. Cada proceso y cada reserva se guarda con su usuario, y un usuario
  jamás puede ver, modificar ni cancelar lo de otro. A la cátedra se le envía un
  identificador estable del usuario como `externalPatientId`.

### Flujo de reserva (comportamiento observable)

1. El usuario elige profesional, fecha y hora.
2. Este servicio valida contra el catálogo y pide un hold: `POST /api/appointment-holds`
   → `201` con `holdId`, `reservationProcessId`, `expiresAt`.
3. Confirma inicialmente: `POST /api/appointment-holds/{holdId}/confirm` (con nombre y
   apellido del paciente) → `202` con estado `WAITING_FOR_PHONE`.
4. Llega `AdditionalInformationRequested` por Kafka (pide `PHONE_NUMBER`). **Guardar su
   `eventId`**: se devuelve como `requestEventId`.
5. Se publica `AdditionalInformationSubmitted` con el teléfono, message key =
   `reservationProcessId`.
6. Llega el resultado final: `AppointmentConfirmed`, `AdditionalInformationRejected`
   (se puede reintentar con un `eventId` nuevo hasta `expiresAt`),
   `AppointmentProcessExpired` o `AppointmentProcessInvalid`.
7. Se actualiza el estado local y el usuario puede consultarlo.

Teléfono: la cátedra normaliza quitando espacios, guiones, paréntesis y puntos, y
luego valida `^\+?[0-9]{7,15}$`.

## 5. Integración con la cátedra

### Endpoints REST que usa este servicio

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/appointment-occupancies?professionalId&from&to` | Ocupaciones (`CONFIRMED` / `HELD`) para armar disponibilidad. |
| `POST` | `/api/appointment-holds` | Crear el bloqueo temporal. |
| `POST` | `/api/appointment-holds/{holdId}/confirm` | Confirmación inicial; dispara el paso Kafka. |
| `GET` | `/api/appointments` | Listar reservas de la cuenta técnica (filtros `status`, `from`, `to`, `externalPatientId`, paginación). |
| `POST` | `/api/appointments/{reservationProcessId}/cancel` | Cancelar (idempotente si ya está `CANCELLED`). |

`POST /api/authenticate` y `GET /api/student/integration` son para la cuenta técnica y
su configuración. Todos los endpoints protegidos van con
`Authorization: Bearer <jwt-tecnico>` y rol `ROLE_STUDENT_CLIENT`.

### Topics Kafka

| Topic | Dirección | Eventos |
|---|---|---|
| `alumnos.turnos.acciones.{groupId}` | cátedra → alumno | `AdditionalInformationRequested`, `AppointmentConfirmed`, `AdditionalInformationRejected`, `AppointmentProcessExpired`, `AppointmentProcessInvalid`, `AppointmentCancelled` |
| `catedra.turnos.telefono.{groupId}` | alumno → cátedra | `AdditionalInformationSubmitted` |

(`catedra.catalog.{groupId}` con `CatalogUpdated` es del servicio de catálogo, no de este.)

### Convenciones del contrato

- Fechas `yyyy-MM-dd`; instantes ISO-8601 en UTC; horas locales, aceptar `HH:mm` y
  `HH:mm:ss`; JSON en `camelCase`.
- Errores REST en `application/problem+json`. **Decidir por `status` + `code`**
  (`SLOT_ALREADY_HELD`, `HOLD_EXPIRED`, `RESERVATION_NOT_CANCELLABLE`, …), nunca por el
  texto de `detail`. La matriz completa está en §13.1 de la referencia.
- Kafka es **entrega al menos una vez**: hay duplicados. `eventId` es la clave de
  idempotencia; la message key de los eventos de turnos es `reservationProcessId`.
- `schemaVersion` = 1. Ignorar campos desconocidos, no romper por campos nuevos.

## 6. Reglas de oro (robustez)

El enunciado evalúa explícitamente estos comportamientos (§8, §11 y §18 de la referencia):

- **Idempotencia por `eventId`**: procesar dos veces el mismo evento no cambia el
  resultado final.
- **Los estados finales no retroceden**: un evento tardío no reabre un proceso ya
  confirmado, vencido o cancelado. REST y Kafka pueden llegar en distinto orden y la
  solución debe converger sin duplicar reservas.
- **Un timeout no prueba que la operación falló**: antes de repetir una acción con
  efectos, reconciliar por los identificadores persistidos (`holdId`,
  `reservationProcessId`).
- **Reintentos limitados y observables**: nada de ciclos infinitos ni estados locales
  irrecuperables.
- **Vencimientos**: `HOLD_EXPIRED` o `AppointmentProcessExpired` cierran el proceso
  local y cortan nuevos intentos de confirmación.
- **Reinicios**: los procesos pendientes se recuperan al arrancar.

## 7. Seguridad

- Dos identidades que no se mezclan: la **cuenta técnica del proyecto** (JWT de la
  cátedra, vigencia un año, la usan los dos backends) y el **usuario final** (se
  registra en **este** servicio y recibe un JWT emitido por nosotros).
- **Los usuarios finales viven acá.** Este servicio es dueño de la tabla, del hasheo y
  de la emisión del JWT de usuario; el de catálogo no tiene usuarios ni login, solo
  valida el token. Así el login y la consulta de reservas propias siguen funcionando
  con el catálogo caído.
- Todo endpoint que no sea explícitamente público exige JWT válido: firma, vigencia y
  permisos. Solo registro y login son públicos.
- **Tres credenciales distintas, que nunca se confunden:**
  - JWT de usuario: HS256 con `JWT_USER_SECRET`, emitido acá y validado también por el
    catálogo (la app KMP le consulta la búsqueda directo a él).
  - JWT técnico interno: `JWT_INTERNAL_SECRET` y autoridad `SERVICE_TURNOS`, para las
    llamadas de este servicio al de catálogo. Secreto **distinto** del de usuario: eso
    es lo que hace criptográfica la separación entre identidades. Trazabilidad por
    header `X-Correlation-Id`.
  - JWT técnico de la cátedra: solo hacia afuera, hacia el servicio central. Nunca se
    acepta como credencial de entrada ni se envía al catálogo ni a KMP.
- Autorización por identidad autenticada: **aislamiento estricto entre usuarios**.
  Nunca confiar en un id de usuario que venga del cliente.
- Contraseñas con **Argon2id** (indicación del profesor, 28-10-2026: BCrypt quedó
  debilitado). Parámetros explícitos y documentados, hash con prefijo `{argon2}` vía
  `DelegatingPasswordEncoder`. Nunca en texto plano, ni en logs, ni en respuestas.
- **Secretos externalizados**: URL de la API, host/puerto/usuario/password de Redis,
  bootstrap servers de Kafka y JWT técnico van en `.env` (ignorado por git); se
  versiona solo `.env.example`. Jamás en el código ni en el repositorio.
- CORS configurado solo para los clientes permitidos.

Si se filtra un secreto: avisar de inmediato. Borrarlo en otro commit no alcanza, hay
que rotarlo.

## 8. Pruebas y entrega

- Pruebas automatizadas obligatorias de los servicios backend para los
  comportamientos centrales: registro y autenticación, sincronización, reservas,
  **autorización** e **idempotencia**. No se exige un porcentaje de cobertura.
- Documentación obligatoria de entrega: README reproducible, diagrama de arquitectura,
  contrato entre los dos servicios propios, modelo de datos y su propiedad, estrategia
  de sincronización, idempotencia y recuperación, decisiones de seguridad, instrucciones
  para correr las pruebas. La documentación debe coincidir con lo entregado.
- La infraestructura local (servicios + bases + dependencias) tiene que levantarse con
  **Docker Compose**.

## 9. Decisiones

### Ya decididas

- **Motor de base de datos**: **PostgreSQL**, el mismo que usa el servicio de catálogo.
  Tiene que ser un gestor servidor: el enunciado rechaza H2, SQLite y cualquier embebida
  como almacenamiento principal. La skill `hexagonal-slicing` propone H2 en su plantilla:
  el `pom.xml` de este repo la reemplaza por el driver de PostgreSQL y **no usa H2 ni
  para tests**.
- **JHipster: no se usa.** Spring Boot a mano siguiendo `hexagonal-slicing`. El modelo de
  usuario sí queda compatible con el contrato de JHipster, como exige el enunciado.
- **Identidad y autenticación entre servicios**: decidido en el ADR 0002 del repo de
  catálogo (`manudiiez-um-programacion/turnos-catalogo-sync#59`), que cerró D1, D4 y D5.
  El detalle operativo está en §7. Las decisiones compartidas entre los dos backends se
  registran allá: antes de decidir algo que cruce los dos servicios, revisar si ya está
  decidido.
- **Hasheo de contraseñas**: Argon2id (ver §7).

### Todavía abiertas

No asumir ninguna de estas sin confirmar con el usuario:

- Contrato concreto que expone el catálogo (endpoints, DTO, errores). Especificado en
  `turnos-catalogo-sync#18` y documentado en su `#20`, pero todavía no implementado:
  leerlo de ahí, no adivinarlo.
- Dónde vive el `docker-compose` que levanta la solución completa, y si se usa una
  instancia de PostgreSQL con dos bases lógicas o una por servicio.
- Herramienta de migraciones (Flyway o Liquibase).
- Máquina de estados local de los procesos de reserva.
- Estrategia de deduplicación, timeouts, backoff y límites de reintentos.
- Parámetros concretos de Argon2id (hay que medirlos, no copiarlos).

## 10. Cómo se trabaja en este repo

El flujo completo está en la skill **`git-flujo-integrador`** (`.claude/skills/`).
Resumen:

- **Nada se trabaja sin issue.** Primero milestone + issue, después rama.
- Ramas: `tipo/mN-NN-slug` → PR a `milestone/mN-slug` (sin aprobación) → un PR
  `milestone/* → develop` por milestone (con aprobación del profesor) → promociones
  `develop → testing → main`.
- `main`, `develop` y `testing` están protegidas por ruleset: PR obligatorio,
  1 aprobación + Code Owners (`@dqmdz-um`), sin force push ni borrado. Nunca pushear
  directo.
- Los PRs internos usan `Refs #N`; los `Closes #N` van solo en el PR a `main`, que es
  donde se cierran los issues.
- Commits: **Conventional Commits en español, con cuerpo obligatorio** y footer
  `Refs #N`. El hook `.githooks/commit-msg` los valida. Activarlo una vez por clon:
  ```bash
  git config core.hooksPath .githooks
  ```
  Nunca usar `--no-verify`.
- La cátedra exige que el historial muestre la evolución del trabajo: commits atómicos,
  issues antes de los PRs.

Para el backend, seguir la skill **`hexagonal-slicing`**: arquitectura hexagonal
(puertos y adaptadores) organizada por slices verticales, un paquete autocontenido por
feature.

## 11. Convenciones

- **Idioma**: documentación, comentarios y mensajes de commit en español.
- Commitear o pushear **solo cuando el usuario lo pida explícitamente**.
- El `README.md` tiene un bloque entre `<!-- calidad:inicio -->` y `<!-- calidad:fin -->`
  generado por la herramienta de evaluación de la cátedra: **no editarlo a mano**.
  El contenido propio del README va debajo de `calidad:fin`.
- No modificar el ruleset, `CODEOWNERS` ni la configuración del repo.

## 12. Estructura

```
.
├── pom.xml                   # Spring Boot 4.1.2, Java 25
├── mvnw / .mvn/              # Maven wrapper (no hace falta Maven instalado)
├── lombok.config             # Marca el código generado para que JaCoCo no lo cuente
├── src/main/java/ar/edu/um/turnos/
│   ├── TurnosReservasApplication.java
│   ├── config/               # Configuración transversal (SecurityConfig)
│   └── health/               # Slice de referencia de la estructura hexagonal
│       ├── domain/           # model, ports/in, ports/out — sin framework
│       ├── application/      # usecases, service — solo depende de domain
│       └── infrastructure/   # web (controller, dto, mapper), config/adapter
├── src/main/resources/application.properties
├── src/test/java/ar/edu/um/turnos/
├── README.md                 # Título + badge de evaluación (bloque autogenerado)
├── CLAUDE.md
├── docs/catedra/             # Enunciado y referencia de integración (fuentes de verdad)
├── .claude/skills/           # git-flujo-integrador, hexagonal-slicing
├── .githooks/commit-msg      # Validación de Conventional Commits
├── .gitattributes            # LF forzado en los hooks
├── .github/workflows/        # build.yml: CI en cada PR
└── .github/CODEOWNERS        # Owner: @dqmdz-um
```

Un paquete por feature, y dentro las tres capas. La regla de dependencias apunta
hacia adentro: `domain` no depende de nada, `application` solo de `domain`,
`infrastructure` de ambos y nunca al revés. La `@Entity` de JPA y los DTO web no
salen de `infrastructure`. El slice `health` es el ejemplo canónico: no tiene
persistencia a propósito, pero sí puerto de salida con su adaptador.

## 13. Comandos

```bash
./mvnw verify                 # compila y corre todas las pruebas
./mvnw -q test                # solo las pruebas
./mvnw spring-boot:run        # levanta el servicio en el puerto 8080
./mvnw clean                  # borra target/
```

Para levantarlo en otro puerto (útil si el 8080 está ocupado):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Una vez arriba:

| URL | Qué es |
|---|---|
| `http://localhost:8080/api/health` | Estado del servicio |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI en JSON |

Activar el hook de commits, una vez por clon:

```bash
git config core.hooksPath .githooks
```

### Integración continua

`.github/workflows/build.yml` corre `./mvnw -B verify` en cada pull request y en los
pushes a `develop`, `testing` y `main`. Publica en el resumen del job la tabla de
pruebas y la de cobertura, y guarda los reportes de Surefire y JaCoCo como artefacto.

No usa ningún secreto: las pruebas no hablan con el servicio central ni necesitan
PostgreSQL. El check es informativo — el ruleset de las ramas protegidas lo administra
la cátedra y este repo no toca su configuración.

La cobertura no tiene umbral que rompa el build: el enunciado no exige un porcentaje.
Se mide para poder mirarla, y el código generado por Lombok no cuenta.

### Variables de entorno

Todas tienen default de desarrollo, así que el servicio arranca sin configurar nada.
Los secretos de verdad se externalizan en `.env` (issue #7).

| Variable | Default | Para qué |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/turnos_reservas` | Base propia |
| `DB_USERNAME` | `turnos` | Usuario de la base |
| `DB_PASSWORD` | `turnos` | Password de la base |

### Estado provisional de este milestone

Tres cosas están puestas a propósito de forma temporal y hay que sacarlas cuando
llegue su issue. Están comentadas en el código con el número de issue:

- **Arranca sin PostgreSQL.** `application.properties` desactiva el fail-fast de
  Hikari y el acceso a los metadatos JDBC de Hibernate, así que el servicio levanta
  con la base caída. Se saca en #5/#6, cuando existan el Docker Compose y las
  migraciones. Por eso el dialecto se declara explícito: sin metadatos, Hibernate no
  puede deducirlo (Hibernate avisa que no haría falta; con esta config, sí).
- **Seguridad abierta.** `SecurityConfig` hace `permitAll()` para que Swagger sea
  accesible. Se reemplaza entero en #11. Lo único definitivo ahí es que la API es
  stateless y sin CSRF.
- **Sin usuarios.** Se excluye `UserDetailsServiceAutoConfiguration` para que Spring
  Security no invente un usuario en memoria con password aleatoria en el log. Los
  usuarios finales llegan en #9/#10.
