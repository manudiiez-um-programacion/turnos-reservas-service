package ar.edu.um.turnos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad <strong>provisional</strong>.
 *
 * <p>Agregar {@code spring-boot-starter-security} bloquea por defecto todos los
 * endpoints, incluido Swagger UI. Este milestone solo tiene que levantar el
 * esqueleto del servicio, asi que se abre todo de forma explicita para que el
 * criterio "Swagger UI disponible en local" se pueda verificar.
 *
 * <p><strong>Esta clase se reemplaza en el issue #11</strong> (proteger los
 * endpoints y configurar CORS), donde solo registro y login quedan publicos y
 * todo lo demas exige un JWT valido. Hasta entonces el servicio no expone
 * ningun dato: el unico endpoint es el estado del propio servicio.
 *
 * <p>Se deja sin sesiones (politica {@code STATELESS}) y sin CSRF porque la API
 * es stateless y se autentica por token, no por cookie: esa parte ya es
 * definitiva y no cambia en #11.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

}
