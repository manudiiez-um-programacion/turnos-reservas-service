package ar.edu.um.turnos.config.catedra.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * La catedra (o su infraestructura) fallo del lado de ellos (5xx): §13.1 de
 * la referencia distingue el {@code 503} ("dependencia requerida no
 * disponible") del {@code 500} ("error interno no esperado"), pero en los dos
 * casos la operacion puede no haber tenido efecto y, a diferencia de un 4xx,
 * reintentarla mas tarde tiene sentido. {@link #esServicioNoDisponible()}
 * distingue el caso puntual del 503 para quien necesite tratarlo distinto del
 * 500.
 *
 * <p>Un 5xx no garantiza {@code code} (§13.1): a diferencia de
 * {@link CatedraClientErrorException}, esta clase no lo traduce a
 * {@link CatedraErrorCode} porque no es un error funcional, es uno de
 * infraestructura.
 */
public class CatedraServerErrorException extends CatedraHttpException {

    public CatedraServerErrorException(HttpStatusCode status, String code, String detail, List<CatedraFieldError> fieldErrors) {
        super(status, code, detail, fieldErrors);
    }

    public boolean esServicioNoDisponible() {
        return getStatus().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
