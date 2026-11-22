package ar.edu.um.turnos.reservas.application.usecases;

import java.util.Objects;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ConsultarReservasUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso (#33): delega la busqueda en el
 * repositorio, con la paginacion saneada para que un valor fuera de rango
 * desde el borde web no se traduzca en una consulta sin limite ni en una
 * pagina negativa.
 */
@Component
@RequiredArgsConstructor
public class ConsultarReservasUseCaseImpl implements ConsultarReservasUseCase {

    private static final int TAMANIO_PAGINA_DEFAULT = 20;

    private static final int TAMANIO_PAGINA_MAXIMO = 50;

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Override
    public PaginaProcesoReserva consultar(FiltroReservasUsuario filtro) {
        Objects.requireNonNull(filtro, "el filtro no puede ser null");
        Objects.requireNonNull(filtro.usuarioId(), "el usuarioId no puede ser null");

        FiltroReservasUsuario filtroSaneado = filtro.toBuilder()
                .pagina(Math.max(filtro.pagina(), 0))
                .tamanio(sanearTamanio(filtro.tamanio()))
                .build();

        return procesoReservaRepositoryPort.buscarPorUsuario(filtroSaneado);
    }

    private int sanearTamanio(int tamanio) {
        if (tamanio <= 0) {
            return TAMANIO_PAGINA_DEFAULT;
        }
        return Math.min(tamanio, TAMANIO_PAGINA_MAXIMO);
    }

}
