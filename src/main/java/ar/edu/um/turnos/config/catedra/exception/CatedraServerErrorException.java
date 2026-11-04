package ar.edu.um.turnos.config.catedra.exception;

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
 */
public class CatedraServerErrorException extends CatedraHttpException {

    public CatedraServerErrorException(HttpStatusCode status, String code, String detail) {
        super(status, code, detail);
    }

    public boolean esServicioNoDisponible() {
        return getStatus().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
