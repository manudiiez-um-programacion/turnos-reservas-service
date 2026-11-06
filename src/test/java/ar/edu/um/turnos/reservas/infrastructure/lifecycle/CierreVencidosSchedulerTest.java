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
@DisplayName("CierreVencidosScheduler")
class CierreVencidosSchedulerTest {

    @Mock
    private ReservasService reservasService;

    private CierreVencidosScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CierreVencidosScheduler(reservasService);
    }

    @Test
    @DisplayName("al correr, delega el cierre de vencidos en ReservasService")
    void delegaElCierreEnReservasService() {
        scheduler.cerrarVencidos();

        verify(reservasService).cerrarProcesosVencidos();
    }

    @Test
    @DisplayName("un error inesperado del cierre no se propaga")
    void unErrorInesperadoNoSePropaga() {
        doThrow(new RuntimeException("fallo inesperado")).when(reservasService).cerrarProcesosVencidos();

        assertThatCode(() -> scheduler.cerrarVencidos()).doesNotThrowAnyException();
    }

}
