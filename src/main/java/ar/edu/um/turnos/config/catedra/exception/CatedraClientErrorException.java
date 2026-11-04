package ar.edu.um.turnos.config.catedra.exception;

import org.springframework.http.HttpStatusCode;

/**
 * La catedra rechazo la operacion por un error funcional (4xx): datos
 * invalidos, un slot ya tomado, un hold vencido, etc. (matriz de §13.1 de la
 * referencia). No es un problema de infraestructura y repetir la misma
 * llamada sin cambiar nada no va a dar otro resultado.
 */
public class CatedraClientErrorException extends CatedraHttpException {

    public CatedraClientErrorException(HttpStatusCode status, String code, String detail) {
        super(status, code, detail);
    }

}
