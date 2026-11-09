package ar.edu.um.turnos.disponibilidad.infrastructure.client.adapter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.config.catedra.exception.CatedraErrorCode;
import ar.edu.um.turnos.disponibilidad.application.exception.RangoFechasInvalidoException;
import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.turnos.disponibilidad.domain.ports.out.ConsultarOcupacionesCatedraPort;
import ar.edu.um.turnos.disponibilidad.infrastructure.client.mapper.OcupacionCatedraMapper;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador del puerto de salida: pide las ocupaciones a {@link CatedraClient}
 * (#14) y traduce la respuesta al modelo de dominio (#22).
 *
 * <p>{@code INVALID_DATE_RANGE} ya se rechaza antes de llamar (ver
 * {@code ObtenerOcupacionesUseCaseImpl}), pero si la catedra lo informa
 * igual se traduce al mismo {@link RangoFechasInvalidoException}: para quien
 * use este puerto es el mismo error de dominio, sin importar en que capa se
 * detecto. Cualquier otro codigo de {@link CatedraClientErrorException} (o
 * un error de infraestructura, que {@link CatedraClient} ya reintenta) se
 * deja propagar sin traducir: no es responsabilidad de este slice.
 */
@Component
@RequiredArgsConstructor
public class ConsultarOcupacionesCatedraAdapter implements ConsultarOcupacionesCatedraPort {

    private final CatedraClient catedraClient;
    private final OcupacionCatedraMapper ocupacionCatedraMapper;

    @Override
    public List<Ocupacion> consultar(Long professionalId, LocalDate from, LocalDate to) {
        try {
            var respuesta = catedraClient.obtenerOcupaciones(professionalId, from, to);
            return ocupacionCatedraMapper.aDominio(respuesta.occupancies());
        } catch (CatedraClientErrorException error) {
            if (error.getErrorCode() == CatedraErrorCode.INVALID_DATE_RANGE) {
                throw new RangoFechasInvalidoException(from, to, error);
            }
            throw error;
        }
    }

}
