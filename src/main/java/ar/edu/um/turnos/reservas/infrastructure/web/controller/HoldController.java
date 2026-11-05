package ar.edu.um.turnos.reservas.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.ConfirmarHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.CrearHoldRequest;
import ar.edu.um.turnos.reservas.infrastructure.web.dto.HoldResponse;
import ar.edu.um.turnos.reservas.infrastructure.web.mapper.HoldWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Bloqueo temporal de un slot y su confirmacion inicial (#27, #28, punto 9
 * del alcance obligatorio del enunciado). Protegido por el JWT de usuario
 * como cualquier endpoint de negocio (#11): el {@code externalPatientId} y
 * el {@code usuarioId} que se usan se derivan siempre del {@code login}
 * autenticado ({@code sub} del token), resuelto via {@link AuthService} —
 * nunca de un valor que mande el cliente (§4.2 del enunciado).
 */
@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
@Tag(name = "Holds", description = "Bloqueo temporal de un slot elegido y su confirmacion inicial")
public class HoldController {

    private final ReservasService reservasService;
    private final AuthService authService;
    private final HoldWebMapper holdWebMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear el hold temporal de un slot")
    public HoldResponse crear(@Valid @RequestBody CrearHoldRequest request, Authentication authentication) {
        Usuario usuario = authService.obtenerPorLogin(authentication.getName());
        ProcesoReserva proceso = reservasService.crearHold(holdWebMapper.toSolicitud(request, usuario));
        return holdWebMapper.toResponse(proceso);
    }

    @PostMapping("/{holdId}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Confirmar inicialmente el hold de un slot")
    public HoldResponse confirmar(
            @PathVariable String holdId, @Valid @RequestBody ConfirmarHoldRequest request, Authentication authentication) {
        Usuario usuario = authService.obtenerPorLogin(authentication.getName());
        ProcesoReserva proceso =
                reservasService.confirmarHold(holdWebMapper.toSolicitudConfirmacion(holdId, request, usuario));
        return holdWebMapper.toResponse(proceso);
    }

}
