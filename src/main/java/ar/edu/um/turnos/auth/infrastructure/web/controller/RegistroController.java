package ar.edu.um.turnos.auth.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.auth.application.service.RegistroService;
import ar.edu.um.turnos.auth.infrastructure.web.dto.RegistroRequest;
import ar.edu.um.turnos.auth.infrastructure.web.mapper.RegistroWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Alta de usuarios finales. Endpoint publico (junto con el login de #10):
 * todo lo demas exige JWT desde #11.
 *
 * <p>Devuelve {@code 201} sin cuerpo, igual que
 * {@code AccountResource#registerAccount} en JHipster: el cliente se
 * autentica despues, en #10, para obtener el JWT.
 */
@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro y autenticacion de usuarios finales")
public class RegistroController {

    private final RegistroService registroService;
    private final RegistroWebMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un usuario final")
    public void registrar(@Valid @RequestBody RegistroRequest request) {
        registroService.registrar(mapper.toSolicitud(request));
    }

}
