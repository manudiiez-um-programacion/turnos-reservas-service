package ar.edu.um.turnos.reservas.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.application.exception.EstadoReservaInvalidoException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservaResponse;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservasPaginaResponse;

class ReservaWebMapperTest {

    private final ReservaWebMapper mapper = new ReservaWebMapper();

    @Test
    void traduceLosParametrosDeFiltroAlDominio() {
        FiltroReservasUsuario filtro = mapper.toFiltro(
                7L, "CONFIRMADA", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 2, 10);

        assertThat(filtro).isEqualTo(FiltroReservasUsuario.builder()
                .usuarioId(7L)
                .estado(EstadoProcesoReserva.CONFIRMADA)
                .desde(LocalDate.of(2026, 9, 1))
                .hasta(LocalDate.of(2026, 9, 30))
                .pagina(2)
                .tamanio(10)
                .build());
    }

    @Test
    void unStatusNuloNoFiltraPorEstado() {
        FiltroReservasUsuario filtro = mapper.toFiltro(7L, null, null, null, 0, 20);

        assertThat(filtro.estado()).isNull();
    }

    @Test
    void esInsensibleAMayusculasYMinusculasEnElStatus() {
        FiltroReservasUsuario filtro = mapper.toFiltro(7L, "confirmada", null, null, 0, 20);

        assertThat(filtro.estado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
    }

    @Test
    void rechazaUnStatusQueNoEsUnEstadoValido() {
        assertThatThrownBy(() -> mapper.toFiltro(7L, "NO_EXISTE", null, null, 0, 20))
                .isInstanceOf(EstadoReservaInvalidoException.class);
    }

    @Test
    void traduceElProcesoDeDominioALaRespuesta() {
        Instant expiresAt = Instant.parse("2026-09-10T14:44:50Z");
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .estado(EstadoProcesoReserva.CONFIRMADA)
                .professionalId(15L)
                .professionalNombreHistorico("Juan")
                .professionalApellidoHistorico("Perez")
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .expiresAt(expiresAt)
                .reservationId(8251L)
                .build();

        ReservaResponse respuesta = mapper.toResponse(proceso);

        assertThat(respuesta).isEqualTo(new ReservaResponse(
                "hold-6c0c2f41", "process-a92b47d0", "CONFIRMADA", 15L, "Juan", "Perez",
                LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), expiresAt, 8251L, null, null));
    }

    @Test
    void traduceLaPaginaDeDominioALaRespuesta() {
        ProcesoReserva proceso = ProcesoReserva.builder().holdId("hold-1").estado(EstadoProcesoReserva.CONFIRMADA).build();
        PaginaProcesoReserva pagina = new PaginaProcesoReserva(List.of(proceso), 5L);

        ReservasPaginaResponse respuesta = mapper.toPaginaResponse(pagina, 1, 10);

        assertThat(respuesta.content()).hasSize(1);
        assertThat(respuesta.totalElements()).isEqualTo(5L);
        assertThat(respuesta.page()).isEqualTo(1);
        assertThat(respuesta.size()).isEqualTo(10);
    }

}
