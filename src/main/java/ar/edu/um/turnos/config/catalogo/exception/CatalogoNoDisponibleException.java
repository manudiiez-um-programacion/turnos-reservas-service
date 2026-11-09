package ar.edu.um.turnos.config.catalogo.exception;

/**
 * El catalogo no respondio tras agotar los reintentos de #16 (#17): un
 * timeout, una conexion rechazada ({@link org.springframework.web.client.ResourceAccessException})
 * o un {@link CatalogoServerErrorException} persistente. A diferencia de
 * {@link CatalogoClientErrorException}, no hay nada mal en el pedido: es el
 * catalogo el que no esta disponible ahora, asi que cualquier operacion que
 * dependa de el (cuando exista, M9/M10) tiene que fallar rapido con este
 * error en vez de colgarse o propagar la excepcion tecnica cruda.
 *
 * <p>La causa original queda encadenada para diagnostico, pero el cliente de
 * este servicio no la ve: {@code CatalogoExceptionHandler} la traduce a un
 * {@code application/problem+json} con un unico {@code code} estable
 * ({@code CATALOGO_NO_DISPONIBLE}), sin importar si el motivo de fondo fue
 * de red o de infraestructura del catalogo.
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

}
