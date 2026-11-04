package ar.edu.um.turnos.auth.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.auth.domain.model.SolicitudAutenticacion;
import ar.edu.um.turnos.auth.infrastructure.web.dto.AutenticacionRequest;

@Component
public class AutenticacionWebMapper {

    public SolicitudAutenticacion toSolicitud(AutenticacionRequest request) {
        if (request == null) {
            return null;
        }
        return SolicitudAutenticacion.builder()
                .login(request.username())
                .password(request.password())
                .build();
    }

}
