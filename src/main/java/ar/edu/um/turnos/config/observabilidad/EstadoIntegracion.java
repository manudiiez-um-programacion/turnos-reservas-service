package ar.edu.um.turnos.config.observabilidad;

import java.time.Instant;

/**
 * Ultimo resultado conocido de una {@link Integracion}, segun la llamada real
 * mas reciente que ya hizo el cliente correspondiente por motivos de negocio.
 *
 * <p>{@code disponible} es {@code null} cuando todavia no hubo ninguna
 * llamada real en lo que lleva corriendo el proceso (recien arrancado, o la
 * funcionalidad que depende de esa integracion nunca se uso): es "desconocido",
 * no "caido". El endpoint de salud (#40) no genera trafico nuevo hacia la
 * catedra o el catalogo solo para contestar una consulta de salud: lee este
 * estado tal como quedo.
 */
public record EstadoIntegracion(Boolean disponible, Instant actualizadoEn) {

    static EstadoIntegracion desconocido() {
        return new EstadoIntegracion(null, null);
    }

    public boolean esConocido() {
        return disponible != null;
    }

}
