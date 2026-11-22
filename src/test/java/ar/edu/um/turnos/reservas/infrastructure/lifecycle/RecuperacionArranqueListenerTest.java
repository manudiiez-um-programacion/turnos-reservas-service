package ar.edu.um.turnos.reservas.infrastructure.lifecycle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.application.service.ReservasService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecuperacionArranqueListener")
class RecuperacionArranqueListenerTest {

    @Mock
    private ReservasService reservasService;

    private RecuperacionArranqueListener listener;

    @BeforeEach
    void setUp() {
        listener = new RecuperacionArranqueListener(reservasService);
    }

    @Test
    @DisplayName("al arrancar, delega la recuperacion en ReservasService")
    void delegaLaRecuperacionEnReservasService() {
        listener.alArrancar();

        verify(reservasService).recuperarProcesosPendientes();
    }

    @Test
    @DisplayName("un error inesperado de la recuperacion no se propaga")
    void unErrorInesperadoNoSePropaga() {
        doThrow(new RuntimeException("fallo inesperado")).when(reservasService).recuperarProcesosPendientes();

        assertThatCode(() -> listener.alArrancar()).doesNotThrowAnyException();
    }

}
