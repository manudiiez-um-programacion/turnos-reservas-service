package ar.edu.um.turnos.config.catalogo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Error HTTP que devolvio el servicio de catalogo al consultar la agenda
 * vigente de un profesional (#16). Lleva el {@code status} y, si el cuerpo
 * era {@code application/problem+json} y lo incluia, el {@code code} y el
 * {@code detail} (docs/contratos/catalogo-turnos.md#6 del repo
 * turnos-catalogo-sync: "decidir por status y code, nunca por el texto de
 * detail"). La traduccion de {@code code} a un error propio y tipado vive en
 * {@link CatalogoClientErrorException#getErrorCode()}: esta clase base solo
 * guarda los datos crudos que llegaron.
 */
@Getter
public abstract class CatalogoHttpException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;
    private final String detail;

    protected CatalogoHttpException(HttpStatusCode status, String code, String detail) {
        super("El catalogo respondio %s%s".formatted(status, code != null ? " (" + code + ")" : ""));
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

}
