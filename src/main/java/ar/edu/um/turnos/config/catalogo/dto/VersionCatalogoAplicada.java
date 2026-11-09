package ar.edu.um.turnos.config.catalogo.dto;

import java.time.Instant;

/**
 * Version del catalogo de la catedra sobre la que respondio esta consulta
 * (#16). No dice si es la ultima version publicada por la catedra, solo
 * sobre cual respondio el catalogo: dos llamadas consecutivas pueden traer
 * versiones distintas si la sincronizacion avanzo entre medio.
 */
public record VersionCatalogoAplicada(

        Long version,

        Instant appliedAt,

        String source

) {
}
