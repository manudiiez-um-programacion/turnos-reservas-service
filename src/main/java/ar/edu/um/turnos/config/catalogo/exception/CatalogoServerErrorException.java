package ar.edu.um.turnos.config.catalogo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * El catalogo (o su infraestructura) no pudo responder la agenda vigente: un
 * 503 ({@code CATALOG_NOT_SYNCHRONIZED}, "todavia no hay un catalogo
 * aplicado", transitorio mientras termina de sincronizar) o un 500 (error
 * interno no esperado, sin {@code code} garantizado). En los dos casos la
 * operacion es una consulta de solo lectura sin efectos secundarios, asi que
 * reintentarla mas tarde tiene sentido. {@link #esNoSincronizado()} distingue
 * el 503, que el contrato documenta como el unico de los dos que recomienda
 * reintentar con backoff.
 *
 * <p>No traduce {@code code} a {@link CatalogoErrorCode}: a diferencia de
 * {@link CatalogoClientErrorException}, esta clase cubre errores de
 * infraestructura, no funcionales, y el 500 no garantiza {@code code}.
 */
public class CatalogoServerErrorException extends CatalogoHttpException {

    public CatalogoServerErrorException(HttpStatusCode status, String code, String detail) {
        super(status, code, detail);
    }

    public boolean esNoSincronizado() {
        return getStatus().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
