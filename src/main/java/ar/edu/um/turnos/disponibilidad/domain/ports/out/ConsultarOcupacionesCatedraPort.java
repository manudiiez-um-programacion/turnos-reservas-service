package ar.edu.um.turnos.disponibilidad.domain.ports.out;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.turnos.disponibilidad.domain.model.Ocupacion;

/**
 * Puerto de salida: pide a la catedra las ocupaciones (reservas confirmadas y
 * holds activos) de un profesional en un rango de fechas. El dominio no sabe
 * que la respuesta llega por HTTP ni como se traduce un error: solo que
 * {@code from} posterior a {@code to} puede volver como un rango invalido
 * (#22, §8 y §13.1 de la referencia).
 */
public interface ConsultarOcupacionesCatedraPort {

    List<Ocupacion> consultar(Long professionalId, LocalDate from, LocalDate to);

}
