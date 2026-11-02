package ar.edu.um.turnos.auth.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.infrastructure.web.dto.AutenticacionRequest;

class AutenticacionWebMapperTest {

    private final AutenticacionWebMapper mapper = new AutenticacionWebMapper();

    @Test
    void mapeaElRequestCampoACampo() {
        AutenticacionRequest request = new AutenticacionRequest("ana.gomez", "una-contrasenia");

        SolicitudAutenticacion solicitud = mapper.toSolicitud(request);

        assertThat(solicitud.getLogin()).isEqualTo("ana.gomez");
        assertThat(solicitud.getPassword()).isEqualTo("una-contrasenia");
    }

    @Test
    void devuelveNullAlMapearUnRequestNull() {
        assertThat(mapper.toSolicitud(null)).isNull();
    }

}
