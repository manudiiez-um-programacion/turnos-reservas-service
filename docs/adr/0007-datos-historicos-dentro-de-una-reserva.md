# ADR-0007: Datos descriptivos históricos dentro de una reserva

- **Estado**: Aceptado
- **Issue**: #27, #45

## Contexto

El enunciado (§4.2) prohíbe a este servicio replicar el catálogo: no puede
guardar una copia de categorías, profesionales u horarios para búsquedas o
para validar nuevas reservas. Al mismo tiempo, cada reserva necesita poder
mostrar con qué profesional fue hecha — nombre y apellido — sin depender de
una llamada en vivo al catálogo cada vez que el usuario consulta su
historial, incluso si ese profesional cambió de nombre, fue deshabilitado o
directamente eliminado del catálogo después de la reserva.

Estas dos necesidades parecen tensionar entre sí: ¿guardar el nombre del
profesional en la reserva es ya "replicar el catálogo"?

## Opciones consideradas

1. **No guardar nada descriptivo del profesional**, y resolver siempre el
   nombre consultando al catálogo en el momento de mostrar una reserva.
   Descartado: una reserva pasada se volvería ilegible (o mostraría datos
   incorrectos) si el profesional cambió de nombre o fue eliminado del
   catálogo después; además generaría una llamada al catálogo por cada
   reserva mostrada, incluso para datos que no van a cambiar en una reserva
   ya concluida.
2. **Guardar el `professionalId` únicamente** y resolver el nombre bajo
   demanda. Mismo problema que la opción 1 en el caso de un profesional
   eliminado o deshabilitado: la reserva quedaría sin nombre que mostrar, o
   dependiente de que el catálogo todavía tenga ese dato.
3. **Guardar el `professionalId` más una copia congelada del nombre y
   apellido, tomada una sola vez al crear el hold**, y nunca releída para
   decidir nada nuevo.

## Decisión

**Opción 3**: `proceso_reserva.professional_nombre_historico` y
`professional_apellido_historico` (migración `V4__proceso_reserva.sql`)
guardan el nombre y apellido del profesional **según el catálogo al momento
de crear el hold** (#27), copiados de la respuesta de
`CatalogoClient.obtenerAgendaVigente` de ese instante.

El criterio que distingue esto de "replicar el catálogo" (§4.2) es el
**uso**, no la presencia del dato:

- se escriben **una sola vez**, al crear el hold;
- **nunca se vuelven a leer** para validar una operación nueva ni para
  resolver una búsqueda — eso siempre va de nuevo a `CatalogoClient`, en el
  momento, para esa operación nueva;
- si el profesional cambia de nombre o se deshabilita después, las reservas
  ya hechas conservan el dato de cuando se reservaron, que es justamente la
  razón de tenerlo: no sería razonable que el historial de una reserva
  pasada cambiara retroactivamente porque el catálogo cambió.

Una copia **prohibida** por el enunciado es la que se usa como fuente para
decidir sobre operaciones nuevas (buscar profesionales, validar un hold
nuevo). Un dato histórico dentro de un registro propio, que nunca se usa
para eso, no lo es.

## Consecuencias

- Consultar el historial de reservas de un usuario (#33) no depende de que
  el catálogo esté arriba ni de que el profesional todavía exista ahí —
  consistente con la regla general del §3 ("las operaciones que dependen
  solo de datos propios siguen atendiéndose" si el catálogo está caído).
- El nombre mostrado en una reserva histórica puede quedar
  **desactualizado** respecto del catálogo vigente (si el profesional
  cambió de nombre después). Es el comportamiento deseado, no un defecto:
  una reserva documenta con quién se hizo la cita en ese momento.
- Cualquier caso de uso nuevo que necesite el nombre **vigente** de un
  profesional (no el histórico de una reserva ya hecha) tiene que consultar
  `CatalogoClient` de nuevo, nunca leer estas dos columnas con ese fin —
  la regla queda documentada acá para que no se reintroduzca el error al
  agregar funcionalidad futura.
