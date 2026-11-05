package pe.edu.upeu.Practica.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.Practica.dto.CategoriaRequestDTO;
import pe.edu.upeu.Practica.dto.CategoriaResponseDTO;
import pe.edu.upeu.Practica.entity.Categoria;
import pe.edu.upeu.Practica.exception.RecursosNoEncontradoException;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.repository.CategoriaRepository;
import pe.edu.upeu.Practica.repository.ProductoRepository;
import pe.edu.upeu.Practica.service.service.CategoriaService;

import java.util.List;


@Service
public class CategoriaServiceImpl implements CategoriaService {
    private static final Logger LOG = LoggerFactory.getLogger(CategoriaServiceImpl.class);
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categorias", allEntries = true)
    public CategoriaResponseDTO create(CategoriaRequestDTO t) {
        String nombre = t.getNombre().trim();
        if(categoriaRepository.existsByNombreIgnoreCase(nombre)){
            throw new ReglaNegocioException(
                    "Ya existe una categoria con el nombre: " + nombre
            );
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catCreada = categoriaRepository.save(categoria);
        return convertirResponse(catCreada);
    }

    @Override
    @Transactional
    // El nombre de la categoria tambien se muestra en los productos: se limpian las dos
    @CacheEvict(cacheNames = { "categorias", "productos" }, allEntries = true)
    public CategoriaResponseDTO update(Long aLong, CategoriaRequestDTO t) {
        Categoria categoria = categoriaRepository.findById(aLong).orElseThrow(()->
        new RecursosNoEncontradoException(
                "Categoria no encontrada con id: " + aLong)
        );
        String nombre = t.getNombre().trim();
        if(categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, aLong)){
            throw new ReglaNegocioException(
                    "Ya existe una categoria con el nombre: " + nombre
            );
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(t.getDescripcion());
        categoria.setEstado(t.getEstado());

        Categoria catActualizada = categoriaRepository.save(categoria);
        return convertirResponse(catActualizada);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categorias", key = "'id-' + #aLong")
    public CategoriaResponseDTO read(Long aLong) {
        Categoria categoria = categoriaRepository.findById(aLong).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Categoria no encontrada con id: " + aLong)
        );
        return convertirResponse(categoria);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categorias", allEntries = true)
    public void delete(Long aLong) {
        Categoria categoria = categoriaRepository.findById(aLong).orElseThrow(() ->
                new RecursosNoEncontradoException(
                        "Categoria no encontrada con id: " + aLong)
        );
        if(productoRepository.existsByCategoriaId(aLong)){
            throw new ReglaNegocioException(
                    "No se puede eliminar la categoria \"" + categoria.getNombre() + "\" porque tiene productos asociados"
            );
        }
        categoriaRepository.delete(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categorias", key = "'todas'")
    public Iterable<CategoriaResponseDTO> readAll() {
        long inicio = System.currentTimeMillis();
        LOG.info("Inicio listar categorias");
        List<CategoriaResponseDTO> categorias =
                categoriaRepository.findAll().stream().map(this::convertirResponse).toList();
        LOG.info("Fin listar categorias | filas={} | duracionMs={}",
                categorias.size(), System.currentTimeMillis() - inicio);
        return categorias;
    }

    private CategoriaResponseDTO convertirResponse(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getFechaCreacion(),
                categoria.getFechaModificacion()
        );
    }
}
