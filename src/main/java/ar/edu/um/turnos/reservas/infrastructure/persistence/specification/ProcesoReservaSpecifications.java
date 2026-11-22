package ar.edu.um.turnos.reservas.infrastructure.persistence.specification;

import org.springframework.data.jpa.domain.Specification;

import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.infrastructure.persistence.entity.ProcesoReservaJpaEntity;

/**
 * Predicados combinables de {@link ProcesoReservaJpaEntity} para
 * {@code buscarPorUsuario} (#33). {@code usuarioId} siempre se aplica: es la
 * condicion que impide que un usuario vea procesos de otro (§4.2). El resto
 * de los campos del filtro son opcionales, asi que cada uno agrega su
 * predicado solo si viene informado.
 */
public final class ProcesoReservaSpecifications {

    private ProcesoReservaSpecifications() {
    }

    public static Specification<ProcesoReservaJpaEntity> deFiltro(FiltroReservasUsuario filtro) {
        Specification<ProcesoReservaJpaEntity> especificacion =
                (root, query, cb) -> cb.equal(root.get("usuarioId"), filtro.usuarioId());

        if (filtro.estado() != null) {
            String estado = filtro.estado().name();
            especificacion = especificacion.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (filtro.desde() != null) {
            especificacion = especificacion.and(
                    (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), filtro.desde()));
        }
        if (filtro.hasta() != null) {
            especificacion = especificacion.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), filtro.hasta()));
        }
        return especificacion;
    }

}
