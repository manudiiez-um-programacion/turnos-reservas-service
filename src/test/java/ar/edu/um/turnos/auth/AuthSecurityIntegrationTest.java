package ar.edu.um.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.auth.domain.model.Usuario;
import ar.edu.um.turnos.auth.domain.ports.out.UsuarioRepositoryPort;
import ar.edu.um.turnos.auth.infrastructure.config.PasswordEncoderConfig;

/**
 * Registro y login a traves de la cadena de seguridad REAL (#13), no
 * {@code @WithMockUser} ni el controlador aislado: lo que se prueba es que el
 * resource server de #11 y el hasheo Argon2id de #9 funcionan juntos, de
 * punta a punta.
 *
 * <p>Solo se mockea {@link UsuarioRepositoryPort}: es el unico borde que
 * necesita una base real, que este perfil no levanta (Testcontainers llega en
 * #41). Todo lo demas —{@code PasswordHasherAdapter} con el encoder Argon2id
 * de verdad, {@code JwtIssuerAdapter}, {@code AuthService}, los casos de uso y
 * {@code SecurityConfig}— corre sin mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Registro y login contra la cadena de seguridad real")
class AuthSecurityIntegrationTest {

    /** Los mismos valores que valida SecurityConfigTest, de application-test.properties. */
    private static final String SECRETO_USUARIO = "secreto-de-usuario-solo-para-pruebas-0001";
    private static final String ISSUER = "turnos-reservas-service-test";

    private static final PasswordEncoder ENCODER = new PasswordEncoderConfig().passwordEncoder();

    // No autowireado: este contexto no tiene un bean de ObjectMapper (ver
    // ProblemDetailResponses, que por eso tampoco lo autowirea). Una instancia
    // propia alcanza para leer un campo simple de la respuesta.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private String registroValido(String login, String email) {
        return """
                {
                  "login": "%s",
                  "password": "una-contrasenia-valida",
                  "firstName": "Ana",
                  "lastName": "Gomez",
                  "email": "%s",
                  "langKey": "es"
                }
                """.formatted(login, email);
    }

    @Nested
    @DisplayName("alta de usuarios")
    class AltaDeUsuarios {

        @Test
        @DisplayName("una solicitud valida responde 201 sin cuerpo y guarda la contrasenia hasheada con Argon2id")
        void altaCorrectaHasheaLaContrasenia() throws Exception {
            when(usuarioRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registroValido("ana.gomez", "ana.gomez@ejemplo.com")))
                    .andExpect(status().isCreated())
                    .andExpect(content -> assertThat(content.getResponse().getContentAsString()).isEmpty());

            var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepositoryPort).guardar(captor.capture());
            Usuario guardado = captor.getValue();

            assertThat(guardado.getPasswordHash())
                    .startsWith("{argon2}$argon2id$")
                    .isNotEqualTo("una-contrasenia-valida")
                    // un hash de BCrypt luce "{bcrypt}$2a$..."; esto descarta esa forma.
                    .doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("$2y$");
        }

        @Test
        @DisplayName("dos altas con la misma contrasenia producen hashes distintos")
        void dosAltasConLaMismaContraseniaProducenHashesDistintos() throws Exception {
            when(usuarioRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            String cuerpoA = registroValido("usuario.a", "usuario.a@ejemplo.com")
                    .replace("una-contrasenia-valida", "la-misma-contrasenia-123");
            String cuerpoB = registroValido("usuario.b", "usuario.b@ejemplo.com")
                    .replace("una-contrasenia-valida", "la-misma-contrasenia-123");

            mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(cuerpoA))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(cuerpoB))
                    .andExpect(status().isCreated());

            var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepositoryPort, org.mockito.Mockito.times(2)).guardar(captor.capture());

            List<Usuario> guardados = captor.getAllValues();
            assertThat(guardados.get(0).getPasswordHash()).isNotEqualTo(guardados.get(1).getPasswordHash());
        }

        @Test
        @DisplayName("un login ya existente responde 400 con code LOGIN_DUPLICADO, sin tocar la contrasenia")
        void loginDuplicadoNoGuardaNada() throws Exception {
            when(usuarioRepositoryPort.existeConLogin("ana.gomez")).thenReturn(true);

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registroValido("ana.gomez", "otro@ejemplo.com")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("LOGIN_DUPLICADO"));

            verify(usuarioRepositoryPort, org.mockito.Mockito.never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private Usuario usuarioConPassword(String password) {
            return Usuario.builder()
                    .id(1L)
                    .login("ana.gomez")
                    .passwordHash(ENCODER.encode(password))
                    .activated(true)
                    .authorities(Set.of("ROLE_USER"))
                    .build();
        }

        @Test
        @DisplayName("credenciales correctas devuelven un JWT verificable con el sub y las autoridades del usuario")
        void credencialesCorrectasDevuelvenUnJwtVerificable() throws Exception {
            when(usuarioRepositoryPort.buscarPorLogin("ana.gomez"))
                    .thenReturn(Optional.of(usuarioConPassword("una-contrasenia-correcta")));

            String respuesta = mockMvc.perform(post("/api/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "ana.gomez", "password": "una-contrasenia-correcta"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String token = OBJECT_MAPPER.readTree(respuesta).get("id_token").asText();

            SecretKey clave = Keys.hmacShaKeyFor(SECRETO_USUARIO.getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser().verifyWith(clave).requireIssuer(ISSUER).build().parseSignedClaims(token);

            assertThat(jws.getPayload().getSubject()).isEqualTo("ana.gomez");
            @SuppressWarnings("unchecked")
            List<String> autoridades = jws.getPayload().get("auth", List.class);
            assertThat(autoridades).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("una contrasenia incorrecta devuelve 401 sin revelar si el login existe")
        void contraseniaIncorrectaDevuelve401() throws Exception {
            when(usuarioRepositoryPort.buscarPorLogin("ana.gomez"))
                    .thenReturn(Optional.of(usuarioConPassword("una-contrasenia-correcta")));

            mockMvc.perform(post("/api/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "ana.gomez", "password": "una-contrasenia-equivocada"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("un login que no existe devuelve el mismo 401 que una contrasenia incorrecta")
        void loginInexistenteDevuelveElMismo401() throws Exception {
            when(usuarioRepositoryPort.buscarPorLogin(eq("no.existe"))).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "no.existe", "password": "cualquier-cosa-123"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
        }
    }

    @Nested
    @DisplayName("aislamiento entre usuarios")
    class AislamientoEntreUsuarios {

        /**
         * Precondicion estructural de la suplantacion: ni el alta ni el login
         * aceptan un campo que el cliente pueda usar para elegir de quien es el
         * dato (id, activated, authorities, externalPatientId). El aislamiento
         * de verdad —que el usuario A no vea ni toque nada del B— recien se
         * puede probar con datos propios de un usuario, y eso llega en #39
         * cuando existan reservas.
         */
        @Test
        @DisplayName("ni RegistroRequest ni AutenticacionRequest exponen un campo para elegir la identidad")
        void losRequestsNoExponenUnCampoDeIdentidad() {
            List<String> camposProhibidos = List.of("id", "activated", "authorities", "externalPatientId", "userId");

            for (var tipo : List.of(
                    ar.edu.um.turnos.auth.infrastructure.web.dto.RegistroRequest.class,
                    ar.edu.um.turnos.auth.infrastructure.web.dto.AutenticacionRequest.class)) {
                List<String> campos = java.util.Arrays.stream(tipo.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList();

                assertThat(campos).as("campos de %s", tipo.getSimpleName())
                        .doesNotContainAnyElementsOf(camposProhibidos);
            }
        }
    }

}
