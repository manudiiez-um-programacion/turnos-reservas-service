package ar.edu.um.turnos.config;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deserializa el cuerpo de un error HTTP no-2xx a un tipo propio chico, sin
 * asumir que siempre venga: un 401/403 que produce directamente Spring
 * Security (tanto llamando a la catedra como al catalogo) puede no traer
 * cuerpo, y uno vacio o no parseable no es un error de quien llama, sencillamente
 * no hay nada que informar.
 *
 * <p>Comun a {@code CatedraClient} (#14) y {@code CatalogoClient} (#16):
 * antes de esta clase, los dos repetian exactamente el mismo chequeo con su
 * propia instancia de {@code ObjectMapper}, lo que SonarCloud marco como
 * codigo duplicado.
 *
 * <p>Usa una instancia propia de Jackson 2 y no el {@code ObjectMapper}
 * Jackson 3 que trae el contexto de Spring Boot 4: solo hace falta parsear
 * un cuerpo {@code problem+json} chico, lo mismo que ya hace
 * {@code ProblemDetailResponses} con el Jackson 2 que trae {@code jjwt-jackson}.
 */
public final class ProblemBodyParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ProblemBodyParser() {
    }

    public static <T> T leer(String cuerpoCrudo, Class<T> tipo) {
        if (cuerpoCrudo == null || cuerpoCrudo.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(cuerpoCrudo, tipo);
        } catch (Exception noParseable) {
            return null;
        }
    }

}
