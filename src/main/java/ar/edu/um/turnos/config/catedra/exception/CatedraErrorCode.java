package ar.edu.um.turnos.config.catedra.exception;

/**
 * Codigo funcional estable que la catedra informa en el {@code code} de un
 * {@code application/problem+json}, traducido a un valor propio y tipado
 * (#15, §13.1 de la referencia) en lugar de que cada caso de uso futuro
 * (disponibilidad, holds, reservas) compare strings sueltos.
 *
 * <p>Solo incluye los codigos que pueden devolver los cinco endpoints que
 * consume {@code CatedraClient} (ocupaciones, crear/confirmar hold, listar y
 * cancelar reservas): los de registro de cuenta tecnica quedan fuera de
 * alcance porque este servicio no los usa.
 *
 * <p>{@link #DESCONOCIDO} es el valor de resguardo tanto para un {@code code}
 * ausente (401/403 directos de Spring Security, §13) como para uno que la
 * catedra agregue mas adelante y este enum todavia no conozca: en los dos
 * casos el cliente sigue funcionando, solo que sin una traduccion especifica.
 */
public enum CatedraErrorCode {

    VALIDATION_ERROR,
    INVALID_DATE_RANGE,
    PROFESSIONAL_NOT_FOUND,
    PROFESSIONAL_DISABLED,
    INVALID_SLOT,
    SLOT_ALREADY_RESERVED,
    SLOT_ALREADY_HELD,
    HOLD_NOT_FOUND,
    HOLD_EXPIRED,
    HOLD_OWNER_MISMATCH,
    EXTERNAL_PATIENT_MISMATCH,
    RESERVATION_PROCESS_ALREADY_CONFIRMED,
    RESERVATION_NOT_FOUND,
    RESERVATION_OWNER_MISMATCH,
    RESERVATION_NOT_CANCELLABLE,
    STUDENT_INTEGRATION_NOT_FOUND,
    DESCONOCIDO;

    public static CatedraErrorCode desde(String code) {
        if (code == null) {
            return DESCONOCIDO;
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException codigoNoReconocido) {
            return DESCONOCIDO;
        }
    }

}
