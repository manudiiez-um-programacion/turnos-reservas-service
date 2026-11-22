# ADR-0004: Hasheo de contraseñas con Argon2id

- **Estado**: Aceptado
- **Issue**: #9, #45

## Contexto

Este servicio es dueño de los usuarios finales (ADR-0003, D1) y por lo
tanto del almacenamiento de sus contraseñas. El 28-10-2026 el profesor
indicó explícitamente que **BCrypt quedó debilitado** frente al poder de
cómputo actual y que corresponde usar un algoritmo más moderno.

## Opciones consideradas

1. **BCrypt** (el default histórico de Spring Security y de JHipster).
   Descartado por la indicación explícita del profesor.
2. **PBKDF2**. Resistente a GPU en menor medida que Argon2id porque no es
   *memory-hard*: un atacante con hardware especializado (GPU/ASIC) lo
   paraleliza con más facilidad.
3. **Argon2id**, ganador del *Password Hashing Competition* (2015) y
   recomendación vigente de OWASP.

## Decisión

**Argon2id**, con parámetros **explícitos** en `PasswordEncoderConfig` (no
`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`, que son los
defaults implícitos de la librería y podrían cambiar de versión a versión
sin que el código lo refleje):

| Parámetro | Valor |
|---|---|
| Sal | 16 bytes |
| Hash | 32 bytes |
| Paralelismo | 1 |
| Memoria | 19456 KiB (19 MiB) |
| Iteraciones | 2 |

Son los valores de partida de la
[recomendación OWASP 2023](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
para Argon2id, **medidos** en la máquina de desarrollo y no copiados a
ciegas: un `encode()` tarda 20–25 ms en estado estable (un costo aceptable
por login, alto para un atacante que tiene que repetirlo millones de
veces) y el hash resultante mide 97 caracteres (105 con el prefijo
`{argon2}`), de ahí que `usuario.password_hash` sea `VARCHAR(120)` en
`V2__usuarios.sql` y no los 60 caracteres fijos de BCrypt.

Se usa `DelegatingPasswordEncoder` con un único encoder registrado bajo el
id `argon2`, en vez de `Argon2PasswordEncoder` directo: el delegante
antepone el prefijo `{argon2}` a cada hash guardado, y verifica delegando
según **ese prefijo**, no según el encoder configurado hoy. Eso deja
abierto cambiar de algoritmo o de parámetros a futuro sin invalidar los
hashes existentes — simplemente convivirían dos prefijos hasta que los
usuarios antiguos vuelvan a loguearse y se puedan rehashear. Es el mismo
patrón que usa JHipster, con el que este servicio ya busca compatibilidad
(ADR-0002).

## Consecuencias

- Cada login cuesta 20–25 ms más de CPU que con BCrypt a un costo
  comparable, un costo aceptado porque es justamente lo que hace cara una
  búsqueda por fuerza bruta.
- Los parámetros quedan documentados y medidos, no heredados de un default
  de librería: una actualización de Spring Security no cambia silenciosamente
  cuánto le cuesta a un atacante forzar una contraseña.
- Si los parámetros necesitan subir en el futuro (más memoria, más
  iteraciones), el cambio es local a `PasswordEncoderConfig` y no rompe los
  hashes existentes gracias al prefijo del `DelegatingPasswordEncoder`.
- La contraseña nunca se guarda ni se loguea en texto plano en ningún punto
  del flujo (válido independientemente del algoritmo elegido, pero vale
  dejarlo explícito).
