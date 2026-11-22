package ar.edu.um.turnos.reservas.infrastructure.web.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CancelarReservaRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservaResponse;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ReservasPaginaResponse;
import ar.edu.um.turnos.reservas.infrastructure.web.mapper.ReservaWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Reservas y procesos de reserva propios del usuario autenticado (#33, #34,
 * §4.2 del enunciado: la asociacion con el usuario final es responsabilidad
 * de este servicio). El listado responde siempre desde el estado local —
 * nunca consulta a la catedra ni al catalogo — asi que sigue funcionando con
 * el catalogo caido (#17); la cancelacion si llama a la catedra, pero nunca
 * necesita el catalogo. El {@code usuarioId} que filtra o cancela se deriva
 * siempre del {@code login} autenticado, nunca de un parametro del cliente:
 * no existe forma de pedir ni cancelar las reservas de otro usuario por
 * estos endpoints.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reservas y procesos de reserva propios del usuario autenticado")
public class ReservaController {

    private final ReservasService reservasService;
    private final AuthService authService;
    private final ReservaWebMapper reservaWebMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Listar las reservas y procesos de reserva propios del usuario autenticado")
    public ReservasPaginaResponse listarPropias(
            @Parameter(description = "Estado local del proceso de reserva (EstadoProcesoReserva)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Fecha desde (inclusive, yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha hasta (inclusive, yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Usuario usuario = authService.obtenerPorLogin(authentication.getName());
        FiltroReservasUsuario filtro = reservaWebMapper.toFiltro(usuario.getId(), status, from, to, page, size);
        PaginaProcesoReserva pagina = reservasService.consultarReservas(filtro);
        return reservaWebMapper.toPaginaResponse(pagina, page, size);
    }

    @PostMapping("/{reservationProcessId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cancelar una reserva confirmada propia")
    public ReservaResponse cancelar(
            @PathVariable String reservationProcessId,
            @RequestBody(required = false) @Valid CancelarReservaRequest request,
            Authentication authentication) {
        Usuario usuario = authService.obtenerPorLogin(authentication.getName());
        ProcesoReserva proceso = reservasService.cancelarReserva(
                reservaWebMapper.toSolicitudCancelacion(reservationProcessId, request, usuario));
        return reservaWebMapper.toResponse(proceso);
    }

}
