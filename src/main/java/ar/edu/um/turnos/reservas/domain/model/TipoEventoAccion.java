package ar.edu.um.turnos.reservas.domain.model;

/**
 * Vocabulario propio para los eventos que la catedra publica en
 * {@code alumnos.turnos.acciones.{groupId}} (§15.2 y §15.4-§15.10 de la
 * referencia).
 *
 * <p>{@link #DESCONOCIDO} es el valor de resguardo para un {@code eventType}
 * que este enum todavia no conoce: la entrega es al menos una vez y el
 * contrato permite campos nuevos dentro de v1 (§15.1), asi que un tipo nuevo
 * no puede romper el consumo (#18); simplemente se registra y se descarta.
 */
public enum TipoEventoAccion {

    SOLICITUD_INFORMACION_ADICIONAL,
    RESERVA_CONFIRMADA,
    INFORMACION_ADICIONAL_RECHAZADA,
    PROCESO_VENCIDO,
    PROCESO_INVALIDO,
    RESERVA_CANCELADA,
    DESCONOCIDO;

    public static TipoEventoAccion desde(String eventType) {
        if (eventType == null) {
            return DESCONOCIDO;
        }
        return switch (eventType) {
            case "AdditionalInformationRequested" -> SOLICITUD_INFORMACION_ADICIONAL;
            case "AppointmentConfirmed" -> RESERVA_CONFIRMADA;
            case "AdditionalInformationRejected" -> INFORMACION_ADICIONAL_RECHAZADA;
            case "AppointmentProcessExpired" -> PROCESO_VENCIDO;
            case "AppointmentProcessInvalid" -> PROCESO_INVALIDO;
            case "AppointmentCancelled" -> RESERVA_CANCELADA;
            default -> DESCONOCIDO;
        };
    }

}
