package ar.edu.um.turnos.config.observabilidad;

/**
 * Las dos integraciones externas de este servicio cuyo estado se observa
 * (#40): la catedra y el catalogo. El consumo de Kafka no entra aca porque no
 * tiene un "estado de la conexion" consultable punto a punto como un cliente
 * REST: se observa por sus propios contadores (duplicados descartados) y por
 * los logs de {@code AccionReservaKafkaListener}.
 */
public enum Integracion {
    CATEDRA,
    CATALOGO
}
