package ar.edu.um.turnos.config.catedra.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * La catedra rechazo la operacion por un error funcional (4xx): datos
 * invalidos, un slot ya tomado, un hold vencido, etc. (matriz de §13.1 de la
 * referencia). No es un problema de infraestructura y repetir la misma
 * llamada sin cambiar nada no va a dar otro resultado.
 *
 * <p>{@link #getErrorCode()} traduce el {@code code} crudo a
 * {@link CatedraErrorCode}: {@link CatedraErrorCode#DESCONOCIDO} cuando no
 * vino {@code code} (401/403 directos de Spring Security) o cuando vino uno
 * que este enum todavia no tiene mapeado. En los dos casos sigue disponible
 * el {@code code} crudo heredado, por si hace falta para diagnostico.
 */
@Getter
public class CatedraClientErrorException extends CatedraHttpException {

    private final CatedraErrorCode errorCode;

    public CatedraClientErrorException(HttpStatusCode status, String code, String detail, List<CatedraFieldError> fieldErrors) {
        super(status, code, detail, fieldErrors);
        this.errorCode = CatedraErrorCode.desde(code);
    }

}
