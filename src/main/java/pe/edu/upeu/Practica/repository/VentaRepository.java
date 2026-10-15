package pe.edu.upeu.Practica.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.Practica.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.Practica.dto.reporte.VentaPorCategoriaDTO;
import pe.edu.upeu.Practica.entity.Venta;
import pe.edu.upeu.Practica.enums.EstadoVenta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    String FILTRO_BUSCAR = """
            WHERE (:clienteId IS NULL OR v.cliente.id = :clienteId)
            AND (:estado IS NULL OR v.estado = :estado)
            AND (:desde IS NULL OR v.fecha >= :desde)
            AND (:hasta IS NULL OR v.fecha <= :hasta)
            """;

    /** Página de ventas con filtros opcionales; los detalles se cargan al convertir cada venta. */
    @Query(value = "SELECT v FROM Venta v " + FILTRO_BUSCAR,
            countQuery = "SELECT COUNT(v) FROM Venta v " + FILTRO_BUSCAR)
    Page<Venta> buscar(
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
    @Query("""
            select new pe.edu.upeu.Practica.dto.reporte.VentaPorCategoriaDTO(
                       cat.id,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.Practica.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by cat.id, cat.nombre
            order by sum(d.subtotal) desc
            """)
    List<VentaPorCategoriaDTO> reporteVentasPorCategoria(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            select new pe.edu.upeu.Practica.dto.reporte.ProductoMasVendidoDTO(
                       p.id,
                       p.nombre,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.Practica.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by p.id, p.nombre, cat.nombre
            order by sum(d.cantidad) desc
            """)
    List<ProductoMasVendidoDTO> reporteProductosMasVendidos(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
