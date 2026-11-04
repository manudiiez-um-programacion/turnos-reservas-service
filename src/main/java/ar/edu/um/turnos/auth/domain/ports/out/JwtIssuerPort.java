package ar.edu.um.turnos.auth.domain.ports.out;

import ar.edu.um.turnos.auth.domain.model.Usuario;

/**
 * Puerto de salida: emision del JWT de usuario. El dominio no sabe con que
 * libreria se firma ni con que secreto: eso es un detalle de
 * {@code infrastructure}.
 *
 * <p>El formato, la firma y los claims de ese token son <strong>contrato
 * entre este servicio y el de catalogo</strong> (ADR 0002,
 * {@code turnos-catalogo-sync#59}), no un detalle interno: ver
 * {@code JwtIssuerAdapter}.
 */
public interface JwtIssuerPort {

    String emitir(Usuario usuario);

}
