package ar.edu.um.turnos.config.catedra.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Cubre el mapeo de {@link CatedraErrorCode#desde(String)} contra los
 * codigos de la matriz de §13.1 de la referencia que afectan a este
 * servicio (#15).
 */
@DisplayName("CatedraErrorCode")
class CatedraErrorCodeTest {

    static Stream<Arguments> codigosDeLaMatriz() {
        return Stream.of(
                Arguments.of("VALIDATION_ERROR", CatedraErrorCode.VALIDATION_ERROR),
                Arguments.of("INVALID_DATE_RANGE", CatedraErrorCode.INVALID_DATE_RANGE),
                Arguments.of("PROFESSIONAL_NOT_FOUND", CatedraErrorCode.PROFESSIONAL_NOT_FOUND),
                Arguments.of("PROFESSIONAL_DISABLED", CatedraErrorCode.PROFESSIONAL_DISABLED),
                Arguments.of("INVALID_SLOT", CatedraErrorCode.INVALID_SLOT),
                Arguments.of("SLOT_ALREADY_RESERVED", CatedraErrorCode.SLOT_ALREADY_RESERVED),
                Arguments.of("SLOT_ALREADY_HELD", CatedraErrorCode.SLOT_ALREADY_HELD),
                Arguments.of("HOLD_NOT_FOUND", CatedraErrorCode.HOLD_NOT_FOUND),
                Arguments.of("HOLD_EXPIRED", CatedraErrorCode.HOLD_EXPIRED),
                Arguments.of("HOLD_OWNER_MISMATCH", CatedraErrorCode.HOLD_OWNER_MISMATCH),
                Arguments.of("EXTERNAL_PATIENT_MISMATCH", CatedraErrorCode.EXTERNAL_PATIENT_MISMATCH),
                Arguments.of("RESERVATION_PROCESS_ALREADY_CONFIRMED", CatedraErrorCode.RESERVATION_PROCESS_ALREADY_CONFIRMED),
                Arguments.of("RESERVATION_NOT_FOUND", CatedraErrorCode.RESERVATION_NOT_FOUND),
                Arguments.of("RESERVATION_OWNER_MISMATCH", CatedraErrorCode.RESERVATION_OWNER_MISMATCH),
                Arguments.of("RESERVATION_NOT_CANCELLABLE", CatedraErrorCode.RESERVATION_NOT_CANCELLABLE),
                Arguments.of("STUDENT_INTEGRATION_NOT_FOUND", CatedraErrorCode.STUDENT_INTEGRATION_NOT_FOUND));
    }

    @ParameterizedTest(name = "{0} se mapea a {1}")
    @MethodSource("codigosDeLaMatriz")
    void mapeaCadaCodigoDeLaMatriz(String code, CatedraErrorCode esperado) {
        assertThat(CatedraErrorCode.desde(code)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "''{0}'' se mapea a DESCONOCIDO")
    @MethodSource("codigosSinMapeoEspecifico")
    void mapeaAlResguardoCuandoNoHayMapeoEspecifico(String code) {
        assertThat(CatedraErrorCode.desde(code)).isEqualTo(CatedraErrorCode.DESCONOCIDO);
    }

    static Stream<String> codigosSinMapeoEspecifico() {
        return Stream.of((String) null, "", "CODE_QUE_NO_EXISTE_TODAVIA", "USERNAME_ALREADY_EXISTS");
    }

}
