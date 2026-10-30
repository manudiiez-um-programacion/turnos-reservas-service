package ar.edu.um.turnos.health.domain.ports.out;

/**
 * Puerto de salida: que necesita el dominio del mundo exterior.
 *
 * <p>El dominio no sabe de donde sale el nombre ni la version del servicio (del
 * {@code pom.xml}, de una variable de entorno o de un descubrimiento remoto):
 * solo declara que los necesita. La implementacion vive en
 * {@code infrastructure} y es reemplazable sin tocar el dominio.
 */
public interface InformacionDelServicioPort {

    String nombre();

    String version();

}
