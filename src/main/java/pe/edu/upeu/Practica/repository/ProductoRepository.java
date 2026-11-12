package pe.edu.upeu.Practica.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.Practica.entity.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    String FILTRO_BUSCAR = """
            where (:patron is null or upper(p.nombre) like :patron)
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:disponibles = false or (p.estado = true and p.stock > 0))
            """;

    /** Página de productos con filtros opcionales; el orden llega en el Pageable. */
    @Query(value = "select p from Producto p " + FILTRO_BUSCAR,
            countQuery = "select count(p) from Producto p " + FILTRO_BUSCAR)
    Page<Producto> buscar(@Param("patron") String patron,
                          @Param("categoriaId") Long categoriaId,
                          @Param("disponibles") boolean disponibles,
                          Pageable pageable);

    List<Producto> findByCategoriaId(Long categoriaId);
    boolean existsByCategoriaId(Long categoriaId);

    /** Descuenta stock solo si alcanza; devuelve 0 si otra venta se adelantó. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Devuelve al stock las unidades de una venta anulada. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
