package ar.edu.um.turnos.config;

import java.time.Duration;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ajuste operativo del cierre de holds y procesos vencidos (#38): ambos
 * valores tienen default y no son datos que entregue la catedra, asi que no
 * tiene sentido impedir el arranque si faltan.
 *
 * @param margen     tiempo de gracia despues de {@code expiresAt} antes de
 *                    considerar vencido un proceso, para no cerrarlo por una
 *                    diferencia de reloj con la catedra (§18.4 de la
 *                    referencia: el reloj que vale es el de ella).
 * @param frecuencia cada cuanto corre la tarea programada que cierra los
 *                    procesos vencidos.
 */
@Validated
@ConfigurationProperties(prefix = "turnos.vencimiento")
public record VencimientoProperties(

        @NotNull(message = "falta TURNOS_VENCIMIENTO_MARGEN")
        Duration margen,

        @NotNull(message = "falta TURNOS_VENCIMIENTO_FRECUENCIA")
        Duration frecuencia

) {
}
