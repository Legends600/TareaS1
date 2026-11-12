package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.dto.PaginaResponseDTO;
import pe.edu.upeu.Practica.dto.ProductoRequestDTO;
import pe.edu.upeu.Practica.dto.ProductoResponseDTO;
import pe.edu.upeu.Practica.service.generic.CrudService;

public interface ProductoService extends CrudService<ProductoRequestDTO, ProductoResponseDTO, Long> {
    /**
     * Página de productos. Los filtros son opcionales: nombre (contiene, sin
     * distinguir mayúsculas), categoría y solo disponibles (activos con stock).
     */
    PaginaResponseDTO<ProductoResponseDTO> listarPaginado(
            int pagina, int tamanio, String ordenarPor, String direccion,
            String nombre, Long categoriaId, boolean disponibles);
}
