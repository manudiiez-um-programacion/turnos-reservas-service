package ar.edu.um.turnos.config.catedra.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Error HTTP que devolvio el servicio central de la catedra.
 *
 * <p>Lleva el {@code status} y, si el cuerpo era {@code application/problem+json}
 * y lo incluia, el {@code code} y el {@code detail} tal como los manda la
 * catedra (§13 de la referencia: "los clientes deben decidir por status y
 * code, nunca por el texto de detail" — por eso {@code code} es el campo
 * relevante para quien atrape esta excepcion, y {@code detail} solo queda
 * disponible para diagnostico). <strong>No traduce</strong> el {@code code} a
 * un error de dominio propio: esa traduccion es responsabilidad de #15.
 */
@Getter
public abstract class CatedraHttpException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;
    private final String detail;

    protected CatedraHttpException(HttpStatusCode status, String code, String detail) {
        super("La catedra respondio %s%s".formatted(status, code != null ? " (" + code + ")" : ""));
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

}
