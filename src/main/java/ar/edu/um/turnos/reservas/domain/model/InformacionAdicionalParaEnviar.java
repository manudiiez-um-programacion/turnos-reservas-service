package ar.edu.um.turnos.reservas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lo que un caso de uso necesita informar para publicar
 * {@code AdditionalInformationSubmitted} en
 * {@code catedra.turnos.telefono.{groupId}} (§15.5 de la referencia).
 *
 * <p>{@code telefono} llega ya validado y normalizado: esa logica es de #30,
 * no de este puerto. Lo mismo {@code requestEventId}, que tiene que ser el
 * {@code eventId} del {@code AdditionalInformationRequested} guardado en #29;
 * si no coincide, la catedra responde {@code REQUEST_EVENT_MISMATCH} (§15.9).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformacionAdicionalParaEnviar {

    private String reservationProcessId;

    private String holdId;

    private String requestEventId;

    private String telefono;

}
