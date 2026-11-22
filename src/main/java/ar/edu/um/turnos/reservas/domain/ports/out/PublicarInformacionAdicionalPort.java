package ar.edu.um.turnos.reservas.domain.ports.out;

import ar.edu.um.turnos.reservas.domain.model.InformacionAdicionalParaEnviar;

/**
 * Puerto de salida: publicar {@code AdditionalInformationSubmitted} en
 * {@code catedra.turnos.telefono.{groupId}} (#19, §15.5 de la referencia).
 *
 * <p>El dominio no sabe que hay un broker de Kafka, un {@code eventId} propio
 * por generar ni un formato JSON exacto que respetar: solo que la cátedra
 * necesita que se le informe el teléfono. La implementación vive en
 * {@code infrastructure/messaging}.
 */
public interface PublicarInformacionAdicionalPort {

    /**
     * Publica el evento. Si la publicación falla, lanza en lugar de devolver
     * silenciosamente: un fallo de envío nunca se da por exitoso.
     */
    void publicar(InformacionAdicionalParaEnviar informacion);

}
