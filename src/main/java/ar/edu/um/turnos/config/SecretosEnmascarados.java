package ar.edu.um.turnos.config;

/**
 * Utilidad para que un secreto no termine impreso en un log.
 *
 * <p>Los {@code record} generan un {@code toString()} que incluye todos sus
 * componentes. Para configuracion que contiene credenciales eso es una fuga
 * esperando a pasar: basta un {@code log.debug("config={}", props)}, el volcado
 * de un {@code BindException} o el reporte de una prueba fallida para que el
 * JWT tecnico quede escrito en texto plano. Las clases de propiedades de este
 * paquete sobreescriben {@code toString()} y pasan sus secretos por aca.
 *
 * <p>Se conserva el prefijo para que el valor siga siendo <em>diagnosticable</em>:
 * alcanza para distinguir "cargo la credencial equivocada" de "no cargo
 * ninguna", que es la pregunta que uno le hace a un log, sin revelar el
 * secreto. Los valores cortos se tapan por completo, porque ahi el prefijo ya
 * seria una parte sustancial del secreto.
 */
final class SecretosEnmascarados {

    /** Cuantos caracteres del principio se dejan ver. */
    private static final int PREFIJO_VISIBLE = 4;

    /** Por debajo de este largo no se muestra nada del valor. */
    private static final int LARGO_MINIMO_PARA_PREFIJO = 12;

    private SecretosEnmascarados() {
    }

    /**
     * Devuelve una representacion del secreto apta para un log.
     *
     * <p>Distingue los tres casos que importan al diagnosticar: ausente, vacio
     * y presente. Nunca devuelve el valor completo.
     *
     * @param secreto el valor a enmascarar; puede ser {@code null}
     * @return {@code "(ausente)"}, {@code "(vacio)"}, {@code "***"} o
     *         {@code "abcd*** (N caracteres)"}
     */
    static String enmascarar(String secreto) {
        if (secreto == null) {
            return "(ausente)";
        }
        if (secreto.isEmpty()) {
            return "(vacio)";
        }
        if (secreto.length() < LARGO_MINIMO_PARA_PREFIJO) {
            return "***";
        }
        return secreto.substring(0, PREFIJO_VISIBLE) + "*** (" + secreto.length() + " caracteres)";
    }

}
