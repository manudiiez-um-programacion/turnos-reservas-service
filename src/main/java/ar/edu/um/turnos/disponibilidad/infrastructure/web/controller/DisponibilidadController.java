package ar.edu.um.turnos.disponibilidad.infrastructure.web.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.disponibilidad.application.service.DisponibilidadService;
import ar.edu.um.turnos.disponibilidad.domain.model.Slot;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.dto.DisponibilidadResponse;
import ar.edu.um.turnos.disponibilidad.infrastructure.web.mapper.DisponibilidadWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Turnos disponibles de un profesional para la app KMP (#24, §7 y punto 8
 * del alcance obligatorio del enunciado). Combina, en cada invocacion y sin
 * cachear ninguna de las dos, la agenda vigente del catalogo (#16) con las
 * ocupaciones de la catedra (#22): un slot libre en esta respuesta puede
 * dejar de estarlo al pedir el hold, que es comportamiento esperado (#27).
 *
 * <p>Protegido por el JWT de usuario como cualquier endpoint de negocio
 * (#11): la disponibilidad de un profesional no es un dato propio de quien
 * consulta, asi que alcanza con estar autenticado.
 */
@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
@Tag(name = "Disponibilidad", description = "Turnos disponibles de un profesional para una fecha")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final DisponibilidadWebMapper disponibilidadWebMapper;

    @GetMapping
    @Operation(summary = "Turnos disponibles de un profesional para una fecha puntual")
    public DisponibilidadResponse obtenerDisponibilidad(
            @Parameter(description = "Id del profesional, segun la copia local del catalogo")
            @RequestParam Long professionalId,
            @Parameter(description = "Fecha puntual a consultar (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        List<Slot> slots = disponibilidadService.obtenerTurnosDisponibles(professionalId, date);
        return disponibilidadWebMapper.toResponse(professionalId, date, slots);
    }

}
