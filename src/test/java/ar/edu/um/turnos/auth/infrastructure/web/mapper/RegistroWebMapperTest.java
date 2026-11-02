package ar.edu.um.turnos.auth.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.infrastructure.web.dto.RegistroRequest;

class RegistroWebMapperTest {

    private final RegistroWebMapper mapper = new RegistroWebMapper();

    @Test
    void mapeaElRequestCampoACampo() {
        RegistroRequest request = new RegistroRequest(
                "ana.gomez", "una-contrasenia", "Ana", "Gomez", "ana@ejemplo.com", "https://ejemplo.com/foto.png", "es");

        SolicitudRegistroUsuario solicitud = mapper.toSolicitud(request);

        assertThat(solicitud.getLogin()).isEqualTo("ana.gomez");
        assertThat(solicitud.getPassword()).isEqualTo("una-contrasenia");
        assertThat(solicitud.getFirstName()).isEqualTo("Ana");
        assertThat(solicitud.getLastName()).isEqualTo("Gomez");
        assertThat(solicitud.getEmail()).isEqualTo("ana@ejemplo.com");
        assertThat(solicitud.getImageUrl()).isEqualTo("https://ejemplo.com/foto.png");
        assertThat(solicitud.getLangKey()).isEqualTo("es");
    }

    @Test
    void devuelveNullAlMapearUnRequestNull() {
        assertThat(mapper.toSolicitud(null)).isNull();
    }

}
