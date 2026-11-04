package ar.edu.um.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre {@link ProblemBodyParser#leer(String, Class)}, extraido de
 * {@code CatedraClient} y {@code CatalogoClient} (#16) para que los dos
 * dejen de duplicar el mismo parseo defensivo de un cuerpo
 * {@code problem+json}.
 */
@DisplayName("ProblemBodyParser")
class ProblemBodyParserTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuerpoDePrueba(String detail, String code) {
    }

    @Test
    @DisplayName("parsea un cuerpo problem+json valido")
    void parseaUnCuerpoValido() {
        CuerpoDePrueba resultado = ProblemBodyParser.leer(
                "{\"status\": 404, \"detail\": \"no existe\", \"code\": \"ALGO_NOT_FOUND\"}", CuerpoDePrueba.class);

        assertThat(resultado).isNotNull();
        assertThat(resultado.detail()).isEqualTo("no existe");
        assertThat(resultado.code()).isEqualTo("ALGO_NOT_FOUND");
    }

    @Test
    @DisplayName("un cuerpo nulo no es un error, devuelve null")
    void cuerpoNuloDevuelveNull() {
        assertThat(ProblemBodyParser.leer(null, CuerpoDePrueba.class)).isNull();
    }

    @Test
    @DisplayName("un cuerpo en blanco no es un error, devuelve null")
    void cuerpoEnBlancoDevuelveNull() {
        assertThat(ProblemBodyParser.leer("   ", CuerpoDePrueba.class)).isNull();
    }

    @Test
    @DisplayName("un cuerpo no parseable no rompe, devuelve null")
    void cuerpoNoParseableDevuelveNull() {
        assertThat(ProblemBodyParser.leer("esto no es json", CuerpoDePrueba.class)).isNull();
    }

}
