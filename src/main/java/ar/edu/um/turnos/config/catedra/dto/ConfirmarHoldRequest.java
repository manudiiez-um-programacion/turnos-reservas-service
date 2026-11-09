package ar.edu.um.turnos.config.catedra.dto;

/** Body de {@code POST /api/appointment-holds/{holdId}/confirm} (§10). */
public record ConfirmarHoldRequest(

        String externalPatientId,

        String patientFirstName,

        String patientLastName

) {
}
