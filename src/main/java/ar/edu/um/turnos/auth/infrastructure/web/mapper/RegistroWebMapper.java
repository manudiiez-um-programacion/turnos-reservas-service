package ar.edu.um.turnos.auth.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.SolicitudRegistroUsuario;
import ar.edu.um.turnos.auth.infrastructure.web.dto.RegistroRequest;

@Component
public class RegistroWebMapper {

    public SolicitudRegistroUsuario toSolicitud(RegistroRequest request) {
        if (request == null) {
            return null;
        }
        return SolicitudRegistroUsuario.builder()
                .login(request.login())
                .password(request.password())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .imageUrl(request.imageUrl())
                .langKey(request.langKey())
                .build();
    }

}
