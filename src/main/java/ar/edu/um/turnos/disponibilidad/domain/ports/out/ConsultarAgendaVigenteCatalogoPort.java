package ar.edu.um.turnos.disponibilidad.domain.ports.out;

import java.util.List;

import ar.edu.um.turnos.disponibilidad.domain.model.ReglaHorariaSemanal;

/**
 * Puerto de salida: pide al servicio de catalogo las reglas horarias
 * semanales vigentes de un profesional (#16, #24). El dominio no sabe que
 * la respuesta llega por HTTP, solo que un profesional inexistente o
 * deshabilitado se informa como tal, nunca como una lista vacia (eso ultimo
 * es "no hay reglas para ese dia", un caso distinto).
 */
public interface ConsultarAgendaVigenteCatalogoPort {

    List<ReglaHorariaSemanal> consultar(Long professionalId);

}
