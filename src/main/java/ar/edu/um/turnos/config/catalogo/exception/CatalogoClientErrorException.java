package ar.edu.um.turnos.config.catalogo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * El catalogo rechazo la consulta de agenda vigente por un error funcional
 * (401, 403, 404 o 409): token invalido o sin autoridad, profesional
 * inexistente o deshabilitado. No es un problema de infraestructura y
 * repetir la misma llamada sin cambiar nada no va a dar otro resultado.
 *
 * <p>{@link #getErrorCode()} traduce el {@code code} crudo a
 * {@link CatalogoErrorCode}: {@link CatalogoErrorCode#DESCONOCIDO} cuando no
 * vino {@code code} (401/403 directos de Spring Security) o cuando vino uno
 * que este enum todavia no tiene mapeado. En los dos casos sigue disponible
 * el {@code code} crudo heredado, por si hace falta para diagnostico.
 */
@Getter
public class CatalogoClientErrorException extends CatalogoHttpException {

    private final CatalogoErrorCode errorCode;

    public CatalogoClientErrorException(HttpStatusCode status, String code, String detail) {
        super(status, code, detail);
        this.errorCode = CatalogoErrorCode.desde(code);
    }

}
