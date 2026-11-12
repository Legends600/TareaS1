package pe.edu.upeu.Practica.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.Practica.entity.AuditoriaEvento;

import java.time.LocalDateTime;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    /** Página de eventos con filtros opcionales (un filtro nulo no restringe). */
    @Query("""
            select a from AuditoriaEvento a
            where (:usuario is null or upper(a.usuario) = upper(:usuario))
              and (:accion is null or a.accion = :accion)
              and (:desde is null or a.fecha >= :desde)
              and (:hasta is null or a.fecha < :hasta)
            """)
    Page<AuditoriaEvento> buscar(@Param("usuario") String usuario,
                                 @Param("accion") String accion,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta,
                                 Pageable pageable);
}
