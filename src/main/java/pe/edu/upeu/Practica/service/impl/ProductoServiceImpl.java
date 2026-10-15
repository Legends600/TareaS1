package pe.edu.upeu.Practica.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.Practica.dto.PaginaResponseDTO;
import pe.edu.upeu.Practica.dto.ProductoRequestDTO;
import pe.edu.upeu.Practica.dto.ProductoResponseDTO;
import pe.edu.upeu.Practica.entity.Categoria;
import pe.edu.upeu.Practica.entity.Producto;
import pe.edu.upeu.Practica.exception.RecursosNoEncontradoException;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.repository.CategoriaRepository;
import pe.edu.upeu.Practica.repository.ProductoRepository;
import pe.edu.upeu.Practica.service.service.ProductoService;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class ProductoServiceImpl implements ProductoService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductoServiceImpl.class);
    private static final Set<String> CAMPOS_ORDEN = Set.of("id", "nombre", "precio", "stock");
    private static final int TAMANIO_MAXIMO = 100;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public ProductoResponseDTO create(ProductoRequestDTO t) {
        String nombre = t.getNombre().trim();
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException(
                    "Ya existe un producto con el nombre: " + nombre
            );
        }

        Categoria categoria = buscarCategoriaActiva(t.getCategoriaId());

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(BigDecimal.valueOf(t.getPrecio()));
        producto.setStock(t.getStock());
        producto.setEstado(t.getEstado());
        producto.setCategoria(categoria);

        Producto productoCreado = productoRepository.save(producto);
        return convertirResponse(productoCreado);
    }

    @Override
    @Transactional
    public ProductoResponseDTO update(Long aLong, ProductoRequestDTO t) {
        Producto producto = productoRepository.findById(aLong).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Producto no encontrado con id: " + aLong)
        );

        String nombre = t.getNombre().trim();
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, aLong)) {
            throw new ReglaNegocioException(
                    "Ya existe un producto con el nombre: " + nombre
            );
        }

        Categoria categoria = buscarCategoriaActiva(t.getCategoriaId());

        producto.setNombre(nombre);
        producto.setPrecio(BigDecimal.valueOf(t.getPrecio()));
        producto.setStock(t.getStock());
        producto.setEstado(t.getEstado());
        producto.setCategoria(categoria);

        Producto productoActualizado = productoRepository.save(producto);
        return convertirResponse(productoActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO read(Long aLong) {
        Producto producto = productoRepository.findById(aLong).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Producto no encontrado con id: " + aLong)
        );
        return convertirResponse(producto);
    }

    @Override
    @Transactional
    public void delete(Long aLong) {
        Producto producto = productoRepository.findById(aLong).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Producto no encontrado con id: " + aLong)
        );
        // Baja lógica: el producto puede estar referenciado en ventas
        if (!Boolean.TRUE.equals(producto.getEstado())) {
            throw new ReglaNegocioException(
                    "El producto \"" + producto.getNombre() + "\" ya se encuentra inactivo"
            );
        }
        producto.setEstado(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ProductoResponseDTO> listarPaginado(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        if (!CAMPOS_ORDEN.contains(ordenarPor)) {
            throw new ReglaNegocioException(
                    "No se puede ordenar por '" + ordenarPor
                            + "'. Valores permitidos: id, nombre, precio, stock"
            );
        }
        if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(
                    "La página debe ser mayor o igual a 0 y el tamaño estar entre 1 y " + TAMANIO_MAXIMO
            );
        }
        Sort.Direction sentido = Sort.Direction.fromOptionalString(direccion)
                .orElseThrow(() -> new ReglaNegocioException("La dirección debe ser 'asc' o 'desc'"));

        // id como segundo criterio para que el orden entre páginas sea estable
        Sort orden = Sort.by(sentido, ordenarPor).and(Sort.by("id"));
        return PaginaResponseDTO.de(
                productoRepository.findAll(PageRequest.of(pagina, tamanio, orden))
                        .map(this::convertirResponse)
        );
    }

    /** La categoría debe existir (404) y estar activa (409) para asociarle un producto. */
    private Categoria buscarCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Categoria no encontrada con id: " + categoriaId)
        );
        if (!Boolean.TRUE.equals(categoria.getEstado())) {
            throw new ReglaNegocioException(
                    "La categoría \"" + categoria.getNombre() + "\" está inactiva; elija una categoría activa"
            );
        }
        return categoria;
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<ProductoResponseDTO> readAll() {
        return productoRepository.findAll().stream().map(this::convertirResponse).toList();
    }

    private ProductoResponseDTO convertirResponse(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio().doubleValue(),
                producto.getStock(),
                producto.getEstado(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getFechaCreacion(),
                producto.getFechaModificacion()
        );
    }
}
