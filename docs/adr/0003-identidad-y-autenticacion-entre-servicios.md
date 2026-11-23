# ADR-0003: Identidad y autenticación entre servicios

- **Estado**: Aceptado
- **Issue**: #10, #11, #12, #45
- **Decisión compartida**: replica D1, D4 y D5 del **ADR 0002** del repo
  `turnos-catalogo-sync` (`manudiiez-um-programacion/turnos-catalogo-sync#59`),
  que es donde esas decisiones se discutieron y cerraron para los **dos**
  backends propios. Este documento no las vuelve a decidir, las deja
  registradas también acá porque este servicio es parte implementadora.
  D2 y D3 de ese ADR son decisiones del otro repo y no se replican acá.

## Contexto

El proyecto tiene **dos identidades que no se pueden mezclar**: la cuenta
técnica del proyecto (JWT de la cátedra, la usan los dos backends) y el
usuario final de la app KMP. La cátedra identifica todo por el `groupId`
técnico del proyecto, así que **alguien** tiene que ser dueño de los
usuarios finales, de su login y de emitir su JWT — y los dos backends
propios necesitan llamarse entre sí (este servicio consulta la agenda
vigente del catálogo) sin usar ninguna de esas dos credenciales para eso.

Al ser una decisión que involucra a los dos repos del proyecto propio,
tiene que resolverse coordinada entre los dos, no de forma independiente en
cada uno.

## Opciones consideradas (D1: dónde viven los usuarios finales)

1. Usuarios y login en el servicio de **catálogo** (que concentra la
   búsqueda) y que este servicio solo consuma su JWT.
2. Usuarios y login en **este servicio** (turnos y reservas), y que el
   catálogo solo valide el token que este servicio emite.
3. Usuarios duplicados en los dos servicios, sincronizados entre sí.

La opción 3 se descarta sin mayor análisis: reintroduce exactamente el
problema que el enunciado prohíbe para el catálogo de profesionales (una
copia que puede desincronizarse), aplicado ahora a usuarios.

## Decisión (D1)

**Los usuarios finales viven en este servicio.** Es dueño de la tabla
`usuario`, del hasheo de contraseñas (ADR-0004) y de la emisión del JWT de
usuario. El servicio de catálogo no tiene usuarios ni login propio, solo
valida el token que este servicio firma. Así el login y la consulta de
reservas propias siguen funcionando aunque el catálogo esté caído (§3 del
enunciado: las operaciones que dependen solo de datos propios tienen que
seguir atendiéndose).

### Formato del JWT de usuario

HS256, firmado con `JWT_USER_SECRET` (compartido entre los dos backends
porque el catálogo necesita verificar, no porque lo necesite firmar).
Claims, detalle completo en `docs/contrato-y-modelo-de-datos.md` §3:
`sub` = `login` (compatible con JHipster, ver ADR-0002), `auth` = autoridades
como arreglo, `iss`/`iat`/`exp`. Nunca lleva email, `externalPatientId` ni
ningún dato de la cuenta técnica de la cátedra.

### Limitación aceptada y camino de mejora

Con HS256 y un secreto simétrico que los dos backends comparten, el
servicio de catálogo es *técnicamente* capaz de **falsificar** un token de
usuario, no solo de verificarlo — HS256 no distingue "puede firmar" de
"puede verificar". Se acepta porque los dos backends son del mismo
proyecto, los mantiene la misma persona y se despliegan juntos: el riesgo
que esto introduce es el de un compañero de equipo, no el de un tercero.

Camino de mejora si esto deja de ser aceptable: **RS256**, con la clave
privada únicamente en este servicio (que firma) y la clave pública en el
`.env` del catálogo (que solo puede verificar). Es un cambio de
configuración del decodificador en los dos repos, no del dominio de
ninguno de los dos.

## Decisión (D4): JWT técnico interno entre backends

Las llamadas de este servicio al de catálogo (hoy, la agenda vigente de
#16) no pueden usar ni el JWT de usuario (no representa a un usuario
cuando el llamador es un backend) ni el JWT de la cátedra (nunca sale hacia
otro cliente propio). Se emite una **tercera credencial**, exclusiva para
esta llamada: JWT técnico interno, HS256, firmado con `JWT_INTERNAL_SECRET`
— **distinto** del secreto de usuario, con una validación
(`JwtProperties.isSecretosDistintos`) que **rechaza el arranque** si
coinciden.

Claims: `sub` = `"turnos-reservas-service"` (identifica al servicio, no a
un usuario), `authorities` = `["SERVICE_TURNOS"]` (nombre de claim distinto
de `auth`, y sin prefijo `ROLE_`: no es un rol). Vigencia corta
(`JWT_INTERNAL_EXPIRATION`, default `PT2M`) y sin caché: se reemite en cada
llamada, porque con una vigencia de minutos no vale la pena la complejidad
de invalidar antes del vencimiento.

Por qué un secreto distinto y no reusar el de usuario: es lo que hace
*criptográfica* la separación entre "un usuario pidiendo algo" y "un backend
pidiendo algo". Con un secreto compartido entre los dos, un token de
usuario robado se podría presentar como si fuera el del servicio.

## Decisión (D5): trazabilidad por `X-Correlation-Id`

El JWT técnico interno no representa a ningún usuario, así que no hay un
`sub` de usuario que correlacionar en los logs de una llamada entre
backends. Cada llamada de este servicio al catálogo (`CatalogoAuthInterceptor`)
agrega el header `X-Correlation-Id`: si el llamador original ya puso uno
(por ejemplo, para encadenarlo con el de un request entrante del usuario),
se respeta; si no, se genera uno nuevo con `UUID.randomUUID()`.

## Consecuencias

- Tres credenciales, tres secretos, cero superposición: JWT de usuario
  (`JWT_USER_SECRET`), JWT técnico interno (`JWT_INTERNAL_SECRET`) y JWT
  técnico de la cátedra (`CATEDRA_JWT`, ajeno a esta decisión, solo hacia
  afuera). Mezclar cualquiera de los dos primeros falla el arranque; mezclar
  el tercero con los otros dos sería un error de integración, no algo que
  el código permita siquiera construir.
- El catálogo depende de este servicio para la identidad de usuario: si
  este servicio no emite el JWT correctamente, el catálogo no tiene manera
  de autenticar a nadie por su cuenta.
- La limitación de D1 (HS256 compartido) queda documentada acá y en
  `docs/idempotencia-recuperacion-seguridad.md` §5.2, no oculta: es una
  decisión consciente con un costo conocido, no un descuido.
- Si esta decisión cambia (por ejemplo, migrar a RS256), hay que actualizar
  el ADR de **los dos repos**: una versión distinta de la misma decisión en
  cada lado es peor que no documentarla.
