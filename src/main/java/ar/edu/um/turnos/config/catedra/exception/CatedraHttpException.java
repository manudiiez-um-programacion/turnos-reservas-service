package ar.edu.um.turnos.config.catedra.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Error HTTP que devolvio el servicio central de la catedra.
 *
 * <p>Lleva el {@code status} y, si el cuerpo era {@code application/problem+json}
 * y lo incluia, el {@code code}, el {@code detail} y los {@code fieldErrors}
 * tal como los manda la catedra (§13 de la referencia: "los clientes deben
 * decidir por status y code, nunca por el texto de detail" — por eso
 * {@code detail} solo queda disponible para diagnostico). La traduccion de
 * {@code code} a un error de dominio propio, tipado, vive en
 * {@link CatedraClientErrorException#getErrorCode()} (#15, §13.1): esta
 * clase base solo guarda los datos crudos que llegaron.
 */
@Getter
public abstract class CatedraHttpException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;
    private final String detail;
    private final List<CatedraFieldError> fieldErrors;

    protected CatedraHttpException(HttpStatusCode status, String code, String detail, List<CatedraFieldError> fieldErrors) {
        super("La catedra respondio %s%s".formatted(status, code != null ? " (" + code + ")" : ""));
        this.status = status;
        this.code = code;
        this.detail = detail;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

}
