package ar.edu.um.turnos.health.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado de cada dependencia externa del servicio (#40, §19 de la referencia).
 *
 * <p>Modelo de dominio puro, sin anotaciones de framework. La base de datos se
 * chequea activa en cada consulta (es local y el costo es bajo); la catedra y
 * el catalogo se observan de forma pasiva, por el ultimo resultado conocido de
 * la llamada real mas reciente que ya hizo {@code CatedraClient} o
 * {@code CatalogoClient} por motivos de negocio (§3: no se genera trafico
 * nuevo hacia ellos solo para contestar una consulta de salud).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDependencias {

    private EstadoDependencia baseDeDatos;
    private EstadoDependencia catedra;
    private EstadoDependencia catalogo;

}
