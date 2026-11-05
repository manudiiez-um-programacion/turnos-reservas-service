package ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catalogo.dto.HorarioSemanalVigente;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import lombok.extern.slf4j.Slf4j;

/**
 * Traduce los horarios semanales vigentes que informa el catalogo (#16) al
 * modelo de dominio de este slice (#24). El contrato solo publica los
 * horarios habilitados (ver {@link HorarioSemanalVigente}), asi que
 * {@code enabled} siempre queda en {@code true}.
 *
 * <p>Un {@code dayOfWeek} que no coincide con ninguna de las siete
 * constantes de {@link DayOfWeek} se descarta con un aviso en el log en vez
 * de romper el mapeo completo de la agenda: es el mismo criterio de
 * robustez que el resto del proyecto aplica a un valor inesperado de un
 * contrato externo.
 */
@Slf4j
@Component
public class ReglaHorariaSemanalCatalogoMapper {

    public List<ReglaHorariaSemanal> aDominio(List<HorarioSemanalVigente> horarios) {
        if (horarios == null) {
            return List.of();
        }
        return horarios.stream().map(this::aDominio).filter(Objects::nonNull).toList();
    }

    private ReglaHorariaSemanal aDominio(HorarioSemanalVigente horario) {
        DayOfWeek dayOfWeek = parsearDiaSemana(horario.dayOfWeek());
        if (dayOfWeek == null) {
            return null;
        }
        return new ReglaHorariaSemanal(dayOfWeek, horario.startTime(), horario.endTime(),
                horario.slotDurationMinutes(), true);
    }

    private DayOfWeek parsearDiaSemana(String dayOfWeek) {
        try {
            return DayOfWeek.valueOf(dayOfWeek);
        } catch (IllegalArgumentException | NullPointerException noValido) {
            log.warn("El catalogo informo un dayOfWeek no reconocido: '{}'", dayOfWeek);
            return null;
        }
    }

}
