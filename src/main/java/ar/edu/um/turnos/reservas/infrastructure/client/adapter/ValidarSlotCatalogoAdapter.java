package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catalogo.CatalogoClient;
import ar.edu.um.turnos.config.catalogo.dto.AgendaProfesionalResponse;
import ar.edu.um.turnos.config.catalogo.dto.HorarioSemanalVigente;
import ar.edu.um.turnos.config.catalogo.dto.ProfesionalVigente;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoClientErrorException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoErrorCode;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.reservas.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.SlotInvalidoException;
import ar.edu.um.turnos.reservas.domain.model.ProfesionalParaHold;
import ar.edu.um.turnos.reservas.domain.ports.out.ValidarSlotCatalogoPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: pide la agenda vigente a
 * {@link CatalogoClient} (#16) y valida localmente que la fecha y hora
 * pedidas correspondan a un slot habilitado, antes de llamar a la catedra
 * (#27). El contrato solo publica horarios habilitados (ver
 * {@link HorarioSemanalVigente}), asi que no hace falta filtrar por
 * {@code enabled}.
 *
 * <p>{@code PROFESSIONAL_NOT_FOUND} y {@code PROFESSIONAL_DISABLED} se
 * traducen a las excepciones propias de este slice: el resto de los
 * {@link CatalogoClientErrorException} se dejan propagar sin traducir, igual
 * que {@code CatalogoNoDisponibleException} (#17), que ya tiene su propia
 * traduccion global a {@code application/problem+json}.
 */
@Component
@RequiredArgsConstructor
public class ValidarSlotCatalogoAdapter implements ValidarSlotCatalogoPort {

    private final CatalogoClient catalogoClient;

    @Override
    public ProfesionalParaHold validar(Long professionalId, LocalDate fecha, LocalTime horaInicio) {
        AgendaProfesionalResponse agenda = obtenerAgenda(professionalId);

        if (!esSlotHabilitado(agenda.schedules(), fecha, horaInicio)) {
            throw new SlotInvalidoException(professionalId, fecha, horaInicio);
        }

        ProfesionalVigente profesional = agenda.professional();
        return new ProfesionalParaHold(profesional.firstName(), profesional.lastName());
    }

    private AgendaProfesionalResponse obtenerAgenda(Long professionalId) {
        try {
            return catalogoClient.obtenerAgendaVigente(professionalId);
        } catch (CatalogoClientErrorException error) {
            if (error.getErrorCode() == CatalogoErrorCode.PROFESSIONAL_NOT_FOUND) {
                throw new ProfesionalNoEncontradoException(professionalId, error);
            }
            if (error.getErrorCode() == CatalogoErrorCode.PROFESSIONAL_DISABLED) {
                throw new ProfesionalDeshabilitadoException(professionalId, error);
            }
            throw error;
        }
    }

    private boolean esSlotHabilitado(List<HorarioSemanalVigente> schedules, LocalDate fecha, LocalTime horaInicio) {
        if (schedules == null) {
            return false;
        }
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        return schedules.stream()
                .filter(horario -> diaSemana.toString().equals(horario.dayOfWeek()))
                .anyMatch(horario -> seAlineaConElHorario(horario, horaInicio));
    }

    /**
     * Misma aritmetica de {@code GenerarSlotsUseCaseImpl} (#23) pero sin
     * generar la lista completa: alcanza con verificar que
     * {@code horaInicio} sea uno de los puntos que esa expansion produciria.
     * En minutos enteros desde medianoche para no depender de la aritmetica
     * de {@link LocalTime}, que da vueltas a las 24 horas.
     */
    private boolean seAlineaConElHorario(HorarioSemanalVigente horario, LocalTime horaInicio) {
        int inicioMinutos = horario.startTime().toSecondOfDay() / 60;
        int finMinutos = horario.endTime().toSecondOfDay() / 60;
        int duracion = horario.slotDurationMinutes();
        int horaInicioMinutos = horaInicio.toSecondOfDay() / 60;

        if (horaInicioMinutos < inicioMinutos || horaInicioMinutos + duracion > finMinutos) {
            return false;
        }
        return (horaInicioMinutos - inicioMinutos) % duracion == 0;
    }

}
