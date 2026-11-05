package ar.edu.um.turnos.disponibilidad.infrastructure.client.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catalogo.CatalogoClient;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoClientErrorException;
import ar.edu.um.turnos.config.catalogo.exception.CatalogoErrorCode;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarAgendaVigenteCatalogoPort;
import ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper.ReglaHorariaSemanalCatalogoMapper;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: pide la agenda vigente a
 * {@link CatalogoClient} (#16) y traduce la respuesta al modelo de dominio
 * (#24).
 *
 * <p>{@code PROFESSIONAL_NOT_FOUND} y {@code PROFESSIONAL_DISABLED} se
 * traducen a sus excepciones de dominio propias: el resto de los
 * {@link CatalogoClientErrorException} (401/403, que no deberian darse con
 * el JWT tecnico interno) se dejan propagar sin traducir, igual que
 * {@code CatalogoNoDisponibleException} (#17), que ya tiene su propia
 * traduccion global a {@code application/problem+json}.
 */
@Component
@RequiredArgsConstructor
public class ConsultarAgendaVigenteCatalogoAdapter implements ConsultarAgendaVigenteCatalogoPort {

    private final CatalogoClient catalogoClient;
    private final ReglaHorariaSemanalCatalogoMapper reglaHorariaSemanalCatalogoMapper;

    @Override
    public List<ReglaHorariaSemanal> consultar(Long professionalId) {
        try {
            var agenda = catalogoClient.obtenerAgendaVigente(professionalId);
            return reglaHorariaSemanalCatalogoMapper.aDominio(agenda.schedules());
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

}
