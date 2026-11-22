package ar.edu.um.turnos.health.domain.ports.out;

import ar.edu.um.turnos.health.domain.model.EstadoDependencias;

/**
 * Puerto de salida (#40): el dominio solo sabe que necesita el estado de sus
 * dependencias, no como se obtiene (una conexion JDBC activa para la base, el
 * ultimo resultado conocido para la catedra y el catalogo). La implementacion
 * vive en {@code infrastructure}.
 */
public interface EstadoDependenciasPort {

    EstadoDependencias consultar();

}
