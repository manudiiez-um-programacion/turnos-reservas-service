package ar.edu.um.turnos.config.catalogo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Cubre el mapeo de {@link CatalogoErrorCode#desde(String)} contra los
 * codigos de la agenda vigente que documenta
 * docs/contratos/catalogo-turnos.md#6 del repo turnos-catalogo-sync (#16).
 */
@DisplayName("CatalogoErrorCode")
class CatalogoErrorCodeTest {

    static Stream<Arguments> codigosDelContrato() {
        return Stream.of(
                Arguments.of("AUTH_TOKEN_MISSING", CatalogoErrorCode.AUTH_TOKEN_MISSING),
                Arguments.of("AUTH_TOKEN_INVALID", CatalogoErrorCode.AUTH_TOKEN_INVALID),
                Arguments.of("AUTH_FORBIDDEN", CatalogoErrorCode.AUTH_FORBIDDEN),
                Arguments.of("PROFESSIONAL_NOT_FOUND", CatalogoErrorCode.PROFESSIONAL_NOT_FOUND),
                Arguments.of("PROFESSIONAL_DISABLED", CatalogoErrorCode.PROFESSIONAL_DISABLED));
    }

    @ParameterizedTest(name = "{0} se mapea a {1}")
    @MethodSource("codigosDelContrato")
    void mapeaCadaCodigoDelContrato(String code, CatalogoErrorCode esperado) {
        assertThat(CatalogoErrorCode.desde(code)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "''{0}'' se mapea a DESCONOCIDO")
    @MethodSource("codigosSinMapeoEspecifico")
    void mapeaAlResguardoCuandoNoHayMapeoEspecifico(String code) {
        assertThat(CatalogoErrorCode.desde(code)).isEqualTo(CatalogoErrorCode.DESCONOCIDO);
    }

    static Stream<String> codigosSinMapeoEspecifico() {
        return Stream.of((String) null, "", "CODE_QUE_NO_EXISTE_TODAVIA", "CATALOG_NOT_SYNCHRONIZED");
    }

}
