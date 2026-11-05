package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.HoldCreado;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProfesionalParaHold;
import ar.edu.um.turnos.reservas.domain.model.SolicitudHold;
import ar.edu.um.turnos.reservas.domain.ports.out.CrearHoldCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ValidarSlotCatalogoPort;

@ExtendWith(MockitoExtension.class)
class CrearHoldUseCaseImplTest {

    @Mock
    private ValidarSlotCatalogoPort validarSlotCatalogoPort;

    @Mock
    private CrearHoldCatedraPort crearHoldCatedraPort;

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private CrearHoldUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CrearHoldUseCaseImpl(validarSlotCatalogoPort, crearHoldCatedraPort, procesoReservaRepositoryPort);
    }

    @Test
    void validaElSlotPideElHoldYPersisteElProcesoNuevoComoHoldCreado() {
        SolicitudHold solicitud = SolicitudHold.builder()
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .usuarioId(7L)
                .externalPatientId("user-42")
                .build();
        when(validarSlotCatalogoPort.validar(15L, solicitud.fecha(), solicitud.horaInicio()))
                .thenReturn(new ProfesionalParaHold("Ana", "Gomez"));
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        when(crearHoldCatedraPort.crear(solicitud))
                .thenReturn(new HoldCreado("hold-6c0c2f41", "process-a92b47d0", expiresAt));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ProcesoReserva proceso = useCase.crear(solicitud);

        assertThat(proceso.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(proceso.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(proceso.getUsuarioId()).isEqualTo(7L);
        assertThat(proceso.getProfessionalId()).isEqualTo(15L);
        assertThat(proceso.getProfessionalNombreHistorico()).isEqualTo("Ana");
        assertThat(proceso.getProfessionalApellidoHistorico()).isEqualTo("Gomez");
        assertThat(proceso.getFecha()).isEqualTo(solicitud.fecha());
        assertThat(proceso.getHoraInicio()).isEqualTo(solicitud.horaInicio());
        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.HOLD_CREADO);
        assertThat(proceso.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(proceso.getHistorial()).hasSize(1);
        assertThat(proceso.getHistorial().get(0).estado()).isEqualTo(EstadoProcesoReserva.HOLD_CREADO);
    }

}
