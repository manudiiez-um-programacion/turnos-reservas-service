package ar.edu.um.turnos.disponibilidad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.turnos.config.JwtProperties;

/**
 * Prueba de punta a punta del endpoint de disponibilidad (#25, ultimo
 * criterio de aceptacion: "prueba de extremo a extremo del endpoint con el
 * catalogo y la catedra simulados"). Levanta el contexto completo y pega al
 * controller real a traves de {@code MockMvc}, sin mockear
 * {@code DisponibilidadService} ni los casos de uso: lo unico simulado son
 * los dos servidores HTTP externos, con el mismo patron que
 * {@code CatalogoRestClientIntegrationTest} (#12) pero cubriendo la cadena
 * completa hasta el controller.
 *
 * <p>De paso prueba que una hora con segundos ({@code HH:mm:ss}) y sin
 * segundos ({@code HH:mm}) se tratan igual: la regla semanal del catalogo
 * llega sin segundos y la ocupacion de la catedra que la excluye llega con
 * segundos, y aun asi coinciden en el mismo slot.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Disponibilidad de punta a punta, con el catalogo y la catedra simulados")
class DisponibilidadEndToEndTest {

    private static HttpServer catalogoSimulado;
    private static HttpServer catedraSimulado;

    private static final AtomicInteger STATUS_CATALOGO = new AtomicInteger(200);
    private static final AtomicReference<String> CUERPO_CATALOGO = new AtomicReference<>("");
    private static final AtomicInteger STATUS_CATEDRA = new AtomicInteger(200);
    private static final AtomicReference<String> CUERPO_CATEDRA = new AtomicReference<>("");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeAll
    static void levantarServidoresSimulados() throws IOException {
        catalogoSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catalogoSimulado.createContext("/api/internal/professionals/",
                exchange -> responder(exchange, STATUS_CATALOGO.get(), CUERPO_CATALOGO.get()));
        catalogoSimulado.start();

        catedraSimulado = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        catedraSimulado.createContext("/api/appointment-occupancies",
                exchange -> responder(exchange, STATUS_CATEDRA.get(), CUERPO_CATEDRA.get()));
        catedraSimulado.start();
    }

    @AfterAll
    static void apagarServidoresSimulados() {
        catalogoSimulado.stop(0);
        catedraSimulado.stop(0);
    }

    @DynamicPropertySource
    static void apuntarALosServidoresSimulados(DynamicPropertyRegistry registro) {
        registro.add("catalogo.api.base-url", () -> "http://localhost:" + catalogoSimulado.getAddress().getPort());
        registro.add("catedra.api.base-url", () -> "http://localhost:" + catedraSimulado.getAddress().getPort());
    }

    @BeforeEach
    void reiniciarRespuestasSimuladas() {
        STATUS_CATALOGO.set(200);
        CUERPO_CATALOGO.set("");
        STATUS_CATEDRA.set(200);
        CUERPO_CATEDRA.set("");
    }

    private static void responder(HttpExchange exchange, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    @DisplayName("combina la agenda del catalogo con las ocupaciones de la catedra: HH:mm y HH:mm:ss se tratan igual, "
            + "y una ocupacion fuera de la agenda no rompe la respuesta")
    void combinaAgendaYOcupacionesDePuntaAPunta() throws Exception {
        CUERPO_CATALOGO.set("""
                {
                  "professional": {"id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true},
                  "schedules": [
                    {"id": 1, "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "10:00:00", "slotDurationMinutes": 30}
                  ],
                  "catalogVersion": null
                }
                """);
        CUERPO_CATEDRA.set("""
                {
                  "professionalId": 15,
                  "from": "2026-09-14",
                  "to": "2026-09-14",
                  "occupancies": [
                    {"date": "2026-09-14", "startTime": "09:00:00", "status": "CONFIRMED", "expiresAt": null},
                    {"date": "2026-09-14", "startTime": "11:00", "status": "HELD", "expiresAt": "2026-09-14T23:00:00Z"}
                  ]
                }
                """);

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14")
                        .header("Authorization", "Bearer " + tokenDeUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value(15))
                .andExpect(jsonPath("$.date").value("2026-09-14"))
                .andExpect(jsonPath("$.availableSlots[0].startTime").value("09:30:00"))
                .andExpect(jsonPath("$.availableSlots[0].endTime").value("10:00:00"))
                .andExpect(jsonPath("$.availableSlots[1]").doesNotExist());
    }

    @Test
    @DisplayName("un profesional deshabilitado en el catalogo se traduce, de punta a punta, a un 409 PROFESSIONAL_DISABLED")
    void profesionalDeshabilitadoDePuntaAPunta() throws Exception {
        STATUS_CATALOGO.set(409);
        CUERPO_CATALOGO.set("""
                {"detail": "el profesional esta deshabilitado", "code": "PROFESSIONAL_DISABLED"}
                """);

        mockMvc.perform(get("/api/disponibilidad")
                        .param("professionalId", "15")
                        .param("date", "2026-09-14")
                        .header("Authorization", "Bearer " + tokenDeUsuario()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_DISABLED"));
    }

    /** JWT de usuario valido, firmado con el mismo secreto que valida {@code SecurityConfig} (#11). */
    private String tokenDeUsuario() {
        SecretKey clave = Keys.hmacShaKeyFor(jwtProperties.userSecret().getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject("usuario.e2e")
                .claim("auth", List.of("ROLE_USER"))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(60)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

}
