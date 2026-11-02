package ar.edu.um.turnos.auth.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.auth.application.service.AuthService;
import ar.edu.um.turnos.auth.infrastructure.web.dto.AutenticacionRequest;
import ar.edu.um.turnos.auth.infrastructure.web.dto.TokenResponse;
import ar.edu.um.turnos.auth.infrastructure.web.mapper.AutenticacionWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Login de usuarios finales. Endpoint publico (junto con el registro de #9):
 * todo lo demas exige JWT desde #11.
 *
 * <p>Responde igual que {@code UserJWTController} en JHipster: {@code 200},
 * el token en el header {@code Authorization: Bearer <token>} y tambien en
 * el cuerpo ({@code id_token}), para que el cliente pueda usar cualquiera de
 * los dos sin que este servicio tenga que adivinar cual prefiere.
 */
@RestController
@RequestMapping("/api/authenticate")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro y autenticacion de usuarios finales")
public class AutenticacionController {

    private final AuthService authService;
    private final AutenticacionWebMapper mapper;

    @PostMapping
    @Operation(summary = "Autenticar un usuario final y emitir su JWT")
    public ResponseEntity<TokenResponse> autenticar(@Valid @RequestBody AutenticacionRequest request) {
        String token = authService.autenticar(mapper.toSolicitud(request));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        return ResponseEntity.ok().headers(headers).body(new TokenResponse(token));
    }

}
