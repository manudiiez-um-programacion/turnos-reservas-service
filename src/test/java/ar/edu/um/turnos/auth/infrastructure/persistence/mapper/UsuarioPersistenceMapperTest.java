package ar.edu.um.turnos.auth.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.infrastructure.persistence.entity.UsuarioJpaEntity;

class UsuarioPersistenceMapperTest {

    private final UsuarioPersistenceMapper mapper = new UsuarioPersistenceMapper();

    private Usuario usuarioCompleto() {
        return Usuario.builder()
                .id(1L)
                .externalPatientId("ext-123")
                .login("ana.gomez")
                .passwordHash("{argon2}hash")
                .firstName("Ana")
                .lastName("Gomez")
                .email("ana.gomez@ejemplo.com")
                .imageUrl("https://ejemplo.com/foto.png")
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    void mapeaUnUsuarioDeDominioAEntidadCampoACampo() {
        UsuarioJpaEntity entidad = mapper.toEntity(usuarioCompleto());

        assertThat(entidad.getId()).isEqualTo(1L);
        assertThat(entidad.getExternalPatientId()).isEqualTo("ext-123");
        assertThat(entidad.getLogin()).isEqualTo("ana.gomez");
        assertThat(entidad.getPasswordHash()).isEqualTo("{argon2}hash");
        assertThat(entidad.getFirstName()).isEqualTo("Ana");
        assertThat(entidad.getLastName()).isEqualTo("Gomez");
        assertThat(entidad.getEmail()).isEqualTo("ana.gomez@ejemplo.com");
        assertThat(entidad.getImageUrl()).isEqualTo("https://ejemplo.com/foto.png");
        assertThat(entidad.getLangKey()).isEqualTo("es");
        assertThat(entidad.isActivated()).isTrue();
        assertThat(entidad.getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void mapeaUnaEntidadADominioCampoACampo() {
        UsuarioJpaEntity entidad = mapper.toEntity(usuarioCompleto());

        Usuario usuario = mapper.toDomain(entidad);

        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getExternalPatientId()).isEqualTo("ext-123");
        assertThat(usuario.getLogin()).isEqualTo("ana.gomez");
        assertThat(usuario.getPasswordHash()).isEqualTo("{argon2}hash");
        assertThat(usuario.getFirstName()).isEqualTo("Ana");
        assertThat(usuario.getLastName()).isEqualTo("Gomez");
        assertThat(usuario.getEmail()).isEqualTo("ana.gomez@ejemplo.com");
        assertThat(usuario.getImageUrl()).isEqualTo("https://ejemplo.com/foto.png");
        assertThat(usuario.getLangKey()).isEqualTo("es");
        assertThat(usuario.isActivated()).isTrue();
        assertThat(usuario.getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void devuelveNullAlMapearNullHaciaEntidadODominio() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void usaUnConjuntoVacioCuandoLasAutoridadesSonNulas() {
        Usuario sinAutoridades = usuarioCompleto();
        sinAutoridades.setAuthorities(null);

        UsuarioJpaEntity entidad = mapper.toEntity(sinAutoridades);
        assertThat(entidad.getAuthorities()).isEmpty();

        entidad.setAuthorities(null);
        Usuario usuario = mapper.toDomain(entidad);
        assertThat(usuario.getAuthorities()).isEmpty();
    }

}
